---
name: minimal-api-endpoint-organization
description: Applies when adding, moving or registering Minimal API endpoints, creating an Endpoints file, or when Program.cs starts accumulating MapGet/MapPost calls. HexMaster structure standard.
---

# Minimal API endpoint organization (HexMaster structure, accepted)

## Files and registration
- Give each base route or feature its own file under `Endpoints/`, named `{Feature}Endpoints.cs` (for example `PlayerEndpoints.cs` for `/players`).
- Each file is a `public static class` with a `Map{Feature}Endpoints(this IEndpointRouteBuilder app)` method. The method creates a `MapGroup(...)`, maps the routes, and **returns the builder** so calls can be chained.
- `Program.cs` only composes (`app.MapPlayerEndpoints();`). Never define endpoints directly in `Program.cs`.
- Name non-resource features by what they do: `AdminEndpoints.cs`, `WebhookEndpoints.cs`.

## Inside a group
- Apply metadata and cross-cutting concerns at the group level: `.WithTags()`, OpenAPI metadata, `.RequireAuthorization()`, `.AddEndpointFilter<T>()`.
- Give each endpoint `.WithName()` and `.Produces<T>()` metadata.
- Endpoint lambdas, or private static handler methods, only bind the request, call the handler and map the result.
- Keep nested sub-resources in the parent file. When they grow large, split them into their own file (e.g. `PlayerScoreEndpoints.cs`).
- Don't put unrelated routes in one method, and keep group nesting shallow.

## Testing
Test endpoints with `WebApplicationFactory<TProgram>` and mocked handlers.

## Source
Distilled from HexMaster coding standard `minimal-api-endpoint-organization`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `minimal-api-endpoint-organization`.
