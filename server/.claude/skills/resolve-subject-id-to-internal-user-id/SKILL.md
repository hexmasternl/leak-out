---
name: resolve-subject-id-to-internal-user-id
description: Applies when adding authentication, reading the JWT `sub` claim, registering players/users from an identity provider, or adding any command, query, entity, DTO, stored record or event that references a user or player. HexMaster standard ADR 0010.
---

# Resolve external SubjectId to internal UserId at the API boundary (HexMaster ADR 0010, accepted)

## Resolving the id
- The endpoint resolves the identity provider's `sub` claim (the SubjectId) to an internal `UserId` **before it dispatches any command or query**.
- Resolve it with `IUserResolver.ResolveAsync(subjectId, ct)`.
- If no user exists for the SubjectId, either return **401 Unauthorized** or create the user, depending on the registration flow.
- Cache resolved ids per SubjectId, in the request context or a distributed cache, so each request doesn't need a lookup.

## The UserId type
Use `Guid` for `UserId` in every new module. Use `int` or `long` only where an existing schema requires it.

## Where the SubjectId may appear
It may appear only in endpoint code and in the `IUserResolver` implementation. It must **never** appear in:
- domain model properties
- commands or queries
- Abstractions DTOs
- stored data records (EF or Table Storage entities)
- domain or integration events

## Who owns the mapping
- Only the Users module stores the SubjectId → UserId mapping. In this repo that is most likely the Players module.
- Other modules do not copy the mapping or resolve ids inline; they refer to users by `UserId` only.

```csharp
var subjectId = principal.FindFirstValue("sub") ?? throw new UnauthorizedAccessException();
var userId = await userResolver.ResolveAsync(subjectId, ct);
var result = await handler.Handle(new SubmitScoreCommand(userId, req.Score), ct);
```

## Source
Distilled from HexMaster coding standard `resolve-subject-id-to-internal-user-id`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `resolve-subject-id-to-internal-user-id`.
