## MODIFIED Requirements

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
