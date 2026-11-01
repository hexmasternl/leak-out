# game-play-playing-field Specification

## Purpose
TBD - created by syncing change app-game-play-playing-field. Update Purpose after archive.
## Requirements
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
The field SHALL draw the grid, the start cell with its open side, the finish cell with its open side, every blocked cell, every leaky tile and every placed section. Each kind of cell SHALL look different from the others. Placed sections SHALL be drawn with the same pipe art as the stack: the section's SVG asset with its glass overlay, scaled to the cell and following pan and zoom. The start SHALL be drawn as the start valve and the finish as the checkered finish, as described in the design system, so the player can tell at a glance where the liquid comes from and where it has to go.

#### Scenario: Level cells are drawn
- **WHEN** a level with blocked cells starts
- **THEN** the field shows the grid, the start valve and the checkered finish with their openings, and the blocked cells, all different from empty cells

#### Scenario: Placed section is drawn
- **WHEN** a section is placed on a cell
- **THEN** the field draws that section's pipe art with its glass overlay on the cell

#### Scenario: Pipe art follows zoom
- **WHEN** the player zooms in on a placed section
- **THEN** the section's pipe art is drawn at the new cell size, filling the cell

#### Scenario: Leaky tiles are drawn
- **WHEN** a level with blocked cells and leaky tiles starts
- **THEN** the leaky tiles are drawn as cracked, damp tiles that look different from empty cells, blocked cells and mandatory sections

### Requirement: Liquid flows through the placed pipes
The field SHALL draw the liquid inside the pipe art of placed sections: every channel the liquid has run through full, and the channel the liquid head is in filled from its entry side up to the head's progress. The progress SHALL come from the engine unchanged, so the liquid moves linearly in time with no easing, and the next section SHALL start filling the moment the previous one is full. The liquid head SHALL be rounded with its front exactly on the head, so it crosses into the next section without a jump. The tile under a pipe SHALL keep its colour when liquid enters it.

#### Scenario: Liquid fills the head section
- **WHEN** the liquid head is half way through a placed straight it entered from the left
- **THEN** the left half of that pipe shows liquid and the right half is dry

#### Scenario: Flow runs on into the next section
- **WHEN** the liquid head moves from one placed section into the next
- **THEN** the first section is full up to its exit border and the next fills from its entry border

#### Scenario: Tile keeps its colour
- **WHEN** liquid enters a placed section
- **THEN** the cell around the pipe looks the same as while it was dry

### Requirement: Tapping a tile places the next section
Besides dragging, a tap on a field cell SHALL place the stack's next section there, under exactly the same rules as a drop: only on an empty cell or an empty leaky tile, or on a placed section that may be replaced, and never while paused, after the level ended, during fast forward or during the replace lock. A tap SHALL be one finger lifted within the touch slop; a pan or a pinch SHALL NOT place anything.

#### Scenario: Tap on an empty cell
- **WHEN** the player taps an empty cell during play
- **THEN** the stack's next section is placed on that cell

#### Scenario: Tap on a cell that does not accept a section
- **WHEN** the player taps a blocked cell, the start, the finish or a section the liquid has entered
- **THEN** nothing is placed and the stack is unchanged

#### Scenario: Pan does not place
- **WHEN** the player swipes or pinches the field
- **THEN** the view moves and nothing is placed

#### Scenario: Tap on a leaky tile
- **WHEN** the player taps an empty leaky tile during play
- **THEN** the stack's next section is placed on that tile

### Requirement: Start valve
The start cell SHALL be drawn on the empty-cell tile, without a coloured tint, as a pipe stub and a valve wheel. The stub SHALL be the half of the straight section's pipe art (horizontal for a left or right open side, vertical for a top or bottom open side) between the centre of the cell and the open side, including the flange and the glass overlay, so it joins a section placed against the open side the same way two sections join. On top of the stub, centred in the cell, the field SHALL draw a brass valve wheel (radius 0.24 of the cell, four spokes, a hub) and, on the stub between the wheel and the open side, a brass chevron pointing out of the open side. The start SHALL use no success or leak colour.

#### Scenario: Start valve faces its open side
- **WHEN** a level starts whose start opens to the right
- **THEN** the start cell shows the brass valve wheel in its centre, a horizontal pipe stub from the centre to the right edge, and a chevron on the stub pointing right

#### Scenario: Stub joins a placed section
- **WHEN** the player places a horizontal straight directly right of a start that opens to the right
- **THEN** the stub's flange and bore line up with the section's flange and bore on the shared edge

#### Scenario: Start valve follows zoom
- **WHEN** the player zooms in on the start
- **THEN** the stub, wheel and chevron are drawn at the new cell size, keeping their proportions

### Requirement: Checkered finish
The finish cell SHALL be drawn on the empty-cell tile, without a coloured tint, as a pipe stub and a finish ring filled with a checker pattern. The stub SHALL be the half of the straight section's pipe art between the open side and the centre of the cell, including the flange and the glass overlay. On top of the stub, centred in the cell, the field SHALL draw a ring in the success colour (radius 0.24 of the cell) whose whole inner disc is filled with a flat checker pattern of 6 × 6 squares alternating light and dark, clipped to the disc. The finish SHALL show no flag or pole, and SHALL use no brass colour.

#### Scenario: Finish shows a checkered disc
- **WHEN** a level starts whose finish opens to the left
- **THEN** the finish cell shows a horizontal pipe stub from the left edge to the centre and, in the centre, a success-coloured ring filled with a light and dark checker pattern

#### Scenario: Start and finish are told apart
- **WHEN** a level starts
- **THEN** the start shows a brass valve wheel and the finish a success-coloured ring filled with a checker pattern, and neither shows the other's symbol

### Requirement: Liquid leaves the start valve
While the liquid head is in the start cell, the field SHALL draw the liquid in the start's stub, from the centre of the cell towards the open side, up to the head's progress, the same way the liquid is drawn in a section's channel. Once the head has left the start, the stub SHALL stay filled for the rest of the attempt. Before the liquid starts, the stub SHALL be dry. The finish's stub SHALL stay dry; reaching the finish is shown by the finish flash.

#### Scenario: Liquid comes out of the valve
- **WHEN** the countdown ends and the head is halfway through the start cell
- **THEN** the start's stub is filled from the centre half way to the open side

#### Scenario: Stub stays filled
- **WHEN** the head has moved on into the first placed section
- **THEN** the start's stub is completely filled

#### Scenario: Restart empties the stub
- **WHEN** the player restarts the level
- **THEN** the start's stub is dry again

### Requirement: Endpoint glows
While the countdown runs, the valve wheel SHALL have a brass glow that pulses with a period of 1.6 seconds. Once the liquid starts, the valve's glow SHALL stop pulsing and stay at a steady 60 %. Until the level is won or lost, the finish ring SHALL have a success glow that pulses with a period of 2 seconds. When the level is won, the finish flash SHALL replace the pulse and the ring SHALL stay solid afterwards. When the level is lost, the finish ring's glow SHALL stop pulsing and stay at a steady 60 %. The pulses SHALL follow game time, so they freeze while the game is paused. When the system setting to remove animations is on, both glows SHALL be steady at 60 % and SHALL NOT pulse.

#### Scenario: Valve pulses during the countdown
- **WHEN** a level starts and the countdown runs
- **THEN** the valve's glow grows and fades with a 1.6 second period

#### Scenario: Valve settles when the liquid starts
- **WHEN** the countdown ends
- **THEN** the valve's glow stays steady at 60 %

#### Scenario: Finish pulses until the win
- **WHEN** the liquid flows and the level is not yet won
- **THEN** the finish ring's glow grows and fades with a 2 second period

#### Scenario: Finish settles after a loss
- **WHEN** the level is lost
- **THEN** the finish ring's glow stays steady at 60 %

#### Scenario: Pulses freeze on pause
- **WHEN** the player pauses the game
- **THEN** the valve and finish glows stay as they were until the game resumes

#### Scenario: Reduced motion
- **WHEN** the system setting to remove animations is on and a level starts
- **THEN** the valve and the finish ring have steady glows at 60 % and do not pulse

### Requirement: Mandatory sections are drawn on a red tint
The field SHALL draw a mandatory section with the same pipe art as a placed section of its kind, on a tile tinted with the design system's `leak.tint` and edged in `leak.500`, so that it stands apart from the sections the player placed and from blocked cells. The tint SHALL stay the same while and after the liquid runs through the section, and SHALL follow pan and zoom.

#### Scenario: Mandatory reservoir is drawn
- **WHEN** a level with a mandatory reservoir starts
- **THEN** the field draws the reservoir's pipe art on a red-tinted tile

#### Scenario: Tint stays when filled
- **WHEN** the liquid runs through a mandatory cross
- **THEN** the cross's tile keeps its red tint and the liquid is drawn in the pipe as in any placed cross

#### Scenario: Placed section next to a mandatory one
- **WHEN** the player places a cross next to a mandatory cross
- **THEN** only the mandatory cross has the red-tinted tile
