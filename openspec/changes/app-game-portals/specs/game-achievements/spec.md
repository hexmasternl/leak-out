## MODIFIED Requirements

### Requirement: Hidden achievements
Hidden achievements SHALL unlock as follows:
- Instant Flood: a level is lost before the liquid has left any placed section.
- Wall to Wall: a level is won while every free cell (not blocked, not the start, not the finish, not a portal) holds a placed section.
- Photo Finish: a level is won and the placement that completed the route was made while the liquid was flowing, on the tile the liquid head's current channel exits into.

#### Scenario: Leak at the start
- **WHEN** the liquid leaves the start valve into an empty tile
- **THEN** the level is lost and Instant Flood is unlocked

#### Scenario: Full board
- **WHEN** a level is won and every free cell holds a section
- **THEN** Wall to Wall is unlocked

#### Scenario: Last-second completion
- **WHEN** the liquid head is in a corner section whose exit points at an empty tile, the player places the section that completes the route on that tile, and the level is then won
- **THEN** Photo Finish is unlocked

#### Scenario: Full board with portals
- **WHEN** a level with one portal pair is won and every cell except the blocked cells, the start, the finish and the two portals holds a section
- **THEN** Wall to Wall is unlocked
