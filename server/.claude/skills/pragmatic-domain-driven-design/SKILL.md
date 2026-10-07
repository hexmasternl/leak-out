---
name: pragmatic-domain-driven-design
description: Applies when designing or changing a domain entity or aggregate, deciding whether to introduce a value object, writing handler logic that changes entity state, or adding a repository. HexMaster design standard (DRAFT).
---

# Pragmatic domain-driven design (HexMaster design, **DRAFT**)

> **Draft standard:** this guidance may still change. Treat it as provisional rather than settled policy, and check the current text when a decision depends on it.

## Rich domain models
- All business operations go through the domain model.
- Entities have:
  - `private set` properties
  - a private parameterless constructor (for EF)
  - static factory methods (`Create(...)`)
  - behavior methods (`Submit()`, `AddLine()`)
- Entities enforce their own invariants and throw `DomainException` when one is violated. An entity can never be put into an invalid state.
- Expose collections as `IReadOnlyList<T>` over a private `List<T>`, never as a public `List<T>`.
- Domain models have no public setters and no EF attributes. EF mapping uses fluent configuration in the data project.

## Handlers orchestrate, entities decide
- A handler does four things:
  1. load the aggregate
  2. call domain methods
  3. persist
  4. translate a `DomainException` into a result
- Handlers contain no business rules and never set entity properties directly.

## Aggregates and repositories
- Code outside an aggregate references only its root.
- Child entities are created through the root, using `internal` factories.
- Repositories exist for aggregate roots only, never for child entities.
- An aggregate may collect domain events for significant occurrences.

## Value objects only when they earn it
- **Use a value object for:**
  - concepts made of several fields (Address, Money, DateRange)
  - fields that depend on each other
  - values that need format validation plus behavior (EmailAddress)
  - domain arithmetic
- **Keep a primitive for:**
  - names, ages and quantities
  - plain Guid ids (no `PlayerId(Guid)` wrappers)
  - simple single-value checks; validate those in the entity

## Validation
Validate in two layers:
1. **Input/endpoint:** format, required fields and ranges.
2. **Domain:** invariants. This layer is authoritative.

## Source
Distilled from HexMaster coding standard `pragmatic-domain-driven-design` (status: draft). Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `pragmatic-domain-driven-design`.
