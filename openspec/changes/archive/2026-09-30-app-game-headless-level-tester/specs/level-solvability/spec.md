## ADDED Requirements

### Requirement: Level solver determines start-to-finish reachability
The system SHALL provide a `LevelSolver` that, given a parsed `Level`, determines whether at least one route of pipe sections can connect the start's facing cell to the finish's facing cell, using only cells that lie inside the grid and are not blocked.

#### Scenario: Open path exists
- **WHEN** the solver is run on a level whose start's facing cell and finish's facing cell are connected by at least one sequence of adjacent, in-grid, non-blocked cells
- **THEN** the solver reports the level as solvable

#### Scenario: Blocked cells seal off the finish
- **WHEN** the solver is run on a level whose blocked cells leave no sequence of adjacent, in-grid, non-blocked cells connecting the start's facing cell to the finish's facing cell
- **THEN** the solver reports the level as unsolvable

#### Scenario: Start and finish share a facing cell
- **WHEN** the start's facing cell is the same cell as the finish's facing cell
- **THEN** the solver reports the level as solvable, since a single pipe section placed there connects both endpoints

#### Scenario: Solver result explains an unsolvable level
- **WHEN** the solver reports a level as unsolvable
- **THEN** the result includes a reason that identifies which endpoint (start or finish) could not be connected

### Requirement: Solver does not require cross or reservoir sections
The system SHALL determine solvability using only the mandatory pipe sections (straight and corner), and SHALL NOT report a level as unsolvable solely because a route would require a cross (`X`) or reservoir (`RES`) section.

#### Scenario: Path exists using only straight and corner sections
- **WHEN** a level has a route from the start's facing cell to the finish's facing cell that only ever needs one channel through each cell it passes through
- **THEN** the solver reports the level as solvable regardless of the level's stack weights for `X` and `RES`

### Requirement: Headless test suite runs the solver over a directory of level files
The system SHALL provide a headless, automated test suite that loads every level file matching the bundled naming convention (`level_NNN.json`) from a configurable directory, runs `LevelSolver` against each parsed level, and fails the test run if any level is unsolvable.

#### Scenario: All levels in the directory are solvable
- **WHEN** the test suite is run against a directory where every level file is solvable
- **THEN** every test case in the suite passes

#### Scenario: One level in the directory is unsolvable
- **WHEN** the test suite is run against a directory containing at least one level file that the solver reports as unsolvable
- **THEN** the test suite fails, and the failure identifies the specific unsolvable level file by name

#### Scenario: Suite reuses the bundled levels directory by default
- **WHEN** the test suite runs as part of the `core` module's test task without additional configuration
- **THEN** it evaluates every level file already bundled under the app's shipped levels directory

#### Scenario: Suite can be pointed at a different directory
- **WHEN** the test suite is configured with a different directory of `level_NNN.json` files (for example, a set of newly generated levels)
- **THEN** it evaluates the levels in that directory instead of, or in addition to, the bundled set, using the same solvability check
