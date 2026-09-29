## Why

Leak Out is a multi-level game, and every level must be described as data rather than code so that difficulty can grow by authoring JSON, not by changing the app. No level model exists yet, and the game engine, level select and progression all depend on one.

## What Changes

- Introduce a level definition stored as one JSON file per level.
- A level has a unique numeric id (1, 2, 3, ...) and a default grid size (columns and rows).
- A level has a start position (x, y, direction) and a finish position (x, y, direction).
- A level has an initial delay: the time before liquid starts flowing from the start position.
- A level has a flow speed: a float between 0 and 1 that controls how fast the liquid flows.
- A level has an allowed pipes object with booleans that say whether the cross section and the reservoir section may appear on the stack. Straight and corner sections are always allowed.
- A level has an array of blocked cells where no pipe section can be placed.
- A level has star benchmarks: three point thresholds for one, two and three stars.
- Add parsing and validation so that malformed or unplayable levels are rejected with clear errors.

## Capabilities

### New Capabilities
- `game-level-model`: The JSON level definition, its fields and value ranges, parsing into an in-app model, and validation of a level's internal consistency.

### Modified Capabilities

None. No specs exist yet.

## Impact

- New code in the Android app module: level model classes, a JSON parser and a validator, with unit tests.
- New JSON level files under the app's assets, plus at least one sample level used by tests.
- A JSON serialization dependency (kotlinx.serialization) is needed.
- Later work depends on this model: the flow engine, the pipe stack, level select and star scoring.
