## Context

Leak Out is an Android pipe-building puzzle, planned in `docs/project-plan.md`. The repository has no code yet. Levels must be data, one JSON file each, so that difficulty is tuned by authoring. This change defines that data and how it is loaded and validated. It does not run the game.

The field set follows the request. `docs/project-plan.md` sketches a richer level file (rules, stack weights, replace penalty). This change deliberately keeps to the smaller set the request names and leaves the rest for later changes.

## Goals / Non-Goals

**Goals:**
- One JSON file per level with the fields: id, grid size, start, finish, initial delay, flow speed, allowed pipes, blocked cells, star benchmarks.
- An immutable in-memory model with no Android dependency, so it is unit-testable on the JVM.
- A parser that turns JSON into the model and a validator that rejects levels that cannot be played.
- Clear, field-specific error messages for authors.

**Non-Goals:**
- Flow simulation, pipe stack generation, scoring, and the mapping from flow speed to real distance per second.
- Level select UI, progress storage, and loading levels from Android assets. This change only parses text.
- Solvability checking of a level.
- Per-section stack weights, replace rules and reservoir speed factor.

## Decisions

**JSON shape.** Flat and explicit, camelCase keys:

```json
{
  "id": 3,
  "grid": { "columns": 7, "rows": 9 },
  "start":  { "x": 0, "y": 1, "direction": "RIGHT" },
  "finish": { "x": 6, "y": 7, "direction": "LEFT" },
  "initialDelayMs": 12000,
  "flowSpeed": 0.4,
  "allowedPipes": { "cross": true, "reservoir": false },
  "blocked": [ { "x": 3, "y": 4 } ],
  "stars": { "one": 100, "two": 200, "three": 350 }
}
```

**Coordinates.** `x` is the column and `y` is the row, both zero-based, origin at the top-left, `y` growing downward. This matches the project plan. Alternative considered: one-based coordinates. Rejected because zero-based maps directly onto array indexes and avoids off-by-one conversions.

**Direction.** The `direction` of a start is the side the liquid exits through. The `direction` of a finish is the side the liquid must enter through. Values are `LEFT`, `RIGHT`, `TOP`, `BOTTOM`, the same names as the project plan. Alternative: `UP` and `DOWN`. Rejected to stay consistent with the plan's section names such as "left to top".

**Initial delay unit.** Stored as integer milliseconds in `initialDelayMs`. The unit is in the key name so no author has to guess. Alternative: seconds as a float. Rejected because the engine ticks in milliseconds and integers avoid rounding.

**Flow speed.** A float where `0 < flowSpeed <= 1`. Exactly zero is rejected because liquid that never moves makes the level unwinnable and untimeable. The model stores the raw value. How `flowSpeed` becomes cells per second is the engine's decision, so the level format does not have to change if the engine is retuned.

**Allowed pipes.** Only `cross` and `reservoir` are listed. Straight and corner sections are always on the stack per the game rules, so they are not configurable. Both keys are required so an author must make an explicit choice.

**Blocked cells.** An array of `{x, y}`. Empty array means none. Duplicates are rejected rather than silently merged, because a duplicate usually signals an authoring mistake.

**Star benchmarks.** `stars.one`, `stars.two` and `stars.three` are point thresholds, each a positive integer with `one < two < three`. Reaching a threshold earns that many stars. The scoring formula that produces points is out of scope.

**Validation is separate from parsing.** Parsing checks shape and types (missing keys, wrong types, unknown direction names). Validation checks meaning (ranges, in-grid positions, start not on finish, port not facing a wall or blocked cell). Both report every problem found rather than stopping at the first, so an author fixes a file in one pass. Alternative: throw on the first error. Rejected as slower for authors.

**Validation rules for positions.** Start, finish and every blocked cell must lie inside the grid. Start and finish must not share a cell. Neither may sit on a blocked cell. The cell that the start exits into, and the cell that feeds the finish, must be inside the grid and not blocked, since otherwise the level is lost or won by nothing on the first tick.

**Library.** kotlinx.serialization for JSON, with unknown keys rejected, so a misspelled key is an error instead of a silently ignored setting. Alternatives: Gson or Moshi. Rejected because kotlinx.serialization is Kotlin-native, works in a pure JVM module, and matches the project plan.

**Location.** Model, parser and validator live in a pure Kotlin module with no Android imports, package `nl.hexmaster.leakout.core.level`, as in the project plan. If the Android project has not been scaffolded when this is implemented, the scaffold task creates the module.

## Risks / Trade-offs

- [The field set is smaller than the plan's sketch and later changes may need more fields] → Unknown keys are rejected today, so adding an optional field later is an explicit, versioned change to the model, not an accident.
- [Flow speed has no fixed real-world meaning until the engine exists] → The spec only fixes the range and stores the raw value, and the engine change owns the mapping.
- [Rejecting unknown keys makes older app versions fail on newer level files] → Levels ship inside the app, so files and parser always come from the same build.
- [Validation cannot prove a level is solvable] → Out of scope. A later headless solver test can cover it.
