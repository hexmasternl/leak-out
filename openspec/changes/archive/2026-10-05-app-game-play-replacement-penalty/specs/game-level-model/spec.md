## MODIFIED Requirements

### Requirement: Engine settings have defaults that a level can override
The system SHALL keep one central set of defaults: `flow.reservoirSpeedFactor` 0.3, `flow.speedRampPerCell` 0.0, `stack.visibleSize` 5, stack weights `H` 3, `V` 3, `LT` 2, `LB` 2, `RT` 2, `RB` 2, `X` 0 and `RES` 0, `rules.allowReplace` true and `rules.replacePenaltyMs` 1500. When a level leaves out an optional field, the parsed level SHALL hold the default value for it. When a level provides an optional field, the parsed level SHALL hold the provided value, and all other optional fields SHALL keep their defaults.

#### Scenario: Minimal level gets all defaults
- **WHEN** a level contains only the required fields
- **THEN** the parsed level has a reservoir speed factor of 0.3, a speed ramp of 0.0, a visible stack size of 5, the default stack weights, replacing allowed and a replace penalty of 1500 milliseconds

#### Scenario: Single field is overridden
- **WHEN** a level provides `flow.reservoirSpeedFactor` 0.5 and no other optional field
- **THEN** the parsed level has a reservoir speed factor of 0.5 and every other optional field at its default

#### Scenario: Single stack weight is overridden
- **WHEN** a level provides `stack.weights` containing only `X` 1
- **THEN** the parsed level has a cross weight of 1 and every other weight at its default

### Requirement: Level has placement rules
The system SHALL define `rules` with a boolean `allowReplace`, which says whether an unfilled pipe may be replaced, and an integer `replacePenaltyMs`, which is how long a replacement sequence lasts: the game time from the moment the old section is destroyed until the new section lands, during which no new placement is accepted. `replacePenaltyMs` SHALL be zero or greater.

#### Scenario: Replacing disabled
- **WHEN** a level has `rules.allowReplace` false
- **THEN** the level reports that replacing is not allowed

#### Scenario: Custom replace penalty
- **WHEN** a level has `rules.replacePenaltyMs` 2500
- **THEN** the parsed level has a replace penalty of 2500 milliseconds

#### Scenario: Negative replace penalty
- **WHEN** a level has a negative `replacePenaltyMs`
- **THEN** validation fails with an error that names `rules.replacePenaltyMs`

#### Scenario: Non-boolean replace flag
- **WHEN** `rules.allowReplace` is a string or number instead of a boolean
- **THEN** parsing fails with an error that names `rules.allowReplace`
