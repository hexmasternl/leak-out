# Design

## Context

See proposal.md (Why) for the motivation. This change builds on three changes that are not implemented yet:

- `players-google-play-auth` (#1): JWT bearer authentication, `IUserResolver` in Players.Abstractions that turns the token's `sub` (the Google Play player id) into an internal `Guid` UserId, and the CQRS interfaces in `HexMaster.LeakOut.Core`.
- `scores-submit-score` (#2): the `LevelScore` entity (`UserId`, `Level`, `Score`, `AchievedAt`), the `level_scores` table with a unique `(user_id, level)` index and an index on `(level, score desc, achieved_at)`, and `ILevelScoreRepository` in the Scores module.
- `scores-level-top-ten` (#3): the `level-leaderboard` capability and its ranking rule, `LevelLeaderboardDto(int Level, IReadOnlyList<LeaderboardEntryDto> Entries)` and `LeaderboardEntryDto(int Position, string DisplayName, int Score, bool IsCurrentPlayer)` in `HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects`, `IPlayerProfileService.GetDisplayNamesAsync` in Players.Abstractions, the `Unknown player` fallback, and the leaderboard endpoint file in the API.

If those changes land with different names, this design follows them; the behaviour in the spec does not change.

## Goals / Non-Goals

**Goals:**
- Return the 10-entry window around the caller with one position query and one window query, without loading all scores of the level.
- Produce exactly the same positions as the top 10 for the same data.

**Non-Goals:**
- A configurable window size or paging through the leaderboard.
- Caching leaderboard results.
- Shared ("dense" or "competition") positions for equal scores.

## Decisions

### Feature slice and handler

`HexMaster.LeakOut.Scores.Features.GetLevelScoresAroundPlayer`:

- `GetLevelScoresAroundPlayerQuery(Guid UserId, int Level)` — `sealed record`.
- `GetLevelScoresAroundPlayerQueryHandler : IQueryHandler<GetLevelScoresAroundPlayerQuery, LevelLeaderboardDto>`, registered in `AddScoresModule()`.

The handler:

1. Loads the caller's `LevelScore` for the level. None → return `new LevelLeaderboardDto(level, [])`.
2. Asks the repository for the caller's position: `1 + count of scores on the level that rank above the caller`.
3. Asks the repository for the total number of scores on the level.
4. Computes the window: `start = max(1, min(position - 5, total - 9))`, `take = min(10, total)`. This one formula covers the middle, the top edge, the bottom edge, and levels with fewer than 10 scores.
5. Fetches the ranked window (`offset = start - 1`, `take`), looks up display names for those UserIds through `IPlayerProfileService`, and maps to `LeaderboardEntryDto` with `Position = start + index` and `IsCurrentPlayer = entry.UserId == query.UserId`.

The window computation is a small pure static method (`LeaderboardWindow.Compute(position, total)`) in the slice so it can be unit tested on its own.

*Alternative considered*: one SQL query with `ROW_NUMBER()` over the whole level, filtered to the window. Simpler to write, but PostgreSQL still numbers every row of the level. The count + offset approach uses the index directly; both are fine at the expected volume, and the count approach keeps the repository methods reusable for the top 10 (which is the window at offset 0).

### Repository methods

Added to `ILevelScoreRepository` (in the Scores module project, per ADR 0002/0009):

- `Task<LevelScore?> GetAsync(Guid userId, int level, CancellationToken ct)` — likely already added by #2; reuse it.
- `Task<int> CountRankedAboveAsync(int level, int score, DateTimeOffset achievedAt, Guid userId, CancellationToken ct)` — counts rows where `score > @score OR (score = @score AND achieved_at < @achievedAt) OR (score = @score AND achieved_at = @achievedAt AND user_id < @userId)`.
- `Task<int> CountAsync(int level, CancellationToken ct)`.
- `Task<IReadOnlyList<LevelScore>> GetRankedAsync(int level, int offset, int take, CancellationToken ct)` — ordered by `score desc, achieved_at asc, user_id asc`. If #3 already added a ranked read for the top 10, extend it with `offset` instead of adding a second method.

The EF Core implementation in `HexMaster.LeakOut.Scores.Data.PostgreSQL` uses the `(level, score desc, achieved_at)` index from #2. No migration.

### Endpoint

In the leaderboard endpoint file from #3 (`Endpoints/LevelScoreEndpoints.cs`, group `/api/levels/{level}/scores`, `.RequireAuthorization()`), add `MapGet("/around-me", ...)` named `GetLevelScoresAroundPlayer`, `.Produces<LevelLeaderboardDto>()`, `.ProducesValidationProblem()`. The endpoint:

1. Validates `level >= 1`; otherwise returns `TypedResults.ValidationProblem`.
2. Reads `sub`, resolves it with `IUserResolver.ResolveAsync` (ADR 0010; the Google Play player id never reaches the query).
3. Dispatches the query and returns `TypedResults.Ok(result)`.

### Assumptions

- **Window size and shape are fixed** at 5 above, 4 below, as in the issue's example. Not configurable.
- **Edges shift the window** so 10 entries are shown whenever the level has 10 or more scores (the issue shows only the middle case).
- **No score on the level → `200 OK` with an empty list**, not `404`, so the client can treat "not ranked yet" as a normal state.
- **Ranking ties** follow #3: score desc, earliest `achieved_at` first, then user id; positions are unique and 1-based.
- **The response shape is shared** with the top 10 (`LevelLeaderboardDto`), so the client can render both with one list component.
- **Route** `GET /api/levels/{level}/scores/around-me`; the caller is always the authenticated player, so no player id is in the route.

## Risks / Trade-offs

- [A score is submitted between the position count and the window read, shifting positions by one] → Acceptable for a game leaderboard; both reads run in one read-only transaction to keep them consistent.
- [Display names come from another module, so a name may be missing] → `Unknown player` fallback from #3.
- [The three dependencies are not implemented yet and may rename types] → The spec is independent of type names; the tasks reference the dependency changes so the implementer reconciles names at apply time.
