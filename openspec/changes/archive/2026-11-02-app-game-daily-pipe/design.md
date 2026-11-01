# Design

## Context

Issue #98 first asked for a daily puzzle worked out on the device from the date. The maintainer changed direction: the daily pipe is generated and published by the server, and the server owns the streak and the highscores. This change touches both products:

- **Server** (`server/`): a .NET 10 modular monolith (`Core`, `Players`, `Scores`) behind one minimal API, PostgreSQL via EF Core, Google Play Games sign-in that issues our own JWT, hosted on Azure Container Apps and orchestrated locally by Aspire.
- **App** (`app/src/`): `core` holds the level model, `LevelParser`, `LevelValidator`, `LevelSolver` and the steered `Dealer`; `app` already talks to the server through `ScoreServerSession`, `HttpScoreServerApi`, `ScoreSubmitter` and `NetworkMonitor` (`scores/online/`).

The app's solver is Kotlin; the server is C#. The level JSON format (`docs/level-format.md`) is the contract between them.

## Goals / Non-Goals

**Goals:**

- One daily pipe per UTC date, generated, checked for solvability and published by a scheduled job at 00:00 UTC.
- Every player gets the same level and the same stack sequence on the same day.
- The server keeps the streak per player and one best score per player per daily pipe, and deletes older days' highscores when a new pipe is published.
- The Daily Pipe button is only shown to signed-in Play Games players, and disabled while offline.

**Non-Goals:**

- Streak freezes, reminders (#104), share cards (#105) and a longer-term daily leaderboard (#99).
- Playing the daily pipe offline or without Play Games sign-in.
- XP or achievements for daily wins.
- Showing daily pipes on the public website.

## Decisions

### 1. A new `DailyPipes` server module

`server/src/DailyPipes/` follows the existing module shape: `HexMaster.LeakOut.DailyPipes` (domain, `Features/` slices, registration), `.Abstractions` (DTOs), `.Data.PostgreSQL` (`DailyPipesDbContext`, migrations), `.Tests`. Features: `GenerateDailyPipe`, `GetTodaysDailyPipe`, `SubmitDailyScore`, `GetDailyTopScores`. Streaks live in the same module because they are only ever changed by a daily win.

*Alternative:* put daily scores in the `Scores` module. Rejected: daily scores have a different key (date, not level number), a different lifetime (deleted daily) and drive the streak; mixing them would complicate the level leaderboard queries.

Tables: `daily_pipes` (date PK, level JSON, stack seed, published at), `daily_scores` (date, player id, score, stars, achieved at; unique on date + player), `daily_streaks` (player id PK, current streak, best streak, last win date).

### 2. Solvability is checked on the server by a C# port of `LevelValidator` and `LevelSolver`

The generator needs to prove a level is valid and winnable before publishing it. The rules are ported to C# inside the `DailyPipes` module (`Generation/LevelRules`, `Generation/LevelSolver`). To prevent drift, a shared conformance fixture is used: the app's bundled levels (`app/src/app/src/main/assets/levels/*.json`) and a set of hand-made unsolvable levels are run through both implementations in their own test suites, and the expected verdict per file is checked in (`server/src/DailyPipes/HexMaster.LeakOut.DailyPipes.Tests/Fixtures/`). Additionally, the app's `core` tests parse, validate and solve a sample of generated levels produced by the server tests, so a level the server publishes is one the app accepts.

*Alternative:* run the Kotlin `core` as a JVM command-line tool in the job container. Rejected for now: it adds a second runtime and build to the server pipeline. Kept as a fallback if the port proves hard to keep in step.

### 3. Generation

The generator picks, from a random source seeded per attempt:

- a grid within the format limits, sized for a phone (columns 7 to 14, rows 5 to 10);
- start and finish cells on different sides of the grid, with open sides facing inward;
- blocked cells up to about 15 % of the grid;
- optionally a few mandatory sections and at most one portal pair, only from features the current app release supports;
- flow settings and stack weights from a small set of difficulty presets, one per weekday (easier early in the week);
- star thresholds derived from the solver's route: `one = route points × 0.6`, `two = × 0.9`, `three = × 1.2`, using the app's `Points` values, rounded and strictly increasing;
- a 64-bit stack seed.

The candidate must pass the validator and the solver; if not, the generator tries again, up to 200 attempts, and fails the run (non-zero exit, error telemetry) if none passes. Container Apps job retries then rerun it.

### 4. Publication and idempotency at 00:00 UTC

The generator runs as a separate console project `HexMaster.LeakOut.DailyPipes.Generator` deployed as an Azure Container Apps **job** with a schedule trigger `0 0 * * *` (UTC), a replica retry limit and a timeout. It is not a hosted service inside the API, because the API scales out to several replicas and must not generate the pipe more than once.

The run computes `today = IClock.UtcNow.Date`. In one transaction it inserts the `daily_pipes` row for `today` (if a row already exists, it does nothing: reruns are harmless) and deletes all `daily_scores` rows whose date is before `today`, and all `daily_pipes` rows older than `today` minus 7 days (kept briefly for diagnosis). Streak rows are never deleted.

If the job has not published today's pipe yet (or failed), `GET /api/daily-pipes/today` answers `404` and the app shows "No Daily Pipe yet, try again later". Locally, the Aspire AppHost registers the generator as a project resource that runs once at startup, so a developer always has a pipe.

### 5. API

All endpoints require a signed-in player (`RequirePlayer()`), because only Play Games players see the daily pipe.

- `GET /api/daily-pipes/today` → `200 { date: "2026-11-01", level: { …level-format JSON… }, stackSeed: 123, streak: { current, best, playedToday } , bestScore: null | { score, stars } }`.
- `POST /api/daily-pipes/{date}/scores` with `{ score, stars }`. Accepted only when `{date}` is the date of the currently published pipe (else `409 Conflict`, "this daily pipe has ended"); only a higher score replaces the stored one; a submission with at least one star is a win and updates the streak. Returns `{ bestScore, rank, streak }`.
- `GET /api/daily-pipes/{date}/scores/top` → the top 10 for that date, ranked by score descending then earliest achieved, the same rule as `server-level-leaderboard`, plus the caller's own rank.

The date in the URL is the server's UTC date. The device clock is never used, so changing the clock cannot skip or replay days.

### 6. Streak rules

- A win is a submission with at least 1 star for the current daily pipe.
- On the first win of date `D`: if `last win date = D − 1` the streak goes up by 1; if `last win date = D` nothing changes; otherwise the streak becomes 1. `best streak` is the maximum ever reached.
- On read, a streak whose `last win date` is before yesterday is reported as 0 (the row is updated lazily on the next win), so no nightly sweep over all players is needed.
- The retry policy: players may play the daily pipe as often as they like; only the best score counts. (Open question below.)

### 7. App

- **Visibility:** the welcome screen reads the Play Games sign-in state (`PlayGamesServices`) and shows the Daily Pipe button only when signed in. It observes `NetworkMonitor`; while offline the button is shown disabled with a "No internet connection" hint.
- **Download:** tapping the button calls `GET /api/daily-pipes/today` through the existing authenticated session, shows a loading state, and parses `level` with `LevelParser` and validates it with `LevelValidator`. A failed download or a level the app cannot parse (for example a newer format) shows an error and returns to the welcome screen.
- **Play:** the game screen starts with the downloaded level and a `Random(stackSeed)` passed to the `Dealer`, so the dealt sequence is identical for every player; a restart reuses the same seed. `kotlin.random.Random(seed)` is deterministic across platforms, and a `core` test pins the first sections dealt for a known seed.
- **Result:** on a win the app submits the score (reusing the retry behaviour of `ScoreSubmitter`); the level-complete dialog shows the streak and the day's top scores. A loss offers retry. Daily results are not written to level history and don't unlock bundled levels or pages, and achievements are not evaluated for daily attempts.
- The daily pipe is not cached for offline play; it is downloaded each time the button is pressed.

## Risks / Trade-offs

- **The C# solver drifts from the Kotlin one** → shared fixtures checked by both test suites, plus app tests over generated sample levels.
- **The job fails at midnight** → Container Apps job retries; the app handles `404` gracefully; an alert on job failure in Application Insights.
- **A generated level is solvable but unfair** (too short a delay for its route) → difficulty presets are conservative and the star thresholds come from the actual route; presets can be tuned without an app release.
- **Clients on an old app version get a level with features they don't support** → the generator only uses features available in the minimum supported app version, and the app rejects unparsable levels with an error instead of crashing.
- **Scores are client-reported** → the same trust model as level scores today; the server only bounds the score (0 to a maximum derived from the level) and the stars (0 to 3).
- **A player mid-attempt at 00:00 UTC** → the submission for the old date is rejected with `409`; the app shows that the daily pipe has ended. Acceptable for a daily format.

## Migration Plan

1. Ship the server first: module, migrations (applied at startup like the other modules), endpoints and the job. Run the job once manually to publish the first pipe.
2. Ship the app release with the Daily Pipe button. Older app versions don't call the new endpoints, so nothing breaks for them.
3. Rollback: hide the button behind the existing feature flags (`feature-flags` spec) and disable the job's schedule.

## Open Questions

- Can the player retry the daily pipe as often as they like, with only the best result counting (current decision), or is there one attempt?
- Streak freezes: should players earn them (for example 1 per 7-day streak, at most 2 held), or should there be none (current decision: none)?
- Should daily wins give XP or achievements (for example a 7-day streak achievement)?
- Should the streak count only wins (current decision) or any completed attempt?
- Are the generator's difficulty presets and star-threshold factors right, or should the maintainer hand-tune them after a test period?
- Should the daily pipe also appear on the public website leaderboard?
