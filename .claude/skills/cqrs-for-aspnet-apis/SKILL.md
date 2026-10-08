---
name: cqrs-for-aspnet-apis
description: Applies when adding or changing an HTTP endpoint and the application logic behind it, writing a command, query, handler or result DTO, or touching the shared ICommandHandler/IQueryHandler interfaces in Core. HexMaster standard ADR 0004.
---

# Lightweight CQRS for ASP.NET APIs (HexMaster ADR 0004, accepted)

The recommended style: each request becomes a command or query that a dedicated handler processes.

## Rules
- Map HTTP payloads (body or parameters) into C# `record` request models.
- Commands and queries are immutable `record`s that express intent.
- Handlers are injected through DI and are **free of HTTP concerns**: no `HttpContext` and no `IResult`.
- Handlers return DTO records and never expose domain entities.
- When a command succeeds without a return value (`ICommandHandler<TCommand>`), the endpoint returns **202 Accepted**.
- Validate at the edge, in the endpoint or a filter, before calling the handler.
- Keep each feature's request, command, result and handler together (a vertical slice).

## Shared base types
- Define the handler interfaces once, in the `Core` project, which every module references:
  - `ICommandHandler<TCommand, TResult>`
  - `ICommandHandler<TCommand>`
  - `IQueryHandler<TQuery, TResult>`
  - optionally `ICommand` / `IQuery` marker interfaces
- Core must not depend on web frameworks or infrastructure.
- Modules must not redefine these interfaces.

```csharp
public interface ICommandHandler<TCommand, TResult> { Task<TResult> Handle(TCommand command, CancellationToken ct); }
public interface ICommandHandler<TCommand> { Task Handle(TCommand command, CancellationToken ct); }
public interface IQueryHandler<TQuery, TResult> { Task<TResult> Handle(TQuery query, CancellationToken ct); }
```

## Source
Distilled from HexMaster coding standard `cqrs-for-aspnet-apis`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `cqrs-for-aspnet-apis`.
