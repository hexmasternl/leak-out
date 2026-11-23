# Tasks

## 1. Data

- [x] 1.1 Replace `DailyTopScores.playerPosition` with `player: DailyPlayerRank?` (position, score, stars), parse it in `DailyPipeJson.topScores` and update the callers; verify with new cases in `DailyPipeApiTest` (player present, player null)
- [x] 1.2 Add `DailyPipes.top(date)` returning loaded or failed, built on the existing top-scores call with its 401 retry; verify with unit tests for success, failure and signing in again
- [x] 1.3 Add the pure row selection `dailyLeaderboardRows` (top 5, own row below when outside the top 5, marked row inside it, none without a result); verify with unit tests for those cases plus fewer than 5 entries and no entries

## 2. Leaderboard UI

- [x] 2.1 Build the `DailyLeaderboard` composable: a Position, Player and Points table, the own row highlighted and marked "(you)", loading, empty and failed states, and one TalkBack item per row, with test tags. Add the strings in every supported language and an entry in `docs/design-system.md`. Verify with an instrumented test for each state and with the string completeness test
- [x] 2.2 Fit the table in the Compact dialog layout in portrait and landscape; verify with the layout measuring test used by the level complete dialog

## 3. Start gate

- [x] 3.1 Open a daily pipe paused behind the leaderboard: fold a start gate into the clock condition, load today's top scores, show Start, hold the intro tips until Start, and make back return to the welcome screen. Verify with instrumented tests in `DailyPipeGameTest`: no flow before Start, flow after Start, back exits, and Start works after a failed download
- [x] 3.2 Keep bundled levels opening without the leaderboard; verify that the existing bundled-level game tests still pass

## 4. Win flow

- [x] 4.1 Give the daily level complete dialog a single Next button, keep the daily result panel and remove its top-3 list and `DAILY_TOP_ROWS`; verify with an instrumented test that only Next is shown and with the updated `DailyResultPanel` tests
- [x] 4.2 Make Next show the leaderboard, downloaded again after the submission has settled, with Restart (same seed, opening paused behind the leaderboard) and Exit (welcome screen); verify with instrumented tests for Next, Restart and Exit
- [x] 4.3 Keep the bundled-level win dialog unchanged; verify that the existing level complete dialog tests still pass

## 5. Integration

- [x] 5.1 Run `./gradlew :core:test :app:testDebugUnitTest` and the affected instrumented tests, and check that they all pass
