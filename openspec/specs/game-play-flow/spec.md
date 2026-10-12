# game-play-flow Specification

## Purpose
TBD - created by syncing change app-game-play-game-engine. Update Purpose after archive.
## Requirements
### Requirement: Countdown before the flow
When an attempt starts, the flow SHALL be waiting with the countdown at the level's start delay. Each tick SHALL lower the countdown by the tick's flow time. When the countdown reaches 0, the liquid SHALL start at the start tile, entering it from the side opposite its open side, and the flow time left over from that tick SHALL be used to move the liquid.

#### Scenario: Countdown runs down
- **WHEN** the start delay is 10,000 ms and ticks for 4,000 ms are sent
- **THEN** the countdown is at 6,000 ms and the liquid has not started

#### Scenario: Liquid starts
- **WHEN** the countdown is at 100 ms and a tick of 300 ms is sent
- **THEN** the liquid is in the start tile with 200 ms of flow already done

### Requirement: The liquid head moves through tiles
While flowing, the liquid SHALL be a single head in one tile, with the side it entered from, the channel it uses and its progress through the tile from 0 (entry side) to 1 (exit side). Each tick SHALL raise the progress by the flow time in seconds times the current speed in cells per second. When the progress reaches 1, the head SHALL leave the tile, and the progress beyond 1 SHALL carry over to the next tile.

#### Scenario: Progress within a tile
- **WHEN** the speed is 0.5 cells per second, the head enters an `H` section and a tick of 1,000 ms is sent
- **THEN** the head is halfway through the `H` section

#### Scenario: Overshoot carries over
- **WHEN** the head is at progress 0.9 in a tile at 1 cell per second, a tick of 300 ms is sent, and the next tile takes the liquid
- **THEN** the head is in the next tile at progress 0.2

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

### Requirement: Fast forward scales flow time
The flow time of a tick SHALL be the tick's duration times 10 while fast forward is on, and the tick's duration otherwise. The countdown and the liquid head SHALL use the flow time. The game clock, the replace lock and the points popups SHALL use the tick's duration.

#### Scenario: Fast countdown
- **WHEN** fast forward is on, the countdown is at 5,000 ms and a tick of 100 ms is sent
- **THEN** the countdown is at 4,000 ms and the game clock is 100 ms further

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

### Requirement: A section is entered as soon as liquid reaches it
A placed section SHALL count as entered by liquid from the moment the head enters it, so that it can no longer be replaced, even before the head leaves it.

#### Scenario: Replace a section the head is in
- **WHEN** the head is at progress 0.1 in an `H` section and a place command for that tile is sent
- **THEN** the game state is unchanged

### Requirement: The cross has two channels
A cross SHALL have a horizontal and a vertical channel. The liquid SHALL pass straight through the channel of the side it enters. Each channel SHALL be used at most once: liquid that reaches a cross on a channel it already used SHALL leak. The points for the first and the second pass SHALL follow the points rules.

#### Scenario: Second pass through a cross
- **WHEN** the liquid has passed a cross from left to right and later enters it from the top
- **THEN** it passes through to the bottom and 50 points are awarded when it leaves

#### Scenario: Used channel
- **WHEN** the liquid has passed a cross from left to right and later reaches it from the right
- **THEN** the level is lost

### Requirement: Large ticks skip no tile
A single tick SHALL be handled as if it were split into steps that each end at the next tile border, so that every tile the liquid passes is left in order, every award and leak happens on the right tile, and the flow stops at the exact tile where the level is won or lost. The flow time left after a win or a loss SHALL be dropped.

#### Scenario: One long tick
- **WHEN** the speed is 2 cells per second, the head is at the start of a route of 3 sections before the finish, and one tick of 5,000 ms is sent
- **THEN** the points for all 3 sections are awarded in route order, the level is won, and the head does not move further

#### Scenario: Long tick into a leak
- **WHEN** one long tick moves the head past two sections and the tile after the second is empty
- **THEN** the points for the two sections are awarded and the level is lost at the second section

### Requirement: The flow and the route check agree
The flow SHALL follow the same channel and portal rules as the complete-route check used for fast forward. On every board for which the route check reports a complete route, the flow SHALL end in a win when no section is changed afterwards.

#### Scenario: Complete route is won
- **WHEN** the route check reports a complete route and ticks are sent until the flow ends
- **THEN** the outcome is won

#### Scenario: Complete route through a portal pair is won
- **WHEN** the route check reports a complete route that passes a portal pair and ticks are sent until the flow ends
- **THEN** the outcome is won
