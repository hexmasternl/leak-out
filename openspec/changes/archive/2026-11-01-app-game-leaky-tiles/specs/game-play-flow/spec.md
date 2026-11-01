## MODIFIED Requirements

### Requirement: Leaving a tile
When the head leaves a tile, the engine SHALL, in this order: mark the channel it used as filled, award the points for that section (or take the leak penalty when the section lies on a leaky tile), and look at the tile on the exit side. If that tile takes the liquid, the head SHALL continue there. If it is the finish entered through its open side, the level SHALL be won. If it is a portal entered through its open side and its pair has not been passed, the head SHALL continue in that pair, and when it leaves the pair it SHALL leave the other portal of the pair through that portal's open side. Otherwise the liquid SHALL leak and the level SHALL be lost, with the leak at the tile it left and the side it left through. The start tile and portals SHALL give no points.

#### Scenario: Points before a leak
- **WHEN** the score is 40 and the head leaves a corner section into an empty tile
- **THEN** the score is 55 and the level is lost at that corner's tile and exit side

#### Scenario: Leak straight from the start
- **WHEN** the tile next to the start's open side is empty and the head leaves the start tile
- **THEN** the level is lost at the start tile and its open side, and the score stays 0

#### Scenario: Win at the finish
- **WHEN** the head leaves the last section into the finish through the finish's open side
- **THEN** the points for that section are awarded and the level is won

#### Scenario: Leaving a section on a leaky tile
- **WHEN** the score is 40 and the head leaves a corner on a leaky tile into the next placed section
- **THEN** the score is 20 and the head continues in the next section

#### Scenario: Into a portal
- **WHEN** the head leaves a straight section into a portal through the portal's open side
- **THEN** the straight's points are awarded and the head continues in the portal pair

#### Scenario: Out of a portal into a leak
- **WHEN** the head leaves a portal pair and the cell the exit portal faces is empty
- **THEN** no points are awarded and the level is lost at the exit portal and its open side
