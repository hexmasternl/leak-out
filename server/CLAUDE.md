# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

A .NET 10 ASP.NET Core minimal API orchestrated by .NET Aspire, organised as a modular monolith (`src/Core`, `src/Players`, `src/Scores`). Players sign in with Google Play Games (`POST /api/players/sessions`); the Scores module is still empty. The solution file is `src/leak-out-server.slnx` (the new XML `.slnx` format, not `.sln`).

## Commands

Run from `src/`:

```bash
dotnet build leak-out-server.slnx                                   # build everything
dotnet run --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost   # run via Aspire (dashboard + API)
aspire run                                                          # alternative, from src/HexMaster.LeakOut.Aspire (aspire.config.json points at the AppHost)
dotnet run --project HexMaster.LeakOut.Api                          # run the API standalone on http://localhost:5090
dotnet test --solution leak-out-server.slnx                         # all tests (xUnit v3 on Microsoft.Testing.Platform)
dotnet test --project Players/HexMaster.LeakOut.Players.Tests --filter-method "*SomeTestName*"   # single test
dotnet ef migrations add <Name> --project Players/HexMaster.LeakOut.Players.Data.PostgreSQL --output-dir Migrations
```

`HexMaster.LeakOut.Api/HexMaster.LeakOut.Api.http` holds sample requests against the standalone API.

## Secrets and configuration

The AppHost needs three parameters in its user secrets: `Parameters:google-client-id` and `Parameters:google-client-secret` (the **web** OAuth client of the Google Cloud project linked to the game) and `Parameters:authentication-signing-key` (≥ 32 bytes). It passes them to the API as `GooglePlayGames:ClientId`, `GooglePlayGames:ClientSecret` and `Authentication:SigningKey`; the API fails at startup when one is missing. The game's `serverClientId` must be the same web client id. Set them with `dotnet user-secrets set "Parameters:<name>" "<value>" --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost`. Full setup in `docs/auth-setup.md`.

## Architecture

- **HexMaster.LeakOut.Api** — the single API host (minimal APIs + `Microsoft.AspNetCore.OpenApi`). `Program.cs` only composes: ServiceDefaults, `AddPlayersModule()`, `AddPlayersPostgreSqlData()`, JWT bearer authentication with a fallback policy that requires an authenticated user, and `Map{Feature}Endpoints()` from `Endpoints/`. Anonymous endpoints opt out with `AllowAnonymous()`. Protected endpoints that act for a player add `.RequirePlayer()` and read the internal id with `httpContext.GetPlayerId()` (`Authentication/PlayerIdentityExtensions.cs`); the token's `sub` (the Google Play Games player id) never reaches commands, queries or other modules. The OpenAPI document is only mapped in Development.
- **Core** (`HexMaster.LeakOut.Core`) — `ICommandHandler<,>`, `ICommandHandler<>`, `IQueryHandler<,>`, `IClock`, `DomainException`. No web or EF references.
- **Players** — `HexMaster.LeakOut.Players` (domain `Player`, `Features/SignInPlayer`, `GooglePlayGamesClient`, `JwtAccessTokenIssuer`, `UserResolver`, `PlayersModuleRegistration`), `.Abstractions` (DTOs, `IUserResolver`), `.Data.PostgreSQL` (EF Core `PlayersDbContext`, `players` table, migrations applied at startup in Development only), `.Tests`.
- **HexMaster.LeakOut.Aspire.AppHost** (Aspire SDK 13.x) — local orchestration. Registers PostgreSQL (`postgres`, data volume) with database `playersdb`, the auth parameters, and the API as resource `hexmaster-leakout-api` in `AppHost.cs`. New services, databases, caches, etc. are added here and referenced into the API with `.WithReference(...)`.
- **HexMaster.LeakOut.Aspire.ServiceDefaults** — shared cross-cutting setup (`Extensions.cs`): OpenTelemetry (logs/metrics/traces, OTLP exporter when `OTEL_EXPORTER_OTLP_ENDPOINT` is set, Azure Monitor when `APPLICATIONINSIGHTS_CONNECTION_STRING` is set), service discovery, standard HTTP resilience on all `HttpClient`s, and health checks. `/health` and `/alive` are mapped **in every environment** as anonymous endpoints, because the Azure Container Apps startup, liveness (`/alive`) and readiness (`/health`) probes depend on them; they are not traced. Never put them behind `IsDevelopment()`. Any new service project should reference this project and call the same two extension methods.

## Git workflow

`main` is production; `development` (from `main`) is the integration branch. Issues and OpenSpec changes are implemented on a branch from `development` in a worktree under `D:\projects\github.com\hexmasternl\leak-out-server-worktrees`, with frequent small commits, an 80% coverage gate, and a PR into `development`. Only proposing and archiving OpenSpec changes is committed directly to `development`. Full rules in the `git-workflow` skill (`.claude/skills/git-workflow/SKILL.md`).

## Conventions

This is a HexMaster C# codebase: nullable reference types and implicit usings are enabled in every project. The project-scoped `hexmaster-coding-standards` MCP server (`.mcp.json`, tools `list_documents`, `get_document`, `find_documents_by_tag`, `recommend_skills`) defines the house style — consult it when writing or reviewing C#. Do not apply Lodder Software standards (the `lodder-csharp` plugin) here; they don't apply to this project.
