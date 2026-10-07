# Proposal

## Why

The Leak Out score server (`leak-out-server`) is live: it signs players in with a Google Play Games server auth code and keeps every player's best score per level for the leaderboards. The game does not talk to it yet, so no score ever reaches it and the leaderboards stay empty. Posting the score of every won level, for players who are signed in to Play Games, fills them. The game is often played without a connection, so scores won offline must not be lost: they wait in a queue and are sent once the device is online again. A small status dot gives the player a quiet sign that the score was (or was not yet) sent, without getting in the way of the level complete dialog.

## What Changes

- When a level from the level library is won and the player is signed in to Google Play Games, the attempt's final total is put in a **pending score queue** and the queue is sent to the score server (`POST /api/scores` with the level id and the score) while the level complete dialog is shown. Nothing is queued or sent when the player is signed out, when Play Games is unavailable, when the score server is not configured in the build, or for a test play from the level designer.
- The queue is stored on the device, survives app restarts and holds **at most one score per level: the highest**. A lower score for a level that is already queued is ignored; a higher one replaces it.
- A score leaves the queue once the server accepted it, or when the server rejects it as invalid. When sending fails (offline, timeout, server error) it stays queued. The queue is sent again when the device gets a network connection, when the player becomes signed in, when the app starts and with every new win.
- The app signs in to the score server with a one-time Play Games server auth code (`requestServerSideAccess` with the server's web client id → `POST /api/players/sessions`), keeps the returned access token in memory until shortly before it expires, and signs in again once when the server answers 401.
- A status dot in the top-left corner of the screen, above the dialog's scrim, shows the sending of the won level's score: it blinks **yellow** while it is in progress, blinks **green once** when the score reached the server and blinks **red three times** when it did not (the score then stays queued), then disappears. Without a queued score there is no dot.
- The app gets the `INTERNET` and `ACCESS_NETWORK_STATE` permissions. The build gets two settings, the score server base URL and the web OAuth client id, with defaults `https://api.leak-out.com` (a placeholder until the real endpoint is known) and `201820010117-uteqrtu77v48c9ovd8d9k9qan0sktcj4.apps.googleusercontent.com`; either can be overridden per build, and a blank one turns score posting off.
- The privacy line in Settings → About also says that scores are sent to the Leak Out score server when the player is signed in to Google Play Games.

## Capabilities

### New Capabilities
- `online-score-submission`: sending the scores of won levels to the Leak Out score server for players signed in to Play Games: when a score is queued and sent, the persistent best-per-level queue and when it is flushed, how the app authenticates with the server, failure handling, the build configuration that turns it on, and the status dot that shows the sending's progress and outcome.

### Modified Capabilities
- `settings-screen`: the About section's privacy line also mentions that scores are sent to the Leak Out score server when signed in to Play Games.

## Impact

- **App code** (`app/`): a new `scores/online` package (score server client over `HttpURLConnection` + kotlinx.serialization, session/token holder, a DataStore-backed pending score queue, a connectivity watcher, the submitter that flushes the queue); a server-auth-code call on the Play Games gateway (`GamesSignInClient.requestServerSideAccess`); `AppContainer` wiring; `GameScreen` queues and flushes when the level complete dialog shows; a new `ScorePostIndicator` composable; About privacy string.
- **Storage**: a new Preferences DataStore file `score_queue` (level id → score). No change to the Room database.
- **Manifest**: `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` (both normal permissions, no prompt).
- **Build**: `buildConfigField`s for the score server URL and web client id with the defaults above, overridable through Gradle properties / environment variables. `docs/deployment.md` documents them.
- **Dependencies**: none new (DataStore, kotlinx.serialization and coroutines are already in the catalog).
- **Server**: none; uses the existing `POST /api/players/sessions` and `POST /api/scores` endpoints. The game's web client id must equal the server's `GooglePlayGames:ClientId`.
- **Play Console**: the Data safety form must now declare that the app collects a user ID (Play Games player, via the server) and game scores (app activity), sent over HTTPS, for app functionality.
