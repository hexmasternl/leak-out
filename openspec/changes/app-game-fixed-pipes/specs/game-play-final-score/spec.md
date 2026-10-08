# Spec Delta

## MODIFIED Requirements

### Requirement: Unused sections cost 3 points each
Every placed section that the liquid never entered SHALL count as an **unused section**. The penalty SHALL be 3 points per unused section. Sections the liquid entered, mandatory sections, fixed pipes, the start tile, the finish tile and blocked tiles SHALL NOT count as unused, so the level complete sequence SHALL never remove a fixed pipe from the field.

#### Scenario: Four unused sections
- **WHEN** the level is won and four placed sections were never entered by the liquid
- **THEN** the penalty is 12 points

#### Scenario: No unused sections
- **WHEN** the level is won and every placed section was entered by the liquid
- **THEN** the penalty is 0 points

#### Scenario: A cross used once is not unused
- **WHEN** the level is won and the liquid passed a cross through one channel only
- **THEN** that cross does not count as unused

#### Scenario: A fixed pipe off the route is not unused
- **WHEN** the level is won, one placed section and two fixed pipes were never entered by the liquid
- **THEN** the penalty is 3 points and both fixed pipes stay on the field after the penalty phase
