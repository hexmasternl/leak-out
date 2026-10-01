## MODIFIED Requirements

### Requirement: Tile renders a section at any size
The app SHALL provide a pipe tile that draws a pipe section from its asset and its glass overlay, scaled to the tile size, so the same asset can be used for the playing field, the stack and previews. A tile without liquid SHALL show only the artwork and the glass overlay. The same drawing SHALL also be available for a rectangle inside another canvas, so a screen that draws many cells on one canvas (the playing field) draws a section exactly like the tile does.

#### Scenario: Scaled to the cell
- **WHEN** a section is drawn in a tile of 48 dp and in a tile of 96 dp
- **THEN** both show the whole section filling the tile, scaled from the same asset

#### Scenario: Tile shows the glass
- **WHEN** a section is drawn in a tile
- **THEN** the tile shows the section's glass overlay on top of its artwork

#### Scenario: Same drawing inside another canvas
- **WHEN** a section is drawn into a rectangle of another canvas
- **THEN** it looks the same as a tile of that size showing the same section and fills
