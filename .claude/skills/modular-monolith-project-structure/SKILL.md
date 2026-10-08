---
name: modular-monolith-project-structure
description: Applies when adding a new module or project to the solution, naming a project or folder, adding a project reference between modules, or deciding where a type (entity, DTO, repository interface, shared contract) belongs. HexMaster standard ADR 0002.
---

# Modular monolith project structure (HexMaster ADR 0002, accepted)

## Top-level `src/` folders
| Folder | Contents |
|---|---|
| `App/` | the frontend |
| `Aspire/` | AppHost and ServiceDefaults |
| `Core/` | solution-wide CQRS interfaces (`ICommandHandler<,>`, `IQueryHandler<,>`, `IClock`, ...) |
| `{Module}/` | one folder per bounded context, with a short name (e.g. `Players/`, `Scores/`) |
| `Shared/` or `IntegrationMessages/` | optional cross-module integration event contracts |

## Projects in each module folder (`src/{Module}/`)
| Project | Contents |
|---|---|
| `Company.Product.{Module}` | domain entities, feature slices, handlers |
| `Company.Product.{Module}.Abstractions` | public contracts (DTOs, cross-module service ports) |
| `Company.Product.{Module}.Api` | minimal API host; the standard is one per web-exposed module, not shared |
| `Company.Product.{Module}.Data.{Store}` | persistence adapter (optional) |
| `Company.Product.{Module}.Tests` | unit and integration tests |
| `Company.Product.{Module}.Integrations` | integration event handlers (optional) |

## Boundary rules
- Never share domain entities across modules. Modules communicate only through Abstractions interfaces and DTOs, or through integration events.
- Infrastructure types such as `DbContext` stay inside the data project.
- Domain projects are persistence-agnostic: no ORM attributes and no SDK references.
- **Repository interfaces (`IXRepository`) live in the module implementation project**, not in Abstractions. Only cross-module service contracts go in Abstractions.
- Each module exposes one DI registration extension (`services.Add{Module}Module()`), which the API calls.
- Start simple. Add an Abstractions project when another module needs a stable contract.
- Avoid a "shared dumping ground" project.
- A little DTO duplication is better than centralizing too early.

## Source
Distilled from HexMaster coding standard `modular-monolith-project-structure`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `modular-monolith-project-structure`.
