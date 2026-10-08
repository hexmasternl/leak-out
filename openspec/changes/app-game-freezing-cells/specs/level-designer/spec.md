# Spec Delta

## MODIFIED Requirements

### Requirement: Live validation and solvability
After every change the designer SHALL validate the design with `LevelValidator` and, when it is valid, check it with `LevelSolver`. It SHALL show the status (valid and solvable, invalid, or unsolvable), list every validation error with its field and message, and show the solver's reason for an unsolvable level. Errors about a cell (start, finish, a blocked cell, a mandatory section or a freezing cell) SHALL also be highlighted on the grid.

#### Scenario: Valid and solvable
- **WHEN** the design is valid and the solver finds a route
- **THEN** the status shows the level as valid and solvable and no errors are listed

#### Scenario: Several errors listed
- **WHEN** the start opens onto the grid edge and the star thresholds are not increasing
- **THEN** both errors are listed with their fields, and the start cell is highlighted

#### Scenario: Unsolvable
- **WHEN** blocked cells wall off the finish
- **THEN** the status shows the level as unsolvable with the solver's reason

#### Scenario: Mandatory cross on a freezing cell
- **WHEN** the design has a mandatory `X` on a freezing cell
- **THEN** the error for that freezing entry is listed and the cell is highlighted on the grid

## ADDED Requirements

### Requirement: Freezing cell tool
The designer SHALL have a Freezing cell tool. With it selected, tapping a cell that is not blocked, not the start and not the finish SHALL make it a freezing cell, and tapping a freezing cell SHALL make it a normal cell again. Dragging across cells SHALL apply the same change to every cell passed, as with the Block tool: mark them when the first cell was not freezing, unmark them when it was. The tool SHALL do nothing on a blocked cell, the start or the finish. Making a cell blocked SHALL remove its freezing mark. The designer grid SHALL draw freezing cells with the frost tile, also under a mandatory section.

#### Scenario: Mark a freezing cell
- **WHEN** the Freezing cell tool is selected and the developer taps the free cell x 3, y 2
- **THEN** the design has a freezing cell at x 3, y 2 and the grid draws it with the frost tile

#### Scenario: Unmark by tapping again
- **WHEN** the Freezing cell tool is selected and the developer taps a freezing cell
- **THEN** that cell is no longer a freezing cell

#### Scenario: Drag marks a row
- **WHEN** the Freezing cell tool is selected and the developer drags from a normal free cell across three more free cells in a row
- **THEN** all four cells are freezing cells

#### Scenario: Blocked cell refused
- **WHEN** the Freezing cell tool is selected and the developer taps a blocked cell
- **THEN** the design is unchanged

#### Scenario: Blocking a freezing cell
- **WHEN** the Block tool is selected and the developer taps a freezing cell
- **THEN** the cell is blocked and no longer a freezing cell

### Requirement: Freezing cells leave the designer with the design
Freezing cells SHALL be part of the exported JSON, SHALL be on the board when a test play starts, and SHALL be kept when the design survives a configuration change or process death. A copy of a bundled level SHALL keep its freezing cells.

#### Scenario: Exported
- **WHEN** a valid, solvable design with a freezing cell at x 3, y 2 is exported
- **THEN** the JSON's `freezing` list holds that cell and parsing it gives back the design

#### Scenario: Test played
- **WHEN** the developer test plays a design with a freezing cell
- **THEN** the game screen starts with that cell drawn as a freezing cell, and a section on it freezes once the liquid has left it

#### Scenario: Process recreated
- **WHEN** the system kills and recreates the app while a design with freezing cells is open
- **THEN** the designer shows the same freezing cells
