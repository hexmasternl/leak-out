## MODIFIED Requirements

### Requirement: Total is earned points minus the penalty, never below 0
The **total** SHALL be the earned points minus the penalty. When the penalty is larger than the earned points, or the earned points are below 0 because of leak penalties, the total SHALL be 0.

#### Scenario: Total with a penalty
- **WHEN** the earned points are 120 and the penalty is 12
- **THEN** the total is 108

#### Scenario: Penalty larger than the earned points
- **WHEN** the earned points are 10 and the penalty is 15
- **THEN** the total is 0

#### Scenario: Negative earned points
- **WHEN** the level is won with earned points of −15 after leak penalties and no unused sections
- **THEN** the dialog shows −15 earned points, a penalty of 0 and a total of 0, and the attempt earns 0 stars
