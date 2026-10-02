# level-designer Specification

## Purpose
TBD - created by archiving change app-game-level-designer. Update Purpose after archive.
## Requirements
### Requirement: Level designer screen
The app SHALL have a Level Designer screen, reachable only from the welcome screen's level designer button while the `levelDesigner` feature flag is on. It SHALL use the game's theme and landscape orientation, show the level grid as the main area and the editing controls in a side panel. Its back button and the system back gesture SHALL return to the welcome screen; when the design has unexported changes, a confirmation dialog SHALL be shown first.

#### Scenario: Open the designer
- **WHEN** the developer taps the level designer button on the welcome screen
- **THEN** the Level Designer screen opens with a new level

#### Scenario: Leave without changes
- **WHEN** the developer presses back on an unchanged design
- **THEN** the welcome screen is shown without a dialog

#### Scenario: Leave with unexported changes
- **WHEN** the developer presses back after changing the design and not exporting it
- **THEN** a dialog asks to discard the design; confirming returns to the welcome screen and cancelling stays in the designer

### Requirement: New level and copy of a bundled level
A new design SHALL start as a valid, solvable level: id one higher than the number of bundled levels, name "New level", a 7 by 5 grid, start at x 0, y 2 open `RIGHT`, finish at x 6, y 2 open `LEFT`, no blocked cells, flow initial delay 20000 ms and 0.4 cells per second, star thresholds 50, 80 and 120, and every optional field at its default. The designer SHALL also offer to start from a copy of any bundled level, which loads all of that level's fields.

#### Scenario: Blank level
- **WHEN** the designer opens and 4 levels are bundled
- **THEN** the design has id 5, name "New level", a 7 by 5 grid with start at 0,2 `RIGHT` and finish at 6,2 `LEFT`, and is reported valid and solvable

#### Scenario: Copy a bundled level
- **WHEN** the developer chooses to start from bundled level 2
- **THEN** the design holds every field of level 2, including its blocked cells

### Requirement: Grid size
The designer SHALL let the developer set the number of columns and rows, each between 4 and 50, with controls that cannot go outside that range. When the grid shrinks, blocked cells outside the new grid SHALL be removed, and a start or finish outside the new grid SHALL be moved to the nearest cell inside it, keeping its open side.

#### Scenario: Grow the grid
- **WHEN** the developer raises the columns from 7 to 10
- **THEN** the grid shows 10 columns and the start, finish and blocked cells keep their positions

#### Scenario: Shrink removes blocked cells
- **WHEN** a blocked cell is at x 8, y 1 and the developer lowers the columns from 10 to 8
- **THEN** that blocked cell is removed and the other blocked cells are kept

#### Scenario: Shrink moves the finish
- **WHEN** the finish is at x 9, y 2 and the developer lowers the columns to 6
- **THEN** the finish is at x 5, y 2 with its open side unchanged

#### Scenario: Range limits
- **WHEN** the columns are 4
- **THEN** the control to lower the columns is disabled, and at 50 the control to raise them is disabled

### Requirement: Placing the start and the finish
The designer SHALL have a Start tool and a Finish tool. With one of them selected, tapping a cell SHALL move that endpoint to the cell, keeping its open side; if the cell was blocked it SHALL be unblocked. Tapping the cell of the other endpoint SHALL do nothing. The designer SHALL show an open side control for the start and for the finish with the choices `LEFT`, `RIGHT`, `TOP` and `BOTTOM`. The grid SHALL draw the start and the finish with their open sides as in the game.

#### Scenario: Move the start
- **WHEN** the Start tool is selected and the developer taps x 2, y 3
- **THEN** the start is at x 2, y 3 with its previous open side

#### Scenario: Start onto a blocked cell
- **WHEN** the Start tool is selected and the developer taps a blocked cell
- **THEN** the start moves there and the cell is no longer blocked

#### Scenario: Endpoints cannot share a cell
- **WHEN** the Finish tool is selected and the developer taps the start's cell
- **THEN** neither endpoint changes

#### Scenario: Change the open side
- **WHEN** the developer sets the finish's open side to `TOP`
- **THEN** the grid shows the finish opening upwards and the design's finish open side is `TOP`

### Requirement: Blocked cells
The designer SHALL have a Block tool. With it selected, tapping a cell SHALL toggle whether it is blocked. Dragging across cells SHALL apply the same change to every cell passed: block them when the first cell was free, unblock them when it was blocked. The start and finish cells SHALL never be blocked by the tool. A Clear blocked action SHALL remove all blocked cells.

#### Scenario: Toggle a cell
- **WHEN** the Block tool is selected and the developer taps a free cell, then taps it again
- **THEN** the cell is blocked after the first tap and free after the second

#### Scenario: Drag to block
- **WHEN** the Block tool is selected and the developer drags from a free cell across three more cells in a row
- **THEN** all four cells are blocked

#### Scenario: Endpoints are skipped
- **WHEN** the developer drags the Block tool across the start's cell
- **THEN** the start's cell stays free and the cells before and after it are blocked

#### Scenario: Clear blocked
- **WHEN** the design has blocked cells and the developer taps Clear blocked
- **THEN** the design has no blocked cells

### Requirement: Other level fields
The designer SHALL let the developer edit the level's id, name, flow settings (initial delay, cells per second, reservoir speed factor, speed ramp per cell), stack settings (visible size and the weight of each of the eight sections), placement rules (allow replace, replace penalty) and star thresholds (one, two, three). A numeric input that is not a number SHALL be marked as invalid and SHALL NOT change the design.

#### Scenario: Edit flow speed
- **WHEN** the developer enters 0.8 as cells per second
- **THEN** the design's `flow.cellsPerSecond` is 0.8

#### Scenario: Enable the cross
- **WHEN** the developer sets the weight of `X` to 1
- **THEN** the design's stack weight for `X` is 1

#### Scenario: Not a number
- **WHEN** the developer types "abc" as the initial delay
- **THEN** the field is marked invalid and the design keeps its previous initial delay

### Requirement: Live validation and solvability
After every change the designer SHALL validate the design with `LevelValidator` and, when it is valid, check it with `LevelSolver`. It SHALL show the status (valid and solvable, invalid, or unsolvable), list every validation error with its field and message, and show the solver's reason for an unsolvable level. Errors about a cell (start, finish or a blocked cell) SHALL also be highlighted on the grid.

#### Scenario: Valid and solvable
- **WHEN** the design is valid and the solver finds a route
- **THEN** the status shows the level as valid and solvable and no errors are listed

#### Scenario: Several errors listed
- **WHEN** the start opens onto the grid edge and the star thresholds are not increasing
- **THEN** both errors are listed with their fields, and the start cell is highlighted

#### Scenario: Unsolvable
- **WHEN** blocked cells wall off the finish
- **THEN** the status shows the level as unsolvable with the solver's reason

### Requirement: Test play
The designer SHALL have a Test play action, enabled only when the design is valid and solvable, that plays the design in the normal game screen. A test play SHALL NOT record anything in the level history and SHALL NOT offer a next level. Leaving the game screen SHALL return to the designer with the design unchanged.

#### Scenario: Play the design
- **WHEN** the developer taps Test play on a valid, solvable design
- **THEN** the game screen starts with the designed grid, endpoints, blocked cells and settings

#### Scenario: No history
- **WHEN** the developer wins a test play
- **THEN** the level history is unchanged and the level complete dialog has no enabled next level

#### Scenario: Back to the designer
- **WHEN** the developer exits the test play
- **THEN** the designer is shown with the same design as before

#### Scenario: Disabled when invalid
- **WHEN** the design has a validation error or is unsolvable
- **THEN** the Test play action is disabled

### Requirement: Export as JSON
The designer SHALL have an Export action, enabled only when the design is valid and solvable, that writes the design with `LevelWriter`, copies the JSON to the clipboard and opens the Android share sheet with the JSON as text and the suggested file name `level_NNN.json` for the design's id. After an export the design SHALL count as having no unexported changes.

#### Scenario: Export a design
- **WHEN** the developer taps Export on a valid, solvable design with id 5
- **THEN** the clipboard holds the level JSON, the share sheet opens with that JSON and the subject `level_005.json`

#### Scenario: Exported JSON is a valid level
- **WHEN** the exported JSON is parsed and validated
- **THEN** parsing succeeds, validation reports no errors and the level equals the design

### Requirement: Design survives configuration changes and process death
The design being edited, the selected tool and the unexported-changes state SHALL be kept across configuration changes and when the system recreates the app process. Designs SHALL NOT be stored permanently.

#### Scenario: Process recreated
- **WHEN** the system kills and recreates the app while the designer is open
- **THEN** the designer shows the same design and selected tool
