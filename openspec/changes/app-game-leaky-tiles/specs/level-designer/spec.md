## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Grid size
The designer SHALL let the developer set the number of columns and rows, each between 4 and 50, with controls that cannot go outside that range. When the grid shrinks, blocked cells and leaky tiles outside the new grid SHALL be removed, a start or finish outside the new grid SHALL be moved to the nearest cell inside it, keeping its open side, and a portal pair with a portal outside the new grid SHALL be removed.

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

#### Scenario: Shrink removes leaky tiles
- **WHEN** a leaky tile is at x 2, y 6 and the developer lowers the rows from 8 to 5
- **THEN** that leaky tile is removed and the leaky tiles inside the grid are kept

#### Scenario: Shrink removes a portal pair
- **WHEN** a portal is at x 9, y 1, its partner is at x 2, y 3, and the developer lowers the columns from 10 to 8
- **THEN** both portals of that pair are removed and the other pairs are kept

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
