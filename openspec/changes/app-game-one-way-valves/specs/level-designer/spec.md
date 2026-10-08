## MODIFIED Requirements

### Requirement: Live validation and solvability
After every change the designer SHALL validate the design with `LevelValidator` and, when it is valid, check it with `LevelSolver`. It SHALL show the status (valid and solvable, invalid, or unsolvable), list every validation error with its field and message, and show the solver's reason for an unsolvable level. Errors about a cell (start, finish, a blocked cell or a valve) SHALL also be highlighted on the grid.

#### Scenario: Valid and solvable
- **WHEN** the design is valid and the solver finds a route
- **THEN** the status shows the level as valid and solvable and no errors are listed

#### Scenario: Several errors listed
- **WHEN** the start opens onto the grid edge and the star thresholds are not increasing
- **THEN** both errors are listed with their fields, and the start cell is highlighted

#### Scenario: Unsolvable
- **WHEN** blocked cells wall off the finish
- **THEN** the status shows the level as unsolvable with the solver's reason

#### Scenario: Valve that can never be passed
- **WHEN** the developer places a valve with direction `RIGHT` in the last column
- **THEN** the error for that valve is listed and the valve's cell is highlighted

#### Scenario: Valve blocks the only route
- **WHEN** the only gap in a wall holds a valve pointing back toward the start
- **THEN** the status shows the level as unsolvable with a reason that names the valve

## ADDED Requirements

### Requirement: Valve tool
The designer SHALL have a Valve tool with a direction control offering `LEFT`, `RIGHT`, `UP` and `DOWN`, defaulting to `RIGHT`. Tapping a free cell SHALL place a valve with the selected direction; tapping a valve with that direction SHALL remove it; tapping a valve with another direction SHALL turn it. A drag from a valve to a free cell SHALL move the valve. The tool SHALL do nothing on a blocked cell, an endpoint or a mandatory section, or on a free cell when the design has 8 valves.

#### Scenario: Place a valve
- **WHEN** the Valve tool is selected with direction `UP` and the developer taps the free cell x 3, y 2
- **THEN** the design has a valve at x 3, y 2 with direction `UP` and the grid draws it as in the game

#### Scenario: Turn a valve
- **WHEN** the Valve tool is selected with direction `LEFT` and the developer taps a valve with direction `UP`
- **THEN** that valve has direction `LEFT` and the number of valves is unchanged

#### Scenario: Remove a valve
- **WHEN** the Valve tool is selected with direction `RIGHT` and the developer taps a valve with direction `RIGHT`
- **THEN** the valve is removed

#### Scenario: Move a valve
- **WHEN** the Valve tool is selected and the developer drags from a valve with direction `DOWN` at x 1, y 1 to the free cell x 4, y 2
- **THEN** x 1, y 1 is free and x 4, y 2 holds a valve with direction `DOWN`

#### Scenario: Blocked cell refused
- **WHEN** the Valve tool is selected and the developer taps a blocked cell
- **THEN** the design is unchanged

#### Scenario: At most eight
- **WHEN** the design has 8 valves and the developer taps a free cell with the Valve tool
- **THEN** no valve is added

### Requirement: Other tools leave valves alone
The Start, Finish, Block, Mandatory cross and Mandatory reservoir tools SHALL do nothing on a cell holding a valve.

#### Scenario: Block tool on a valve
- **WHEN** the Block tool is dragged across a cell holding a valve
- **THEN** the valve stays and that cell is not blocked

#### Scenario: Endpoint onto a valve
- **WHEN** the Finish tool is selected and the developer taps a cell holding a valve
- **THEN** the finish and the valve do not change

### Requirement: Valves leave the designer with the design
Valves SHALL be part of the exported JSON, SHALL be on the board when a test play starts, and SHALL be kept when the design survives a configuration change or process death. Copying a bundled level SHALL copy its valves.

#### Scenario: Exported
- **WHEN** a valid, solvable design with a valve at x 3, y 2 with direction `RIGHT` is exported
- **THEN** the JSON's `valves` list holds that valve and parsing it gives back the design

#### Scenario: Test played
- **WHEN** the developer test plays a design with a valve
- **THEN** the game screen starts with that valve on the board, pointing in its direction

#### Scenario: Process recreated
- **WHEN** the system kills and recreates the app while a design with valves is open
- **THEN** the designer shows the same valves with the same directions
