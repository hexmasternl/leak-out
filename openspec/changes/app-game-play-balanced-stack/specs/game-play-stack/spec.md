## MODIFIED Requirements

### Requirement: Sections are drawn by the level's weights
The system SHALL draw every new section at random, steered towards the target shares of the level's stack weights. Each allowed section's target share SHALL be its weight divided by the sum of all weights. After `n` draws in an attempt, a section's deficit SHALL be its target share times `n` minus the number of times it was dealt. Each section SHALL be drawn with a chance proportional to its target share times `e` raised to the steering strength (1.0) times its deficit. A section with weight 0 SHALL never be drawn. The initial sections of the stack SHALL be drawn the same way, one after the other. The level holds no seed, so each attempt SHALL get a new random order. The random source SHALL be replaceable, so that tests can get a repeatable order.

#### Scenario: Section with weight 0 is never drawn
- **WHEN** a level with an `X` weight of 0 draws 10,000 sections
- **THEN** none of the drawn sections is `X`

#### Scenario: Draws follow the weights
- **WHEN** 100,000 fresh stacks are started for a level with the weights `H` 7, `V` 7, `LT` 7, `LB` 7, `RT` 7, `RB` 7, `X` 3 and `RES` 3, and the first section of each is recorded
- **THEN** each section's share of those first sections is within 1 percentage point of its weight divided by 48

#### Scenario: A section that is behind becomes more likely
- **WHEN** a level with default weights has drawn 12 sections, `H` 0 times and `V` 4 times
- **THEN** the chance of drawing `H` next is higher than its target share, and the chance of drawing `V` next is lower than its target share

#### Scenario: Shares follow the weights over a level
- **WHEN** 10,000 seeded runs of 40 draws are made for a level with default weights and an `X` weight of 6
- **THEN** the cross makes up between 11.5 % and 13.5 % of all draws

#### Scenario: Standard sections come out nearly evenly spread
- **WHEN** 10,000 seeded runs of 40 draws are made with the default weights
- **THEN** the mean standard deviation of the per-section counts is at most 0.8, and at most 12 % of the draws repeat the section drawn just before

#### Scenario: Same random source gives the same stack
- **WHEN** two stacks are created for the same level with random sources that use the same seed
- **THEN** both stacks hold the same sections in the same order, and they stay the same after the same number of placements

## ADDED Requirements

### Requirement: No standard section stays away too long
Before each draw, the system SHALL check every straight and corner section (`H`, `V`, `LT`, `LB`, `RT`, `RB`) for the number of draws since it was last dealt, counted from the start of the attempt. When that number has reached the section's drought limit, the system SHALL deal that section instead of drawing at random. The drought limit SHALL be 2.5 divided by the section's target share, rounded up. When several sections have reached their limit, the system SHALL deal the one that has been away longest, and on a tie the first in the order `H`, `V`, `LT`, `LB`, `RT`, `RB`. The cross `X` and the reservoir `RES` SHALL never be dealt by the drought limit.

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

### Requirement: Dealing state belongs to one attempt
The number of draws, the count of each dealt section and the draws since each section was last dealt SHALL be part of the stack state of the attempt. They SHALL carry over from one placement to the next, also across pause and resume, and SHALL be reset when the level is started or restarted.

#### Scenario: State carries over between placements
- **WHEN** the next section `RB` is placed and `H` is drawn to refill the stack
- **THEN** the stack's dealing state counts every draw since the attempt started, including the draw of `H`

#### Scenario: Restart resets the dealing state
- **WHEN** the player restarts a level after 12 placements
- **THEN** the new stack is drawn as on a first attempt, with no draws counted
