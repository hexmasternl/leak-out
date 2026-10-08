# Spec Delta

## ADDED Requirements

### Requirement: Fixed pipes are on the board from the start
When an attempt at a level starts or restarts, the board SHALL hold every fixed pipe the level lists, on its cell, with no liquid in it. The stack SHALL NOT be used to put them there, and the stack's contents SHALL NOT depend on them.

#### Scenario: Level starts with its fixed pipes
- **WHEN** a level that lists a fixed `LT` at x 3, y 2 and a fixed `H` at x 5, y 4 starts
- **THEN** the board holds an `LT` at x 3, y 2 and an `H` at x 5, y 4, both without liquid, and the stack is dealt as for any other level

#### Scenario: Restart puts them back unfilled
- **WHEN** the liquid has run through a fixed pipe and the player restarts the level
- **THEN** the fixed pipe is on its cell again without liquid

### Requirement: Fixed pipes are never replaced or removed
A fixed pipe SHALL stay on its cell for the whole attempt. It SHALL NOT be replaced, whether or not the liquid has entered it and whatever the level's `rules.allowReplace`. A drop or a tap on it SHALL be refused and SHALL leave the stack unchanged. It SHALL NOT be taken off the board when the unused sections are removed after a win.

#### Scenario: Drop on a dry fixed pipe
- **WHEN** the player drops a section on a fixed pipe the liquid has not entered, in a level that allows replacing
- **THEN** the drop is refused, the fixed pipe stays, no replacement sequence starts and the stack does not change

#### Scenario: Tap on a fixed pipe
- **WHEN** the player taps a fixed pipe
- **THEN** nothing is placed and the stack does not change

#### Scenario: Removal of unused sections after a win
- **WHEN** a level is won and the liquid never entered one of its fixed pipes
- **THEN** that fixed pipe stays on the board while the unused placed sections are removed

### Requirement: Fixed pipes behave like placed sections for the liquid
For the liquid, the route check, the gap warning, points and the flow speed, a fixed pipe SHALL behave exactly like a placed section of the same kind: the liquid enters it through an open side, leaves it through the other end of the channel it entered, earns the section's points when it leaves, and runs at the reservoir speed inside a fixed reservoir. A fixed cross SHALL have two channels that can each be used once.

#### Scenario: Liquid follows a fixed corner
- **WHEN** the liquid enters a fixed `LB` from the left
- **THEN** it leaves the corner at the bottom and the corner's 15 points are awarded

#### Scenario: Fixed reservoir slows the liquid
- **WHEN** the liquid is inside a fixed reservoir
- **THEN** the liquid moves at the level's speed times `flow.reservoirSpeedFactor`

#### Scenario: Leak out of a fixed pipe
- **WHEN** the liquid leaves a fixed `H` to the right and the tile to its right is empty
- **THEN** the level is lost by a leak at the fixed pipe's tile and its right side

#### Scenario: Closed side of a fixed pipe
- **WHEN** the liquid leaves a placed section towards a fixed `V` through the fixed pipe's left side
- **THEN** the liquid leaks at the placed section, as with a placed `V`

#### Scenario: Route through a fixed pipe completes
- **WHEN** the player's sections and a fixed pipe together form a route from the start to the finish
- **THEN** the route is complete and fast forward becomes available

#### Scenario: Gap warning counts a fixed pipe
- **WHEN** the head has just entered a placed section whose exit leads into a fixed `H`, and the fixed pipe's exit faces an empty tile
- **THEN** the gap distance is 2

### Requirement: A fixed pipe is not required
The level SHALL NOT require the liquid to pass a fixed pipe. Reaching the finish through its open side SHALL win the level whether or not the liquid entered any of the level's fixed pipes.

#### Scenario: Route that avoids every fixed pipe
- **WHEN** the liquid reaches the finish through its open side without entering any of the level's fixed pipes
- **THEN** the level is won

### Requirement: Fixed pipes are drawn bolted down
The field SHALL draw a fixed pipe with the same pipe art and liquid as a placed section of its kind, on a darker steel tile edged in steel with a bolt in each corner, as described in the design system. A fixed pipe SHALL look different from an empty cell, a placed section, a mandatory section and a blocked cell. The look SHALL stay the same while and after the liquid runs through it, and SHALL follow pan and zoom.

#### Scenario: Fixed pipe is drawn
- **WHEN** a level with a fixed `RT` starts
- **THEN** the field draws the corner's pipe art on a darker steel tile with four bolts

#### Scenario: Look stays when filled
- **WHEN** the liquid runs through a fixed cross
- **THEN** the cross keeps its bolted tile and the liquid is drawn in its pipe as in any placed cross

#### Scenario: Placed section next to a fixed one
- **WHEN** the player places an `H` next to a fixed `H`
- **THEN** only the fixed `H` has the bolted tile

### Requirement: Fixed pipes are described for accessibility
The accessibility description of a cell with a fixed pipe SHALL say that the section is fixed, which section it is, and whether the liquid has entered it.

#### Scenario: Description of a dry fixed corner
- **WHEN** a screen reader focuses a fixed `LT` the liquid has not entered
- **THEN** the description says that it is a fixed corner from left to top and that it is empty

### Requirement: A chapter of levels introduces fixed pipes
The game SHALL ship a chapter of 24 bundled levels with fixed pipes, filling exactly one level library page. Every level in the chapter SHALL have at least one fixed pipe, and the first levels of the chapter SHALL introduce the mechanic with few fixed pipes on small grids. Every level in the chapter SHALL pass the solver check. The page SHALL unlock by the existing page star rule.

#### Scenario: Chapter fills a page
- **WHEN** the level library shows the fixed pipe chapter's page
- **THEN** it holds 24 levels, each with at least one fixed pipe

#### Scenario: Chapter levels are solvable
- **WHEN** the headless solver test suite runs over the bundled levels
- **THEN** every level of the fixed pipe chapter is reported solvable

#### Scenario: Chapter unlocks by stars
- **WHEN** the page before the chapter totals fewer stars than the page unlock threshold
- **THEN** the chapter's page is closed

### Requirement: Existing levels and progress are unaffected
Adding fixed pipes SHALL NOT change levels 1 to 96, their level files, their scores, their star thresholds or the player's saved level history, achievements and seen tips.

#### Scenario: Existing level unchanged
- **WHEN** level 12 is played after this change
- **THEN** its board, stack, points and star thresholds are the same as before

#### Scenario: Saved progress kept
- **WHEN** a player who finished levels 1 to 30 updates the app
- **THEN** the level history still holds those 30 results and the same pages are open
