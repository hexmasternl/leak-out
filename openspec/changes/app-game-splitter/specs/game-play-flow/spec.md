# Spec Delta

## MODIFIED Requirements

### Requirement: The liquid head moves through tiles
While flowing, the liquid SHALL be one head in one tile, or two heads after a splitter, each with the side it entered from, the channel it uses and its progress through the tile from 0 (entry side) to 1 (exit side). Each tick SHALL raise each head's progress by the flow time in seconds times that head's current speed in cells per second. When a head's progress reaches 1, the head SHALL leave the tile, and the progress beyond 1 SHALL carry over to the next tile.

#### Scenario: Progress within a tile
- **WHEN** the speed is 0.5 cells per second, the head enters an `H` section and a tick of 1,000 ms is sent
- **THEN** the head is halfway through the `H` section

#### Scenario: Overshoot carries over
- **WHEN** the head is at progress 0.9 in a tile at 1 cell per second, a tick of 300 ms is sent, and the next tile takes the liquid
- **THEN** the head is in the next tile at progress 0.2

#### Scenario: Two heads move on their own
- **WHEN** two heads flow at 1 cell per second, one in an `H` section and one in a reservoir with a reservoir factor of 0.5, both at progress 0, and a tick of 500 ms is sent
- **THEN** the head in the `H` is at progress 0.5 and the head in the reservoir at progress 0.25

### Requirement: Flow speed
The speed of a liquid head SHALL be the level's cells per second times `1 + speedRampPerCell × n`, where `n` is the number of pipe sections on that head's own path that the liquid has left so far in this attempt: the sections before the splitter and the sections of its own branch. Inside a reservoir the speed SHALL be multiplied by the level's reservoir speed factor. The HUD SHALL show the speed without the reservoir factor, and with two heads the higher of their two speeds.

#### Scenario: Speed ramp
- **WHEN** the level has 1.0 cells per second and a ramp of 0.05, and the liquid has left 4 sections
- **THEN** the speed is 1.2 cells per second and the HUD shows 1.2

#### Scenario: Reservoir slow-down
- **WHEN** the base speed is 1.0 cells per second, the reservoir factor is 0.3 and the head is in a reservoir
- **THEN** the head moves at 0.3 cells per second and the HUD still shows 1.0

#### Scenario: Speed per branch
- **WHEN** the level has 1.0 cells per second and a ramp of 0.05, the liquid left 4 sections before the splitter and the splitter, and one branch has since left 2 sections and the other 1
- **THEN** the heads move at 1.35 and 1.3 cells per second and the HUD shows 1.35

### Requirement: Leaving a tile
When a head leaves a tile, the engine SHALL, in this order: mark the channel it used as filled, award the points for that section, and look at the tile on each exit side. If that tile takes the liquid, a head SHALL continue there; a splitter sends one head out through each of its two exit sides. If it is a finish entered through its open side, that head SHALL stop there, and the level SHALL be won when every finish of the level is reached. Otherwise the liquid SHALL leak and the level SHALL be lost, with the leak at the tile it left and the side it left through. The start tile SHALL give no points.

#### Scenario: Points before a leak
- **WHEN** the score is 40 and the head leaves a corner section into an empty tile
- **THEN** the score is 55 and the level is lost at that corner's tile and exit side

#### Scenario: Leak straight from the start
- **WHEN** the tile next to the start's open side is empty and the head leaves the start tile
- **THEN** the level is lost at the start tile and its open side, and the score stays 0

#### Scenario: Win at the finish
- **WHEN** the head leaves the last section into the finish through the finish's open side, in a level with one finish
- **THEN** the points for that section are awarded and the level is won

#### Scenario: Leaving the splitter
- **WHEN** the head leaves a splitter closed at the bottom that it entered from the left, and the tiles above and to the right take the liquid
- **THEN** the splitter's points are awarded once and two heads continue, one in the tile above and one in the tile to the right

#### Scenario: One exit of the splitter is empty
- **WHEN** the head leaves a splitter and the tile on one of its two exit sides is empty
- **THEN** the level is lost at the splitter's tile, on that exit side

### Requirement: Large ticks skip no tile
A single tick SHALL be handled as if it were split into steps that each end at the next tile border of any head, so that every tile the liquid passes is left in order, every award and leak happens on the right tile, and the flow stops at the exact tile where the level is won or lost. When two heads reach a border at the same moment, they SHALL leave their tiles in a fixed order: the head that left the splitter through the side first in the order left, top, right, bottom goes first. The flow time left after a win or a loss SHALL be dropped.

#### Scenario: One long tick
- **WHEN** the speed is 2 cells per second, the head is at the start of a route of 3 sections before the finish, and one tick of 5,000 ms is sent
- **THEN** the points for all 3 sections are awarded in route order, the level is won, and the head does not move further

#### Scenario: Long tick into a leak
- **WHEN** one long tick moves the head past two sections and the tile after the second is empty
- **THEN** the points for the two sections are awarded and the level is lost at the second section

#### Scenario: Long tick with two heads
- **WHEN** one long tick moves two heads, one through 3 sections to its finish and the other through 2 sections into an empty tile
- **THEN** the awards follow the order in which the heads reached each border, and the level is lost at the second head's last section

### Requirement: The flow and the route check agree
The flow SHALL follow the same channel rules as the complete-route check used for fast forward, including the splitter, both branches and the rule that a branch cannot enter a channel that holds liquid. On every board for which the route check reports a complete route, the flow SHALL end in a win when no section is changed afterwards.

#### Scenario: Complete route is won
- **WHEN** the route check reports a complete route and ticks are sent until the flow ends
- **THEN** the outcome is won

#### Scenario: Complete split route is won
- **WHEN** the route check reports a complete route through a splitter to both finishes and ticks are sent until the flow ends
- **THEN** the outcome is won
