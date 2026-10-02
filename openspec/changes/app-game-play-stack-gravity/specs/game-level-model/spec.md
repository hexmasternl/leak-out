## MODIFIED Requirements

### Requirement: Level has stack settings with a weight per pipe section
The system SHALL define `stack` with a `visibleSize` and `weights`. `visibleSize` SHALL be between 1 and 10 inclusive. `visibleSize` is the number of stack sections the player is guaranteed to see; the stack itself SHALL hold ten sections more than `visibleSize`. `weights` holds one non-negative integer per pipe section, keyed by the section ids `H`, `V`, `LT`, `LB`, `RT`, `RB`, `X` and `RES`. The weights of `H`, `V`, `LT`, `LB`, `RT` and `RB` SHALL be at least 1. A section with weight 0 SHALL never appear on the stack. The level SHALL report a section as allowed when its weight is greater than 0.

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

#### Scenario: Unknown section id
- **WHEN** `stack.weights` contains a key that is not a section id, such as `Z`
- **THEN** parsing fails with an error that names the unknown key

#### Scenario: Visible size out of range
- **WHEN** a level has a `visibleSize` of 0 or 11
- **THEN** validation fails with an error that names `stack.visibleSize`

#### Scenario: Default visible size gives a stack of fifteen
- **WHEN** a level has no `stack` field
- **THEN** its `visibleSize` is 5 and its stack holds 15 sections
