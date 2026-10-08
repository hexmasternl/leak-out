# Design

## Context

See proposal.md - Why. The `Scores` module projects exist (`HexMaster.LeakOut.Scores`, `.Abstractions`, `.Data.PostgreSQL`, `.Tests`) but are empty and not yet in `src/leak-out-server.slnx`. The API host still only serves the template `/weatherforecast` endpoint.

This change builds on `players-google-play-auth` (#1), which delivers:

- the CQRS interfaces `ICommandHandler<TCommand, TResult>`, `ICommandHandler<TCommand>`, `IQueryHandler<TQuery, TResult>` and `IClock` in `HexMaster.LeakOut.Core`;
- JWT bearer authentication in the API, where the token's `sub` claim is the Google Play player id (the external SubjectId);
- `IUserResolver` in `HexMaster.LeakOut.Players.Abstractions`, which resolves a SubjectId to the internal `Guid` UserId (ADR 0010);
- a PostgreSQL server resource `postgres` in the Aspire AppHost, the Aspire Npgsql EF Core integration, and the endpoint layout `src/HexMaster.LeakOut.Api/Endpoints/{Feature}Endpoints.cs` with `/api` routes.

Applicable HexMaster standards: ADR 0002 (modular monolith), ADR 0003 (Aspire), ADR 0004 (CQRS), ADR 0005 (Minimal APIs), ADR 0007/0009 (feature slices), ADR 0010 (SubjectId to UserId at the boundary), the xUnit/Moq/Bogus testing standard and pragmatic DDD.

## Goals / Non-Goals

**Goals:**
- One feature slice that stores a best score per player per level, correct under concurrent submissions.
- A data model and index that the leaderboard features (#3 top 10, #4 scores around the player) can query efficiently.

**Non-Goals:**
- Anti-cheat or plausibility checks on scores (for example a maximum score per level).
- Score history: only the best score is kept, earlier submissions are not recorded.
- Deleting or resetting scores.
- Leaderboard queries (#3 and #4).

## Decisions

### Endpoint: `POST /api/scores` in `ScoreEndpoints.cs`

`src/HexMaster.LeakOut.Api/Endpoints/ScoreEndpoints.cs` defines `MapScoreEndpoints(this IEndpointRouteBuilder app)`, which creates the group `/api/scores` with `.WithTags("Scores")` and `.RequireAuthorization()`, and maps `POST /` named `SubmitScore`, `.Produces<SubmitScoreResult>(200)`, `.ProducesValidationProblem()`, `.Produces(401)`. `Program.cs` only calls `app.MapScoreEndpoints()`.

The endpoint:
1. binds `SubmitScoreRequest(int Level, int Score)` from the body;
2. validates it (Level ≥ 1, Score ≥ 0) and returns `TypedResults.ValidationProblem(...)` on failure, before any lookup (validation at the edge, ADR 0004);
3. reads `sub` from the principal and resolves it with `IUserResolver.ResolveAsync(subjectId, ct)`; an unresolved player returns `401 Unauthorized` (ADR 0010: the SubjectId never goes past the endpoint);
4. dispatches `SubmitScoreCommand(userId, request.Level, request.Score)` to `ICommandHandler<SubmitScoreCommand, SubmitScoreResult>`;
5. returns `TypedResults.Ok(result)`.

Alternative considered: `PUT /api/levels/{level}/scores/me` (idempotent upsert on a resource). Rejected because the issue describes a post of level and score, and because a lower score is not a replacement, so `PUT` semantics would be misleading.

The response is `200 OK` with a result, not `202 Accepted`: ADR 0004's 202 rule applies to commands without a return value, and the game needs the stored best score to show "new best" feedback.

### Feature slice `HexMaster.LeakOut.Scores.Features.SubmitScore`

- `SubmitScoreCommand(Guid UserId, int Level, int Score)` — `sealed record`, in the module project.
- `SubmitScoreCommandHandler : ICommandHandler<SubmitScoreCommand, SubmitScoreResult>` — free of HTTP concerns. It throws `ArgumentNullException` for a null command, calls `ILevelScoreRepository.SubmitAsync(userId, level, score, clock.UtcNow, ct)` and maps the outcome to `SubmitScoreResult`.
- `SubmitScoreRequest(int Level, int Score)` and `SubmitScoreResult(int Level, int BestScore, bool IsNewBest)` — `sealed record`s in `HexMaster.LeakOut.Scores.Abstractions.DataTransferObjects`.

### Domain model `LevelScore`

`HexMaster.LeakOut.Scores.DomainModels.LevelScore` has `Id` (`Guid`), `UserId` (`Guid`), `Level` (`int`), `Score` (`int`) and `AchievedAt` (`DateTimeOffset`). A factory `LevelScore.Create(userId, level, score, achievedAt)` enforces level ≥ 1 and score ≥ 0 (throws `DomainException`, defined in Core if #1 has not already added it), and `bool TryImprove(int score, DateTimeOffset achievedAt)` replaces score and moment only when `score > Score` and reports whether it did. The entity has no persistence attributes.

The endpoint validation and the domain invariants overlap on purpose: the endpoint gives the client a proper `400`, the entity guarantees no invalid state exists regardless of the caller.

### Persistence: atomic upsert in `Scores.Data.PostgreSQL`

`ILevelScoreRepository` lives in the module project (ADR 0002/0009) with one method, `Task<LevelScoreSubmission> SubmitAsync(Guid userId, int level, int score, DateTimeOffset achievedAt, CancellationToken ct)`, returning the stored best score and whether it changed.

`HexMaster.LeakOut.Scores.Data.PostgreSQL` contains `ScoresDbContext`, the EF Core configuration and the initial migration for table `level_scores`:

| Column | Type |
| --- | --- |
| `id` | `uuid`, primary key |
| `user_id` | `uuid`, not null |
| `level` | `integer`, not null, check ≥ 1 |
| `score` | `integer`, not null, check ≥ 0 |
| `achieved_at` | `timestamptz`, not null |

Indexes: unique `(user_id, level)`, and `(level, score DESC, achieved_at)` for the leaderboards of #3 and #4.

The repository implements the rule in one statement, so two simultaneous submissions can neither create two rows nor overwrite a higher score with a lower one:

```sql
INSERT INTO level_scores (id, user_id, level, score, achieved_at)
VALUES (@id, @userId, @level, @score, @achievedAt)
ON CONFLICT (user_id, level) DO UPDATE
   SET score = EXCLUDED.score, achieved_at = EXCLUDED.achieved_at
 WHERE level_scores.score < EXCLUDED.score
RETURNING score;
```

A returned row means the score was inserted or improved (`IsNewBest = true`). When the `WHERE` filters the update out, nothing is returned; the repository then reads the existing row to return the stored best score with `IsNewBest = false`.

Alternative considered: load the `LevelScore`, call `TryImprove`, save, and rely on the unique index plus a retry on conflict. Rejected as the primary path because it needs a retry loop and still races between read and write for the "higher score" check; `TryImprove` stays as the domain statement of the rule and is unit-tested, while the SQL is the concurrency-safe enforcement of the same rule.

The data project references the module project to implement `ILevelScoreRepository`, following ADR 0002/0009 (the modular-solution-structure note about this conflict is acknowledged; this repo follows the ADRs).

### Registration and orchestration

`ScoresModuleRegistration.AddScoresModule(this IServiceCollection services)` registers `ICommandHandler<SubmitScoreCommand, SubmitScoreResult>`. The data project adds `AddScoresPostgreSql(this IHostApplicationBuilder builder)`, which calls `builder.AddNpgsqlDbContext<ScoresDbContext>("scoresdb")` and registers the repository. The API calls both, and in Development applies migrations at startup the same way #1 does for the Players module.

`AppHost.cs` adds `var scoresDb = postgres.AddDatabase("scoresdb");` and `.WithReference(scoresDb).WaitFor(scoresDb)` on `hexmaster-leakout-api`. Scores gets its own database because the standard allows one data project per storage boundary and no module may read another module's tables.

The Scores projects are added to `src/leak-out-server.slnx` if #1 has not already done so.

### Tests (`HexMaster.LeakOut.Scores.Tests`)

- `DomainModels/LevelScoreTests`: create with valid and invalid input, `TryImprove` with higher, equal and lower scores.
- `SubmitScore/SubmitScoreCommandHandlerTests` (Moq repository, fake `IClock`): first score, higher, equal, lower; null command throws `ArgumentNullException`; repository called `Times.Once`.
- Endpoint tests with `WebApplicationFactory<Program>` and a mocked handler and resolver: 401 without token, 401 for an unknown player, 400 for level 0 and for a negative score, 200 with the handler's result.
- `Factories/LevelScoreFaker` (Bogus) for test data.
- Coverage of the module at least 80%.

## Assumptions

- **No upper bound on the level number.** The server has no catalogue of levels, so any level ≥ 1 is accepted. A level that does not exist in the game simply never gets scores from a genuine client.
- **Score 0 is a valid score.** The game's `FinalScore.total` is never below 0, so 0 can be a real result.
- **No plausibility check on the score.** The server trusts the authenticated client; anti-cheat is a separate concern.
- **Equal score keeps the original moment.** The earlier achiever keeps the earlier `achieved_at`, which the leaderboards use as the tie-breaker.
- **The response is `200 OK` in every accepted case**, also for a first score; `201 Created` is not used because there is no addressable score resource.
- **A player that the token names but `IUserResolver` cannot resolve gets `401`**, not auto-registration: players are registered when they sign in (#1).

## Risks / Trade-offs

- [The upsert is PostgreSQL-specific SQL] → It lives only in the PostgreSQL data project behind `ILevelScoreRepository`; the domain rule is also expressed and tested in `LevelScore.TryImprove`.
- [Forged or replayed high scores from a modified client] → Out of scope; access requires a valid token from #1, and plausibility checks can be added later without changing this contract.
- [This change cannot run before #1 is implemented] → Implement #1 first; the tasks assume its Core interfaces, authentication and Postgres resource exist.

## Migration Plan

New database and table only; nothing existing changes. Rollback is reverting the change and dropping `scoresdb`.
