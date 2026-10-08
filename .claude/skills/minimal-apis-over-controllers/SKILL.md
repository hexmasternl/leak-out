---
name: minimal-apis-over-controllers
description: Applies when adding HTTP endpoints, setting up OpenAPI/API documentation UI, adding endpoint validation or filters, or when tempted to add an MVC controller. HexMaster standard ADR 0005.
---

# Minimal APIs over controllers (HexMaster ADR 0005, accepted)

## Rules
- Build new APIs with Minimal APIs and do not add `[ApiController]` controllers. Existing controller projects may stay as they are.
- Use the built-in OpenAPI support (`builder.Services.AddOpenApi()` + `app.MapOpenApi()`) with Scalar for the UI (`app.MapScalarApiReference()` from `Scalar.AspNetCore`). **Do not use Swashbuckle or Swagger.**
- Group related endpoints with `MapGroup("/route")`, one extension method per group.
- Keep endpoint lambdas thin; they delegate to CQRS handlers. Request and response models are records.
- Do **shallow validation** at the edge, in endpoint filters or with manual checks: required fields, format and ranges, answered with 400 / `Results.ValidationProblem`.
- Business invariants belong to the domain model, which is the authoritative check.
- Add cross-cutting concerns (logging, telemetry, error handling) with `.AddEndpointFilter<T>()`.
- Return responses through the `Results` / `TypedResults` helpers (`Ok`, `NotFound`, `Accepted`, `Created`).

## Source
Distilled from HexMaster coding standard `minimal-apis-over-controllers`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `minimal-apis-over-controllers`.
