## MODIFIED Requirements

### Requirement: Gap distance
While the liquid is flowing and the attempt is being played, the system SHALL measure the **gap distance**: how far, in sections, the liquid head is from the point where it would leak if nothing changes. The distance SHALL be the part of the head's current tile it still has to run through (1 minus its progress), plus 1 for every placed section or portal pair it would enter after that tile before it leaks. The walk ahead SHALL follow the same rules as the flow: an empty, blocked or start tile, the grid edge, a section without a matching opening, a cross channel the liquid already used, a portal entered through a closed side or a pair already passed, and a finish entered from the wrong side or with a mandatory section unused are all gaps. A tile whose replacement section has not yet landed SHALL count as empty. When the walk ahead reaches the finish and the liquid would win there, there SHALL be no gap. There SHALL be no gap while the flow waits to start and after the attempt is won or lost. The distance SHALL be measured again on every frame and after every placement.

#### Scenario: Gap right after the head's tile
- **WHEN** the liquid head is halfway through a straight whose exit faces an empty tile
- **THEN** the gap distance is 0.5

#### Scenario: Gap further ahead
- **WHEN** the liquid head has just entered a tile, and four connected sections follow it before an empty tile
- **THEN** the gap distance is 5

#### Scenario: Route into the finish
- **WHEN** the sections ahead of the liquid lead into the finish through its open side and every mandatory section is used on the way
- **THEN** there is no gap

#### Scenario: Mandatory section missed
- **WHEN** the sections ahead lead into the finish, but a mandatory section is not on the way
- **THEN** the gap is at the finish, and the distance counts the sections up to it

#### Scenario: Placement moves the gap
- **WHEN** the gap distance is 2.5 and the player places a connected section on the empty tile at the gap
- **THEN** the gap distance becomes 3.5 or more, depending on what lies beyond the new section

#### Scenario: Pending replacement
- **WHEN** a replacement is running on the tile right after the head's tile
- **THEN** that tile counts as a gap until the new section lands

#### Scenario: Countdown
- **WHEN** the flow is still counting down
- **THEN** there is no gap and no warning, however the field looks

#### Scenario: Gap behind a portal pair
- **WHEN** the liquid head is halfway through a straight whose exit leads into a portal, and the cell the other portal faces is empty
- **THEN** the gap distance is 1.5: half the straight plus one for the portal pair

#### Scenario: Head inside a portal pair
- **WHEN** the head is at progress 0.25 in a portal pair and the cell the exit portal faces is empty
- **THEN** the gap distance is 0.75
