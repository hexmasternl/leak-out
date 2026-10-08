---
name: adopt-dotnet-10
description: Applies when creating a new project, editing a .csproj or Directory.Build.props, choosing a target framework, or deciding which C# language features may be used. HexMaster standard ADR 0001.
---

# .NET 10 as target framework (HexMaster ADR 0001, accepted)

- Every project targets `net10.0` (`<TargetFramework>net10.0</TargetFramework>`). New projects must not target older frameworks.
- .NET 10 is the LTS baseline (3 years of support). C# 14 language features are allowed and encouraged where they make code clearer and safer.
- Do not move to a newer framework ad hoc. A move to .NET 12 LTS is decided by a superseding ADR.
- Set the framework once in `Directory.Build.props` rather than in each project.

## Source
Distilled from HexMaster coding standard `adopt-dotnet-10`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `adopt-dotnet-10`.
