## ADDED Requirements

### Requirement: Route is checked after every placement
After every accepted placement or replacement, the system SHALL check whether the board holds a complete route from the start to the finish. When a level starts or restarts, the route SHALL count as not complete.

#### Scenario: Placing the last missing section
- **WHEN** the start and finish are connected by placed sections except for one tile, and the player places a section on that tile that connects both neighbours
- **THEN** the route is complete

#### Scenario: New level
- **WHEN** a level starts or restarts
- **THEN** the route is not complete

### Requirement: Route follows the liquid's rules
The route SHALL be followed the way the liquid flows. It SHALL leave the start through the start's open side. It SHALL enter each next tile through the side facing the previous tile, and SHALL leave it through the other end of the channel it entered: a straight or corner section through its other open side, a cross straight through to the opposite side, and a reservoir only between left and right. The route SHALL be complete only when it enters the finish tile through the finish's open side. The route SHALL NOT be complete when it reaches an empty tile, a blocked tile, the start tile, a side of a section without an opening, the finish from a side other than its open side, the edge of the grid, or a channel of a cross it already used. Whether the liquid has already entered a section SHALL NOT change the result.

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

### Requirement: Fast forward button appears when the route is complete
While the route is complete and fast forward is off, the game screen SHALL show a fast forward button in the bottom right corner of the screen, over the playing field, with a margin of 16 dp from the right and bottom edges. When the route is not complete and fast forward is off, the button SHALL NOT be shown. The button SHALL have a touch target of at least 56 dp by 56 dp. The button SHALL appear and disappear with a short scale and fade animation.

#### Scenario: Route is completed
- **WHEN** the player places a section that completes the route
- **THEN** the fast forward button appears in the bottom right corner of the screen

#### Scenario: Route is broken before fast forward
- **WHEN** the button is shown, fast forward is off, and the player replaces a section so that the route is no longer complete
- **THEN** the button disappears

#### Scenario: Incomplete route
- **WHEN** a level is played and the route is not complete
- **THEN** no fast forward button is shown

### Requirement: Fast forward speeds up the liquid 10 times
Tapping the fast forward button SHALL turn fast forward on. While it is on, the liquid SHALL move 10 times as fast as it would without fast forward, including inside a reservoir and with the level's speed ramp. When the countdown before the flow is still running, the countdown SHALL also run 10 times as fast. Fast forward SHALL NOT change the points awarded, the order in which sections fill, or the result of the level. Other timing on the game screen, such as the points popups, SHALL keep its normal speed.

#### Scenario: Flow at 10x
- **WHEN** the liquid flows at 0.5 cells per second and the player turns fast forward on
- **THEN** the liquid moves 5 cells per second

#### Scenario: Countdown at 10x
- **WHEN** the countdown has 8 seconds left and the player turns fast forward on
- **THEN** the liquid starts flowing after 0.8 seconds of play

#### Scenario: Same points
- **WHEN** a level is won with fast forward on
- **THEN** the score is the same as when the same route is won without fast forward

### Requirement: Fast forward locks the route
While fast forward is on, the player SHALL NOT be able to drag a section from the stack, and no tile SHALL accept a drop or be highlighted. A drag that is going on when fast forward is turned on SHALL be cancelled and the section SHALL go back to its slot. Panning and zooming the playing field SHALL still work. When fast forward is turned off again, placing SHALL work as before.

#### Scenario: No placing after fast forward
- **WHEN** fast forward is on and the player presses and drags the next section
- **THEN** no drag starts and the board and stack stay the same

#### Scenario: Viewing still works
- **WHEN** fast forward is on and the player pans or pinches the playing field
- **THEN** the field pans or zooms as usual

#### Scenario: Placing after slowing down
- **WHEN** fast forward was turned on and then off again, and the player drags the next section onto an empty tile
- **THEN** the section is placed

### Requirement: Fast forward button toggles between fast and normal speed
While fast forward is on, the button SHALL stay visible and SHALL be shown as a slow button. Tapping the slow button SHALL turn fast forward off: the liquid and the countdown SHALL go back to the level's normal speed, and the button SHALL be shown as the fast forward button again. The player SHALL be able to switch between fast and normal speed as often as they like. Pausing and resuming SHALL keep the current speed. While the game is paused, the button SHALL NOT react. Restarting the level SHALL turn fast forward off and hide the button.

#### Scenario: Slow button after fast forward
- **WHEN** the player taps the fast forward button
- **THEN** fast forward is on and the button is shown as a slow button

#### Scenario: Back to normal speed
- **WHEN** fast forward is on and the player taps the slow button
- **THEN** the liquid flows at the level's normal speed and the button is shown as the fast forward button

#### Scenario: Pause and resume
- **WHEN** fast forward is on and the player pauses and resumes
- **THEN** fast forward is still on and the liquid continues at 10 times the speed

#### Scenario: Restart
- **WHEN** fast forward is on and the player restarts the level
- **THEN** fast forward is off and no fast forward button is shown

#### Scenario: Tapping while paused
- **WHEN** the game is paused and the player taps the button
- **THEN** the speed does not change

### Requirement: Fast forward button is accessible
The fast forward button SHALL have the content description "Fast forward" while fast forward is off. While fast forward is on, the slow button SHALL have the content description "Normal speed" and SHALL report that fast forward is on.

#### Scenario: Screen reader reads the button
- **WHEN** a screen reader reaches the fast forward button before it is tapped
- **THEN** it reads "Fast forward"

#### Scenario: Screen reader reads the slow button
- **WHEN** a screen reader reaches the button while fast forward is on
- **THEN** it reads "Normal speed" and that fast forward is on
