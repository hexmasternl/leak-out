---
name: vertical-slice-architecture
description: Applies when implementing a new feature or use case, deciding whether code belongs in a slice or in shared code, adding validation, or when tempted to add methods to a broad service class. HexMaster standard ADR 0007.
---

# Vertical slice architecture (HexMaster ADR 0007, accepted)

This is required. Organize code by feature or use case, not by technical layer. In this modular monolith, `feature-slices-within-modules` (ADR 0009) defines the physical layout and wins wherever the two differ.

## What goes in a slice
- **Every new feature is a new slice.** Never add a feature as a method on an existing broad service class.
- A slice holds its command or query, its handler, its slice-only result type and its slice-only validation.
- Tests live in the module's separate `.Tests` project, in folders that mirror the feature folders.
- A result type used only by its own slice (e.g. `CreateOrderResult`) can stay in the feature folder. Move it to the Abstractions DTOs once the API project or another module uses it.

## Sharing between slices
- Keep dependencies between slices to a minimum. Share only through ports:
  - repositories and `IClock` at the module root or in `Core`
  - domain entities in `DomainModels/`
  - DTOs in `.Abstractions/DataTransferObjects/`
- Do not share handler logic or "helper handlers" between slices, and keep each slice's validation inside that slice.
- Move a pattern into `Core` only when three or more slices genuinely share it.

## Adapters and validation
- Adapters such as endpoints and message handlers stay thin.
- Validation has two levels:
  1. **Endpoint filter (shallow):** checks presence, format and range, and returns 400.
  2. **Domain model (authoritative):** a factory or behavior method throws `DomainException`.
- Prefer registering handlers directly in DI. Use a mediator library only when you need pipeline behaviors.

## Source
Distilled from HexMaster coding standard `vertical-slice-architecture`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `vertical-slice-architecture`.
