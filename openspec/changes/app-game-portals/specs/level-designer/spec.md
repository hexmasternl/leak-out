## ADDED Requirements

### Requirement: Portal tool
The designer SHALL have a Portal tool. With it selected and no portal selected, tapping a free cell SHALL place a portal: as the second portal of a pair that has only one, otherwise as the first portal of a new pair. Tapping a portal SHALL select it; tapping it again SHALL deselect it. With a portal selected, tapping a free cell SHALL move it there, keeping its open side. A drag SHALL only act on the cell it starts on.

#### Scenario: Place a pair
- **WHEN** the Portal tool is selected and the developer taps the free cells x 1, y 1 and x 6, y 3
- **THEN** the design has one portal pair with portals at those two cells, drawn in the first pair colour and symbol

#### Scenario: Unfinished pair
- **WHEN** the developer has placed only the first portal of a pair
- **THEN** the design is reported invalid with an error saying that the pair needs a second portal, and that portal is highlighted

#### Scenario: Move a portal
- **WHEN** the developer taps the portal at x 6, y 3 and then taps the free cell x 7, y 4
- **THEN** that portal is at x 7, y 4 with its previous open side and the other portal of the pair has not moved

### Requirement: Portal open side and removal
When a portal is selected, the designer SHALL show an open side control for it with the choices `LEFT`, `RIGHT`, `TOP` and `BOTTOM`, and a Remove pair action. A new portal SHALL get the first of `RIGHT`, `LEFT`, `BOTTOM` and `TOP` whose facing cell lies inside the grid. Remove pair SHALL remove both portals of the selected portal's pair; the pairs after it SHALL move up one place, taking that pair's number, colour and symbol.

#### Scenario: Change a portal's open side
- **WHEN** a portal is selected and the developer sets its open side to `TOP`
- **THEN** the grid shows the portal opening upwards and the design's portal has open side `TOP`

#### Scenario: Remove a pair
- **WHEN** the design has two pairs, a portal of pair 1 is selected and the developer taps Remove pair
- **THEN** the design has one pair, which is the former pair 2, now drawn as pair 1

#### Scenario: New portal at the right edge
- **WHEN** the developer places a portal in the last column
- **THEN** the new portal opens to the left

### Requirement: Where portals can be placed
The Portal tool SHALL do nothing on a blocked cell, the start, the finish or a cell holding a mandatory section, and SHALL NOT start a new pair when the design already has 3 pairs.

#### Scenario: Blocked cell refused
- **WHEN** the Portal tool is selected and the developer taps a blocked cell
- **THEN** the design is unchanged

#### Scenario: At most three pairs
- **WHEN** the design has 3 complete pairs and the developer taps a free cell with the Portal tool and no portal selected
- **THEN** no portal is added

### Requirement: Portals leave the designer with the design
Portal pairs SHALL be part of the exported JSON, SHALL be on the board when a test play starts, and SHALL be kept when the design survives a configuration change or process death. Export and test play SHALL stay disabled while a pair is unfinished, as for any invalid design.

#### Scenario: Exported
- **WHEN** a valid, solvable design with one portal pair is exported
- **THEN** the JSON's `portals` list holds that pair and parsing it gives back the design

#### Scenario: Test played
- **WHEN** the developer test plays a design with a portal pair
- **THEN** the game screen starts with both portals on the board and the liquid can pass them

#### Scenario: Process recreated
- **WHEN** the system kills and recreates the app while a design with portals, one of them selected, is open
- **THEN** the designer shows the same portals and the same selected portal

## MODIFIED Requirements

### Requirement: New level and copy of a bundled level
A new design SHALL start as a valid, solvable level: id one higher than the number of bundled levels, name "New level", a 7 by 5 grid, start at x 0, y 2 open `RIGHT`, finish at x 6, y 2 open `LEFT`, no blocked cells, no mandatory sections, no portals, flow initial delay 20000 ms and 0.4 cells per second, the calculated star thresholds 200, 250 and 330, and every optional field at its default. The designer SHALL also offer to start from a copy of any bundled level, which loads all of that level's fields except the star thresholds, which are calculated.

#### Scenario: Blank level
- **WHEN** the designer opens and 4 levels are bundled
- **THEN** the design has id 5, name "New level", a 7 by 5 grid with start at 0,2 `RIGHT` and finish at 6,2 `LEFT`, star thresholds 200, 250 and 330, and is reported valid and solvable

#### Scenario: Copy a bundled level
- **WHEN** the developer chooses to start from bundled level 2
- **THEN** the design holds every field of level 2, including its blocked cells, mandatory sections and portal pairs, with the star thresholds calculated from its free cells

### Requirement: Grid size
The designer SHALL let the developer set the number of columns and rows, each between 4 and 50, with controls that cannot go outside that range. When the grid shrinks, blocked cells outside the new grid SHALL be removed, a start or finish outside the new grid SHALL be moved to the nearest cell inside it, keeping its open side, and a portal pair with a portal outside the new grid SHALL be removed.

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

#### Scenario: Shrink removes a portal pair
- **WHEN** a portal is at x 9, y 1, its partner is at x 2, y 3, and the developer lowers the columns from 10 to 8
- **THEN** both portals of that pair are removed and the other pairs are kept

### Requirement: Placing the start and the finish
The designer SHALL have a Start tool and a Finish tool. With one of them selected, tapping a cell SHALL move that endpoint to the cell, keeping its open side; if the cell was blocked it SHALL be unblocked. Tapping the cell of the other endpoint, a cell holding a mandatory section or a portal SHALL do nothing. The designer SHALL show an open side control for the start and for the finish with the choices `LEFT`, `RIGHT`, `TOP` and `BOTTOM`. The grid SHALL draw the start and the finish with their open sides as in the game.

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

#### Scenario: Endpoint onto a portal
- **WHEN** the Finish tool is selected and the developer taps a portal
- **THEN** the finish and the portal do not change

### Requirement: Blocked cells
The designer SHALL have a Block tool. With it selected, tapping a cell SHALL toggle whether it is blocked. Dragging across cells SHALL apply the same change to every cell passed: block them when the first cell was free, unblock them when it was blocked. The start and finish cells, cells holding a mandatory section and portals SHALL never be blocked by the tool. A Clear blocked action SHALL remove all blocked cells.

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

#### Scenario: Portals are skipped
- **WHEN** the developer drags the Block tool across a portal
- **THEN** the portal stays and the cells before and after it are blocked

### Requirement: Live validation and solvability
After every change the designer SHALL validate the design with `LevelValidator` and, when it is valid, check it with `LevelSolver`. It SHALL show the status (valid and solvable, invalid, or unsolvable), list every validation error with its field and message, and show the solver's reason for an unsolvable level. Errors about a cell (start, finish, a blocked cell or a portal) SHALL also be highlighted on the grid.

#### Scenario: Valid and solvable
- **WHEN** the design is valid and the solver finds a route
- **THEN** the status shows the level as valid and solvable and no errors are listed

#### Scenario: Several errors listed
- **WHEN** the start opens onto the grid edge and the star thresholds are not increasing
- **THEN** both errors are listed with their fields, and the start cell is highlighted

#### Scenario: Unsolvable
- **WHEN** blocked cells wall off the finish
- **THEN** the status shows the level as unsolvable with the solver's reason

#### Scenario: Walled-off portal highlighted
- **WHEN** a portal's open side faces a blocked cell
- **THEN** the error for that portal is listed with its field, such as `portals[0].b`, and the portal is highlighted

#### Scenario: Solvable only through a portal
- **WHEN** blocked cells wall off the finish and a portal pair crosses the wall
- **THEN** the status shows the level as valid and solvable

### Requirement: Where mandatory sections can be placed
The mandatory section tools SHALL do nothing on a blocked cell, on the start or the finish, on a portal, or when the design already has 6 mandatory sections and the tapped cell holds none.

#### Scenario: Blocked cell refused
- **WHEN** the Mandatory cross tool is selected and the developer taps a blocked cell
- **THEN** the design is unchanged

#### Scenario: Endpoint refused
- **WHEN** the Mandatory reservoir tool is selected and the developer taps the start's cell
- **THEN** the design is unchanged

#### Scenario: At most six
- **WHEN** the design has 6 mandatory sections and the developer taps a free cell with a mandatory section tool
- **THEN** no section is added, and tapping one of the 6 still removes or changes it

#### Scenario: Portal refused
- **WHEN** the Mandatory cross tool is selected and the developer taps a portal
- **THEN** the design is unchanged

### Requirement: Calculated star thresholds
The designer SHALL calculate the star thresholds from the free cells: every cell of the grid except blocked cells, the start, the finish and portals; cells holding a mandatory section count as free. Three stars SHALL be free cells times 10. Two stars and one star SHALL be 75% and 60% of the three-star value, each rounded to the nearest 10, with halves rounded up. The thresholds SHALL be recalculated after every edit and SHALL NOT be editable.

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

#### Scenario: Portals are not free cells
- **WHEN** the developer places a portal pair in a design with 60 free cells
- **THEN** the design has 58 free cells and the star thresholds become 350, 440 and 580
