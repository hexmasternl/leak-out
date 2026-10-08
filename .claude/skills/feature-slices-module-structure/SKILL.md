---
name: feature-slices-module-structure
description: Applies when scaffolding a new module or a new feature slice and you need the concrete folder tree, file names and code templates (command, handler, DTO, registration, endpoint, tests). HexMaster structure standard; companion to feature-slices-within-modules.
---

# Feature slices module structure: scaffold (HexMaster structure, accepted)

```
src/{Module}/
  {Product}.{Module}/
    DomainModels/{Entity}.cs
    I{Entity}Repository.cs
    Features/{FeatureName}/{FeatureName}Command.cs + {FeatureName}CommandHandler.cs
    Features/{FeatureName}/{FeatureName}Query.cs   + {FeatureName}QueryHandler.cs
    Observability/{Module}Metrics.cs
    Services/  Extensions/
    {Module}ModuleRegistration.cs
  {Product}.{Module}.Abstractions/
    DataTransferObjects/{FeatureName}Request.cs, {Entity}Dto.cs
    Services/I{Module}Service.cs          (only if consumed cross-module)
  {Product}.{Module}.Data.{Store}/        ({Entity}Repository, DbContext, Migrations)
  {Product}.{Module}.Tests/
    {FeatureName}/{FeatureName}CommandHandlerTests.cs
    DomainModels/{Entity}Tests.cs
    Helpers/   Factories/{Entity}Faker.cs (Bogus)
```

## Naming
| Artifact | Name |
|---|---|
| Command / query | `{FeatureName}Command` / `{FeatureName}Query` |
| Command / query handler | `{FeatureName}CommandHandler` / `{FeatureName}QueryHandler` |
| Request DTO | `{FeatureName}Request` |
| Response | `{Entity}Dto` or `{FeatureName}Result` |
| Module registration method | `Add{Module}Module` |
| Endpoint mapping method | `Map{Feature}Endpoints` |

## Rules
1. Every feature lives under `.Features.{FeatureName}` and has at least two files.
2. Every type that crosses the module boundary lives in `Abstractions/DataTransferObjects`.
3. Commands, queries, DTOs and results are `sealed record` types.
4. Endpoints do nothing but map DTO → command/query.
5. DTOs never reference domain or EF types.
6. Each module has one registration method that wires its handlers and infrastructure, e.g. `AddScoped<ICommandHandler<X, XResult>, XCommandHandler>()`.

## Anti-patterns
- DTOs in the module project.
- Commands or queries in Abstractions.
- Logic in endpoint lambdas.
- One handler shared by several features.
- Referencing another module's implementation project.
- Using `class` instead of `sealed record` for messages.

## Handler tests
Build Moq mocks of the repository (and logger) in the test class constructor. Then cover:
- the happy path, verifying the repository call happened `Times.Once`
- a domain rule violation, which throws `DomainException`
- a null command, which throws `ArgumentNullException`

## Source
Distilled from HexMaster coding standard `feature-slices-module-structure`. Retrieve the complete text, with full code templates, from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `feature-slices-module-structure`.
