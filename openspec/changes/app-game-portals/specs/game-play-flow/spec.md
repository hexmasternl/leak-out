## MODIFIED Requirements

### Requirement: Flow speed
The speed of the liquid SHALL be the level's cells per second times `1 + speedRampPerCell × n`, where `n` is the number of pipe sections the liquid has left so far in this attempt. A portal pair is not a pipe section and SHALL NOT count in `n`. Inside a reservoir the speed SHALL be multiplied by the level's reservoir speed factor. The HUD SHALL show the speed without the reservoir factor.

#### Scenario: Speed ramp
- **WHEN** the level has 1.0 cells per second and a ramp of 0.05, and the liquid has left 4 sections
- **THEN** the speed is 1.2 cells per second and the HUD shows 1.2

#### Scenario: Reservoir slow-down
- **WHEN** the base speed is 1.0 cells per second, the reservoir factor is 0.3 and the head is in a reservoir
- **THEN** the head moves at 0.3 cells per second and the HUD still shows 1.0

#### Scenario: Portal pair does not ramp the speed
- **WHEN** the level has 1.0 cells per second and a ramp of 0.05, and the liquid has left 4 sections and one portal pair
- **THEN** the speed is 1.2 cells per second

### Requirement: Leaving a tile
When the head leaves a tile, the engine SHALL, in this order: mark the channel it used as filled, award the points for that section, and look at the tile on the exit side. If that tile takes the liquid, the head SHALL continue there. If it is the finish entered through its open side, the level SHALL be won. If it is a portal entered through its open side and its pair has not been passed, the head SHALL continue in that pair, and when it leaves the pair it SHALL leave the other portal of the pair through that portal's open side. Otherwise the liquid SHALL leak and the level SHALL be lost, with the leak at the tile it left and the side it left through. The start tile and portals SHALL give no points.

#### Scenario: Points before a leak
- **WHEN** the score is 40 and the head leaves a corner section into an empty tile
- **THEN** the score is 55 and the level is lost at that corner's tile and exit side

#### Scenario: Leak straight from the start
- **WHEN** the tile next to the start's open side is empty and the head leaves the start tile
- **THEN** the level is lost at the start tile and its open side, and the score stays 0

#### Scenario: Win at the finish
- **WHEN** the head leaves the last section into the finish through the finish's open side
- **THEN** the points for that section are awarded and the level is won

#### Scenario: Into a portal
- **WHEN** the head leaves a straight section into a portal through the portal's open side
- **THEN** the straight's points are awarded and the head continues in the portal pair

#### Scenario: Out of a portal into a leak
- **WHEN** the head leaves a portal pair and the cell the exit portal faces is empty
- **THEN** no points are awarded and the level is lost at the exit portal and its open side

### Requirement: The flow and the route check agree
The flow SHALL follow the same channel and portal rules as the complete-route check used for fast forward. On every board for which the route check reports a complete route, the flow SHALL end in a win when no section is changed afterwards.

#### Scenario: Complete route is won
- **WHEN** the route check reports a complete route and ticks are sent until the flow ends
- **THEN** the outcome is won

#### Scenario: Complete route through a portal pair is won
- **WHEN** the route check reports a complete route that passes a portal pair and ticks are sent until the flow ends
- **THEN** the outcome is won
