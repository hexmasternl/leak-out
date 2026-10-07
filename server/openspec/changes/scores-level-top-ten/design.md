# Design

## Context

See proposal.md for the motivation. This change builds on two earlier changes:

- `players-google-play-auth` (#1) adds JWT bearer authentication, the Players module with the `players` table (internal `Guid` id, display name), `IUserResolver` that maps the Google Play player id (`sub`) to the internal `UserId`, and the shared CQRS interfaces in `HexMaster.LeakOut.Core`.
- `scores-submit-score` (#2) adds the Scores module's `LevelScore` entity, the `level_scores` table (`user_id`, `level`, `score`, `achieved_at`, unique on `(user_id, level)`) and an index on `(level, score desc, achieved_at)`.

The Scores module stores players only by `UserId` (ADR 0010). Display names are owned by the Players module.

## Goals / Non-Goals

**Goals:**
- One deterministic ranking rule for a level, written once and reused by the "scores around the player" view (#4).
- A top 10 query served from a single indexed read on `level_scores` plus one batched display-name lookup.

**Non-Goals:**
- Paging beyond the top 10, a global (cross-level) leaderboard, response caching.
- Copying display names into the Scores database.

## Decisions

### Endpoint
`GET /api/levels/{level}/scores/top`, mapped in a new sub-resource file `Endpoints/LevelScoreEndpoints.cs` (`MapLevelScoreEndpoints()`, group `/api/levels/{level}/scores`, which #4 extends with `/around-me`), in a group with `.RequireAuthorization()` and `.WithTags("Scores")`. The endpoint:
1. validates `level >= 1`, otherwise returns `TypedResults.ValidationProblem` (400) keyed on `level`;
2. reads `sub` from the principal and resolves it with `IUserResolver.ResolveAsync` (from #1);
3. dispatches `GetLevelTopScoresQuery(userId, level)` to `IQueryHandler<GetLevelTopScoresQuery, LevelLeaderboardDto>`;
4. returns `TypedResults.Ok(result)` with `.Produces<LevelLeaderboardDto>()` and `.WithName("GetLevelTopScores")`.

The route sits under `/api/levels/{level}` rather than `/api/scores` because #4 adds `/api/levels/{level}/scores/around-me` beside it, so both leaderboard views share one group.

### Feature slice
`HexMaster.LeakOut.Scores.Features.GetLevelTopScores`:
- `GetLevelTopScoresQuery(Guid UserId, int Level)` — `sealed record`.
- `GetLevelTopScoresQueryHandler : IQueryHandler<GetLevelTopScoresQuery, LevelLeaderboardDto>`.

DTOs in `HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects`, both `sealed record`:
- `LevelLeaderboardDto(int Level, IReadOnlyList<LeaderboardEntryDto> Entries)`
- `LeaderboardEntryDto(int Position, string DisplayName, int Score, bool IsCurrentPlayer)`

#4 returns the same DTOs, so the client renders both views with one model.

### Ranking rule
`ORDER BY score DESC, achieved_at ASC, user_id ASC`; position is the 1-based row number. Alternatives considered:
- *Shared positions for ties (1, 2, 2, 4)*: closer to sports rankings, but makes "5 above / 4 below" in #4 ambiguous. Rejected.
- *Order only by score*: non-deterministic for ties. Rejected.

Rewarding the earliest achiever for a tie matches the intuition that whoever got there first leads. The rule is implemented once, in the repository read model, so #4 reuses it.

### Repository
`ILevelScoreRepository` (in the Scores module project, from #2) gets `Task<IReadOnlyList<RankedLevelScore>> GetTopAsync(int level, int count, CancellationToken ct)`. `RankedLevelScore(int Position, Guid UserId, int Score)` is a small read model in the module project. The PostgreSQL implementation uses `ROW_NUMBER() OVER (ORDER BY score DESC, achieved_at, user_id)` with `WHERE level = @level ... LIMIT @count` (via EF Core LINQ `OrderBy...Take` with positions assigned in memory, which is equivalent for the top N). The `(level, score desc, achieved_at)` index from #2 serves it.

### Display names across the module boundary
New contract in `HexMaster.LeakOut.Players.Abstractions.Services`:

```csharp
public interface IPlayerProfileService
{
    Task<IReadOnlyDictionary<Guid, string>> GetDisplayNamesAsync(IReadOnlyCollection<Guid> userIds, CancellationToken ct);
}
```

Implemented in the Players module (`Services/PlayerProfileService`), backed by a batched `WHERE id = ANY(@ids)` read on `players` through `IPlayerRepository`, registered in `AddPlayersModule`. `HexMaster.LeakOut.Scores` references `HexMaster.LeakOut.Players.Abstractions` only (allowed by the modular solution structure). The handler calls it once with the up to 10 user ids. Alternative considered: denormalising the display name into `level_scores` — rejected, it duplicates Players-owned data and goes stale when a name changes.

A user id missing from the dictionary, or with an empty name, maps to `"Unknown player"`.

### Tests
`HexMaster.LeakOut.Scores.Tests/GetLevelTopScores/GetLevelTopScoresQueryHandlerTests.cs` (xUnit, Moq, Bogus) with a mocked repository and `IPlayerProfileService`: ordering including ties, fewer than 10 rows, empty level, `IsCurrentPlayer`, name fallback, null query → `ArgumentNullException`. Repository ordering and tie-breaking are covered by tests against the PostgreSQL implementation if #2 established such tests; otherwise by an in-memory ranking test of the shared ordering. `PlayerProfileService` tests in `HexMaster.LeakOut.Players.Tests`. Endpoint tests with `WebApplicationFactory`: 200, 400 for level 0, 401 without token.

## Risks / Trade-offs

- [Ties broken by `achieved_at` require #2 to keep the original `achieved_at` when an equal score is resubmitted] → #2 only updates on a strictly higher score; called out in the tests.
- [Cross-module call per request] → one batched lookup of at most 10 ids; caching is deferred until needed.
- [Display name of a player who never signed in again stays old] → acceptable; #1 refreshes the name on every sign-in.

## Assumptions

- The endpoint requires authentication, like every API endpoint except sign-in (#1).
- Route `GET /api/levels/{level}/scores/top`; the count is fixed at 10 (no query parameter).
- `IsCurrentPlayer` is included so the client can highlight the player; not asked for in the issue.
- A level without scores returns 200 with an empty list rather than 404, because the level number is not validated against a list of known levels (the server does not know the game's level count).
- Missing display name falls back to "Unknown player".
