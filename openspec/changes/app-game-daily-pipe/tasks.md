## 1. Server: DailyPipes module skeleton

- [x] 1.1 Create `server/src/DailyPipes/` with `HexMaster.LeakOut.DailyPipes`, `.Abstractions`, `.Data.PostgreSQL` and `.Tests`, add them to `leak-out-server.slnx`, and add `AddDailyPipesModule()` / `AddDailyPipesPostgreSqlData()` registration
- [x] 1.2 Add the domain models `DailyPipe` (date, level JSON, stack seed, published at), `DailyScore` (date, player id, score, stars, achieved at) and `DailyStreak` (player id, current, best, last win date) with unit tests for their invariants
- [x] 1.3 Add `DailyPipesDbContext` with the `daily_pipes`, `daily_scores` (unique on date + player) and `daily_streaks` tables, the initial migration, and apply migrations at startup like the other modules
- [x] 1.4 Register the database in the Aspire AppHost and reference it from the API

## 2. Server: level rules and solvability

- [ ] 2.1 Port the level model and `LevelValidator` rules to C# (`Generation/LevelRules`), reading and writing the level-format JSON
- [ ] 2.2 Port `LevelSolver` to C# (`Generation/LevelSolver`), including mandatory sections and portal pairs and the search work limit
- [ ] 2.3 Add conformance fixtures: copy the bundled levels and a set of unsolvable levels with their expected verdicts into `HexMaster.LeakOut.DailyPipes.Tests/Fixtures/` and test that the C# validator and solver agree with them
- [ ] 2.4 Add an app `core` test that runs the same fixtures through the Kotlin `LevelValidator` and `LevelSolver`, so both sides are checked against one set of expected verdicts

## 3. Server: generation and publication

- [ ] 3.1 Implement the random level generator (grid, start and finish, blocked cells, optional mandatory sections and portals, weekday difficulty preset, star thresholds from the solver's route, stack seed) with a seedable random source
- [ ] 3.2 Implement the `GenerateDailyPipe` command: generate candidates until one passes validation and solvability or the attempt limit is reached, then publish for today's UTC date (from `IClock`) and delete older daily scores and pipes older than 7 days in one transaction; do nothing when today already has a pipe
- [ ] 3.3 Unit tests: unsolvable candidates are discarded, the attempt limit fails the run without publishing, a rerun leaves the pipe unchanged, older scores are deleted and streaks are kept, a year of generator seeds always yields valid and solvable levels
- [ ] 3.4 Write a sample of generated levels to the conformance fixtures and check in the app `core` test that `LevelParser`, `LevelValidator` and `LevelSolver` accept them
- [ ] 3.5 Create the console project `HexMaster.LeakOut.DailyPipes.Generator` that runs `GenerateDailyPipe` once with ServiceDefaults telemetry and exits non-zero on failure; register it in the AppHost so it runs once at local startup

## 4. Server: API

- [ ] 4.1 `GET /api/daily-pipes/today` (`GetTodaysDailyPipe` query): date, level, stack seed, streak (current, best, won today) and the caller's best score; `404` when today has no pipe
- [ ] 4.2 `POST /api/daily-pipes/{date}/scores` (`SubmitDailyScore` command): validation (score 0 to the level's maximum, stars 0 to 3), `409` for any date other than the current pipe, keep only the best score, update the streak on a win, return best score, rank and streak
- [ ] 4.3 `GET /api/daily-pipes/{date}/scores/top` (`GetDailyTopScores` query): top 10 with display names, ranked by score then earliest achievement, plus the caller's rank; `404` for an unknown date
- [ ] 4.4 Implement the streak rules (consecutive UTC days, same-day wins ignored, 0 stars is not a win, missed day reported as 0 and restarted at 1) with unit tests for every scenario in `server-daily-pipe-streaks`
- [ ] 4.5 Endpoint tests in `HexMaster.LeakOut.Api.Tests` for authentication (`401`), `404`, `409`, `400` and the happy paths; add sample requests to `HexMaster.LeakOut.Api.http`
- [ ] 4.6 Add tracing and metrics for generation runs, downloads and submissions; keep the 80 % coverage gate

## 5. Server: infrastructure and deployment

- [ ] 5.1 Add a Container Apps job module in `server/infrastructure/` with a schedule trigger `0 0 * * *`, retry limit, timeout, database connection, secrets and Application Insights settings
- [ ] 5.2 Build and push the generator image and deploy the job from `server-deploy.yml`
- [ ] 5.3 Add an Application Insights alert on a failed generator run
- [ ] 5.4 Update the server README and `docs/` with the module, the endpoints and how to run the generator by hand

## 6. App: core

- [ ] 6.1 Let a game be started with a given stack seed so the `Dealer` draws from `Random(seed)` for the initial stack and every later deal, and a restart reuses the seed
- [ ] 6.2 Unit test: a known seed deals a pinned sequence of sections, and two games with the same seed deal identical sequences

## 7. App: client and welcome screen

- [ ] 7.1 Add a `DailyPipeApi` client on top of `ScoreServerSession` for today's pipe, score submission and top scores, with JVM unit tests for parsing and the `404`, `409` and error cases
- [ ] 7.2 Add the Daily Pipe button to the welcome screen below Play, shown only when signed in with Play Games and disabled with a hint while `NetworkMonitor` reports no connection
- [ ] 7.3 Tapping the button downloads today's pipe with a loading state, parses and validates the level, and opens the game screen; show an error and stay on the welcome screen when there is no pipe yet, the download fails or the level is rejected

## 8. App: playing and results

- [ ] 8.1 Run the daily pipe on the game screen with the seeded stack, without writing level history, unlocking levels or pages, or evaluating achievements
- [ ] 8.2 Submit a win to the server and show the score, streak and the day's top scores in the level-complete dialog; show "the daily pipe has ended" on `409`
- [ ] 8.3 Add all new strings in every supported language
- [ ] 8.4 Instrumented tests: button hidden when signed out, disabled when offline, opening and winning the daily pipe against a fake server
- [ ] 8.5 Update `docs/level-format.md` (levels from the server, stack seed)

## 9. Verification

- [ ] 9.1 Run `dotnet test --solution leak-out-server.slnx` from `server/src` and confirm it passes with at least 80 % coverage
- [ ] 9.2 Run `./gradlew :core:test` and `./gradlew :app:testDebugUnitTest` from `app/src` and confirm they pass
- [ ] 9.3 Run the affected instrumented tests on an emulator
