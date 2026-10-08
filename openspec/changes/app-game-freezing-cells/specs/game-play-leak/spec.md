# Spec Delta

## MODIFIED Requirements

### Requirement: Leak when the next section is not connected
The liquid SHALL leak when it leaves a tile into a placed section that has no opening on the side facing the tile it came from. A reservoir SHALL only be connected on its left and right sides. The liquid SHALL leak when it enters a cross through a channel that the liquid already used. The liquid SHALL leak when it reaches a frozen section on a freezing cell, through any side. The liquid SHALL leak when it enters the finish tile from a side other than the finish's open side.

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

#### Scenario: Frozen cross
- **WHEN** the liquid has passed a cross on a freezing cell from left to right and later leaves the tile above it downwards
- **THEN** the liquid leaks at the tile above the cross, through its bottom side

#### Scenario: Finish reached from the wrong side
- **WHEN** the liquid leaves a tile to the right into the finish tile and the finish is open at the top
- **THEN** the liquid leaks
