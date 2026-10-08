## MODIFIED Requirements

### Requirement: Level solver determines start-to-finish reachability
The system SHALL provide a `LevelSolver` that, given a parsed `Level`, determines whether at least one route of pipe sections can connect the start's facing cell to the finish's facing cell, using only cells that lie inside the grid and are not blocked. Leaky tiles SHALL count as usable cells. When the level has mandatory sections, the route SHALL run through every channel of every mandatory section, entering each one through an open side, and SHALL NOT pass a cell other than a mandatory cross more than once.

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

## ADDED Requirements

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
