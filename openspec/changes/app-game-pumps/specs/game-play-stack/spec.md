## MODIFIED Requirements

### Requirement: No standard section stays away too long
Before each draw, the system SHALL check every straight and corner section (`H`, `V`, `LT`, `LB`, `RT`, `RB`) for the number of draws since it was last dealt, counted from the start of the attempt. When that number has reached the section's drought limit, the system SHALL deal that section instead of drawing at random. The drought limit SHALL be 2.5 divided by the section's target share, rounded up. When several sections have reached their limit, the system SHALL deal the one that has been away longest, and on a tie the first in the order `H`, `V`, `LT`, `LB`, `RT`, `RB`. The cross `X`, the reservoir `RES` and the pump `PUMP` SHALL never be dealt by the drought limit.

#### Scenario: Drought limit on a level with default weights
- **WHEN** a level with default weights has dealt 15 sections since `RB` was last dealt
- **THEN** the next section dealt is `RB`

#### Scenario: Limit depends on the share
- **WHEN** a level has default weights and an `X` weight of 6
- **THEN** the drought limit of every straight and corner section is 18 draws

#### Scenario: No standard section is missing too long
- **WHEN** 10,000 seeded runs of 40 draws are made with the default weights
- **THEN** in no run is a straight or corner section missing for more than 15 draws in a row

#### Scenario: Special sections are never forced
- **WHEN** a level with an `X` weight of 1 has drawn 200 sections without a cross
- **THEN** the next section is still drawn at random, and the cross is not forced

#### Scenario: Pump is never forced
- **WHEN** a level with a `PUMP` weight of 1 has drawn 200 sections without a pump
- **THEN** the next section is still drawn at random, and the pump is not forced
