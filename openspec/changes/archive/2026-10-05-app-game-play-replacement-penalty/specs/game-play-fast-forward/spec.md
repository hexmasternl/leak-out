## MODIFIED Requirements

### Requirement: Route is checked after every placement
After every accepted placement, after a replacement sequence starts and after a replacement section lands, the system SHALL check whether the board holds a complete route from the start to the finish. While a replacement sequence runs, its tile SHALL count as empty for the route. When a level starts or restarts, the route SHALL count as not complete.

#### Scenario: Placing the last missing section
- **WHEN** the start and finish are connected by placed sections except for one tile, and the player places a section on that tile that connects both neighbours
- **THEN** the route is complete

#### Scenario: Replacing a section on the route
- **WHEN** the route is complete and the player replaces a section on the route
- **THEN** the route is not complete while the replacement sequence runs

#### Scenario: Replacement lands and completes the route
- **WHEN** a replacement section lands on the only tile missing from the route and connects both neighbours
- **THEN** the route is complete

#### Scenario: New level
- **WHEN** a level starts or restarts
- **THEN** the route is not complete
