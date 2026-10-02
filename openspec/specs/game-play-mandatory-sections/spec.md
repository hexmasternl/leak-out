# game-play-mandatory-sections Specification

## Purpose
Lets a level pre-place cross and reservoir sections that the player cannot remove and that the liquid must run through before the level can be won, so levels can force a route's shape.
## Requirements
### Requirement: Mandatory sections are on the board from the start
When an attempt at a level starts or restarts, the board SHALL hold every mandatory section the level lists, on its cell, with no liquid in it. The stack SHALL NOT be used to put them there, and the stack's contents SHALL NOT depend on them.

#### Scenario: Level starts with its mandatory sections
- **WHEN** a level that lists a mandatory cross at x 3, y 2 and a mandatory reservoir at x 5, y 4 starts
- **THEN** the board holds a cross at x 3, y 2 and a reservoir at x 5, y 4, both without liquid, and the stack is drawn as for any other level

#### Scenario: Restart puts them back unfilled
- **WHEN** the liquid has passed a mandatory section and the player restarts the level
- **THEN** the mandatory section is on its cell again without liquid

### Requirement: Mandatory sections behave like placed sections for the liquid
For the liquid, the route check, points and the flow speed, a mandatory section SHALL behave exactly like a placed section of the same kind: the liquid enters it through an open side, leaves it through the other end of the channel it entered, earns the section's points when it leaves, and runs at the reservoir speed inside a mandatory reservoir.

#### Scenario: Liquid passes a mandatory cross
- **WHEN** the liquid enters a mandatory cross from the left
- **THEN** it leaves the cross on the right and the cross's points are awarded

#### Scenario: Mandatory reservoir slows the liquid
- **WHEN** the liquid is inside a mandatory reservoir
- **THEN** the liquid moves at the level's speed times `flow.reservoirSpeedFactor`

#### Scenario: Leak at a mandatory section
- **WHEN** the liquid leaves a mandatory reservoir to the right and the tile to its right is empty
- **THEN** the level is lost by a leak, as with a placed reservoir

### Requirement: A mandatory section is used when every channel is passed
A mandatory section SHALL count as **used** once the liquid has run through every channel it has: both the horizontal and the vertical channel of a cross, and the single left-to-right channel of a reservoir, in either direction. A channel the liquid is still running through SHALL count as passed once the liquid has left it.

#### Scenario: Reservoir passed once
- **WHEN** the liquid has run through a mandatory reservoir from right to left
- **THEN** the reservoir is used

#### Scenario: Cross passed on one channel
- **WHEN** the liquid has run through a mandatory cross from left to right only
- **THEN** the cross is not used

#### Scenario: Cross passed on both channels
- **WHEN** the liquid has run through a mandatory cross from left to right and later from bottom to top
- **THEN** the cross is used

### Requirement: Mandatory sections are never removed
A mandatory section SHALL stay on its cell for the whole attempt. It SHALL NOT be replaced, whether or not the liquid has entered it and whatever the level's `rules.allowReplace`, and it SHALL NOT be taken off the board when the unused sections are removed after a win.

#### Scenario: Replacing a dry mandatory section
- **WHEN** the player drops a section on a mandatory section the liquid has not entered, in a level that allows replacing
- **THEN** the drop is refused, the mandatory section stays and the stack does not change

#### Scenario: Removal of unused sections after a win
- **WHEN** a level with mandatory sections is won and the unused placed sections are removed one by one
- **THEN** every mandatory section stays on the board

### Requirement: Mandatory sections are described for accessibility
The accessibility description of a cell with a mandatory section SHALL say that the section is mandatory, which section it is, and whether it is used.

#### Scenario: Description of a mandatory cross
- **WHEN** a screen reader focuses a mandatory cross the liquid has passed on one channel only
- **THEN** the description says that it is a mandatory cross and that it is not used yet
