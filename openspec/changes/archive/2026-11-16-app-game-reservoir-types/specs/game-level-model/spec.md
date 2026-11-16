## MODIFIED Requirements

### Requirement: Level has flow settings
The system SHALL define `flow` with an `initialDelayMs`, a `cellsPerSecond`, a `reservoirSpeedFactor` and a `speedRampPerCell`. `initialDelayMs` is the integer number of milliseconds between the start of the level and the moment liquid begins to flow, and SHALL be zero or greater. `cellsPerSecond` is the number of cell lengths the liquid front advances per second at base speed, and SHALL be greater than 0 and at most 10. `reservoirSpeedFactor` multiplies the speed while liquid passes a reservoir and selects the level's reservoir type; it SHALL be one of 0.1 (red), 0.2 (blue), 0.3 (purple) or 0.4 (orange). `speedRampPerCell` is the speed increase per filled cell, and SHALL be zero or greater.

#### Scenario: Valid flow settings
- **WHEN** a level has `initialDelayMs` 12000, `cellsPerSecond` 0.6, `reservoirSpeedFactor` 0.3 and `speedRampPerCell` 0.02
- **THEN** the level is accepted with those values

#### Scenario: Zero delay
- **WHEN** a level has `initialDelayMs` 0
- **THEN** the level is accepted and liquid begins to flow immediately

#### Scenario: Negative delay
- **WHEN** a level has a negative `initialDelayMs`
- **THEN** validation fails with an error that names `flow.initialDelayMs`

#### Scenario: Non-positive speed
- **WHEN** a level has a `cellsPerSecond` of 0 or less
- **THEN** validation fails with an error that names `flow.cellsPerSecond`

#### Scenario: Speed above the upper bound
- **WHEN** a level has a `cellsPerSecond` of 60
- **THEN** validation fails with an error that names `flow.cellsPerSecond`

#### Scenario: Each reservoir type accepted
- **WHEN** a level has a `reservoirSpeedFactor` of 0.1, 0.2, 0.3 or 0.4
- **THEN** the level is accepted with the red, blue, purple or orange reservoir type respectively

#### Scenario: Reservoir speed factor out of range
- **WHEN** a level has a `reservoirSpeedFactor` of 0, a negative value, 0.25, 0.5 or a value above 1
- **THEN** validation fails with an error that names `flow.reservoirSpeedFactor` and lists the allowed values 0.1, 0.2, 0.3 and 0.4

#### Scenario: Negative speed ramp
- **WHEN** a level has a negative `speedRampPerCell`
- **THEN** validation fails with an error that names `flow.speedRampPerCell`
