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
A new design SHALL start as a valid, solvable level: id one higher than the number of bundled levels, name "New level", a 7 by 5 grid, start at x 0, y 2 open `RIGHT`, finish at x 6, y 2 open `LEFT`, no blocked cells, no mandatory sections, no portals, flow initial delay 20000 ms and 0.4 cells per second, the calculated star thresholds 200, 250 and 330, and every optional field at its default. The designer SHALL also offer to start from a copy of any bundled level, which loads all of that level's fields except the star thresholds, which are calculated.

#### Scenario: Blank level
- **WHEN** the designer opens and 4 levels are bundled
- **THEN** the design has id 5, name "New level", a 7 by 5 grid with start at 0,2 `RIGHT` and finish at 6,2 `LEFT`, star thresholds 200, 250 and 330, and is reported valid and solvable

#### Scenario: Copy a bundled level
- **WHEN** the developer chooses to start from bundled level 2
- **THEN** the design holds every field of level 2, including its blocked cells, mandatory sections and portal pairs, with the star thresholds calculated from its free cells

### Requirement: Grid size
The designer SHALL let the developer set the number of columns between 4 and 25 and the number of rows between 4 and 15, with controls that cannot go outside those ranges. When the grid shrinks, blocked cells and leaky tiles outside the new grid SHALL be removed, a start or finish outside the new grid SHALL be moved to the nearest cell inside it, keeping its open side, and a portal pair with a portal outside the new grid SHALL be removed. A design that is larger than 25 columns or 15 rows when it is restored (for example one kept from before an app update) SHALL be shrunk to fit with the same rules.

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
- **THEN** the control to lower the columns is disabled, at 25 columns the control to raise the columns is disabled, and at 15 rows the control to raise the rows is disabled

#### Scenario: Shrink removes leaky tiles
- **WHEN** a leaky tile is at x 2, y 6 and the developer lowers the rows from 8 to 5
- **THEN** that leaky tile is removed and the leaky tiles inside the grid are kept

#### Scenario: Shrink removes a portal pair
- **WHEN** a portal is at x 9, y 1, its partner is at x 2, y 3, and the developer lowers the columns from 10 to 8
- **THEN** both portals of that pair are removed and the other pairs are kept

#### Scenario: Oversized design is shrunk on restore
- **WHEN** the designer restores a design of 24 columns by 24 rows whose finish is at x 6, y 23
- **THEN** the design has 24 columns and 15 rows and the finish is at x 6, y 14 with its open side unchanged

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

### Requirement: Live validation and solvability
After every change the designer SHALL validate the design with `LevelValidator` and, when it is valid, check it with `LevelSolver`. It SHALL show the status (valid and solvable, invalid, or unsolvable), list every validation error with its field and message, and show the solver's reason for an unsolvable level. Errors about a cell (start, finish, a blocked cell, a portal or a leaky tile) SHALL also be highlighted on the grid. When a solvable design has no leak-free route, the status SHALL add a note that every route crosses a leaky tile; the note SHALL NOT disable test play or export.

#### Scenario: Valid and solvable
- **WHEN** the design is valid and the solver finds a route
- **THEN** the status shows the level as valid and solvable and no errors are listed

#### Scenario: Several errors listed
- **WHEN** the start opens onto the grid edge and the star thresholds are not increasing
- **THEN** both errors are listed with their fields, and the start cell is highlighted

#### Scenario: Unsolvable
- **WHEN** blocked cells wall off the finish
- **THEN** the status shows the level as unsolvable with the solver's reason

#### Scenario: Every route crosses a leaky tile
- **WHEN** the design is valid and solvable and every route passes a leaky tile
- **THEN** the status shows the level as valid and solvable with a note that every route crosses a leaky tile, and Test play and Export stay enabled

#### Scenario: Walled-off portal highlighted
- **WHEN** a portal's open side faces a blocked cell
- **THEN** the error for that portal is listed with its field, such as `portals[0].b`, and the portal is highlighted

#### Scenario: Solvable only through a portal
- **WHEN** blocked cells wall off the finish and a portal pair crosses the wall
- **THEN** the status shows the level as valid and solvable

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
The mandatory section tools SHALL do nothing on a blocked cell, on a leaky tile, on the start or the finish, on a portal, or when the design already has 6 mandatory sections and the tapped cell holds none.

#### Scenario: Blocked cell refused
- **WHEN** the Mandatory cross tool is selected and the developer taps a blocked cell
- **THEN** the design is unchanged

#### Scenario: Endpoint refused
- **WHEN** the Mandatory reservoir tool is selected and the developer taps the start's cell
- **THEN** the design is unchanged

#### Scenario: At most six
- **WHEN** the design has 6 mandatory sections and the developer taps a free cell with a mandatory section tool
- **THEN** no section is added, and tapping one of the 6 still removes or changes it

#### Scenario: Leaky tile refused
- **WHEN** the Mandatory cross tool is selected and the developer taps a leaky tile
- **THEN** the design is unchanged

#### Scenario: Portal refused
- **WHEN** the Mandatory cross tool is selected and the developer taps a portal
- **THEN** the design is unchanged

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
The designer SHALL calculate the star thresholds from the free cells: every cell of the grid except blocked cells, leaky tiles, the start, the finish and portals; cells holding a mandatory section count as free. Three stars SHALL be free cells times 10. Two stars and one star SHALL be 75% and 60% of the three-star value, each rounded to the nearest 10, with halves rounded up. The thresholds SHALL be recalculated after every edit and SHALL NOT be editable.

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

#### Scenario: Leaky tiles do not count
- **WHEN** the developer makes one free cell leaky in a design with 60 free cells
- **THEN** the star thresholds become 350, 440 and 590

#### Scenario: Portals are not free cells
- **WHEN** the developer places a portal pair in a design with 60 free cells
- **THEN** the design has 58 free cells and the star thresholds become 350, 440 and 580

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

### Requirement: Leaky tile tool
The designer SHALL have a Leaky tile tool. With it selected, tapping a free cell SHALL make it leaky and tapping a leaky tile SHALL make it free again. Dragging across cells SHALL apply the same change to every cell passed, chosen by the first cell. A blocked cell passed SHALL become leaky and no longer blocked. The start and finish cells, portals and cells holding a mandatory section SHALL never be made leaky. The grid SHALL draw leaky tiles as in the game.

#### Scenario: Toggle a leaky tile
- **WHEN** the Leaky tile tool is selected and the developer taps a free cell, then taps it again
- **THEN** the cell is leaky after the first tap and free after the second, and the grid draws it as a leaky tile in between

#### Scenario: Drag to make leaky
- **WHEN** the Leaky tile tool is selected and the developer drags from a free cell across two more free cells in a row
- **THEN** all three cells are leaky

#### Scenario: Blocked cell becomes leaky
- **WHEN** the Leaky tile tool is selected and the developer taps a blocked cell
- **THEN** the cell is leaky and no longer blocked

#### Scenario: Endpoints and mandatory sections are skipped
- **WHEN** the developer drags the Leaky tile tool across the start's cell, a portal and a cell holding a mandatory section
- **THEN** none of those cells becomes leaky and the cells around them do

### Requirement: Other tools on a leaky tile
The Block tool SHALL turn a leaky tile into a blocked cell that is no longer leaky. The Start and Finish tools SHALL move an endpoint onto a leaky tile, and the Portal tool SHALL place a portal on one, each making that cell no longer leaky. A Clear leaky action SHALL remove all leaky tiles.

#### Scenario: Block a leaky tile
- **WHEN** the Block tool is selected and the developer taps a leaky tile
- **THEN** the cell is blocked and no longer leaky

#### Scenario: Endpoint onto a leaky tile
- **WHEN** the Finish tool is selected and the developer taps a leaky tile
- **THEN** the finish moves there and the cell is no longer leaky

#### Scenario: Clear leaky
- **WHEN** the design has leaky tiles and the developer taps Clear leaky
- **THEN** the design has no leaky tiles and its blocked cells are unchanged

### Requirement: Leaky tiles leave the designer with the design
Leaky tiles SHALL be loaded when the designer starts from a copy of a bundled level, SHALL be part of the exported JSON, SHALL be on the board when a test play starts, and SHALL be kept when the design survives a configuration change or process death.

#### Scenario: Copied
- **WHEN** the developer starts from a copy of a bundled level with leaky tiles
- **THEN** the design holds the same leaky tiles

#### Scenario: Exported
- **WHEN** a valid, solvable design with a leaky tile at x 3, y 2 is exported
- **THEN** the JSON's `leaky` list holds that cell and parsing it gives back the design

#### Scenario: Test played
- **WHEN** the developer test plays a design with leaky tiles
- **THEN** the game screen starts with those leaky tiles on the board

#### Scenario: Process recreated
- **WHEN** the system kills and recreates the app while a design with leaky tiles is open
- **THEN** the designer shows the same leaky tiles
