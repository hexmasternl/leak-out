# Proposal

## Why

Once a player has three-starred the 96 bundled levels, nothing new appears, and because every attempt deals an unseeded stack, two players never play the same puzzle and their scores can't be compared. A **Daily Pipe**, one new level a day that every player gets in exactly the same form, with a streak for consecutive wins and a highscore table per day, gives players a reason to open Leak Out every day. Generating it on the server (instead of from the date on the device) keeps the puzzle, the streak and the highscores out of reach of device-clock tricks and lets the level change without an app release.

Tracked in [#98](https://github.com/hexmasternl/leak-out/issues/98).

## What Changes

- **Server: daily pipe generation.** A scheduled backend job runs every day at 00:00 UTC. It generates a random level within the level-format limits, checks it with a solvability simulation (a .NET port of the app's `LevelValidator` and `LevelSolver`), retries with a new random level until one passes, and publishes it as the daily pipe for that UTC date together with a stack seed. Publishing is idempotent per date.
- **Server: daily pipe download.** A new authenticated endpoint returns today's daily pipe: its UTC date, the level in the level-format JSON and the stack seed.
- **Server: daily highscores.** Players submit their result for the daily pipe of the current day; the server keeps one best score per player per daily pipe and serves the top scores for it. When a new daily pipe is published, the highscores of every older daily pipe are deleted.
- **Server: streaks.** The server keeps a streak per player: the number of consecutive UTC days with a win on the daily pipe. A missed day resets it to 0. The streak is returned with the daily pipe and after a submission.
- **Server: hosting.** The generator runs as an Azure Container Apps job with a cron trigger, defined in the existing Bicep templates and orchestrated locally by Aspire.
- **App: Daily Pipe entry.** The welcome screen shows a **Daily Pipe** button only to players signed in with Google Play Games. The button is disabled while the device has no internet connection.
- **App: playing the daily pipe.** Tapping the button downloads today's daily pipe, parses it with `LevelParser`, and starts it on the game screen with the stack dealt from the server's seed, so every player gets the same sections in the same order. A win is submitted to the server; the result dialog shows the score, the player's streak and the day's highscores. Daily results do not unlock bundled levels or pages.
- The original plan in #98 (date-derived level on the device, local midnight, no server) is replaced by this server-side design.

## Capabilities

### New Capabilities

- `server-daily-pipe`: the nightly generation, solvability check and publication of the daily pipe, and the endpoint that serves today's daily pipe to the app.
- `server-daily-pipe-scores`: submitting a daily pipe result, one best score per player per daily pipe, the daily top scores, and removing older days' highscores when a new pipe is published.
- `server-daily-pipe-streaks`: the per-player streak of consecutive UTC days with a daily pipe win.
- `daily-pipe`: the app side: the Daily Pipe button (visibility and offline state), downloading and playing the daily pipe with the seeded stack, submitting the result, and showing the streak and highscores.

### Modified Capabilities

- `welcome-screen`: the menu gains a Daily Pipe button that is shown only to signed-in Play Games players.
- `server-azure-hosting`: the infrastructure gains a scheduled Container Apps job that runs the daily pipe generator at 00:00 UTC.

## Impact

- **Server code**: a new `DailyPipes` module (`server/src/DailyPipes/`: domain, features, `.Abstractions`, `.Data.PostgreSQL` with migrations, `.Tests`), a level generator and C# solvability checker, new endpoints in `HexMaster.LeakOut.Api/Endpoints/`, and a generator worker project run as a job. The AppHost registers the job and a new database.
- **API** (all require a signed-in player): `GET /api/daily-pipes/today`, `POST /api/daily-pipes/{date}/scores`, `GET /api/daily-pipes/{date}/scores/top`.
- **Infrastructure**: a Container Apps job with a `0 0 * * *` schedule in `server/infrastructure/`.
- **App code**: `app/` gains a daily pipe client on top of `ScoreServerSession` and `NetworkMonitor`, a Daily Pipe button on the welcome screen, a daily game entry that loads a downloaded level, and result-dialog additions. `core` gains a seeded dealing entry point for the stack. New strings in every supported language.
- **Docs**: `docs/level-format.md` (a level may now come from the server and have a stack seed) and the server README.
- **Dependencies**: none new expected.
