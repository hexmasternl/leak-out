---
name: feature-slices-within-modules
description: Applies when adding a feature (command or query) to a module such as Players or Scores, creating request/response DTOs, mapping an endpoint payload to a command/query, or registering a module's handlers. HexMaster standard ADR 0009.
---

# Feature slices within module projects (HexMaster ADR 0009, accepted)

This is required.

## Placement
| Item | Location |
|---|---|
| Feature namespace and folder | `{Product}.{Module}.Features.{FeatureName}` in the module project |
| Command or query record | the feature folder; it is **internal to the module and never goes in Abstractions** |
| Handler | the feature folder, named `{FeatureName}CommandHandler` or `{FeatureName}QueryHandler`, implementing the Core `ICommandHandler` / `IQueryHandler` |
| Repository port (`I{Entity}Repository`) | the module project root, not Abstractions |
| Domain entities | `DomainModels/` |
| Metrics and ActivitySource | `Observability/` |
| DI registration | `{Module}ModuleRegistration.cs`, exposing `Add{Module}Module(this IServiceCollection)` |
| DTOs (API requests and responses) | `{Product}.{Module}.Abstractions.DataTransferObjects` (the namespace is `DataTransferObjects`, not `Dtos`) |
| Cross-module service ports | `Abstractions/Services/` |

## DTO rules
- DTOs are `sealed record`s that carry only data: no behavior, and no ORM or serializer attributes.
- DTO names end in `Request` (incoming), `Response` or `Dto` (outgoing), or `Event`.
- DTOs used by more than one module go in a shared Abstractions project.

## Request flow
1. The endpoint receives a `...Request` DTO from the HTTP request.
2. The endpoint **maps the DTO to a command or query**.
3. The handler runs the use case and returns a result DTO.
4. The endpoint maps that result to an HTTP result.

## Other rules
- Every handler starts with `ArgumentNullException.ThrowIfNull(command)`.
- Other modules and the host depend only on a module's Abstractions project, never on its implementation project.
- An Abstractions project must not reference its module project.

## Source
Distilled from HexMaster coding standard `feature-slices-within-modules`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `feature-slices-within-modules`.
