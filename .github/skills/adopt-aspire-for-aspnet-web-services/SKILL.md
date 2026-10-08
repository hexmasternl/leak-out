---
name: adopt-aspire-for-aspnet-web-services
description: Applies when adding a web-facing service, database, cache, broker or other dependency to the solution, wiring it into the AppHost, or adding telemetry, health checks, resilience or service discovery to an ASP.NET project. HexMaster standard ADR 0003.
---

# Use .NET Aspire for ASP.NET web services (HexMaster ADR 0003, accepted)

This is a strong recommendation, not a mandate, for every web-facing ASP.NET service (APIs, Blazor, gRPC). It is optional for domain-only libraries, console tools and test harnesses.

## Rules
- Orchestrate services and infrastructure (Postgres, Redis, brokers, AI services) in the Aspire AppHost. Reference resources into projects with `.WithReference(...)` and `.WaitFor(...)`.
- Every web project calls `builder.AddServiceDefaults()` before `Build()` and `app.MapDefaultEndpoints()` after it.
- Take telemetry, health checks, resilience and service discovery from Aspire ServiceDefaults **before** writing custom equivalents.
- The minimum adoption set is:
  - the AppHost plus service defaults
  - OpenTelemetry logs, metrics and traces
  - health checks for each service and its dependencies
  - resilience (retry, timeout, circuit breaker) on outbound calls
  - service discovery instead of hardcoded URLs
- Never hardcode service endpoints. Use the environment variables and connection strings that Aspire injects.
- Name the Aspire projects `Company.Product.Aspire.AppHost` and `Company.Product.Aspire.ServiceDefaults`, and keep them in the Aspire folder under `src/`.
- Deviating from the Aspire defaults is allowed, but document the deviation in an ADR that references ADR 0003.

```csharp
var postgres = builder.AddPostgres("postgres");
var db = postgres.AddDatabase("playersdb");
builder.AddProject<Projects.HexMaster_LeakOut_Api>("hexmaster-leakout-api")
    .WithReference(db)
    .WaitFor(db);
```

## Source
Distilled from HexMaster coding standard `adopt-aspire-for-aspnet-web-services`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `adopt-aspire-for-aspnet-web-services`.
