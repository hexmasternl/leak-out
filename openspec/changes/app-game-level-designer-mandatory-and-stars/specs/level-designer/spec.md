## ADDED Requirements

### Requirement: Mandatory section tools
The designer SHALL have a Mandatory cross tool and a Mandatory reservoir tool. With one selected, tapping a free cell SHALL place a mandatory section of that kind (`X` or `RES`). Tapping a mandatory section of the same kind SHALL remove it; tapping one of the other kind SHALL change its kind. No other section can be made mandatory. A drag with these tools SHALL only act on the cell it starts on.

#### Scenario: Place a mandatory cross
- **WHEN** the Mandatory cross tool is selected and the developer taps the free cell x 3, y 2
- **THEN** the design has a mandatory `X` at x 3, y 2 and the grid draws it on a red-tinted tile

#### Scenario: Remove by tapping again
- **WHEN** the Mandatory reservoir tool is selected and the developer taps a cell holding a mandatory `RES`
- **THEN** the mandatory section is removed

#### Scenario: Change the kind
- **WHEN** the Mandatory reservoir tool is selected and the developer taps a cell holding a mandatory `X`
- **THEN** that cell holds a mandatory `RES` and the number of mandatory sections is unchanged

#### Scenario: Drag only acts on the first cell
- **WHEN** the Mandatory cross tool is selected and the developer drags from a free cell across three more cells
- **THEN** only the cell the drag started on holds a mandatory `X`

### Requirement: Where mandatory sections can be placed
The mandatory section tools SHALL do nothing on a blocked cell, on the start or the finish, or when the design already has 6 mandatory sections and the tapped cell holds none.

#### Scenario: Blocked cell refused
- **WHEN** the Mandatory cross tool is selected and the developer taps a blocked cell
- **THEN** the design is unchanged

#### Scenario: Endpoint refused
- **WHEN** the Mandatory reservoir tool is selected and the developer taps the start's cell
- **THEN** the design is unchanged

#### Scenario: At most six
- **WHEN** the design has 6 mandatory sections and the developer taps a free cell with a mandatory section tool
- **THEN** no section is added, and tapping one of the 6 still removes or changes it

### Requirement: Mandatory sections leave the designer with the design
Mandatory sections SHALL be part of the exported JSON, SHALL be on the board when a test play starts, and SHALL be kept when the design survives a configuration change or process death.

#### Scenario: Exported
- **WHEN** a valid, solvable design with a mandatory `X` at x 3, y 2 is exported
- **THEN** the JSON's `mandatory` list holds that section and parsing it gives back the design

#### Scenario: Test played
- **WHEN** the developer test plays a design with a mandatory `RES`
- **THEN** the game screen starts with that reservoir on the board as a mandatory section

#### Scenario: Process recreated
- **WHEN** the system kills and recreates the app while a design with mandatory sections is open
- **THEN** the designer shows the same mandatory sections

### Requirement: Calculated star thresholds
The designer SHALL calculate the star thresholds from the free cells: every cell of the grid except blocked cells, the start and the finish; cells holding a mandatory section count as free. Three stars SHALL be free cells times 10. Two stars and one star SHALL be 75% and 60% of the three-star value, each rounded to the nearest 10, with halves rounded up. The thresholds SHALL be recalculated after every edit and SHALL NOT be editable.

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

#### Scenario: Read-only
- **WHEN** the developer looks at the star thresholds in the panel
- **THEN** they are shown as values that cannot be typed into

#### Scenario: Too few free cells
- **WHEN** the design has 3 free cells
- **THEN** the star thresholds are 20, 20 and 30 and the design is reported invalid because they are not strictly increasing

## MODIFIED Requirements

### Requirement: New level and copy of a bundled level
A new design SHALL start as a valid, solvable level: id one higher than the number of bundled levels, name "New level", a 7 by 5 grid, start at x 0, y 2 open `RIGHT`, finish at x 6, y 2 open `LEFT`, no blocked cells, no mandatory sections, flow initial delay 20000 ms and 0.4 cells per second, the calculated star thresholds 200, 250 and 330, and every optional field at its default. The designer SHALL also offer to start from a copy of any bundled level, which loads all of that level's fields except the star thresholds, which are calculated.

#### Scenario: Blank level
- **WHEN** the designer opens and 4 levels are bundled
- **THEN** the design has id 5, name "New level", a 7 by 5 grid with start at 0,2 `RIGHT` and finish at 6,2 `LEFT`, star thresholds 200, 250 and 330, and is reported valid and solvable

#### Scenario: Copy a bundled level
- **WHEN** the developer chooses to start from bundled level 2
- **THEN** the design holds every field of level 2, including its blocked cells and mandatory sections, with the star thresholds calculated from its free cells

### Requirement: Placing the start and the finish
The designer SHALL have a Start tool and a Finish tool. With one of them selected, tapping a cell SHALL move that endpoint to the cell, keeping its open side; if the cell was blocked it SHALL be unblocked. Tapping the cell of the other endpoint or a cell holding a mandatory section SHALL do nothing. The designer SHALL show an open side control for the start and for the finish with the choices `LEFT`, `RIGHT`, `TOP` and `BOTTOM`. The grid SHALL draw the start and the finish with their open sides as in the game.

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

#### Scenario: Change the open side
- **WHEN** the developer sets the finish's open side to `TOP`
- **THEN** the grid shows the finish opening upwards and the design's finish open side is `TOP`

### Requirement: Blocked cells
The designer SHALL have a Block tool. With it selected, tapping a cell SHALL toggle whether it is blocked. Dragging across cells SHALL apply the same change to every cell passed: block them when the first cell was free, unblock them when it was blocked. The start and finish cells and cells holding a mandatory section SHALL never be blocked by the tool. A Clear blocked action SHALL remove all blocked cells.

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

#### Scenario: Clear blocked
- **WHEN** the design has blocked cells and the developer taps Clear blocked
- **THEN** the design has no blocked cells

### Requirement: Other level fields
The designer SHALL let the developer edit the level's id, name, flow settings (initial delay, cells per second, reservoir speed factor, speed ramp per cell), stack settings (visible size and the weight of each of the eight sections) and placement rules (allow replace, replace penalty). A numeric input that is not a number SHALL be marked as invalid and SHALL NOT change the design.

#### Scenario: Edit flow speed
- **WHEN** the developer enters 0.8 as cells per second
- **THEN** the design's `flow.cellsPerSecond` is 0.8

#### Scenario: Enable the cross
- **WHEN** the developer sets the weight of `X` to 1
- **THEN** the design's stack weight for `X` is 1

#### Scenario: Not a number
- **WHEN** the developer types "abc" as the initial delay
- **THEN** the field is marked invalid and the design keeps its previous initial delay
