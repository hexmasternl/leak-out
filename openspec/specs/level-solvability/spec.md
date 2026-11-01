# level-solvability Specification

## Purpose
TBD - created by archiving change app-game-headless-level-tester. Update Purpose after archive.
## Requirements
### Requirement: Level solver determines start-to-finish reachability
The system SHALL provide a `LevelSolver` that, given a parsed `Level`, determines whether at least one route of pipe sections can connect the start's facing cell to the finish's facing cell, using only cells that lie inside the grid and are not blocked. Leaky tiles SHALL count as usable cells. A route MAY pass a portal pair once: from the facing cell of one portal it continues at the facing cell of the other portal of that pair. When the level has mandatory sections, the route SHALL run through every channel of every mandatory section, entering each one through an open side, and SHALL NOT pass a cell other than a mandatory cross more than once.

#### Scenario: Open path exists
- **WHEN** the solver is run on a level whose start's facing cell and finish's facing cell are connected by at least one sequence of adjacent, in-grid, non-blocked cells
- **THEN** the solver reports the level as solvable

#### Scenario: Blocked cells seal off the finish
- **WHEN** the solver is run on a level whose blocked cells leave no sequence of adjacent, in-grid, non-blocked cells connecting the start's facing cell to the finish's facing cell
- **THEN** the solver reports the level as unsolvable

#### Scenario: Start and finish share a facing cell
- **WHEN** the start's facing cell is the same cell as the finish's facing cell, in a level without mandatory sections
- **THEN** the solver reports the level as solvable, since a single pipe section placed there connects both endpoints

#### Scenario: Route through a mandatory reservoir
- **WHEN** a level has a mandatory reservoir whose left and right neighbours are free and can be reached from the start's and the finish's facing cells
- **THEN** the solver reports the level as solvable and its route runs through the reservoir from left to right or right to left

#### Scenario: Mandatory reservoir that cannot be entered
- **WHEN** a level has a mandatory reservoir whose left neighbour is a blocked cell
- **THEN** the solver reports the level as unsolvable, with a reason that names the mandatory section

#### Scenario: Mandatory cross needs both channels
- **WHEN** a level has a mandatory cross whose top and bottom neighbours are blocked
- **THEN** the solver reports the level as unsolvable, with a reason that names the mandatory section

#### Scenario: Solver result explains an unsolvable level
- **WHEN** the solver reports a level as unsolvable
- **THEN** the result includes a reason that identifies which endpoint (start or finish) or which mandatory section could not be connected

#### Scenario: Only route runs over a leaky tile
- **WHEN** the only free cells between the start's facing cell and the finish's facing cell form a corridor with one leaky tile in it
- **THEN** the solver reports the level as solvable

#### Scenario: Only solvable through a portal pair
- **WHEN** a wall of blocked cells separates the start's side of the grid from the finish's side, and a portal pair has one portal facing a cell on each side
- **THEN** the solver reports the level as solvable, and its route runs into one portal of the pair and out of the other

#### Scenario: Portal pair that leads nowhere useful
- **WHEN** the same wall separates the start from the finish, and both portals of the only pair face cells on the start's side
- **THEN** the solver reports the level as unsolvable

#### Scenario: Portal pair together with a mandatory section
- **WHEN** a mandatory reservoir can only be reached through a portal pair
- **THEN** the solver reports the level as solvable and its route passes the pair before the reservoir

### Requirement: Solver does not require cross or reservoir sections
The system SHALL determine solvability using only straight and corner sections from the stack, besides the level's mandatory sections, and SHALL NOT report a level as unsolvable solely because a route would require a cross (`X`) or reservoir (`RES`) section from the stack.

#### Scenario: Path exists using only straight and corner sections
- **WHEN** a level has a route from the start's facing cell to the finish's facing cell that only ever needs one channel through each cell it passes through
- **THEN** the solver reports the level as solvable regardless of the level's stack weights for `X` and `RES`

#### Scenario: Mandatory cross with zero stack weight
- **WHEN** a level has a solvable route through its mandatory cross and its stack weight for `X` is 0
- **THEN** the solver reports the level as solvable

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

### Requirement: Solver reports whether a leak-free route exists
For a solvable level, the solver SHALL also report whether a route exists that passes no leaky tile, found with the same rules as the main route but with leaky tiles treated as unusable. A level without a leak-free route SHALL still be solvable.

#### Scenario: Detour around a leaky tile
- **WHEN** a level has a leaky tile on the shortest route and a longer route of free cells around it
- **THEN** the solver reports the level as solvable with a leak-free route

#### Scenario: Leaky tile is the price of the route
- **WHEN** every route from the start's facing cell to the finish's facing cell passes a leaky tile
- **THEN** the solver reports the level as solvable without a leak-free route

#### Scenario: Level without leaky tiles
- **WHEN** a solvable level has no leaky tiles
- **THEN** the solver reports a leak-free route
