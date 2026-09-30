## Context

Leak Out is an Android pipe-building puzzle, planned in `docs/project-plan.md`. Levels must be data, one JSON file each, so that difficulty is tuned by authoring. This change defines that data, its defaults and how it is loaded and validated. It does not run the game.

The field set follows the level file sketched in the project plan (section 2.7), with three deliberate differences: coordinates are `x`/`y`, the endpoint side is called `openSide`, and there is a `stars.one` threshold so that a win can earn 0 stars.

## Goals / Non-Goals

**Goals:**
- One JSON file per level with: id, name, grid size, start, finish, blocked cells, flow settings, stack settings, placement rules and star benchmarks.
- Central engine defaults that a level can override field by field.
- An immutable in-memory model with no Android dependency, so it is unit-testable on the JVM.
- A parser that turns JSON into the model and a validator that rejects levels that cannot be played.
- Clear, field-specific error messages for authors.

**Non-Goals:**
- Flow simulation, pipe stack generation, and the scoring formula that produces points.
- Level select UI, progress storage, and reading assets through Android APIs. This change only parses text.
- Solvability checking, and checking that the star thresholds are reachable.
- A random seed per level.

## Decisions

**JSON shape.** Grouped, camelCase keys. A full level:

```json
{
  "id": 3,
  "name": "First Crossing",
  "grid": { "columns": 7, "rows": 9 },
  "start":  { "x": 0, "y": 1, "openSide": "RIGHT" },
  "finish": { "x": 6, "y": 7, "openSide": "LEFT" },
  "blocked": [ { "x": 3, "y": 4 } ],
  "flow": {
    "initialDelayMs": 12000,
    "cellsPerSecond": 0.6,
    "reservoirSpeedFactor": 0.3,
    "speedRampPerCell": 0.0
  },
  "stack": {
    "visibleSize": 5,
    "weights": { "H": 3, "V": 3, "LT": 2, "LB": 2, "RT": 2, "RB": 2, "X": 1, "RES": 1 }
  },
  "rules": { "allowReplace": true, "replacePenaltyMs": 500 },
  "stars": { "one": 100, "two": 200, "three": 350 }
}
```

A minimal level leaves out every defaulted field:

```json
{
  "id": 1,
  "name": "First Leak",
  "grid": { "columns": 6, "rows": 4 },
  "start":  { "x": 0, "y": 1, "openSide": "RIGHT" },
  "finish": { "x": 5, "y": 2, "openSide": "LEFT" },
  "blocked": [],
  "flow": { "initialDelayMs": 15000, "cellsPerSecond": 0.5 },
  "stars": { "one": 60, "two": 100, "three": 150 }
}
```

**Required and defaulted fields.** The values that make a level what it is are required: `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow.initialDelayMs`, `flow.cellsPerSecond` and `stars`. Engine settings are optional and fall back to the defaults:

| Field | Default |
|-------|---------|
| `flow.reservoirSpeedFactor` | 0.3 |
| `flow.speedRampPerCell` | 0.0 |
| `stack.visibleSize` | 5 |
| `stack.weights.H`, `V` | 3 |
| `stack.weights.LT`, `LB`, `RT`, `RB` | 2 |
| `stack.weights.X`, `RES` | 0 |
| `rules.allowReplace` | true |
| `rules.replacePenaltyMs` | 500 |

Overrides are per field. A level can give `"stack": { "weights": { "X": 1 } }` and keep every other default. A whole group (`stack`, `rules`) can be left out. The defaults live in one `LevelDefaults` object in `core`. The parser resolves them, so a `Level` always holds concrete values and the engine never has to know which ones were defaulted. Alternative: require every field. Rejected, because every level file would repeat the same engine numbers, and retuning a default would mean editing every file.

**Stack weights replace the allowed-pipes flags.** Each of the eight sections (`H`, `V`, `LT`, `LB`, `RT`, `RB`, `X`, `RES`, the ids from the project plan) has a non-negative integer weight. A section's draw chance is its weight divided by the sum. A weight of 0 means the section never appears, so "is the cross allowed" is simply `weights.X > 0`. The six straight and corner sections must have a weight of at least 1. Cross and reservoir default to 0, so a level only uses them when it opts in. Alternative: keep `allowedPipes` booleans next to the weights. Rejected, because the two could contradict each other (`cross: false` with `X: 3`).

**No random seed.** The level does not fix the stack sequence. Every attempt draws from a fresh random source, so a retry can deal the player an easier stack. The engine takes a `Random` as a constructor argument, so that tests can still be deterministic. Consequence: whether a level can be won, and how many stars are reachable, depends on the stack of an attempt. That is accepted.

**Coordinates.** `x` is the column and `y` is the row, both zero-based, with the origin at the top-left and `y` growing downward. This matches the project plan. Alternative: one-based. Rejected because zero-based maps directly onto array indexes.

**Open side.** The start and the finish each have one open side, `openSide`, with the values `LEFT`, `RIGHT`, `TOP` and `BOTTOM`. For the start, liquid leaves through it. For the finish, liquid must enter through it. One term describes both, because in both cases it is the side of the cell that has an opening. The model type is a `Side` enum with `opposite()` and the cell offset `dx`/`dy`. The start and the finish are both an `Endpoint(position, openSide)`. The cell next to the open side is the endpoint's *facing cell*. Alternative: keep `direction`. Rejected as ambiguous, since it reads as a movement direction and means opposite things for start and finish.

**Name.** A required, non-blank display string, trimmed. It is not unique and not used as a key. The id stays the only identity.

**Grid size.** Columns and rows are each between 4 and 50 inclusive. Below 4 there is no room for a route. Above 50 the cells become too small to tap on a phone in landscape.

**Initial delay.** Integer milliseconds in `flow.initialDelayMs`, zero or greater. The unit is in the key name.

**Flow speed.** `flow.cellsPerSecond` is how many cell lengths the liquid front advances per second at base speed. It must be greater than 0 and at most 10. The upper bound catches typing mistakes such as `60` for `0.6`. The plan's typical range is 0.4 to 2.0. `flow.reservoirSpeedFactor` multiplies the speed inside a reservoir. It must be greater than 0 and at most 1, because a reservoir exists to slow the liquid. `flow.speedRampPerCell` is zero or greater.

**Stack and rules.** `stack.visibleSize` is at least 1 and at most 10. `rules.replacePenaltyMs` is zero or greater.

**Blocked cells.** An array of `{x, y}`. An empty array means none. Duplicates are rejected rather than silently merged, because a duplicate usually signals an authoring mistake.

**Star benchmarks and 0-star wins.** `stars.one`, `stars.two` and `stars.three` are positive integer point thresholds with `one < two < three`. `starsFor(points)` returns 0 to 3. A won level whose points are below `stars.one` earns 0 stars and still counts as won. Stars reward longer, more elaborate routes: a short route may finish a level without reaching a high star count. So thresholds are a design choice, and the validator does not check that they can be reached.

**Validation is separate from parsing.** Parsing checks shape and types: missing required keys, wrong types, unknown keys (including unknown weight ids), and unknown side names. Validation checks meaning: ranges, in-grid positions and the endpoint rules. Validation reports every problem it finds rather than stopping at the first.

**Endpoint validation.** For both the start and the finish:
1. The cell lies inside the grid and is not blocked.
2. The start and the finish are different cells.
3. The facing cell lies inside the grid. An open side that points at the grid edge can never be connected.
4. The facing cell is not blocked.
5. The start's facing cell is not the finish, and the finish's facing cell is not the start. Two endpoints that face each other directly would win on the first tick with no pipes placed. A start that faces the finish's closed side would leak on the first tick.

```
  valid                  rule 3                 rule 5
  ┌───┬───┬───┬───┐      ┌───┬───┬───┬───┐      ┌───┬───┬───┬───┐
  │ S→│ · │ · │ · │     ←│ S │ · │ · │ · │      │ S→│←F │ · │ · │
  ├───┼───┼───┼───┤      ├───┼───┼───┼───┤      ├───┼───┼───┼───┤
  │ · │ · │ · │←F │      │ · │ · │ · │←F │      │ · │ · │ · │ · │
  └───┴───┴───┴───┘      └───┴───┴───┴───┘      └───┴───┴───┴───┘
                         start opens onto        start and finish
                         the grid edge           face each other
```

The start and the finish may share a facing cell. One pipe section then connects them.

**Library.** kotlinx.serialization for JSON, with unknown keys rejected so that a misspelled key is an error rather than a silently ignored setting. Defaults are expressed as Kotlin default parameter values on the DTOs, taken from `LevelDefaults`.

**Location.** Model, defaults, parser and validator live in the pure Kotlin `core` module, package `nl.hexmaster.leakout.core.level`. The bundled level files live in `app/src/main/assets/levels/level_NNN.json`. The `core` test that validates every bundled level reads that folder through a path that the Gradle test task passes in as a system property, so there is only one copy of the files.

## Risks / Trade-offs

- [Changing a default in `LevelDefaults` silently changes every level that does not override it] → That is the point of a default. The bundled-level test re-validates every level after any change, and `docs/level-format.md` lists the defaults.
- [An unseeded stack means a level may be unwinnable on some attempts] → Accepted game design: a retry deals a new stack. Replacing sections (on by default) softens bad draws.
- [Star thresholds may be unreachable with some stacks or grids] → Accepted. Thresholds are tuned by play-testing, not validated.
- [Rejecting unknown keys makes older app versions fail on newer level files] → Levels ship inside the app, so the files and the parser always come from the same build.
- [Validation cannot prove a level is solvable] → Out of scope.
- [`game-level-history` rejects 0 stars today] → That change must accept 0 to 3 stars so a 0-star win can be recorded.
