---
name: modular-solution-structure
description: Applies when laying out or reorganising the solution, creating a module, data or test project, choosing a project name, checking whether a project reference is allowed, or deciding whether a module should get its own API or become a separate service. HexMaster design standard.
---

# Modular solution structure (HexMaster design, accepted)

## Naming
- Name projects `{Company}.{Product}.{Module}[.{Layer}][.{Technology}]`, e.g. `HexMaster.LeakOut.Players.Abstractions` or `HexMaster.LeakOut.Players.Data.PostgreSQL`.
- Put a dot before every segment: `Players.Abstractions`, not `PlayersAbstractions`.
- Spell storage types the same way everywhere: `SqlServer`, `PostgreSQL` (not `Postgres`), `MongoDb`, `CosmosDb`, `Redis`, `TableStorage`, `Blob`.
- Name folders with the short module name only: `Players/`, not `HexMaster.LeakOut.Players/`.
- Name test files `{ClassUnderTest}Tests.cs`.

## Allowed dependency direction
Aspire → Module API → Module core → Module Abstractions ← Module Data. Any project may depend on the shared kernel / Core.

## Forbidden dependencies
- Module core → another module's core (use that module's Abstractions instead).
- Abstractions → module core (this creates a cycle).
- Tests → another module's core.
- Data project → module core (the data project references Abstractions).

ADR 0002 and ADR 0009 put repository ports in the module project, which the data project then implements. That conflicts with the last rule above; when it matters, get the full ADRs to settle it.

## Rules
- Every module **must** have a `.Tests` project (xUnit) with at least 80% line coverage.
- Abstractions projects hold record DTOs and service interfaces, have minimal dependencies, and never expose EF or other storage types.
- Business logic never goes in API endpoints or data projects.
- Use one data project per storage boundary. Never let one data project access several modules' databases.
- Never create one "God" Abstractions project for the whole solution.
- Modules must not depend on each other in a cycle.
- Put shared build settings in the root `Directory.Build.props`: `net10.0`, nullable, implicit usings, `LangVersion latest`, `TreatWarningsAsErrors`, Company and Product.

## When to split
- **New module:** when there is a distinct bounded context with its own data model and rules, or when ownership, scaling or deployment needs differ.
- **Separate API per module:** when the module needs independent scaling, deployment, ownership or release cycles. Keep a single API when modules share a release cadence, the team is small, or cross-module transactions are common.
- **Extracting to a microservice:**
  1. Make sure the module has its own data project and shares no database.
  2. Add a `.Api` project.
  3. Replace direct references with HTTP or messaging.
  4. Update the AppHost.

## Source
Distilled from HexMaster coding standard `modular-solution-structure`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `modular-solution-structure`.
