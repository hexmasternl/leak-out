## MODIFIED Requirements

### Requirement: Start and finish tiles give no points
The start tile, the finish tile and portals SHALL NOT give points. The liquid leaving the start tile, entering the finish tile or passing a portal pair SHALL NOT change the score.

#### Scenario: Liquid leaves the start tile
- **WHEN** the flow starts and the liquid leaves the start tile into the first placed section
- **THEN** the score stays the same

#### Scenario: Liquid reaches the finish
- **WHEN** the liquid exits the last placed section and enters the finish tile
- **THEN** the score goes up only by the points for that last section

#### Scenario: Liquid passes a portal pair
- **WHEN** the liquid exits a straight section into a portal, passes the pair and exits a corner section after the other portal
- **THEN** the score goes up by 10 and 15 only, and the portal pair adds nothing
