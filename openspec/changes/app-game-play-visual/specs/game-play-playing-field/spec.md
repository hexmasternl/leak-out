## MODIFIED Requirements

### Requirement: Field draws the level cells
The field SHALL draw the grid, the start cell with its open side, the finish cell with its open side, every blocked cell and every placed section. Each kind of cell SHALL look different from the others. Placed sections SHALL be drawn with the same pipe art as the stack: the section's SVG asset with its glass overlay, scaled to the cell and following pan and zoom.

#### Scenario: Level cells are drawn
- **WHEN** a level with blocked cells starts
- **THEN** the field shows the grid, the start and finish with their openings, and the blocked cells, all different from empty cells

#### Scenario: Placed section is drawn
- **WHEN** a section is placed on a cell
- **THEN** the field draws that section's pipe art with its glass overlay on the cell

#### Scenario: Pipe art follows zoom
- **WHEN** the player zooms in on a placed section
- **THEN** the section's pipe art is drawn at the new cell size, filling the cell

## ADDED Requirements

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
Besides dragging, a tap on a field cell SHALL place the stack's next section there, under exactly the same rules as a drop: only on an empty cell, or on a placed section that may be replaced, and never while paused, after the level ended, during fast forward or during the replace lock. A tap SHALL be one finger lifted within the touch slop; a pan or a pinch SHALL NOT place anything.

#### Scenario: Tap on an empty cell
- **WHEN** the player taps an empty cell during play
- **THEN** the stack's next section is placed on that cell

#### Scenario: Tap on a cell that does not accept a section
- **WHEN** the player taps a blocked cell, the start, the finish or a section the liquid has entered
- **THEN** nothing is placed and the stack is unchanged

#### Scenario: Pan does not place
- **WHEN** the player swipes or pinches the field
- **THEN** the view moves and nothing is placed
