## MODIFIED Requirements

### Requirement: Sections are drawn by the level's weights
The system SHALL draw every new section at random according to the stack's current likeliness. Each section the level allows SHALL start an attempt with a likeliness equal to its stack weight. The chance of each section SHALL be its current likeliness divided by the sum of the current likeliness of all allowed sections. A section with weight 0 SHALL never be drawn. The initial sections of the stack SHALL be drawn the same way, one after the other, updating the likeliness after each draw. The level holds no seed, so each attempt SHALL get a new random order. The random source SHALL be replaceable, so that tests can get a repeatable order.

#### Scenario: Section with weight 0 is never drawn
- **WHEN** a level with an `X` weight of 0 draws 10,000 sections
- **THEN** none of the drawn sections is `X`

#### Scenario: Draws follow the weights
- **WHEN** 100,000 fresh stacks are started for a level with the weights `H` 10, `V` 10, `LT` 10, `LB` 10, `RT` 10, `RB` 10, `X` 6 and `RES` 4, and the first section of each is recorded
- **THEN** each section's share of those first sections is within 1 percentage point of its weight divided by 70

#### Scenario: Same random source gives the same stack
- **WHEN** two stacks are created for the same level with random sources that use the same seed
- **THEN** both stacks hold the same sections in the same order and the same likeliness, and they stay the same after the same number of placements

## ADDED Requirements

### Requirement: Likeliness adapts after every draw
After every draw the system SHALL update the likeliness: when the drawn section is adaptive its likeliness SHALL drop by 3, and every other allowed adaptive section SHALL gain 1, also when the drawn section is fixed. The likeliness of an adaptive section SHALL never go below 1 or above 20. The straight sections `H` and `V` and the corner sections `LT`, `LB`, `RT` and `RB` SHALL always be adaptive.

#### Scenario: Drawn section drops, others gain
- **WHEN** every standard section has likeliness 10, cross and reservoir are disabled, and `LT` is drawn
- **THEN** `LT` has likeliness 7 and `H`, `V`, `LB`, `RT` and `RB` have likeliness 11

#### Scenario: Likeliness never drops below 1
- **WHEN** `H` has likeliness 2 and `H` is drawn
- **THEN** `H` has likeliness 1

#### Scenario: Likeliness never rises above 20
- **WHEN** `V` has likeliness 20 and another section is drawn
- **THEN** `V` still has likeliness 20

#### Scenario: No allowed section is eliminated
- **WHEN** a level with default weights draws 100,000 sections
- **THEN** the likeliness of every standard section stays between 1 and 20 after every draw

#### Scenario: Standard sections come out nearly evenly spread
- **WHEN** 10,000 seeded runs of 40 draws are made with the default weights
- **THEN** the mean standard deviation of the per-section counts is at most 1.8, and at least 99.9 % of the runs draw every standard section at least once

### Requirement: Special sections are fixed or adaptive per level
The cross `X` and the reservoir `RES` SHALL be fixed unless the level marks them adaptive in `stack.adaptive`. A fixed section's likeliness SHALL stay at its weight for the whole attempt. An adaptive special section SHALL follow the same drop, gain and limits as the standard sections.

#### Scenario: Fixed cross keeps its likeliness
- **WHEN** a level has an `X` weight of 6 and no `stack.adaptive`, and the cross and then 20 other sections are drawn
- **THEN** the cross has likeliness 6 after every draw

#### Scenario: Drawing a fixed section lets the others gain
- **WHEN** a level has a fixed `X` with weight 6, every standard section has likeliness 10, and `X` is drawn
- **THEN** `X` has likeliness 6 and every standard section has likeliness 11

#### Scenario: Adaptive reservoir drops when drawn
- **WHEN** a level has an `RES` weight of 8 and `stack.adaptive.RES` true, and `RES` is drawn
- **THEN** `RES` has likeliness 5

### Requirement: Likeliness belongs to one attempt
The current likeliness SHALL be part of the stack state of the attempt. It SHALL carry over from one placement to the next, also across pause and resume, and SHALL be reset to the level's weights when the level is started or restarted.

#### Scenario: Likeliness carries over between placements
- **WHEN** the next section `RB` is placed and `H` is drawn to refill the stack
- **THEN** the stack's likeliness reflects every draw since the attempt started, including the draw of `H`

#### Scenario: Restart resets the likeliness
- **WHEN** the player restarts a level after 12 placements
- **THEN** the new stack is drawn starting from the level's weights, as on a first attempt
