## MODIFIED Requirements

### Requirement: The flow and the route check agree
The flow SHALL follow the same channel rules as the complete-route check used for fast forward, including the direction of every valve. On every board for which the route check reports a complete route, the flow SHALL end in a win when no section is changed afterwards.

#### Scenario: Complete route is won
- **WHEN** the route check reports a complete route and ticks are sent until the flow ends
- **THEN** the outcome is won

#### Scenario: Route against a valve is not complete
- **WHEN** the placed sections connect the start to the finish only through a valve passed against its direction
- **THEN** the route check does not report a complete route, and the flow ends in a leak at that valve
