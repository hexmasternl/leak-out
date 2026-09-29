## 1. Project setup

- [ ] 1.1 Ensure a pure Kotlin JVM module exists for game logic (create it if the Android project is not scaffolded yet) with package `nl.hexmaster.leakout.core.level`
- [ ] 1.2 Add kotlinx.serialization (plugin and JSON library) and a unit test framework to that module
- [ ] 1.3 Confirm `./gradlew test` runs on the module with a trivial passing test

## 2. Level model

- [ ] 2.1 Add a `Direction` enum with `LEFT`, `RIGHT`, `TOP`, `BOTTOM` and helpers for the opposite direction and the cell offset (dx, dy)
- [ ] 2.2 Add immutable `Position` (x, y), `Portal` for start and finish (position plus direction), `GridSize`, `AllowedPipes`, `StarBenchmarks`
- [ ] 2.3 Add the immutable `Level` class with id, grid, start, finish, `initialDelayMs`, `flowSpeed`, allowed pipes, blocked set and star benchmarks
- [ ] 2.4 Implement `starsFor(points)` on the star benchmarks and cover the boundary cases from the spec

## 3. JSON parsing

- [ ] 3.1 Add serializable DTOs matching the JSON shape in the design, with all fields required and unknown keys rejected
- [ ] 3.2 Implement `LevelParser.parse(text)` that maps DTOs to the `Level` model and returns field-named errors for missing fields, wrong types, unknown keys, invalid directions and malformed JSON
- [ ] 3.3 Test parsing of a complete level and of each parse failure scenario in the spec

## 4. Validation

- [ ] 4.1 Add a `ValidationError` type carrying the field name and a message, and a result type that holds all errors
- [ ] 4.2 Validate id (positive), grid (positive columns and rows), `initialDelayMs` (zero or more) and `flowSpeed` (greater than 0, at most 1)
- [ ] 4.3 Validate that start, finish and blocked cells are inside the grid, that start and finish differ, and that blocked cells do not cover start or finish
- [ ] 4.4 Validate that start and finish face a cell inside the grid and that the neighbouring cell is not blocked
- [ ] 4.5 Validate that blocked cells are unique and that star thresholds are positive and strictly increasing
- [ ] 4.6 Validate a set of levels for duplicate ids
- [ ] 4.7 Make validation collect every error instead of stopping at the first, and test the multi-error scenario
- [ ] 4.8 Write one unit test per validation scenario in the spec

## 5. Sample levels

- [ ] 5.1 Add level files `level_001.json` to `level_003.json` in the app assets folder, with increasing difficulty and using cross and reservoir from level 2 or 3
- [ ] 5.2 Add a parameterized test that loads every bundled level file, parses it and asserts there are no validation errors
- [ ] 5.3 Add a test that the bundled set has no duplicate ids

## 6. Wrap up

- [ ] 6.1 Document the level JSON format with an annotated example in `docs/level-format.md`
- [ ] 6.2 Run the full test suite and confirm every spec scenario has a passing test
