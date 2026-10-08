## MODIFIED Requirements

### Requirement: Level solver determines start-to-finish reachability
The system SHALL provide a `LevelSolver` that, given a parsed `Level`, determines whether at least one route of pipe sections can connect the start's facing cell to the finish's facing cell, using only cells that lie inside the grid and are not blocked. When the level has mandatory sections, the route SHALL run through every channel of every mandatory section, entering each one through an open side, and SHALL NOT pass a cell other than a mandatory cross more than once. A route MAY run through a valve only from its inlet to its outlet, and SHALL NOT need a valve to be passed.

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

#### Scenario: Route through a valve in its direction
- **WHEN** a wall of blocked cells separates the start from the finish and its only gap holds a valve that points from the start's side to the finish's side
- **THEN** the solver reports the level as solvable and its route runs through the valve from its inlet to its outlet

#### Scenario: Only route runs against a valve
- **WHEN** a wall of blocked cells separates the start from the finish and its only gap holds a valve that points from the finish's side to the start's side
- **THEN** the solver reports the level as unsolvable, with a reason that names the valve, its position and its direction

#### Scenario: Route around a valve
- **WHEN** a level has a valve that points away from the finish and a free route from the start to the finish that does not use the valve's cell
- **THEN** the solver reports the level as solvable

#### Scenario: Solver result explains an unsolvable level
- **WHEN** the solver reports a level as unsolvable
- **THEN** the result includes a reason that identifies which endpoint (start or finish), which mandatory section or which valve could not be connected
