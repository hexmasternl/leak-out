## Why

Leak Out is a multi-level game, and every level must be described as data rather than code so that difficulty can grow by authoring JSON, not by changing the app. No level model exists yet, and the game engine, level select and progression all depend on one.

## What Changes

- Introduce a level definition stored as one JSON file per level.
- A level has a unique numeric id (1, 2, 3, ...), a display name and a grid size (columns and rows), between 4 by 4 and 50 by 50 cells.
- A level has a start and a finish, each with a cell (x, y) and an open side: the side of the cell through which liquid leaves the start or enters the finish.
- A level has flow settings: an initial delay before liquid starts flowing, a base speed in cells per second, a reservoir speed factor (default 0.3) and an optional speed ramp per filled cell.
- A level has stack settings: the number of visible sections and a weight per pipe section. Weights decide how often each section is drawn. Straight and corner sections always have a weight of at least 1; cross and reservoir are off unless a level gives them a weight.
- A level has placement rules: whether replacing an unfilled pipe is allowed and the time penalty for doing so.
- Engine settings (reservoir speed factor, speed ramp, visible stack size, stack weights, replace rules) have central defaults. A level may leave them out, or override any of them individually.
- A level has an array of blocked cells where no pipe section can be placed.
- A level has star benchmarks: three point thresholds for one, two and three stars. A level can be won with 0 stars. Stars reward longer, more sophisticated routes, not merely finishing.
- The level carries no random seed. The stack is drawn fresh on every attempt.
- Add parsing and validation so that malformed or unplayable levels are rejected with clear errors, including a start or finish whose open side faces the grid edge, a blocked cell, or the other endpoint.

## Capabilities

### New Capabilities
- `game-level-model`: The JSON level definition, its fields, defaults and value ranges, parsing into an in-app model, and validation of a level's internal consistency.

### Modified Capabilities

None. No specs exist yet.

## Impact

- New code in the pure Kotlin `core` module (`nl.hexmaster.leakout.core.level`): level model classes, engine defaults, a JSON parser and a validator, with unit tests.
- New JSON level files under the app's `assets/levels/`, plus at least one sample level used by tests.
- A JSON serialization dependency (kotlinx.serialization) is needed.
- Later work depends on this model: the flow engine, the pipe stack, level select and star scoring.
- `game-level-history` currently stores 1-3 stars and rejects 0. A 0-star win must be recordable there, so that change needs to align.
