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
