## MODIFIED Requirements

### Requirement: HUD shows the current flow speed
The HUD SHALL show the current flow speed as a whole number next to the flow speed icon: the speed in cells per second times 10, rounded to the nearest whole number with halves rounded up, and without a unit. This is for display only. When a level starts, the speed SHALL be the level's `flow.cellsPerSecond`. When the speed changes during play, also when a pump boosts it, the HUD SHALL show the new number.

#### Scenario: Speed at the start of a level
- **WHEN** a level with a `flow.cellsPerSecond` of 0.2 starts
- **THEN** the HUD shows the flow speed as 2

#### Scenario: Speed changes during play
- **WHEN** the current flow speed changes from 0.8 to 0.9 cells per second
- **THEN** the HUD shows 9

#### Scenario: Speed is rounded
- **WHEN** the current flow speed is 1.25 cells per second
- **THEN** the HUD shows 13

#### Scenario: Small speed change
- **WHEN** the current flow speed changes from 0.80 to 0.82 cells per second
- **THEN** the HUD keeps showing 8

#### Scenario: Speed after a pump
- **WHEN** the current flow speed is 0.8 cells per second and the liquid leaves a pump with a pump speed factor of 1.5
- **THEN** the HUD shows 12
