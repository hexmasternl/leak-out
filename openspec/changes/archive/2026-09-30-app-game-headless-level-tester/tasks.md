## 1. Level solver

- [x] 1.1 Add `Position`/grid-graph helpers as needed (reuse existing `Position`, `GridSize`, `Side`, `Endpoint` in `core/src/main/kotlin/nl/hexmaster/leakout/core/level/Geometry.kt`; no changes expected unless a gap is found).
- [x] 1.2 Create `core/src/main/kotlin/nl/hexmaster/leakout/core/level/LevelSolver.kt` with a `LevelSolver` object exposing `solve(level: Level): SolverResult`.
- [x] 1.3 Define `SolverResult` as a sealed type with `Solvable(path: List<Position>)` and `Unsolvable(reason: String)` cases (or equivalent), in the same file or a nearby one.
- [x] 1.4 Implement BFS over non-blocked, in-grid cells from `level.start.facingCell` to `level.finish.facingCell`, treating the graph as unweighted with 4-directional adjacency; return `Solvable` with the discovered path (or an empty/degenerate path) when the two facing cells are equal.
- [x] 1.5 Return `Unsolvable` with a reason naming which endpoint (start or finish) is unreachable when no path exists; guard defensively against a facing cell that is itself blocked or outside the grid (should not occur for `LevelValidator`-passing levels, but must not throw).

## 2. Solver unit tests

- [x] 2.1 Create `core/src/test/kotlin/nl/hexmaster/leakout/core/level/LevelSolverTest.kt` covering: a simple open-grid level is solvable; a level with blocked cells sealing off the finish is unsolvable; a level where start and finish share a facing cell is solvable; an unsolvable result's reason identifies the correct unreachable endpoint; a solvable path exists using only straight/corner logic even when `X`/`RES` weights are 0.
- [x] 2.2 Use existing test helpers/patterns from `LevelValidatorTest.kt` (e.g. building `Level` instances directly) rather than round-tripping through JSON, to keep solver unit tests fast and focused.

## 3. Headless bundled-level test suite

- [x] 3.1 Create `core/src/test/kotlin/nl/hexmaster/leakout/core/level/LevelSolvabilityTest.kt`, mirroring `BundledLevelsTest`'s pattern: read the levels directory from the `leakout.levelsDir` system property, list files matching `level_\d{3}\.json`, and expose them via `@MethodSource`.
- [x] 3.2 Add a `@ParameterizedTest(name = "{0}")` that parses each level file and asserts `LevelSolver.solve(level)` is `Solvable`, failing with a message that includes the file name and the `Unsolvable` reason when it is not.
- [x] 3.3 Confirm the test class needs no new Gradle configuration: it reuses the same `leakout.levelsDir` system property already wired in `core/build.gradle.kts` for `BundledLevelsTest`, so it runs automatically as part of `:core:test` against `app/src/main/assets/levels`.

## 4. Validate against current bundled levels

- [x] 4.1 Run `./gradlew :core:test` (from `src/`) and confirm every one of the 24 bundled levels passes the new solvability test.
- [x] 4.2 If any bundled level fails, investigate whether it is a genuine authoring bug (fix the level's `blocked` cells or endpoints under `app/src/main/assets/levels/`) or a solver defect (fix `LevelSolver`), and re-run until green.

## 5. Documentation

- [x] 5.1 Update `docs/level-format.md` to mention that a dedicated solver/test suite (distinct from `LevelValidator`) checks that a level is solvable, alongside the existing note about `BundledLevelsTest` parsing and validating every bundled file.
