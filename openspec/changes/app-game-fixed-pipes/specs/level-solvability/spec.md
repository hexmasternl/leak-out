# Spec Delta

## MODIFIED Requirements

### Requirement: Level solver determines start-to-finish reachability
The system SHALL provide a `LevelSolver` that, given a parsed `Level`, determines whether at least one route of pipe sections can connect the start's facing cell to the finish's facing cell, using only cells that lie inside the grid and are not blocked. When the level has mandatory sections, the route SHALL run through every channel of every mandatory section, entering each one through an open side, and SHALL NOT pass a cell other than a mandatory or fixed cross more than once. When the level has fixed pipes, the route MAY run through a fixed pipe, but only into one of its open sides and out of the other end of that channel; it SHALL NOT be required to pass any fixed pipe.

#### Scenario: Open path exists
- **WHEN** the solver is run on a level whose start's facing cell and finish's facing cell are connected by at least one sequence of adjacent, in-grid, non-blocked cells
- **THEN** the solver reports the level as solvable

#### Scenario: Blocked cells seal off the finish
- **WHEN** the solver is run on a level whose blocked cells leave no sequence of adjacent, in-grid, non-blocked cells connecting the start's facing cell to the finish's facing cell
- **THEN** the solver reports the level as unsolvable

#### Scenario: Start and finish share a facing cell
- **WHEN** the start's facing cell is the same cell as the finish's facing cell, in a level without mandatory sections or fixed pipes
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

#### Scenario: Route along a fixed pipe's channel
- **WHEN** the only way to the finish runs through a fixed `H`, and the cells left and right of it can be reached from the start's and the finish's facing cells
- **THEN** the solver reports the level as solvable and its route enters the `H` from one side and leaves it on the other

#### Scenario: Fixed pipe that turns the route the wrong way
- **WHEN** the only way to the finish runs through a fixed `LT` entered from the bottom, and no other route exists
- **THEN** the solver reports the level as unsolvable, since a fixed `LT` has no bottom opening

#### Scenario: Route around a fixed pipe
- **WHEN** a fixed `V` lies on the shortest way to the finish but a longer route through free cells avoids it
- **THEN** the solver reports the level as solvable with a route that does not pass the fixed pipe

#### Scenario: Fixed pipe on the start's facing cell
- **WHEN** a fixed `V` lies on the facing cell of a start that opens to the right
- **THEN** the solver reports the level as unsolvable, with a reason that names the start

#### Scenario: Fixed cross passed twice
- **WHEN** a route needs to cross itself and the only crossing point is a fixed cross
- **THEN** the solver reports the level as solvable and its route passes the fixed cross once horizontally and once vertically

### Requirement: Solver does not require cross or reservoir sections
The system SHALL determine solvability using only straight and corner sections from the stack, besides the level's mandatory sections and fixed pipes, and SHALL NOT report a level as unsolvable solely because a route would require a cross (`X`) or reservoir (`RES`) section from the stack.

#### Scenario: Path exists using only straight and corner sections
- **WHEN** a level has a route from the start's facing cell to the finish's facing cell that only ever needs one channel through each cell it passes through
- **THEN** the solver reports the level as solvable regardless of the level's stack weights for `X` and `RES`

#### Scenario: Mandatory cross with zero stack weight
- **WHEN** a level has a solvable route through its mandatory cross and its stack weight for `X` is 0
- **THEN** the solver reports the level as solvable

#### Scenario: Fixed reservoir with zero stack weight
- **WHEN** a level has a solvable route through its fixed reservoir and its stack weight for `RES` is 0
- **THEN** the solver reports the level as solvable
