# Tasks

## 1. Build configuration and permissions

- [ ] 1.1 In `app/build.gradle.kts`, add `buildConfigField`s `SCORE_SERVER_URL` and `SCORE_SERVER_CLIENT_ID`, read from the Gradle properties `leakout.scoreServerUrl` / `leakout.scoreServerClientId`, then the env vars `LEAKOUT_SCORE_SERVER_URL` / `LEAKOUT_SCORE_SERVER_CLIENT_ID`, then the defaults `https://api.leak-out.com` and `201820010117-uteqrtu77v48c9ovd8d9k9qan0sktcj4.apps.googleusercontent.com` (comment that the URL is a placeholder); verify `./gradlew :app:assembleDebug` succeeds and the generated `BuildConfig` holds the defaults, and that `-Pleakout.scoreServerUrl=` yields a blank URL
- [ ] 1.2 Add `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` to `AndroidManifest.xml` with comments; verify the merged manifest of `:app:processDebugMainManifest` contains both
- [ ] 1.3 Add `ScoreServerConfig(baseUrl, webClientId)` in `app/.../scores/online` with `enabled` true only when both are non-blank and the URL starts with `https://`, plus a factory from `BuildConfig`; verify with a JVM unit test covering blank URL, blank client id, `http://` URL and the default pair

## 2. Score server client

- [ ] 2.1 Add the `ScoreServerApi` interface, `Session(accessToken, expiresAt)`, `ScoreServerException` (`Unauthorized`, `Rejected`, `Unavailable`) and the kotlinx.serialization DTOs for `POST /api/players/sessions` and `POST /api/scores` (`ignoreUnknownKeys`); verify a JVM test that decodes the server's `SessionDto` / `SubmitScoreResult` JSON and encodes `{"serverAuthCode":...}` and `{"level":4,"score":520}`
- [ ] 2.2 Implement `HttpScoreServerApi` with `HttpURLConnection` on `Dispatchers.IO` (10 s connect/read timeouts, 15 s overall, JSON content type, bearer header; 401 → `Unauthorized`, other 4xx → `Rejected`, 5xx / IO / timeout → `Unavailable`); verify JVM tests against a local `com.sun.net.httpserver` server for 200, 401, 400, 503, a refused connection and a timeout

## 3. Server auth code and session

- [ ] 3.1 Add `ServerAuthCodeSource` and its GMS implementation calling `GamesSignInClient.requestServerSideAccess(webClientId, false)`; bind/unbind it in `PlayGamesServices.attach/detach` next to the achievement sync, only when Play Games is configured; verify existing `PlayGamesServices` tests still pass and a new test shows the source is bound on attach and cleared on detach of the same activity
- [ ] 3.2 Implement `ScoreServerSession`: cached token reused until 60 s before `expiresAt`, sign-in with a fresh code otherwise, a `Mutex` against concurrent sign-ins, `invalidate()`; verify JVM tests (injected clock) for first sign-in, reuse within validity, refresh near expiry, two concurrent `token()` calls signing in once, and a failed auth code surfacing as `Unavailable`

## 4. Pending score queue

- [ ] 4.1 Add `PendingScoreQueue` and `DataStorePendingScoreQueue` (own Preferences DataStore file `score_queue`, key `level_<id>`): `offer` keeps the highest score per level atomically, `snapshot`, `pending` flow, and `remove(level, score)` that only removes when the stored value still equals `score`; verify JVM tests (DataStore on a temp file) for add, higher replaces, equal/lower ignored, several levels, conditional remove keeping a higher score queued meanwhile, and values surviving a new DataStore instance on the same file
- [ ] 4.2 Add `NetworkMonitor` and its `ConnectivityManager.registerDefaultNetworkCallback` implementation emitting when a network with internet capability becomes available; verify a JVM test of the callback-to-flow adapter with a fake registrar (emit on available, unregister on cancel)

## 5. Score submitter

- [ ] 5.1 Implement `ScoreSubmitter.flush()`: only when configured, `SignedIn` and a code source is bound; single-flight on the app scope (a request during a run causes one more run); sends the snapshot in level order; accepted or `Rejected` → conditional remove; `Unauthorized` → invalidate and retry that entry once; `Unavailable` → stop and keep the rest; logs through `PlayGamesLog`; verify JVM tests with fakes for: not configured / signed out / no source → no request and queue unchanged; all accepted → queue empty; offline → queue unchanged; 400 on one entry → removed and others still sent; 401 then success → one re-sign-in; 401 twice → entry kept; higher score queued during a send stays; concurrent flush requests → no duplicate sends
- [ ] 5.2 Implement `ScorePostStatus` (`InProgress`, `Succeeded`, `Failed`) and `ScoreSubmitter.submitWin(key, level, score)`: `null` when posting is not possible; otherwise offers to the queue, flushes and returns a status flow for that level (`Succeeded` when its entry was accepted, `Failed` when still queued or rejected); a repeated key returns the existing flow and queues nothing; caller cancellation does not cancel the flush; verify JVM tests for each outcome, the repeated key, a new attempt key being queued again, and a signed-out win queuing nothing
- [ ] 5.3 Wire the flush triggers: collect `playGames.auth.state` becoming `SignedIn` and `NetworkMonitor` availability on the app scope, and call `flush()` from `PlayGamesServices.attach`; wire everything in `AppContainer` (lazy, `ScoreServerConfig.fromBuildConfig()`); verify JVM tests that a sign-in transition and a network-available event each cause one flush, and `./gradlew :app:testDebugUnitTest` passes

## 6. Queuing from the game and the status dot

- [ ] 6.1 Add the pure `dotFrame(status, elapsedMs)` blink function (yellow blinking 400 ms while in progress; one green blink of 300 ms on / 300 ms off; three red blinks of 200 ms on / 200 ms off; `done` afterwards) using design-system colour tokens; verify JVM tests for each phase boundary and that terminal states finish with `done`
- [ ] 6.2 Add the `ScorePostIndicator` composable: 10 dp dot at the top-start safe-drawing inset + 12 dp, wall-clock animation via `dotFrame`, no semantics and no pointer input, plays nothing for a status already terminal when first composed; verify a Compose instrumented test (`ScorePostIndicatorTest`) with a controlled clock: yellow while in progress, gone after a green blink, gone after three red blinks, and no semantics node
- [ ] 6.3 Add `scores: ScoreSubmitter? = null` to `GameScreen`; when `showWinDialog` becomes true for a won attempt, call `submitWin((level.id, attempt), level.id, finalScore.total)` and show `ScorePostIndicator` above the level complete dialog's scrim while the dialog is shown; pass the container's submitter from `MainActivity` and `null` from `TestPlayScreen`; verify a Compose instrumented test (`GameScreenScorePostTest`) with a fake submitter: no submit before the dialog shows, one submit with the total once it shows, dot visible, buttons enabled while the send is still in progress, dot gone after Next Level, no submit when `scores` is null
- [ ] 6.4 Run only the affected instrumented tests (`ScorePostIndicatorTest`, `GameScreenScorePostTest`, existing level complete dialog tests) with `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...`, and verify they pass

## 7. Privacy line and documentation

- [ ] 7.1 Update `settings_about_privacy` in every `res/values*/strings.xml` to say that, only when signed in to Google Play Games, achievements are shared with Google Play and scores of won levels are sent to the Leak Out score server; verify the settings screen About test asserts the new text and passes
- [ ] 7.2 Document score posting in `docs/deployment.md`: the two settings with their defaults and override properties/env vars, that the URL is a placeholder, that the client id must equal the server's `GooglePlayGames:ClientId`, the https requirement, the offline queue, and the Data safety form changes; verify the doc's commands and links resolve
- [ ] 7.3 Note score posting and the offline queue in `docs/project-plan.md` (level complete flow) and remove the "no network" note from `docs/missing-features.md`; verify by reading the updated sections

## 8. Integration check

- [ ] 8.1 Run `./gradlew :core:test :app:testDebugUnitTest` and `openspec validate app-game-post-scores-on-win --strict`; verify both pass
