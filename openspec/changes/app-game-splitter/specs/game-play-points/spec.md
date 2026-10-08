# Spec Delta

## MODIFIED Requirements

### Requirement: Points per section type
The system SHALL award these points for one pass of the liquid through a section:
- a straight section (`H` or `V`): 10 points,
- a corner section (`LT`, `LB`, `RT` or `RB`): 15 points,
- a reservoir (`RES`): 25 points,
- a cross (`X`): 10 points for the first pass and 50 points for the second pass,
- a splitter: 30 points, once, when the liquid leaves it through both exits.

#### Scenario: Straight section
- **WHEN** the liquid exits an `H` or a `V` section
- **THEN** the score goes up by 10

#### Scenario: Corner section
- **WHEN** the liquid exits an `LT`, `LB`, `RT` or `RB` section
- **THEN** the score goes up by 15

#### Scenario: Reservoir
- **WHEN** the liquid exits a reservoir
- **THEN** the score goes up by 25

#### Scenario: Route through all section types
- **WHEN** the liquid runs through two straight sections, three corner sections and one reservoir and reaches the finish
- **THEN** the score is 2 × 10 + 3 × 15 + 25 = 90

#### Scenario: Splitter
- **WHEN** the liquid leaves a splitter through both of its exits
- **THEN** the score goes up by 30, once

#### Scenario: Both branches score
- **WHEN** after the splitter one branch exits two straight sections and the other branch exits one corner section
- **THEN** the score goes up by 30 for the splitter, 20 for the straights and 15 for the corner
