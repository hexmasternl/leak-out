## MODIFIED Requirements

### Requirement: Other level fields
The designer SHALL let the developer edit the level's id, name, flow settings (initial delay, cells per second, reservoir speed factor, pump speed factor, speed ramp per cell), stack settings (visible size and the weight of each of the nine sections) and placement rules (allow replace, replace penalty). A numeric input that is not a number SHALL be marked as invalid and SHALL NOT change the design.

#### Scenario: Edit flow speed
- **WHEN** the developer enters 0.8 as cells per second
- **THEN** the design's `flow.cellsPerSecond` is 0.8

#### Scenario: Enable the cross
- **WHEN** the developer sets the weight of `X` to 1
- **THEN** the design's stack weight for `X` is 1

#### Scenario: Enable the pump
- **WHEN** the developer sets the weight of `PUMP` to 2 and the pump speed factor to 2.0
- **THEN** the design's stack weight for `PUMP` is 2 and its `flow.pumpSpeedFactor` is 2.0

#### Scenario: Pump speed factor out of range
- **WHEN** the developer enters 3.5 as the pump speed factor
- **THEN** the design takes the value and the live validation lists an error for `flow.pumpSpeedFactor`

#### Scenario: Not a number
- **WHEN** the developer types "abc" as the initial delay
- **THEN** the field is marked invalid and the design keeps its previous initial delay

### Requirement: Mandatory section tools
The designer SHALL have a Mandatory cross tool, a Mandatory reservoir tool and a Mandatory pump tool. With one selected, tapping a free cell SHALL place a mandatory section of that kind (`X`, `RES` or `PUMP`). Tapping a mandatory section of the same kind SHALL remove it; tapping one of the other kind SHALL change its kind. No other section can be made mandatory. A drag with these tools SHALL only act on the cell it starts on.

#### Scenario: Place a mandatory cross
- **WHEN** the Mandatory cross tool is selected and the developer taps the free cell x 3, y 2
- **THEN** the design has a mandatory `X` at x 3, y 2 and the grid draws it on a red-tinted tile

#### Scenario: Remove by tapping again
- **WHEN** the Mandatory reservoir tool is selected and the developer taps a cell holding a mandatory `RES`
- **THEN** the mandatory section is removed

#### Scenario: Change the kind
- **WHEN** the Mandatory reservoir tool is selected and the developer taps a cell holding a mandatory `X`
- **THEN** that cell holds a mandatory `RES` and the number of mandatory sections is unchanged

#### Scenario: Drag only acts on the first cell
- **WHEN** the Mandatory cross tool is selected and the developer drags from a free cell across three more cells
- **THEN** only the cell the drag started on holds a mandatory `X`

#### Scenario: Place a mandatory pump
- **WHEN** the Mandatory pump tool is selected and the developer taps the free cell x 4, y 1
- **THEN** the design has a mandatory `PUMP` at x 4, y 1, the grid draws it on a red-tinted tile, and the live solvability check routes through it from left to right or right to left

#### Scenario: Change a reservoir into a pump
- **WHEN** the Mandatory pump tool is selected and the developer taps a cell holding a mandatory `RES`
- **THEN** that cell holds a mandatory `PUMP` and the number of mandatory sections is unchanged
