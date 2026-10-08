## MODIFIED Requirements

### Requirement: Flow speed
The ramped speed SHALL be the level's cells per second times `1 + speedRampPerCell × n`, where `n` is the number of pipe sections the liquid has left so far in this attempt. The speed of the liquid SHALL be the ramped speed times `pumpSpeedFactor` raised to `p`, the number of pumps the liquid has left so far in this attempt, but never above the larger of 10 cells per second and the ramped speed. Inside a reservoir the speed SHALL be multiplied by the level's reservoir speed factor. The HUD SHALL show the speed without the reservoir factor.

#### Scenario: Speed ramp
- **WHEN** the level has 1.0 cells per second and a ramp of 0.05, and the liquid has left 4 sections
- **THEN** the speed is 1.2 cells per second and the HUD shows 1.2

#### Scenario: Reservoir slow-down
- **WHEN** the base speed is 1.0 cells per second, the reservoir factor is 0.3 and the head is in a reservoir
- **THEN** the head moves at 0.3 cells per second and the HUD still shows 1.0

#### Scenario: Pump boost
- **WHEN** the level has 1.0 cells per second, no ramp and a pump speed factor of 1.5, and the liquid has left one pump
- **THEN** the speed is 1.5 cells per second and the HUD shows 15

#### Scenario: Pump boost and ramp together
- **WHEN** the level has 1.0 cells per second, a ramp of 0.05 and a pump speed factor of 2, and the liquid has left 4 sections, one of them a pump
- **THEN** the speed is 2.4 cells per second

#### Scenario: Reservoir after a pump
- **WHEN** the boosted speed is 1.5 cells per second, the reservoir factor is 0.3 and the head is in a reservoir
- **THEN** the head moves at 0.45 cells per second and the HUD shows 15

#### Scenario: Level without pumps
- **WHEN** the liquid has left no pump
- **THEN** the speed is the ramped speed, exactly as before pumps existed
