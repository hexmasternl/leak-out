# Design

## Context

- Play Games lives in `app/.../achievements/playgames`: `PlayGamesAuth` exposes `state: StateFlow<SignInState>`, and `GmsSignInGateway` wraps `GamesSignInClient` per activity. The gateway has no server-side access call yet. `PlayGamesServices` is created lazily in `AppContainer` and bound to `MainActivity` (`attach`/`detach`).
- The app has no network code and no `INTERNET` permission. DataStore (Preferences), kotlinx.serialization and coroutines are already dependencies; settings and music rotation already use Preferences DataStore files.
- `GameScreen` takes its collaborators as nullable parameters (`history`, `achievements`, `haptics`, ...) and the designer's `TestPlayScreen` passes `null` for the ones a test play must not touch. `GameSession.showWinDialog` tells when the level complete dialog is up.
- The score server (`leak-out-server`, see its README and `docs/auth-setup.md`):
  - `POST /api/players/sessions` `{ "serverAuthCode": "..." }` → `200 { accessToken, expiresAt, playerId, displayName }`, `401` for a rejected code, `400` for a missing one, `503` when Google is down.
  - `POST /api/scores` with `Authorization: Bearer <token>` and `{ "level": <int ≥ 1>, "score": <int ≥ 0> }` → `200 { level, bestScore, isNewBest }`, `401` for a missing/expired token, `400` for invalid values. Only a higher score replaces the stored best, so sending a score twice is harmless.
  - The game must request the server auth code with the server's **web** OAuth client id (`requestServerSideAccess(webClientId, false)`).

## Goals / Non-Goals

**Goals:**
- No score won while signed in is lost to a missing connection.
- Keep the server and HTTP code behind small interfaces so the queue and sending rules are JVM-unit-testable with fakes.
- No new dependencies.

**Non-Goals:**
- Showing leaderboards in the app.
- Sending while the app is not running (WorkManager): a server auth code needs a Play Games client, and that needs an activity.
- Back-filling local history results won before this feature or while signed out.
- Tying queued scores to a Play Games account: they are sent for whoever is signed in when the queue is flushed (see Risks).
- Persisting the access token across process restarts.

## Decisions

### 1. Package and pieces (`app/.../scores/online`)
- `ScoreServerConfig(baseUrl, webClientId)` with `enabled` when both are non-blank and the URL starts with `https://`; built from `BuildConfig`.
- `ScoreServerApi` (interface): `suspend fun createSession(serverAuthCode): Session` and `suspend fun submitScore(token, level, score)`. Throws `ScoreServerException.Unauthorized` for 401, `ScoreServerException.Rejected` for other 4xx, and `ScoreServerException.Unavailable` for IO errors, timeouts and 5xx. `HttpScoreServerApi` implements it with `HttpURLConnection` on `Dispatchers.IO`, 10 s connect/read timeouts and an overall 15 s `withTimeout` per request, JSON via kotlinx.serialization (`ignoreUnknownKeys = true`).
- `ServerAuthCodeSource` (interface): `suspend fun serverAuthCode(webClientId): String`. The GMS implementation calls `PlayGames.getGamesSignInClient(activity).requestServerSideAccess(webClientId, false)`. It is bound in `PlayGamesServices.attach/detach`, like the achievement sync, because every Play Games client needs an activity.
- `ScoreServerSession`: holds the token in memory; `token()` returns the cached one if it expires more than 60 s from now, otherwise gets a code and signs in; `invalidate()` drops it.
- `PendingScoreQueue` (interface) with `DataStorePendingScoreQueue`: `suspend fun offer(level, score)` (keeps the max, inside one `edit` so it is atomic), `suspend fun snapshot(): Map<Int, Int>`, `suspend fun remove(level, score)` (removes only if the stored value is still `score`, so a higher score queued during a send survives), `val pending: Flow<Map<Int, Int>>`. Stored in its own Preferences DataStore file `score_queue`, key `level_<id>` → int.
- `ScoreSubmitter`: the entry point.
  - `fun submitWin(level, score): StateFlow<ScorePostStatus>?` (the `ScoreSubmissions` interface the game screen depends on) returns `null` (no dot) when not configured or not `SignedIn`; otherwise offers the score to the queue, triggers a flush, and returns a status flow for that level: `InProgress` until the flush ends, then `Succeeded` if the level's score was accepted, `Failed` if it is still queued or was rejected. Once per attempt is kept by the `GameSession` (decision 2), the same way it records a win in the history once.
  - `fun flush()`: when configured, `SignedIn` and a code source is bound, launches one flush on the **app** scope (a `Mutex`/single-flight job: a flush requested while one runs causes one more run after it). A flush takes a snapshot and sends each entry in level order; accepted or `Rejected` → `remove(level, score)`; `Unauthorized` → invalidate the session and retry that entry once; `Unavailable` (or a failed sign-in) → stop the flush, leaving the rest queued.
  - Flush triggers: `submitWin`; `PlayGamesServices.attach` (app start / activity created); the sign-in state becoming `SignedIn` (collected on the app scope); and a `NetworkMonitor` reporting a network became available.
- `NetworkMonitor` (interface) with an implementation over `ConnectivityManager.registerDefaultNetworkCallback`, emitting when the default network becomes **validated** (only on the step to validated, not on every capability change, so signal changes do not cause flushes). Needs `ACCESS_NETWORK_STATE`.
- Every storage or network-watch failure inside the submitter is caught and logged, so nothing can take the app scope down.

Alternatives: Retrofit/Ktor/OkHttp were rejected: two JSON POSTs do not justify a new dependency in a pinned catalog. A Room table for the queue was rejected: it needs a schema migration for what is a small key→value map; DataStore gives atomic read-modify-write with no migration. WorkManager was rejected (see Non-Goals), and it is a new dependency.

### 2. Where the win is queued
`GameScreen` gets a `scores: ScoreSubmitter? = null` parameter; `MainActivity` passes the container's submitter for library levels and `TestPlayScreen` passes `null`. A `LaunchedEffect(attempt, showWinDialog)` calls `session.submitScore(scores)` once `showWinDialog` is true. `GameSession.submitScore` hands `state.level.id` and `finalScore.total` to `submitWin` at most once per attempt and keeps the returned flow as `scorePost` for the dot; a restart is a new attempt. Keeping it in the session (rather than a key in the submitter) means a new session for the same level, after leaving and coming back, is posted again, while coming back from the background is not.

### 3. Status dot (`ScorePostIndicator`)
- The level complete dialog is its own window, so anything drawn by `GameScreen` would sit under its scrim. The dot is therefore drawn inside the dialog window: `ResultDialogWindow` gets a `cornerOverlay` slot, drawn after the scrim and panel, aligned `TopStart` with the safe-drawing insets plus 12 dp; `LevelCompleteDialog` takes `scorePost` and puts `ScorePostIndicator` there. Size 10 dp, outside the panel so it never covers its content. No semantics (`clearAndSetSemantics {}`) and no pointer input. Background flushes have no status flow, so they never show a dot.
- Colours from the design system tokens: warning/brass yellow for in progress, the success colour for success, the leak colour for failure (exact tokens picked from `docs/design-system.md` when implementing).
- Timing on the wall clock, not the game clock: the network does not pause with the game. In progress: alpha toggles 1 ↔ 0.2 every 400 ms. Success: one blink (on 300 ms, off 300 ms), then gone. Failure: three blinks of 200 ms on / 200 ms off, then gone. A terminal state already reached when the indicator first composes (for example after returning from the background) plays nothing.
- The blink sequences are a pure function `ScorePostDot.frame(status, elapsedMs) -> DotFrame(color, alpha, done)`, JVM-testable without Compose. The endless yellow blink waits on `withInfiniteAnimationFrameMillis`, so it does not keep Compose (and its tests) busy.

### 4. Build configuration
`app/build.gradle.kts` adds `buildConfigField("String", "SCORE_SERVER_URL", ...)` and `SCORE_SERVER_CLIENT_ID`, read from the Gradle properties `leakout.scoreServerUrl` / `leakout.scoreServerClientId`, then the environment variables `LEAKOUT_SCORE_SERVER_URL` / `LEAKOUT_SCORE_SERVER_CLIENT_ID`, then the defaults `https://rg-leakout-prod-neu-api.jollywater-e41c70f8.northeurope.azurecontainerapps.io` and `201820010117-uteqrtu77v48c9ovd8d9k9qan0sktcj4.apps.googleusercontent.com`. Both values are public (a URL and an OAuth client id are not secrets), so the defaults live in the build file and CI needs nothing until the URL changes. The URL default is the live Container App's own host; moving to a custom domain later is a one-line edit or a CI variable. A URL that is not `https://` counts as not configured, so cleartext traffic stays off.

### 5. Permissions and privacy
Add `INTERNET` and `ACCESS_NETWORK_STATE` (normal permissions, no prompt). Update `settings_about_privacy`, and note the Data safety form changes in `docs/deployment.md`.

## Risks / Trade-offs

- [The server moves away from the Container App host `https://rg-leakout-prod-neu-api.jollywater-e41c70f8.northeurope.azurecontainerapps.io` (custom domain, new region)] → old builds' sends fail, their scores pile up in the queue (at most one per level, so bounded by the level count) and the dot blinks red; they are sent once the player updates. Mitigation: keep the old host answering (or redirecting) for a while after a move.
- [The web client id differs from the server's `GooglePlayGames:ClientId`] → every sign-in fails, scores stay queued. Mitigation: documented in `docs/deployment.md`; failures logged through `PlayGamesLog`.
- [A different Play Games account signs in on the device] → its flush sends scores queued under the previous account. Accepted: rare on a phone, and the server only keeps bests.
- [Scores can be forged by a modified client] → accepted for now; the server keeps only bests per player and needs a Play Games identity.
- [Requesting a server auth code may show consent UI] → `forceRefreshToken = false` requests no extra scopes; a failure leaves the score queued, never a crash.
- [Flush outlives the activity] → it runs on the app scope; if the code source is unbound mid-flush the sign-in fails as `Unavailable` and the rest stays queued for the next trigger.
- [Offline at start, silent sign-in fails] → nothing is queued for wins while signed out (by design: only signed-in players post).

## Migration Plan

1. Update the Play Console Data safety form, then ship with the defaults: score posting is on and goes to the live host.
2. When the server gets another address, change the default (or set `LEAKOUT_SCORE_SERVER_URL` in CI) and release; scores still queued are sent on the first flush.
3. Rollback: set the URL to blank and release; the app stops queuing and sending, and an existing queue is kept untouched.

