# Tasks

## 1. Display names contract (Players module)

- [x] 1.1 Add `IPlayerProfileService` with `GetDisplayNamesAsync(IReadOnlyCollection<Guid> userIds, CancellationToken ct)` to `HexMaster.LeakOut.Players.Abstractions/Services` and verify the Abstractions project builds without referencing the Players core project
- [x] 1.2 Add a batched display-name read to `IPlayerRepository` and its PostgreSQL implementation in `HexMaster.LeakOut.Players.Data.PostgreSQL`; verify with a repository test that unknown ids are absent from the result
- [x] 1.3 Implement `PlayerProfileService` in `HexMaster.LeakOut.Players/Services`, register it in `AddPlayersModule`, and verify `PlayerProfileServiceTests` cover found names, unknown ids and an empty id list

## 2. Ranking and top 10 query (Scores module)

- [x] 2.1 Add `LevelLeaderboardDto` and `LeaderboardEntryDto` as `sealed record` types in `HexMaster.LeakOut.Scores.Abstractions/DataTransferObjects` and verify the project builds
- [x] 2.2 Add `RankedLevelScore` and `GetTopAsync(int level, int count, CancellationToken ct)` to `ILevelScoreRepository`, implement it in `HexMaster.LeakOut.Scores.Data.PostgreSQL` ordering by score desc, achieved_at asc, user_id asc with 1-based positions; verify a test covers ties and the count limit
- [x] 2.3 Add a project reference from `HexMaster.LeakOut.Scores` to `HexMaster.LeakOut.Players.Abstractions` and verify `dotnet build src/leak-out-server.slnx` succeeds
- [x] 2.4 Add `Features/GetLevelTopScores/GetLevelTopScoresQuery` and `GetLevelTopScoresQueryHandler` (maps ranked rows to entries, sets `IsCurrentPlayer`, falls back to "Unknown player"), register it in `AddScoresModule`; verify `GetLevelTopScoresQueryHandlerTests` cover ordering with ties, fewer than 10 rows, an empty level, `IsCurrentPlayer`, the name fallback and a null query

## 3. Endpoint

- [x] 3.1 Map `GET /api/levels/{level}/scores/top` in a new `src/HexMaster.LeakOut.Api/Endpoints/LevelScoreEndpoints.cs` (`MapLevelScoreEndpoints()`, group `/api/levels/{level}/scores`, called from `Program.cs`) with `.RequireAuthorization()`, validate `level >= 1` (400 validation problem), resolve the caller via `IUserResolver`, dispatch the query, and add `.WithName("GetLevelTopScores")` and `.Produces<LevelLeaderboardDto>()`; verify endpoint tests return 200 with entries, 400 for level 0 and 401 without a token
- [ ] 3.2 Add a sample request for the top 10 to `HexMaster.LeakOut.Api.http` and verify it returns 200 against the running API with a valid token

## 4. Integration check

- [x] 4.1 Run `dotnet test --solution src/leak-out-server.slnx` with coverage and verify all tests pass and line coverage is at least 80%
