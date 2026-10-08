# Spec Delta

## MODIFIED Requirements

### Requirement: Route follows the liquid's rules
The route SHALL be followed the way the liquid flows. It SHALL leave the start through the start's open side. It SHALL enter each next tile through the side facing the previous tile, and SHALL leave it through the other end of the channel it entered: a straight or corner section through its other open side, a cross straight through to the opposite side, a reservoir only between left and right, and a splitter through both of its other open sides, after which each branch is followed on its own. The route SHALL be complete only when every branch enters a different finish through its open side and no channel is used by both branches. The route SHALL NOT be complete when it reaches an empty tile, a blocked tile, the start tile, a side of a section without an opening, the finish from a side other than its open side, the edge of the grid, or a channel of a cross it already used. Whether the liquid has already entered a section SHALL NOT change the result.

#### Scenario: Straight and corner route
- **WHEN** the start opens to the right, the tile to its right holds `H`, the next tile holds `LB`, the tile below that holds `V`, and the tile below that is the finish, open at the top
- **THEN** the route is complete

#### Scenario: Section without a matching opening
- **WHEN** the route leaves a tile to the right and the next tile holds `V`
- **THEN** the route is not complete

#### Scenario: Finish reached from the wrong side
- **WHEN** the route enters the finish tile from its left side and the finish is open at the top
- **THEN** the route is not complete

#### Scenario: Cross is passed straight through
- **WHEN** the route enters a cross from the left
- **THEN** the route leaves the cross on the right

#### Scenario: Cross used on both channels
- **WHEN** the route passes a cross from left to right, and later comes back to the same cross from the top, and continues to the finish
- **THEN** the route is complete

#### Scenario: Reservoir entered from the top
- **WHEN** the route enters a reservoir from the top
- **THEN** the route is not complete

#### Scenario: Route with liquid already in it
- **WHEN** the liquid has already entered the first sections of a route that is complete
- **THEN** the route is still complete

#### Scenario: Both branches reach a finish
- **WHEN** the route runs into a splitter, one branch leads into the first finish and the other into the second finish, each through its open side
- **THEN** the route is complete

#### Scenario: One branch is open
- **WHEN** the route runs into a splitter, one branch leads into a finish and the other ends at an empty tile
- **THEN** the route is not complete

#### Scenario: Branches share a pipe
- **WHEN** both branches after a splitter run through the same straight section
- **THEN** the route is not complete
