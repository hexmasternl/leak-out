# Spec Delta

## MODIFIED Requirements

### Requirement: Reaching the finish wins the level
When the liquid leaves a section and enters the finish tile through the finish's open side, and every mandatory section of the level is used, the system SHALL mark the current attempt as **won**. A level SHALL be won at most once per attempt, and a won attempt SHALL NOT become lost. The points for the section the liquid just left SHALL be awarded before the level is won.

#### Scenario: Liquid enters the finish
- **WHEN** the liquid leaves a section into the finish tile through the finish's open side, in a level without mandatory sections
- **THEN** the level is won

#### Scenario: Liquid enters the finish after every mandatory section
- **WHEN** the liquid has used every mandatory section and then leaves a section into the finish tile through the finish's open side
- **THEN** the level is won

#### Scenario: Points before the win
- **WHEN** the liquid leaves a corner section into the finish with a score of 40
- **THEN** the score is 55 and the level is won

#### Scenario: Won at most once
- **WHEN** the level is already won and the finish is reported as reached again
- **THEN** nothing changes
