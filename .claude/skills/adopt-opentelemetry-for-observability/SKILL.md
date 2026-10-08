---
name: adopt-opentelemetry-for-observability
description: Applies when writing or reviewing a command/query handler, background job, outbound integration or business operation that needs tracing, metrics or logging, or when configuring telemetry exporters. HexMaster standard ADR 0008.
---

# OpenTelemetry for observability (HexMaster ADR 0008, accepted)

This is required for all backend services. Use OpenTelemetry (`System.Diagnostics` `ActivitySource` / `Meter`) for traces, metrics and logs, not per-pillar vendor SDKs.

## Automatic instrumentation (from Aspire `AddServiceDefaults()`)
- ASP.NET Core requests, HttpClient calls, database calls (EF Core, ADO.NET, Npgsql) and messaging must all be instrumented.
- Do not wrap these calls in manual activities; the automatic instrumentation already covers them.

## Activities (spans)
- Every command/query handler creates an activity named after its use case: `using var activity = ActivitySource.StartActivity("CreateOrder");`. A static `ActivitySource` named `{Product}.{Module}` is fine.
- Create activities for:
  - multi-step operations (three or more steps)
  - external calls that are not instrumented automatically
  - background jobs
  - complex domain operations
- Do not create activities for trivial getters, pure functions or hot inner loops.
- Set useful tags with domain prefixes (`player.`, `score.`).
- On success, set the status to `Ok`. On failure, call `SetStatus(ActivityStatusCode.Error, ex.Message)`, record the exception and rethrow.
- A handler decorator may add activities generically to cut boilerplate.

## Metrics
- Business operations emit metrics:
  - counters for counts (created, failed)
  - histograms for durations and values
  - gauges for point-in-time values
- Create meters through `IMeterFactory` in a module metrics class (e.g. `Observability/{Module}Metrics.cs`) and register that class in DI.
- Name metrics like `orders.created` and give each a `unit` and a `description`.
- **Keep metric tags low-cardinality.** Never use user ids, emails or GUIDs as metric dimensions; put those on traces instead.

## Logging and tags
- Log with structured message templates (`"Order {OrderId} created"`). The OTel logging provider adds the trace context for correlation.
- Follow the OTel semantic conventions (`http.*`, `db.*`, `messaging.*`).
- Never put PII, passwords or tokens in tags or logs.
- Configure exporters through environment variables (`OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_SERVICE_NAME`), not in code.

## Review rules
- A PR that adds a handler must include an activity with relevant tags.
- A PR that adds a business operation must emit success and failure metrics.
- Unit tests should verify activities with an `ActivityListener`.

## Source
Distilled from HexMaster coding standard `adopt-opentelemetry-for-observability`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `adopt-opentelemetry-for-observability`.
