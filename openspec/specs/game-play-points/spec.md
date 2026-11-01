# game-play-points Specification

## Purpose
TBD - created by syncing change app-game-play-points. Update Purpose after archive.
## Requirements
### Requirement: Points are awarded when the liquid exits a section
The system SHALL award points for a placed pipe section at the moment the liquid exits that section through the other port of the channel it entered. The system SHALL NOT award points when the liquid enters a section. A placed section that the liquid never reaches, or that the liquid has entered but not yet exited, SHALL NOT give points.

#### Scenario: No points while the liquid is inside a section
- **WHEN** the liquid has entered a straight section and has not reached its exit port yet
- **THEN** the score does not change

#### Scenario: Points when the liquid leaves a section
- **WHEN** the liquid reaches the exit port of a straight section and leaves it
- **THEN** the score goes up by 10

#### Scenario: Sections off the route give nothing
- **WHEN** the level ends and the player has placed sections that the liquid never reached
- **THEN** those sections have added nothing to the score

### Requirement: Points per section type
The system SHALL award these points for one pass of the liquid through a section that does not lie on a leaky tile:
- a straight section (`H` or `V`): 10 points,
- a corner section (`LT`, `LB`, `RT` or `RB`): 15 points,
- a reservoir (`RES`): 25 points,
- a cross (`X`): 10 points for the first pass and 50 points for the second pass.

A pass through a section on a leaky tile SHALL award none of these points and SHALL take the leak penalty of 20 points instead.

#### Scenario: Straight section
- **WHEN** the liquid exits an `H` or a `V` section
- **THEN** the score goes up by 10

#### Scenario: Corner section
- **WHEN** the liquid exits an `LT`, `LB`, `RT` or `RB` section
- **THEN** the score goes up by 15

#### Scenario: Reservoir
- **WHEN** the liquid exits a reservoir
- **THEN** the score goes up by 25

#### Scenario: Route through all section types
- **WHEN** the liquid runs through two straight sections, three corner sections and one reservoir and reaches the finish
- **THEN** the score is 2 × 10 + 3 × 15 + 25 = 90

#### Scenario: Route over a leaky tile
- **WHEN** the liquid runs through two straight sections and three corner sections, one of the corners on a leaky tile, and reaches the finish
- **THEN** the score is 2 × 10 + 2 × 15 − 20 = 30

### Requirement: Cross section scores its first and second pass differently
The system SHALL count the passes through each cross section separately per tile. The first time the liquid exits a cross, through either channel, the system SHALL award 10 points. The second time the liquid exits the same cross, through the other channel, the system SHALL award 50 points. The order of the channels SHALL NOT matter. Each cross tile SHALL be counted on its own. A cross on a leaky tile SHALL take the leak penalty on each pass instead.

#### Scenario: First pass through a cross
- **WHEN** the liquid exits a cross through its horizontal channel and the vertical channel has not been used
- **THEN** the score goes up by 10

#### Scenario: Second pass through the same cross
- **WHEN** the liquid has already passed through a cross once, and now exits the same cross through its other channel
- **THEN** the score goes up by 50

#### Scenario: Vertical channel first
- **WHEN** the liquid first exits a cross through its vertical channel and later through its horizontal channel
- **THEN** the first exit adds 10 and the second exit adds 50

#### Scenario: Two different crosses
- **WHEN** the liquid passes once through a cross on one tile and then once through a cross on another tile
- **THEN** each exit adds 10

#### Scenario: Cross on a leaky tile
- **WHEN** the liquid exits a cross on a leaky tile twice, once through each channel
- **THEN** each exit takes 20 points off the score

### Requirement: Start and finish tiles give no points
The start tile, the finish tile and portals SHALL NOT give points. The liquid leaving the start tile, entering the finish tile or passing a portal pair SHALL NOT change the score.

#### Scenario: Liquid leaves the start tile
- **WHEN** the flow starts and the liquid leaves the start tile into the first placed section
- **THEN** the score stays the same

#### Scenario: Liquid reaches the finish
- **WHEN** the liquid exits the last placed section and enters the finish tile
- **THEN** the score goes up only by the points for that last section

#### Scenario: Liquid passes a portal pair
- **WHEN** the liquid exits a straight section into a portal, passes the pair and exits a corner section after the other portal
- **THEN** the score goes up by 10 and 15 only, and the portal pair adds nothing

### Requirement: The last section before a leak still scores
When the liquid exits a placed section and the next tile does not accept it (an empty tile, a blocked tile, a closed side of a section, the edge of the grid, the wrong side of the finish or a used cross channel), the system SHALL still award the points for the section the liquid exited.

#### Scenario: Leak after a corner
- **WHEN** the liquid exits a corner section into an empty tile and leaks
- **THEN** the score goes up by 15 for the corner

### Requirement: Score total for the attempt
The score SHALL be the sum of all points awarded during the current attempt, minus every leak penalty taken, and MAY go below 0. It SHALL start at 0 when a level starts and SHALL be set back to 0 when the level restarts. The HUD SHALL show the new total at the moment points are awarded or taken. While the game is paused, no points SHALL be awarded or taken.

#### Scenario: HUD follows the awards
- **WHEN** the score is 40 and the liquid exits a corner section
- **THEN** the HUD shows 55

#### Scenario: Restart clears the score and the cross passes
- **WHEN** the player has scored points, including a first pass through a cross, and restarts the level
- **THEN** the score is 0, and the next exit from a cross on that tile in the new attempt counts as a first pass for 10 points

#### Scenario: No points while paused
- **WHEN** the game is paused
- **THEN** the score does not change until the game is resumed

#### Scenario: Score below zero
- **WHEN** the score is 0 and the liquid exits a straight on a leaky tile
- **THEN** the score is −20 and the HUD shows −20 with an empty score bar
