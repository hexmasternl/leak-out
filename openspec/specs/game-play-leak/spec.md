# game-play-leak Specification

## Purpose
TBD - created by syncing change app-game-play-level-lost. Update Purpose after archive.
## Requirements
### Requirement: Liquid leaving a tile is checked for a leak
Each time the liquid leaves a tile through a side, the system SHALL look at the tile next to that side and decide one of three results: the liquid **enters** that tile, the liquid **reaches the finish**, or the liquid **leaks**. The start tile SHALL be checked the same way when the liquid leaves it through its open side.

#### Scenario: Liquid enters a connected section
- **WHEN** the liquid leaves a tile to the right and the tile to its right holds `H`
- **THEN** the liquid enters that tile from its left side

#### Scenario: Liquid reaches the finish
- **WHEN** the liquid leaves a tile downwards and the tile below it is the finish, open at the top
- **THEN** the liquid reaches the finish and there is no leak

### Requirement: Leak when there is no next pipe section
The liquid SHALL leak when it leaves a tile into an empty tile, a blocked tile, the start tile, or over the edge of the grid.

#### Scenario: Leak into an empty tile
- **WHEN** the liquid leaves a tile to the right and the tile to its right is empty
- **THEN** the liquid leaks

#### Scenario: Leak into a blocked tile
- **WHEN** the liquid leaves a tile upwards and the tile above it is blocked
- **THEN** the liquid leaks

#### Scenario: Leak over the edge of the grid
- **WHEN** the liquid leaves a tile in the rightmost column to the right
- **THEN** the liquid leaks

#### Scenario: Leak into the start tile
- **WHEN** the liquid leaves a tile into the start tile
- **THEN** the liquid leaks

### Requirement: Leak when the next section is not connected
The liquid SHALL leak when it leaves a tile into a placed section that has no opening on the side facing the tile it came from. A reservoir SHALL only be connected on its left and right sides. The liquid SHALL leak when it enters a cross through a channel that the liquid already used. The liquid SHALL leak when it enters the finish tile from a side other than the finish's open side.

#### Scenario: Section without a matching opening
- **WHEN** the liquid leaves a tile to the right and the tile to its right holds `V`
- **THEN** the liquid leaks

#### Scenario: Reservoir entered from above
- **WHEN** the liquid leaves a tile downwards and the tile below it holds a reservoir
- **THEN** the liquid leaks

#### Scenario: Cross channel already used
- **WHEN** the liquid has passed a cross from left to right and later enters the same cross from its right side
- **THEN** the liquid leaks

#### Scenario: Cross second channel is free
- **WHEN** the liquid has passed a cross from left to right and later enters the same cross from its top side
- **THEN** the liquid enters the cross and there is no leak

#### Scenario: Finish reached from the wrong side
- **WHEN** the liquid leaves a tile to the right into the finish tile and the finish is open at the top
- **THEN** the liquid leaks

### Requirement: Leak at the start of the flow
When the flow starts and the tile next to the start's open side cannot take the liquid, the liquid SHALL leak from the start tile at that moment.

#### Scenario: Nothing placed next to the start
- **WHEN** the countdown ends and the tile next to the start's open side is empty
- **THEN** the liquid leaks from the start tile through its open side

### Requirement: Leak location
A leak SHALL be reported at the tile the liquid left and the side it left through, so the game can show where the pipe leaks.

#### Scenario: Location of a leak
- **WHEN** the liquid leaves the tile at column 3, row 2 to the right and leaks
- **THEN** the leak is reported at column 3, row 2 on the right side
