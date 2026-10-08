# Spec Delta

## ADDED Requirements

### Requirement: Fixed pipe tool
The designer SHALL have a Fixed pipe tool with a choice of the eight sections `H`, `V`, `LT`, `LB`, `RT`, `RB`, `X` and `RES`. With it selected, tapping a free cell SHALL place a fixed pipe of the chosen section. Tapping a fixed pipe of the chosen section SHALL remove it; tapping a fixed pipe of another section SHALL change it to the chosen section. The grid SHALL draw fixed pipes as the game does.

#### Scenario: Place a fixed corner
- **WHEN** the Fixed pipe tool is selected with `LT` chosen and the developer taps the free cell x 3, y 2
- **THEN** the design has a fixed `LT` at x 3, y 2 and the grid draws it on a bolted tile

#### Scenario: Remove by tapping again
- **WHEN** the Fixed pipe tool is selected with `H` chosen and the developer taps a cell holding a fixed `H`
- **THEN** the fixed pipe is removed

#### Scenario: Change the section
- **WHEN** the Fixed pipe tool is selected with `V` chosen and the developer taps a cell holding a fixed `RES`
- **THEN** that cell holds a fixed `V` and the number of fixed pipes is unchanged

### Requirement: Moving a fixed pipe
With the Fixed pipe tool selected, a drag that starts on a fixed pipe SHALL move that fixed pipe, keeping its section, to the cell where the drag ends, when that cell is free. When the drag ends on a cell that is not free, or outside the grid, the design SHALL be unchanged. A drag that starts on a free cell SHALL only act on that cell, as a tap.

#### Scenario: Move a fixed pipe
- **WHEN** the developer drags a fixed `RB` from x 2, y 2 to the free cell x 4, y 3
- **THEN** the design has a fixed `RB` at x 4, y 3 and nothing at x 2, y 2

#### Scenario: Move onto a blocked cell
- **WHEN** the developer drags a fixed pipe onto a blocked cell
- **THEN** the fixed pipe stays where it was

#### Scenario: Drag from a free cell
- **WHEN** the Fixed pipe tool is selected and the developer drags from a free cell across three more cells
- **THEN** only the cell the drag started on holds a fixed pipe

### Requirement: Where fixed pipes can be placed
The Fixed pipe tool SHALL do nothing on a blocked cell, on the start or the finish, or on a cell holding a mandatory section.

#### Scenario: Blocked cell refused
- **WHEN** the Fixed pipe tool is selected and the developer taps a blocked cell
- **THEN** the design is unchanged

#### Scenario: Endpoint refused
- **WHEN** the Fixed pipe tool is selected and the developer taps the finish's cell
- **THEN** the design is unchanged

#### Scenario: Mandatory section refused
- **WHEN** the Fixed pipe tool is selected and the developer taps a cell holding a mandatory cross
- **THEN** the mandatory cross stays and no fixed pipe is added

### Requirement: Fixed pipes leave the designer with the design
Fixed pipes SHALL be part of the exported JSON, SHALL be on the board when a test play starts, and SHALL be kept when the design survives a configuration change or process death.

#### Scenario: Exported
- **WHEN** a valid, solvable design with a fixed `LT` at x 3, y 2 is exported
- **THEN** the JSON's `fixed` list holds that pipe and parsing it gives back the design

#### Scenario: Test played
- **WHEN** the developer test plays a design with a fixed `H`
- **THEN** the game screen starts with that `H` on the board as a fixed pipe, and a drop on it is refused

#### Scenario: Process recreated
- **WHEN** the system kills and recreates the app while a design with fixed pipes is open
- **THEN** the designer shows the same fixed pipes and the chosen fixed pipe section

## MODIFIED Requirements

### Requirement: New level and copy of a bundled level
A new design SHALL start as a valid, solvable level: id one higher than the number of bundled levels, name "New level", a 7 by 5 grid, start at x 0, y 2 open `RIGHT`, finish at x 6, y 2 open `LEFT`, no blocked cells, no mandatory sections, no fixed pipes, flow initial delay 20000 ms and 0.4 cells per second, the calculated star thresholds 200, 250 and 330, and every optional field at its default. The designer SHALL also offer to start from a copy of any bundled level, which loads all of that level's fields except the star thresholds, which are calculated.

#### Scenario: Blank level
- **WHEN** the designer opens and 4 levels are bundled
- **THEN** the design has id 5, name "New level", a 7 by 5 grid with start at 0,2 `RIGHT` and finish at 6,2 `LEFT`, star thresholds 200, 250 and 330, and is reported valid and solvable

#### Scenario: Copy a bundled level
- **WHEN** the developer chooses to start from bundled level 2
- **THEN** the design holds every field of level 2, including its blocked cells, mandatory sections and fixed pipes, with the star thresholds calculated from its free cells

### Requirement: Placing the start and the finish
The designer SHALL have a Start tool and a Finish tool. With one of them selected, tapping a cell SHALL move that endpoint to the cell, keeping its open side; if the cell was blocked it SHALL be unblocked. Tapping the cell of the other endpoint, a cell holding a mandatory section or a cell holding a fixed pipe SHALL do nothing. The designer SHALL show an open side control for the start and for the finish with the choices `LEFT`, `RIGHT`, `TOP` and `BOTTOM`. The grid SHALL draw the start and the finish with their open sides as in the game.

#### Scenario: Move the start
- **WHEN** the Start tool is selected and the developer taps x 2, y 3
- **THEN** the start is at x 2, y 3 with its previous open side

#### Scenario: Start onto a blocked cell
- **WHEN** the Start tool is selected and the developer taps a blocked cell
- **THEN** the start moves there and the cell is no longer blocked

#### Scenario: Endpoints cannot share a cell
- **WHEN** the Finish tool is selected and the developer taps the start's cell
- **THEN** neither endpoint changes

#### Scenario: Endpoint onto a mandatory section
- **WHEN** the Start tool is selected and the developer taps a cell holding a mandatory section
- **THEN** the start and the mandatory section do not change

#### Scenario: Endpoint onto a fixed pipe
- **WHEN** the Finish tool is selected and the developer taps a cell holding a fixed pipe
- **THEN** the finish and the fixed pipe do not change

#### Scenario: Change the open side
- **WHEN** the developer sets the finish's open side to `TOP`
- **THEN** the grid shows the finish opening upwards and the design's finish open side is `TOP`

### Requirement: Blocked cells
The designer SHALL have a Block tool. With it selected, tapping a cell SHALL toggle whether it is blocked. Dragging across cells SHALL apply the same change to every cell passed: block them when the first cell was free, unblock them when it was blocked. The start and finish cells and cells holding a mandatory section or a fixed pipe SHALL never be blocked by the tool. A Clear blocked action SHALL remove all blocked cells.

#### Scenario: Toggle a cell
- **WHEN** the Block tool is selected and the developer taps a free cell, then taps it again
- **THEN** the cell is blocked after the first tap and free after the second

#### Scenario: Drag to block
- **WHEN** the Block tool is selected and the developer drags from a free cell across three more cells in a row
- **THEN** all four cells are blocked

#### Scenario: Endpoints are skipped
- **WHEN** the developer drags the Block tool across the start's cell
- **THEN** the start's cell stays free and the cells before and after it are blocked

#### Scenario: Mandatory sections are skipped
- **WHEN** the developer taps a cell holding a mandatory section with the Block tool
- **THEN** the cell is not blocked and the mandatory section stays

#### Scenario: Fixed pipes are skipped
- **WHEN** the developer drags the Block tool across a cell holding a fixed pipe
- **THEN** that cell is not blocked, the fixed pipe stays and the cells before and after it are blocked

#### Scenario: Clear blocked
- **WHEN** the design has blocked cells and the developer taps Clear blocked
- **THEN** the design has no blocked cells

### Requirement: Where mandatory sections can be placed
The mandatory section tools SHALL do nothing on a blocked cell, on the start or the finish, on a cell holding a fixed pipe, or when the design already has 6 mandatory sections and the tapped cell holds none.

#### Scenario: Blocked cell refused
- **WHEN** the Mandatory cross tool is selected and the developer taps a blocked cell
- **THEN** the design is unchanged

#### Scenario: Endpoint refused
- **WHEN** the Mandatory reservoir tool is selected and the developer taps the start's cell
- **THEN** the design is unchanged

#### Scenario: Fixed pipe refused
- **WHEN** the Mandatory cross tool is selected and the developer taps a cell holding a fixed pipe
- **THEN** the design is unchanged

#### Scenario: At most six
- **WHEN** the design has 6 mandatory sections and the developer taps a free cell with a mandatory section tool
- **THEN** no section is added, and tapping one of the 6 still removes or changes it

### Requirement: Live validation and solvability
After every change the designer SHALL validate the design with `LevelValidator` and, when it is valid, check it with `LevelSolver`. It SHALL show the status (valid and solvable, invalid, or unsolvable), list every validation error with its field and message, and show the solver's reason for an unsolvable level. Errors about a cell (start, finish, a blocked cell, a mandatory section or a fixed pipe) SHALL also be highlighted on the grid.

#### Scenario: Valid and solvable
- **WHEN** the design is valid and the solver finds a route
- **THEN** the status shows the level as valid and solvable and no errors are listed

#### Scenario: Several errors listed
- **WHEN** the start opens onto the grid edge and the star thresholds are not increasing
- **THEN** both errors are listed with their fields, and the start cell is highlighted

#### Scenario: Unsolvable
- **WHEN** blocked cells wall off the finish
- **THEN** the status shows the level as unsolvable with the solver's reason

#### Scenario: Fixed pipe turns the route away
- **WHEN** the developer places a fixed `V` on the facing cell of a start that opens to the right
- **THEN** the status shows the level as unsolvable with the solver's reason, which names the start

### Requirement: Calculated star thresholds
The designer SHALL calculate the star thresholds from the free cells: every cell of the grid except blocked cells, the start and the finish; cells holding a mandatory section or a fixed pipe count as free. Three stars SHALL be free cells times 10. Two stars and one star SHALL be 75% and 60% of the three-star value, each rounded to the nearest 10, with halves rounded up. The thresholds SHALL be recalculated after every edit and SHALL NOT be editable.

#### Scenario: Example grid
- **WHEN** the design is a 7 by 9 grid with 1 blocked cell
- **THEN** it has 60 free cells and the star thresholds are 360, 450 and 600

#### Scenario: Halves round up
- **WHEN** the design has 10 free cells
- **THEN** the star thresholds are 60, 80 and 100

#### Scenario: Recalculated on edit
- **WHEN** the developer blocks one more free cell in a design with 60 free cells
- **THEN** the star thresholds become 350, 440 and 590

#### Scenario: Mandatory cells count as free
- **WHEN** the developer places a mandatory section on a free cell
- **THEN** the star thresholds are unchanged

#### Scenario: Fixed pipe cells count as free
- **WHEN** the developer places a fixed pipe on a free cell
- **THEN** the star thresholds are unchanged

#### Scenario: Read-only
- **WHEN** the developer looks at the star thresholds in the panel
- **THEN** they are shown as values that cannot be typed into

#### Scenario: Too few free cells
- **WHEN** the design has 3 free cells
- **THEN** the star thresholds are 20, 20 and 30 and the design is reported invalid because they are not strictly increasing

### Requirement: Grid size
The designer SHALL let the developer set the number of columns and rows, each between 4 and 50, with controls that cannot go outside that range. When the grid shrinks, blocked cells and fixed pipes outside the new grid SHALL be removed, and a start or finish outside the new grid SHALL be moved to the nearest cell inside it, keeping its open side.

#### Scenario: Grow the grid
- **WHEN** the developer raises the columns from 7 to 10
- **THEN** the grid shows 10 columns and the start, finish, blocked cells and fixed pipes keep their positions

#### Scenario: Shrink removes blocked cells
- **WHEN** a blocked cell is at x 8, y 1 and the developer lowers the columns from 10 to 8
- **THEN** that blocked cell is removed and the other blocked cells are kept

#### Scenario: Shrink removes fixed pipes
- **WHEN** a fixed pipe is at x 9, y 3 and the developer lowers the columns from 10 to 8
- **THEN** that fixed pipe is removed and the other fixed pipes are kept

#### Scenario: Shrink moves the finish
- **WHEN** the finish is at x 9, y 2 and the developer lowers the columns to 6
- **THEN** the finish is at x 5, y 2 with its open side unchanged

#### Scenario: Range limits
- **WHEN** the columns are 4
- **THEN** the control to lower the columns is disabled, and at 50 the control to raise them is disabled
