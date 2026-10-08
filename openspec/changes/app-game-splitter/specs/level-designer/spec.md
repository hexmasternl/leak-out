# Spec Delta

## MODIFIED Requirements

### Requirement: Live validation and solvability
After every change the designer SHALL validate the design with `LevelValidator` and, when it is valid, check it with `LevelSolver`. It SHALL show the status (valid and solvable, invalid, or unsolvable), list every validation error with its field and message, and show the solver's reason for an unsolvable level. Errors about a cell (start, finish, second finish, splitter or a blocked cell) SHALL also be highlighted on the grid. For a solvable design with a splitter, the solver's route SHALL be shown faintly for the part before the splitter and for both branches.

#### Scenario: Valid and solvable
- **WHEN** the design is valid and the solver finds a route
- **THEN** the status shows the level as valid and solvable and no errors are listed

#### Scenario: Several errors listed
- **WHEN** the start opens onto the grid edge and the star thresholds are not increasing
- **THEN** both errors are listed with their fields, and the start cell is highlighted

#### Scenario: Unsolvable
- **WHEN** blocked cells wall off the finish
- **THEN** the status shows the level as unsolvable with the solver's reason

#### Scenario: Splitter without a second finish
- **WHEN** the design has a splitter and no second finish
- **THEN** the error that a splitter needs a second finish is listed and the splitter cell is highlighted

#### Scenario: Route with two branches
- **WHEN** a design with a splitter is valid and solvable
- **THEN** the grid shows the route to the splitter and both branches to the finishes

## ADDED Requirements

### Requirement: Splitter and second finish tools
The designer SHALL have a Splitter tool and a Second finish tool. With the Splitter tool, tapping a free cell SHALL place the splitter there, or move it when the design already has one; tapping the splitter SHALL turn its closed side a quarter clockwise. With the Second finish tool, tapping a free cell SHALL place or move the second finish, keeping its open side. The panel SHALL set the second finish's open side and SHALL offer removing the splitter and the second finish. A drag with these tools SHALL only act on the cell it starts on.

#### Scenario: Place a splitter
- **WHEN** the Splitter tool is selected and the developer taps the free cell x 4, y 3 in a design without a splitter
- **THEN** the design has a splitter at x 4, y 3 and the grid draws it

#### Scenario: Turn the splitter
- **WHEN** the Splitter tool is selected and the developer taps the splitter closed at the bottom
- **THEN** the splitter is closed at the left

#### Scenario: Move the splitter
- **WHEN** the design has a splitter at x 4, y 3 and the developer taps the free cell x 2, y 2 with the Splitter tool
- **THEN** the splitter is at x 2, y 2 with the same closed side, and x 4, y 3 is free

#### Scenario: Place the second finish
- **WHEN** the Second finish tool is selected and the developer taps a free cell
- **THEN** the design has a second finish there

#### Scenario: Tools refuse taken cells
- **WHEN** the Splitter tool or the Second finish tool is selected and the developer taps a blocked cell, the start, a finish or a mandatory section
- **THEN** the design is unchanged

### Requirement: Splitter and second finish leave the designer with the design
The splitter and the second finish SHALL be part of the exported JSON, SHALL be on the board when a test play starts, SHALL be kept when a bundled level is copied, and SHALL be kept when the design survives a configuration change or process death.

#### Scenario: Exported
- **WHEN** a valid, solvable design with a splitter and a second finish is exported
- **THEN** the JSON holds `splitter` and `secondFinish`, and parsing it gives back the design

#### Scenario: Test played
- **WHEN** the developer test plays a design with a splitter
- **THEN** the game screen starts with the splitter and both finishes on the board

#### Scenario: Process recreated
- **WHEN** the system kills and recreates the app while a design with a splitter is open
- **THEN** the designer shows the same splitter and second finish
