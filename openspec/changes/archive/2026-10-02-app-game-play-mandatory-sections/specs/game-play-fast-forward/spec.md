# Spec Delta

## ADDED Requirements

### Requirement: Route passes every mandatory section
A route that enters the finish through its open side SHALL only count as complete when it runs through every channel of every mandatory section of the level: both channels of a mandatory cross and the channel of a mandatory reservoir. A level without mandatory sections SHALL be unaffected.

#### Scenario: Route skips a mandatory reservoir
- **WHEN** the placed sections connect the start to the finish without running through the level's mandatory reservoir
- **THEN** the route is not complete and the fast forward button is not shown

#### Scenario: Route uses one channel of a mandatory cross
- **WHEN** the route runs through a mandatory cross from left to right only and then enters the finish through its open side
- **THEN** the route is not complete

#### Scenario: Route through every mandatory section
- **WHEN** the route runs through the mandatory reservoir, through both channels of the mandatory cross, and enters the finish through its open side
- **THEN** the route is complete and the fast forward button is shown
