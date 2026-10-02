## MODIFIED Requirements

### Requirement: Engine settings have defaults that a level can override
The system SHALL keep one central set of defaults: `flow.reservoirSpeedFactor` 0.3, `flow.speedRampPerCell` 0.0, `stack.visibleSize` 5, stack weights `H` 10, `V` 10, `LT` 10, `LB` 10, `RT` 10, `RB` 10, `X` 0 and `RES` 0, `stack.adaptive` `X` false and `RES` false, `rules.allowReplace` true and `rules.replacePenaltyMs` 500. When a level leaves out an optional field, the parsed level SHALL hold the default value for it. When a level provides an optional field, the parsed level SHALL hold the provided value, and all other optional fields SHALL keep their defaults.

#### Scenario: Minimal level gets all defaults
- **WHEN** a level contains only the required fields
- **THEN** the parsed level has a reservoir speed factor of 0.3, a speed ramp of 0.0, a visible stack size of 5, the default stack weights, a fixed cross and reservoir, replacing allowed and a replace penalty of 500 milliseconds

#### Scenario: Single field is overridden
- **WHEN** a level provides `flow.reservoirSpeedFactor` 0.5 and no other optional field
- **THEN** the parsed level has a reservoir speed factor of 0.5 and every other optional field at its default

#### Scenario: Single stack weight is overridden
- **WHEN** a level provides `stack.weights` containing only `X` 6
- **THEN** the parsed level has a cross weight of 6, every other weight at its default and the cross fixed

### Requirement: Level has stack settings with a weight per pipe section
The system SHALL define `stack` with a `visibleSize`, `weights` and `adaptive`. `visibleSize` SHALL be between 1 and 10 inclusive. `weights` holds one integer per pipe section between 0 and 20 inclusive, keyed by the section ids `H`, `V`, `LT`, `LB`, `RT`, `RB`, `X` and `RES`; it is the section's initial likeliness. The weights of `H`, `V`, `LT`, `LB`, `RT` and `RB` SHALL be at least 1. A section with weight 0 SHALL never appear on the stack. The level SHALL report a section as allowed when its weight is greater than 0. `adaptive` holds an optional boolean for `X` and for `RES` only, which says whether that section's likeliness adapts while sections are drawn.

#### Scenario: Cross and reservoir enabled
- **WHEN** a level has stack weights with `X` 1 and `RES` 2
- **THEN** the level reports the cross and the reservoir as allowed

#### Scenario: Cross and reservoir disabled by default
- **WHEN** a level has no `stack` field
- **THEN** the level reports the cross and the reservoir as not allowed, and all straight and corner sections as allowed

#### Scenario: Mandatory section with zero weight
- **WHEN** a level has a stack weight of 0 for `LT`
- **THEN** validation fails with an error that names `stack.weights.LT`

#### Scenario: Negative weight
- **WHEN** a level has a negative stack weight for `X`
- **THEN** validation fails with an error that names `stack.weights.X`

#### Scenario: Weight above the maximum likeliness
- **WHEN** a level has a stack weight of 21 for `H`
- **THEN** validation fails with an error that names `stack.weights.H`

#### Scenario: Unknown section id
- **WHEN** `stack.weights` contains a key that is not a section id, such as `Z`
- **THEN** parsing fails with an error that names the unknown key

#### Scenario: Visible size out of range
- **WHEN** a level has a `visibleSize` of 0 or 11
- **THEN** validation fails with an error that names `stack.visibleSize`

#### Scenario: Adaptive reservoir
- **WHEN** a level has `stack.adaptive` containing `RES` true
- **THEN** the parsed level reports the reservoir as adaptive and the cross as fixed

#### Scenario: Standard section in adaptive is rejected
- **WHEN** `stack.adaptive` contains the key `H`
- **THEN** parsing fails with an error that names the unknown key, because straight and corner sections are always adaptive
