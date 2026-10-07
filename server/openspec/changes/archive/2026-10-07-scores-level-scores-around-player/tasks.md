# Tasks

## 1. Prerequisites

- [x] 1.1 Confirm `players-google-play-auth` (#1), `scores-submit-score` (#2) and `scores-level-top-ten` (#3) are implemented on `development`, and note the actual names of `ILevelScoreRepository`, `LevelLeaderboardDto`, `LeaderboardEntryDto`, `IPlayerProfileService` and the leaderboard endpoint file; verify `dotnet build src/leak-out-server.slnx` succeeds

## 2. Window computation

- [x] 2.1 Add `LeaderboardWindow.Compute(int position, int total)` in `HexMaster.LeakOut.Scores/Features/GetLevelScoresAroundPlayer/` returning the start position and the number of entries; verify `LeaderboardWindowTests` cover position 128 of 200 (→ 123, 10), position 3 of 50 (→ 1, 10), position 49 of 50 (→ 41, 10), position 4 of 6 (→ 1, 6), position 1 of 1 (→ 1, 1)

## 3. Repository reads

- [x] 3.1 Add `CountRankedAboveAsync`, `CountAsync` and an offset-aware `GetRankedAsync` to `ILevelScoreRepository` (reuse or extend what #2 and #3 added); verify the Scores project builds
- [x] 3.2 Implement them in `HexMaster.LeakOut.Scores.Data.PostgreSQL` ordered by `score desc, achieved_at asc, user_id asc`, without a migration; verify with a repository test (EF Core against PostgreSQL or the test setup #2 introduced) that ties on score and `achieved_at` produce the expected order and count

## 4. Query slice

- [x] 4.1 Add `GetLevelScoresAroundPlayerQuery(Guid UserId, int Level)` and `GetLevelScoresAroundPlayerQueryHandler` returning `LevelLeaderboardDto`, register the handler in `AddScoresModule()`; verify handler tests with Moq and Bogus cover: player in the middle (positions 123..132, only 128 marked as caller), player near the top, player near the bottom, level with fewer than 10 scores, caller without a score (empty list, no window read), missing display name (`Unknown player`), null query (`ArgumentNullException`)

## 5. Endpoint

- [x] 5.1 Map `GET /api/levels/{level}/scores/around-me` in the leaderboard endpoint file with `.WithName("GetLevelScoresAroundPlayer")`, `.Produces<LevelLeaderboardDto>()` and `.ProducesValidationProblem()`, validating `level >= 1` and resolving `sub` with `IUserResolver` before dispatching; verify endpoint tests with `WebApplicationFactory` and a mocked handler return 200 with entries, 400 for level 0, and 401 without a token
- [x] 5.2 Add a sample request to `src/HexMaster.LeakOut.Api/HexMaster.LeakOut.Api.http`; verify it runs against the local API with a valid session token

## 6. Integration check

- [x] 6.1 Run all tests with coverage per the `git-workflow` skill; verify every test passes and line coverage is at least 80%
