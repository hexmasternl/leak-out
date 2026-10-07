# Tasks

## 1. Migrate at startup in every environment

- [x] 1.1 In `src/HexMaster.LeakOut.Api/Program.cs`, register `PlayersDatabaseMigrator` and `ScoresDatabaseMigrator` without the `IsDevelopment()` guard, and update both migrators' XML comments so they no longer say production migrations are a deployment step. Verify with a new `HexMaster.LeakOut.Api.Tests` test that hosts `Program` in `Production` (and in `Development`), captures the registered `IHostedService` descriptors before they are removed, and finds both migrators. The test must fail with the guard still in place.
- [ ] 1.2 Verify locally against empty databases: delete the `postgres` data volume (or point the API at a fresh database), start the API with `ASPNETCORE_ENVIRONMENT=Production` and the Aspire connection strings, and check that `players`, `level_scores` and both `__EFMigrationsHistory` tables exist before `/alive` answers.

## 2. Documentation

- [ ] 2.1 Update `CLAUDE.md` (Players data project: "migrations applied at startup in every environment"), `docs/auth-setup.md` and `infrastructure/README.md`. Replace the manual "First database migration" section with a short note that the API migrates on startup and a failed migration keeps the previous revision. Verify that `grep -rn -i "migrat" CLAUDE.md docs infrastructure src --include=*.md --include=*.cs` finds no remaining claim that migrations only run in Development or are a deployment step.

## 3. Integration check

- [ ] 3.1 After the next deployment to Azure, verify the container app's new revision becomes healthy, its logs show the migrations being applied (or none pending), and `POST /api/players/sessions` with a valid server auth code returns a token instead of the `42P01` error.
