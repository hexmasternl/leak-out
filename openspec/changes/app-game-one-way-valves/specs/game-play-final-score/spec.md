## MODIFIED Requirements

### Requirement: Unused sections cost 3 points each
Every placed section that the liquid never entered SHALL count as an **unused section**. The penalty SHALL be 3 points per unused section. Sections the liquid entered, the start tile, the finish tile, blocked tiles, mandatory sections and valves SHALL NOT count as unused.

#### Scenario: Four unused sections
- **WHEN** the level is won and four placed sections were never entered by the liquid
- **THEN** the penalty is 12 points

#### Scenario: No unused sections
- **WHEN** the level is won and every placed section was entered by the liquid
- **THEN** the penalty is 0 points

#### Scenario: A cross used once is not unused
- **WHEN** the level is won and the liquid passed a cross through one channel only
- **THEN** that cross does not count as unused

#### Scenario: An unused valve costs nothing
- **WHEN** the level is won, the liquid never entered the level's valve and every section the player placed was entered
- **THEN** the penalty is 0 points
