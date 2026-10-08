## MODIFIED Requirements

### Requirement: Level is described by a JSON document
The system SHALL read a level from a JSON document. The fields `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow.initialDelayMs`, `flow.cellsPerSecond` and `stars` SHALL be required. The fields `flow.reservoirSpeedFactor`, `flow.pumpSpeedFactor`, `flow.speedRampPerCell`, `stack`, `rules` and `mandatory`, and every field inside `stack` and `rules`, SHALL be optional.

#### Scenario: Complete level is parsed
- **WHEN** a JSON document containing every field with valid values is parsed
- **THEN** the system produces a level whose fields equal the values in the document

#### Scenario: Missing required field is reported
- **WHEN** a JSON document omits a required field such as `flow.cellsPerSecond`
- **THEN** parsing fails with an error that names the missing field

#### Scenario: Unknown field is rejected
- **WHEN** a JSON document contains a key that is not part of the level format
- **THEN** parsing fails with an error that names the unknown key

#### Scenario: Malformed JSON is rejected
- **WHEN** the input is not valid JSON
- **THEN** parsing fails with an error and no level is produced

#### Scenario: Level with a pump speed factor is parsed
- **WHEN** a JSON document has `flow.pumpSpeedFactor` 2.0 and `stack.weights.PUMP` 3
- **THEN** the parsed level has a pump speed factor of 2.0 and a pump weight of 3

### Requirement: Engine settings have defaults that a level can override
The system SHALL keep one central set of defaults: `flow.reservoirSpeedFactor` 0.3, `flow.pumpSpeedFactor` 1.5, `flow.speedRampPerCell` 0.0, `stack.visibleSize` 5, stack weights `H` 7, `V` 7, `LT` 7, `LB` 7, `RT` 7, `RB` 7, `X` 0, `RES` 0 and `PUMP` 0, `rules.allowReplace` true and `rules.replacePenaltyMs` 1500. When a level leaves out an optional field, the parsed level SHALL hold the default value for it. When a level provides an optional field, the parsed level SHALL hold the provided value, and all other optional fields SHALL keep their defaults.

#### Scenario: Minimal level gets all defaults
- **WHEN** a level contains only the required fields
- **THEN** the parsed level has a reservoir speed factor of 0.3, a pump speed factor of 1.5, a speed ramp of 0.0, a visible stack size of 5, a stack weight of 7 for every straight and corner section and 0 for the cross, the reservoir and the pump, replacing allowed and a replace penalty of 1500 milliseconds

#### Scenario: Single field is overridden
- **WHEN** a level provides `flow.reservoirSpeedFactor` 0.5 and no other optional field
- **THEN** the parsed level has a reservoir speed factor of 0.5 and every other optional field at its default

#### Scenario: Single stack weight is overridden
- **WHEN** a level provides `stack.weights` containing only `X` 3
- **THEN** the parsed level has a cross weight of 3 and every other weight at its default

### Requirement: Level has flow settings
The system SHALL define `flow` with an `initialDelayMs`, a `cellsPerSecond`, a `reservoirSpeedFactor`, a `pumpSpeedFactor` and a `speedRampPerCell`. `initialDelayMs` is the integer number of milliseconds between the start of the level and the moment liquid begins to flow, and SHALL be zero or greater. `cellsPerSecond` is the number of cell lengths the liquid front advances per second at base speed, and SHALL be greater than 0 and at most 10. `reservoirSpeedFactor` multiplies the speed while liquid passes a reservoir, and SHALL be greater than 0 and at most 1. `pumpSpeedFactor` multiplies the speed each time the liquid leaves a pump, and SHALL be greater than 1 and at most 3. `speedRampPerCell` is the speed increase per filled cell, and SHALL be zero or greater.

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

#### Scenario: Reservoir speed factor out of range
- **WHEN** a level has a `reservoirSpeedFactor` of 0, a negative value or a value above 1
- **THEN** validation fails with an error that names `flow.reservoirSpeedFactor`

#### Scenario: Pump speed factor out of range
- **WHEN** a level has a `pumpSpeedFactor` of 1, a value below 1 or a value above 3
- **THEN** validation fails with an error that names `flow.pumpSpeedFactor`

#### Scenario: Largest pump speed factor
- **WHEN** a level has a `pumpSpeedFactor` of 3
- **THEN** the level is accepted

#### Scenario: Negative speed ramp
- **WHEN** a level has a negative `speedRampPerCell`
- **THEN** validation fails with an error that names `flow.speedRampPerCell`

### Requirement: Level has stack settings with a weight per pipe section
The system SHALL define `stack` with a `visibleSize` and `weights`. `visibleSize` SHALL be between 1 and 10 inclusive. `visibleSize` is the number of stack sections the player is guaranteed to see; the stack itself SHALL hold ten sections more than `visibleSize`. `weights` holds one non-negative integer per pipe section, keyed by the section ids `H`, `V`, `LT`, `LB`, `RT`, `RB`, `X`, `RES` and `PUMP`. A section's weight divided by the sum of all weights is its target share of the sections dealt in an attempt. The weights of `H`, `V`, `LT`, `LB`, `RT` and `RB` SHALL be at least 1. A section with weight 0 SHALL never appear on the stack. The level SHALL report a section as allowed when its weight is greater than 0.

#### Scenario: Cross and reservoir enabled
- **WHEN** a level has stack weights with `X` 1 and `RES` 2
- **THEN** the level reports the cross and the reservoir as allowed

#### Scenario: Cross and reservoir disabled by default
- **WHEN** a level has no `stack` field
- **THEN** the level reports the cross, the reservoir and the pump as not allowed, and all straight and corner sections as allowed

#### Scenario: Pump enabled
- **WHEN** a level has a stack weight of 2 for `PUMP`
- **THEN** the level reports the pump as allowed

#### Scenario: Mandatory section with zero weight
- **WHEN** a level has a stack weight of 0 for `LT`
- **THEN** validation fails with an error that names `stack.weights.LT`

#### Scenario: Negative weight
- **WHEN** a level has a negative stack weight for `X`
- **THEN** validation fails with an error that names `stack.weights.X`

#### Scenario: Unknown section id
- **WHEN** `stack.weights` contains a key that is not a section id, such as `Z`
- **THEN** parsing fails with an error that names the unknown key

#### Scenario: Visible size out of range
- **WHEN** a level has a `visibleSize` of 0 or 11
- **THEN** validation fails with an error that names `stack.visibleSize`

#### Scenario: Default visible size gives a stack of fifteen
- **WHEN** a level has no `stack` field
- **THEN** its `visibleSize` is 5 and its stack holds 15 sections

### Requirement: Level lists mandatory sections
The system SHALL define `mandatory` as an array of at most 6 entries, each with an `x`, a `y` and a `section`. An absent or empty array SHALL mean the level has no mandatory sections. The parsed level SHALL keep the entries in authored order. A mandatory section's `section` SHALL NOT depend on its stack weight, so a level MAY list a mandatory cross while `stack.weights.X` is 0.

#### Scenario: Level without mandatory sections
- **WHEN** a level has no `mandatory` field
- **THEN** the level is accepted with no mandatory sections

#### Scenario: Valid mandatory sections
- **WHEN** a level on a 7 by 9 grid lists a mandatory `X` at x 3, y 4 and a mandatory `RES` at x 5, y 2, and its stack weights for `X` and `RES` are 0
- **THEN** the level is accepted with those two mandatory sections

#### Scenario: Mandatory pump with zero weight
- **WHEN** a level lists a mandatory `PUMP` and its stack weight for `PUMP` is 0
- **THEN** the level is accepted with that mandatory pump

#### Scenario: Missing field in an entry
- **WHEN** a mandatory entry omits `section`
- **THEN** parsing fails with an error that names `mandatory[0].section`

#### Scenario: Unknown section id
- **WHEN** a mandatory entry has a `section` that is not a section id, such as `Z`
- **THEN** parsing fails with an error that names the invalid value

#### Scenario: Too many mandatory sections
- **WHEN** a level lists 7 mandatory sections
- **THEN** validation fails with an error that names `mandatory`

### Requirement: Only a cross, a reservoir or a pump can be mandatory
The system SHALL accept a mandatory entry only when its `section` is `X`, `RES` or `PUMP`.

#### Scenario: Mandatory straight
- **WHEN** the second mandatory entry has `section` `H`
- **THEN** validation fails with an error that names `mandatory[1].section` and states that only `X`, `RES` and `PUMP` can be mandatory

#### Scenario: Mandatory reservoir
- **WHEN** a mandatory entry has `section` `RES`
- **THEN** the entry is accepted

#### Scenario: Mandatory pump
- **WHEN** a mandatory entry has `section` `PUMP`
- **THEN** the entry is accepted

## RENAMED Requirements

- FROM: `### Requirement: Only a cross or a reservoir can be mandatory`
- TO: `### Requirement: Only a cross, a reservoir or a pump can be mandatory`
