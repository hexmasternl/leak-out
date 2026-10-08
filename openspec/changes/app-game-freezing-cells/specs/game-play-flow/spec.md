# Spec Delta

## MODIFIED Requirements

### Requirement: The cross has two channels
A cross SHALL have a horizontal and a vertical channel. The liquid SHALL pass straight through the channel of the side it enters. Each channel SHALL be used at most once: liquid that reaches a cross on a channel it already used SHALL leak. A cross on a freezing cell SHALL freeze once the liquid has left it, so it SHALL be passed through one channel only: liquid that reaches it again, on either channel, SHALL leak. The points for the first and the second pass SHALL follow the points rules.

#### Scenario: Second pass through a cross
- **WHEN** the liquid has passed a cross from left to right and later enters it from the top
- **THEN** it passes through to the bottom and 50 points are awarded when it leaves

#### Scenario: Used channel
- **WHEN** the liquid has passed a cross from left to right and later reaches it from the right
- **THEN** the level is lost

#### Scenario: Second pass through a cross on a freezing cell
- **WHEN** a cross lies on a freezing cell, the liquid has passed it from left to right and later reaches it from the top
- **THEN** the level is lost at the tile above the cross, and no 50 points are awarded

### Requirement: The flow and the route check agree
The flow SHALL follow the same channel rules as the complete-route check used for fast forward, including the rule that a frozen section refuses the liquid. On every board for which the route check reports a complete route, the flow SHALL end in a win when no section is changed afterwards.

#### Scenario: Complete route is won
- **WHEN** the route check reports a complete route and ticks are sent until the flow ends
- **THEN** the outcome is won

#### Scenario: Route back to a freezing cell
- **WHEN** a board's route passes a cross on a freezing cell twice
- **THEN** the route check reports the route as not complete, and the flow on that board ends in a loss at the second visit
