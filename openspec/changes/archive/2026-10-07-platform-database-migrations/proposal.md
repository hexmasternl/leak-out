# Proposal

## Why

The deployed API fails every player sign-in with `42P01: relation "players" does not exist`. The API applies EF Core migrations at startup only in Development (`Program.cs`). The `platform-azure-infrastructure` change deferred production migrations to a pipeline step that was never built, so `playersdb` and `scoresdb` in Azure have no tables. The only workaround is the manual `dotnet ef database update` in `infrastructure/README.md`, which someone has to remember after every deployment that adds a migration.

## What Changes

- The API applies pending EF Core migrations of `playersdb` and `scoresdb` at startup in **every** environment, not only in Development. A new revision does not accept traffic until both databases are up to date.
- A failed migration stops the API from starting. In Azure Container Apps the new revision then fails its startup probe, and the previous revision keeps serving.
- Several replicas starting at the same time migrate safely. EF Core 10 serializes `Migrate` with a database lock.
- Documentation drops the rule that production migrations are a deployment step: `CLAUDE.md`, `infrastructure/README.md` (the manual "First database migration" section), `docs/auth-setup.md` and the XML comments of `PlayersDatabaseMigrator` and `ScoresDatabaseMigrator`.
- Out of scope: a separate migration job or EF Core migration bundle in the deploy workflow, and a separate least-privilege database user for the API. The API already connects as the server administrator.

## Capabilities

### New Capabilities
- `database-migrations`: When and how the server brings its PostgreSQL schemas up to date, in every environment.

### Modified Capabilities

## Impact

- **Code**: `src/HexMaster.LeakOut.Api/Program.cs` registers `PlayersDatabaseMigrator` and `ScoresDatabaseMigrator` without the `IsDevelopment()` guard. The two migrator classes change only their comments.
- **Tests**: `HexMaster.LeakOut.Api.Tests` checks that both migrators are registered outside Development.
- **Docs**: `CLAUDE.md`, `infrastructure/README.md`, `docs/auth-setup.md`.
- **Runtime**: each start, including a cold start after scaling from zero, runs one query against `__EFMigrationsHistory` per database before the API listens. That fits within the startup probe's 50-second window.
- **Supersedes** the follow-up in `openspec/changes/platform-azure-infrastructure/design.md` ("Databases are empty after deployment").
