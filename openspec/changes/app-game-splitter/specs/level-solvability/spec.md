# Spec Delta

## MODIFIED Requirements

### Requirement: Level solver determines start-to-finish reachability
The system SHALL provide a `LevelSolver` that, given a parsed `Level`, determines whether at least one route of pipe sections can connect the start's facing cell to the finish's facing cell, using only cells that lie inside the grid and are not blocked. When the level has mandatory sections, the route SHALL run through every channel of every mandatory section, entering each one through an open side, and SHALL NOT pass a cell other than a mandatory cross more than once. When the level has a splitter, the route SHALL enter the splitter through an open side and continue from each of its two other open sides as a branch to a different finish's facing cell, and the two branches and the part before the splitter SHALL NOT share a cell other than a mandatory cross.

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

#### Scenario: Route that splits to both finishes
- **WHEN** a level has a splitter that can be reached from the start's facing cell, and its other two open sides can each be joined to a different finish's facing cell without sharing a cell
- **THEN** the solver reports the level as solvable, with the route to the splitter and both branches

#### Scenario: One finish cannot be reached
- **WHEN** a level has a splitter and blocked cells wall off the second finish from both of the splitter's open sides
- **THEN** the solver reports the level as unsolvable, with a reason that names the second finish

#### Scenario: Splitter cannot be reached
- **WHEN** blocked cells leave no route from the start's facing cell to any open side of the splitter
- **THEN** the solver reports the level as unsolvable, with a reason that names the splitter
