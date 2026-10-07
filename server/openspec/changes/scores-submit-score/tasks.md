# Tasks

## 1. Prerequisites and project setup

- [x] 1.1 Confirm `players-google-play-auth` (#1) is implemented on `development` (Core CQRS interfaces and `IClock`, JWT authentication, `IUserResolver`, `postgres` resource in the AppHost) and verify `dotnet build src/leak-out-server.slnx` succeeds
- [x] 1.2 Make sure the four Scores projects are in `src/leak-out-server.slnx`, add the Core reference to `HexMaster.LeakOut.Scores`, the Abstractions reference where needed, and the Aspire Npgsql EF Core package to `HexMaster.LeakOut.Scores.Data.PostgreSQL`; verify the solution builds

## 2. Domain model

- [x] 2.1 Add `DomainModels/LevelScore.cs` with `Create(userId, level, score, achievedAt)` (rejects level < 1 and score < 0 with `DomainException`) and `TryImprove(score, achievedAt)`; verify `LevelScoreTests` cover valid and invalid creation and higher, equal and lower scores
- [x] 2.2 Add `Factories/LevelScoreFaker.cs` (Bogus) to the test project and verify it is used by the domain and handler tests

## 3. SubmitScore feature slice

- [x] 3.1 Add `SubmitScoreRequest` and `SubmitScoreResult` sealed records in `HexMaster.LeakOut.Scores.Abstractions/DataTransferObjects` and verify the Abstractions project has no reference to the module or EF Core
- [x] 3.2 Add `ILevelScoreRepository` (`SubmitAsync` returning the stored best score and whether it changed) to the module project
- [x] 3.3 Add `Features/SubmitScore/SubmitScoreCommand.cs` and `SubmitScoreCommandHandler.cs`, using `IClock` for the moment; verify `SubmitScoreCommandHandlerTests` cover first, higher, equal and lower score, a null command, and the repository called `Times.Once`
- [x] 3.4 Add `ScoresModuleRegistration.AddScoresModule()` registering the handler and verify a test resolves `ICommandHandler<SubmitScoreCommand, SubmitScoreResult>` from a service collection

## 4. Persistence

- [x] 4.1 Add `ScoresDbContext` with the `level_scores` configuration (columns, check constraints, unique `(user_id, level)`, index `(level, score DESC, achieved_at)`) and verify the model builds
- [x] 4.2 Add the initial EF Core migration and verify the generated SQL creates the table, the constraints and both indexes
- [x] 4.3 Implement `LevelScoreRepository.SubmitAsync` with the `INSERT ... ON CONFLICT (user_id, level) DO UPDATE ... WHERE level_scores.score < EXCLUDED.score` upsert and the fallback read; verify with an integration test against PostgreSQL (first, higher, equal, lower, and two concurrent submissions leaving one row with the highest score)
- [x] 4.4 Add `AddScoresPostgreSql()` (`AddNpgsqlDbContext<ScoresDbContext>("scoresdb")` plus the repository) and apply migrations at startup in Development; verify the API starts against an empty database and creates `level_scores`

## 5. Endpoint and orchestration

- [x] 5.1 Add `src/HexMaster.LeakOut.Api/Endpoints/ScoreEndpoints.cs` with `MapScoreEndpoints()`: group `/api/scores`, `.RequireAuthorization()`, `POST /` named `SubmitScore` that validates, resolves the `sub` claim with `IUserResolver`, dispatches the command and returns `200` with `SubmitScoreResult`; call `AddScoresModule()`, `AddScoresPostgreSql()` and `MapScoreEndpoints()` from `Program.cs`
- [x] 5.2 Add endpoint tests with `WebApplicationFactory<Program>` and mocked handler and resolver: 401 without a token, 401 for an unknown player, 400 for level 0 and for a negative score, 200 with the handler result
- [x] 5.3 Add `scoresdb` to `AppHost.cs` (`postgres.AddDatabase("scoresdb")`, `.WithReference(scoresDb).WaitFor(scoresDb)` on the API) and verify `aspire run` shows the database and the API healthy
- [ ] 5.4 Add a `POST /api/scores` sample to `HexMaster.LeakOut.Api.http` and verify it returns `200` with a token from the sign-in endpoint of #1

## 6. Verification

- [ ] 6.1 Run `dotnet test --solution src/leak-out-server.slnx` with coverage and verify all tests pass and line coverage is at least 80%
