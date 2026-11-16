## MODIFIED Requirements

### Requirement: Other level fields
The designer SHALL let the developer edit the level's id, name, flow settings (initial delay, cells per second, reservoir type, speed ramp per cell), stack settings (visible size and the weight of each of the eight sections) and placement rules (allow replace, replace penalty). The reservoir type SHALL be chosen from the four types 10 % (red), 20 % (blue), 30 % (purple) and 40 % (orange), and SHALL set `flow.reservoirSpeedFactor` to 0.1, 0.2, 0.3 or 0.4. A design restored with a reservoir speed factor that is not one of these SHALL snap to the nearest type, a tie going to the higher factor. A numeric input that is not a number SHALL be marked as invalid and SHALL NOT change the design.

#### Scenario: Edit flow speed
- **WHEN** the developer enters 0.8 as cells per second
- **THEN** the design's `flow.cellsPerSecond` is 0.8

#### Scenario: Choose a reservoir type
- **WHEN** the developer chooses the 10 % (red) reservoir type
- **THEN** the design's `flow.reservoirSpeedFactor` is 0.1

#### Scenario: Restored factor snaps to a type
- **WHEN** a design is restored with a reservoir speed factor of 0.5
- **THEN** the design's reservoir type is 40 % (orange) and its `flow.reservoirSpeedFactor` is 0.4

#### Scenario: Enable the cross
- **WHEN** the developer sets the weight of `X` to 1
- **THEN** the design's stack weight for `X` is 1

#### Scenario: Not a number
- **WHEN** the developer types "abc" as the initial delay
- **THEN** the field is marked invalid and the design keeps its previous initial delay
