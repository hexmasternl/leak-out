## 1. Project setup

- [x] 1.1 Ensure the pure Kotlin JVM `core` module exists (create it if the Android project is not scaffolded yet) with package `nl.hexmaster.leakout.core.level`
- [x] 1.2 Add kotlinx.serialization (plugin and JSON library) and a unit test framework to that module
- [x] 1.3 Confirm `./gradlew test` runs on the module with a trivial passing test

## 2. Level model

- [x] 2.1 Add a `Side` enum with `LEFT`, `RIGHT`, `TOP`, `BOTTOM` and helpers for the opposite side and the cell offset (dx, dy)
- [x] 2.2 Add immutable `Position` (x, y), `Endpoint` (position plus `openSide`, with a `facingCell`), `GridSize`, `FlowSettings`, `StackSettings` (visible size and a weight per section id), `PlacementRules` and `StarBenchmarks`
- [x] 2.3 Add the immutable `Level` class with id, name, grid, start, finish, blocked set, flow, stack, rules and star benchmarks, plus `isAllowed(section)` derived from the weight
- [x] 2.4 Add a `LevelDefaults` object holding every default from the design (reservoir speed factor 0.3, speed ramp 0.0, visible size 5, stack weights, allow replace, replace penalty 500 ms)
- [x] 2.5 Implement `starsFor(points)` returning 0 to 3 and cover the boundary cases from the spec, including a 0-star win

## 3. JSON parsing

- [x] 3.1 Add serializable DTOs matching the JSON shape in the design: required fields without defaults, optional fields defaulting to `LevelDefaults`, unknown keys rejected (also inside `stack.weights`)
- [x] 3.2 Implement `LevelParser.parse(text)` that maps DTOs to the `Level` model, trims the name, and returns field-named errors for missing fields, wrong types, unknown keys, invalid open sides and malformed JSON
- [x] 3.3 Test parsing of a complete level, a minimal level (all defaults), single-field overrides, a single-weight override, and each parse failure scenario in the spec

## 4. Validation

- [ ] 4.1 Add a `ValidationError` type carrying the field name and a message, and a result type that holds all errors
- [ ] 4.2 Validate id (positive), name (not blank) and grid (columns and rows each 4 to 50)
- [ ] 4.3 Validate flow settings: `initialDelayMs` at least 0, `cellsPerSecond` above 0 and at most 10, `reservoirSpeedFactor` above 0 and at most 1, `speedRampPerCell` at least 0
- [ ] 4.4 Validate stack settings (visible size 1 to 10, mandatory weights at least 1, no negative weight) and rules (replace penalty at least 0)
- [ ] 4.5 Validate that start, finish and blocked cells are inside the grid, that start and finish differ, and that blocked cells do not cover start or finish
- [ ] 4.6 Validate that each endpoint's facing cell is inside the grid and not blocked, and that neither endpoint faces the other's cell; allow a shared facing cell
- [ ] 4.7 Validate that blocked cells are unique and that star thresholds are positive and strictly increasing
- [ ] 4.8 Validate a set of levels for duplicate ids
- [ ] 4.9 Make validation collect every error instead of stopping at the first, and test the multi-error scenario
- [ ] 4.10 Write one unit test per validation scenario in the spec

## 5. Sample levels

- [ ] 5.1 Add level files `level_001.json` to `level_003.json` in `app/src/main/assets/levels/`, with increasing difficulty; level 1 relies on defaults, and cross and reservoir are enabled through weights from level 2 or 3
- [ ] 5.2 Pass the `app/src/main/assets/levels` path to the `core` test task as a system property, and add a parameterized test that parses and validates every bundled level file with no errors
- [ ] 5.3 Add a test that the bundled set has no duplicate ids

## 6. Wrap up

- [ ] 6.1 Document the level JSON format in `docs/level-format.md`: an annotated full example, a minimal example, the table of defaults and the endpoint rules
- [ ] 6.2 Run the full test suite and confirm every spec scenario has a passing test
