# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Early scaffold: a .NET 10 ASP.NET Core minimal API orchestrated by .NET Aspire. `Program.cs` still contains only the template `/weatherforecast` endpoint; no domain code or test projects exist yet. The solution file is `src/leak-out-server.slnx` (the new XML `.slnx` format, not `.sln`).

## Commands

Run from `src/`:

```bash
dotnet build leak-out-server.slnx                                   # build everything
dotnet run --project HexMaster.LeakOut.Aspire/HexMaster.LeakOut.Aspire.AppHost   # run via Aspire (dashboard + API)
aspire run                                                          # alternative, from src/HexMaster.LeakOut.Aspire (aspire.config.json points at the AppHost)
dotnet run --project HexMaster.LeakOut.Api                          # run the API standalone on http://localhost:5090
dotnet test leak-out-server.slnx                                    # once test projects exist
dotnet test --filter "FullyQualifiedName~SomeTestName"              # single test
```

`HexMaster.LeakOut.Api/HexMaster.LeakOut.Api.http` holds sample requests against the standalone API.

## Architecture

- **HexMaster.LeakOut.Api** — the web API (minimal APIs + `Microsoft.AspNetCore.OpenApi`). Calls `builder.AddServiceDefaults()` and `app.MapDefaultEndpoints()` from ServiceDefaults. The OpenAPI document is only mapped in Development.
- **HexMaster.LeakOut.Aspire.AppHost** (Aspire SDK 13.x) — local orchestration. Registers the API as resource `hexmaster-leakout-api` in `AppHost.cs`. New services, databases, caches, etc. are added here and referenced into the API with `.WithReference(...)`.
- **HexMaster.LeakOut.Aspire.ServiceDefaults** — shared cross-cutting setup (`Extensions.cs`): OpenTelemetry (logs/metrics/traces, OTLP exporter when `OTEL_EXPORTER_OTLP_ENDPOINT` is set), service discovery, standard HTTP resilience on all `HttpClient`s, and health checks. `/health` and `/alive` are mapped **only in Development**. Any new service project should reference this project and call the same two extension methods.

## Conventions

This is a HexMaster C# codebase: nullable reference types and implicit usings are enabled in every project. The project-scoped `hexmaster-coding-standards` MCP server (`.mcp.json`, tools `list_documents`, `get_document`, `find_documents_by_tag`, `recommend_skills`) defines the house style — consult it when writing or reviewing C#. Do not apply Lodder Software standards (the `lodder-csharp` plugin) here; they don't apply to this project.
