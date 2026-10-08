---
name: unit-testing-with-xunit-moq-bogus
description: Applies when writing or reviewing unit tests, creating test data or test factories, mocking a dependency, adding a package to a test project, or checking coverage. HexMaster testing standard.
---

# Unit testing with xUnit, Moq and Bogus (HexMaster design, accepted)

## Libraries
- Write all unit tests with **xUnit**.
- Mock interfaces and collaborators with **Moq**.
- Generate realistic test data with **Bogus**.
- Do **not** add other test libraries (assertion libraries, snapshot testing, other mocking frameworks or data generators) without approval through an ADR or an issue/PR discussion.

## Test data
- Create test objects through factory methods or builder classes (e.g. `Factories/PlayerFactory.cs` or a `PlayerFaker`). These use Bogus for plausible defaults and accept optional overrides.
- Don't scatter ad-hoc object construction across tests.
- Seed Bogus (`Randomizer.Seed = new Random(1234)`) whenever a test asserts on generated values. Never assert on unseeded random data.

## Mocking
Test domain logic with real domain objects rather than heavy mocking. Mock ports such as repositories, not domain behavior.

```csharp
var repo = new Mock<IPlayerRepository>();
repo.Setup(r => r.AddAsync(It.IsAny<Player>(), It.IsAny<CancellationToken>())).Returns(Task.CompletedTask);
```

## Coverage and layout
- Keep unit test coverage at **80% or higher**, enforced in CI with a coverage collector.
- Test projects target `net10.0`.
- Mirror the module's feature folders in the test project, and name test files `{ClassUnderTest}Tests.cs`.

## Source
Distilled from HexMaster coding standard `unit-testing-with-xunit-moq-bogus`. Retrieve the complete text from the HexMaster coding standards MCP server (`hexmaster-coding-standards`) with its `get_document` tool, id `unit-testing-with-xunit-moq-bogus`.
