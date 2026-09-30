## ADDED Requirements

### Requirement: Playing field fills the space left by the HUD and the stack
The game screen SHALL show the playing field in all the space that is left after the HUD bar at the top and the stack column on the left. The field SHALL NOT cover the HUD or the stack, and the HUD and the stack SHALL NOT cover the field. Level content SHALL be drawn only inside the field: when the level is larger than the field, the part outside the field SHALL be clipped.

#### Scenario: Field is placed next to the stack and below the HUD
- **WHEN** the game screen is shown in landscape
- **THEN** the field starts directly right of the stack column and directly below the HUD, and reaches the right and bottom edges of the screen

#### Scenario: Large level is clipped to the field
- **WHEN** a level is shown that is larger than the field
- **THEN** no part of the level is drawn over the HUD or the stack column

### Requirement: Cell size limits
The field SHALL draw every cell as a square. The cell size SHALL never be smaller than 40 dp and never be larger than 120 dp, whatever the level size, screen size or zoom.

#### Scenario: Cells never get too small
- **WHEN** the player zooms out as far as possible on a 16 × 12 level on a 360 dp tall screen
- **THEN** every cell is at least 40 dp wide and high

#### Scenario: Cells never get too large
- **WHEN** the player zooms in as far as possible
- **THEN** every cell is at most 120 dp wide and high

### Requirement: The complete level is shown when it fits
When a level starts, the field SHALL use the largest cell size at which the complete level fits inside the field, limited to the allowed cell sizes. When the complete level fits at that size, the field SHALL show the complete level, centred in the field. When the level does not fit even at the smallest cell size, the field SHALL start at the smallest cell size and SHALL show the part of the level around the start cell.

#### Scenario: Small level is shown completely
- **WHEN** an 8 × 6 level starts on a screen where the field is 600 dp wide and 300 dp high
- **THEN** the cells are 50 dp, and the complete level is shown centred in the field

#### Scenario: Level that would need huge cells is capped
- **WHEN** a level starts whose complete grid would fit at a cell size above 120 dp
- **THEN** the cells are 120 dp and the complete level is shown centred in the field

#### Scenario: Large level starts around the start cell
- **WHEN** a 16 × 12 level starts on a screen where 12 rows of 40 dp do not fit in the field height
- **THEN** the cells are 40 dp and the start cell is visible inside the field

### Requirement: Pinch to zoom within the limits
The player SHALL be able to zoom the field with a two-finger pinch. The zoom SHALL keep the point between the two fingers in the same place on the screen. The field SHALL NOT zoom out further than the starting cell size, which is the size at which the complete level fits or the smallest allowed cell size. The field SHALL NOT zoom in beyond 120 dp cells. A pinch that goes past a limit SHALL stop at the limit.

#### Scenario: Zooming in around the fingers
- **WHEN** the player pinches outwards with the fingers around a cell
- **THEN** the cells get larger and that cell stays under the fingers

#### Scenario: Zoom out stops when the level fits
- **WHEN** the complete level is shown and the player pinches inwards
- **THEN** the cell size does not get smaller

#### Scenario: Zoom in stops at the upper limit
- **WHEN** the player keeps pinching outwards
- **THEN** the cell size stops at 120 dp

### Requirement: Pan when the level is larger than the field
When the level is wider or higher than the field, the player SHALL be able to press on the field and drag with one finger to move the level. The level SHALL move with the finger. The level SHALL NOT be moved so far that empty space appears between a level edge and the field edge on that side. On an axis where the level fits inside the field, the level SHALL stay centred and SHALL NOT move. A pan SHALL start only after the finger has moved past the touch slop, so a short tap does not move the level.

#### Scenario: Panning a large level
- **WHEN** a level is higher than the field and the player drags upwards on the field
- **THEN** the level moves up with the finger and more rows at the bottom become visible

#### Scenario: Pan stops at the level edge
- **WHEN** the player keeps dragging after the bottom row is fully visible
- **THEN** the level does not move further and the bottom row stays against the bottom edge of the field

#### Scenario: Level that fits cannot be panned
- **WHEN** the complete level is shown centred and the player drags on the field
- **THEN** the level does not move

#### Scenario: Zooming out keeps the level inside its limits
- **WHEN** the player has panned to a corner of a zoomed-in level and then zooms out
- **THEN** the level stays within its pan limits, and is centred again on every axis where it fits

### Requirement: The level is never rotated
The field SHALL always show the level with its rows horizontal and its columns vertical. A two-finger twist SHALL NOT rotate the level.

#### Scenario: Twisting two fingers
- **WHEN** the player twists two fingers on the field
- **THEN** the level is not rotated

### Requirement: Dragging a section does not move the field
While the player drags a section from the stack, the field SHALL NOT pan or zoom, also not when the section is dragged over the field or a second finger touches the field.

#### Scenario: Section drag over the field
- **WHEN** the player drags a section from the stack across the field
- **THEN** the level stays where it is

### Requirement: Pause keeps the view
Pausing, resuming and going to the background SHALL keep the zoom and the pan position. Restarting the level SHALL reset the view to the starting cell size and position.

#### Scenario: Resume keeps the view
- **WHEN** the player has zoomed and panned, pauses and resumes
- **THEN** the field shows the same part of the level at the same cell size

#### Scenario: Restart resets the view
- **WHEN** the player has zoomed and panned, and restarts the level
- **THEN** the field shows the level at the starting cell size and position

### Requirement: Field draws the level cells
The field SHALL draw the grid, the start cell with its open side, the finish cell with its open side, every blocked cell and every placed section. Each kind of cell SHALL look different from the others. Placed sections SHALL be drawn with the same section drawing as the stack.

#### Scenario: Level cells are drawn
- **WHEN** a level with blocked cells starts
- **THEN** the field shows the grid, the start and finish with their openings, and the blocked cells, all different from empty cells

#### Scenario: Placed section is drawn
- **WHEN** a section is placed on a cell
- **THEN** the field draws that section on the cell
