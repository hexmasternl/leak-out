## Why

`LevelValidator` checks that every level's shape and values are valid, but its own doc comment admits it "does not check that a level is solvable". Nothing in the codebase confirms that a real path of pipe sections can actually connect a level's start to its finish. A malformed layout (for example, blocked cells that seal off the finish) can currently pass validation and ship to players as an unwinnable level. As more levels are authored or generated, this gap needs a headless, automated check.

## What Changes

- Add a `LevelSolver` in the `core` module that determines whether a level can be won: whether a route of mandatory pipe sections (straight and corner; cross and reservoir only when their weight is positive) can connect the start's facing cell to the finish's facing cell without crossing blocked cells or leaving the grid.
- Add a headless test suite that runs the solver against every bundled level file in `app/src/main/assets/levels/` and fails the build if any level has no solution.
- The suite is reusable against any directory of level files (not only the bundled set), so newly generated or authored levels are covered without extra wiring.
- Report, per unsolvable level, why no route could be found (id, name, and the unreachable endpoint) so a failure is actionable from the test output alone.

## Capabilities

### New Capabilities
- `level-solvability`: a core-module solver that decides whether a level's grid, blocked cells, and endpoints admit at least one route from start to finish, plus a headless test suite that runs this solver over a set of level files and fails on any unsolvable level.

### Modified Capabilities
(none — `LevelValidator`'s existing behavior and requirements are unchanged; solvability is a new, separate check)

## Impact

- Affected code: new files under `core/src/main/kotlin/nl/hexmaster/leakout/core/level/` (solver) and `core/src/test/kotlin/nl/hexmaster/leakout/core/level/` (test suite), alongside the existing `LevelParser`, `LevelValidator`, and `BundledLevelsTest`.
- Affected systems: the `core` module's Gradle test task (`./gradlew :core:test` from `src/`) gains new test cases; CI already runs this task, so an unsolvable level will fail the pipeline automatically.
- No changes to the Android `app` module, level file format, or shipped level content are required by this change itself (though it may surface existing unsolvable levels that then need fixing in a follow-up).
