# Spec Delta

## Purpose

The splitter section during play: a T-shaped section placed by the level that divides the liquid into two branches, each running to its own finish, with the rules for how the branches move, finish, collide and leak.

## ADDED Requirements

### Requirement: Splitter shape and orientations
A splitter SHALL have three open sides and one closed side. It SHALL come in four orientations, named by the closed side: left, right, top or bottom. Liquid SHALL be able to enter through any of the three open sides. Liquid that reaches the closed side SHALL leak.

#### Scenario: Open sides of a splitter closed at the bottom
- **WHEN** a splitter is closed at the bottom
- **THEN** its left, top and right sides are open

#### Scenario: Liquid reaches the closed side
- **WHEN** the liquid leaves a section towards a splitter whose closed side faces that section
- **THEN** the liquid leaks at the section it left, through the side it left

### Requirement: Splitter divides the liquid into two heads
When the liquid leaves a splitter, it SHALL leave through both open sides other than the one it entered, at the same moment, as two liquid heads. Each head SHALL then move on its own, by the normal flow rules.

#### Scenario: Entering from the left
- **WHEN** the liquid enters a splitter closed at the bottom through its left side and reaches the centre and the far ends
- **THEN** one head leaves through the top and one through the right, at the same moment

#### Scenario: Entering from the top
- **WHEN** the liquid enters a splitter closed at the bottom through its top side
- **THEN** one head leaves through the left and one through the right

### Requirement: The splitter is placed by the level and cannot be changed
A level's splitter SHALL be on the board when an attempt starts or restarts, without liquid. It SHALL never be dealt from the stack. The player SHALL NOT be able to replace or remove it, whatever `rules.allowReplace` says, and it SHALL stay on the board when the unused sections are removed after a win.

#### Scenario: Splitter at the start of an attempt
- **WHEN** a level with a splitter starts
- **THEN** the splitter is on the board at its cell, in its orientation, without liquid

#### Scenario: Drop on the splitter
- **WHEN** the player drops a section on the splitter in a level that allows replacing
- **THEN** the drop is refused and the stack is unchanged

### Requirement: A branch that reaches a finish stops there
When a head enters a finish through its open side, that head SHALL stop at that finish and the finish SHALL count as reached. The other head SHALL keep flowing. The level SHALL be won only when both finishes are reached.

#### Scenario: First branch finishes
- **WHEN** one head enters the first finish while the other head is still two sections away from the second finish
- **THEN** the first finish is reached, the level is not won yet and the other head keeps flowing

#### Scenario: Both branches finish
- **WHEN** the second head then enters the second finish through its open side
- **THEN** the level is won

### Requirement: Either branch leaking loses the level
When either head leaks, the level SHALL be lost at once at that head's tile and exit side, and the other head SHALL stop where it is. A finish that is already reached SHALL NOT make the attempt won.

#### Scenario: One branch leaks after the other finished
- **WHEN** one head has reached its finish and the other head leaves a section into an empty tile
- **THEN** the level is lost at that section and its exit side

#### Scenario: One branch leaks while the other flows
- **WHEN** both heads are flowing and one leaves a section into a blocked tile
- **THEN** the level is lost at that section, and the other head does not move further

### Requirement: A branch cannot enter the other branch's channel
A head SHALL leak when it tries to enter a channel that already holds liquid: a channel the other branch has filled, or the channel the other head is in. A head MAY enter the unused channel of a cross that the other branch passed through the other channel. When both heads try to enter the same channel at the same moment, the head that left the splitter through the side first in the order left, top, right, bottom SHALL enter and the other SHALL leak.

#### Scenario: Branch runs into a filled pipe
- **WHEN** one branch has filled a straight section and the other head leaves a section into that straight through one of its open sides
- **THEN** the level is lost at the section that head left

#### Scenario: Branches cross on a cross section
- **WHEN** one branch has passed a cross horizontally and the other branch enters it from the top
- **THEN** the head passes through to the bottom and the cross scores its second pass

#### Scenario: Both heads reach one channel at once
- **WHEN** the heads that left through the left and the right side of a splitter both reach an empty-channel straight from opposite sides in the same moment
- **THEN** the head from the left side enters it and the head from the right side leaks

### Requirement: The splitter must be passed before a finish counts
In a level with a splitter, liquid that reaches a finish before it has passed the splitter SHALL be refused there, the level SHALL be lost at that finish's open side, and the loss SHALL count as a missed mandatory section.

#### Scenario: Route around the splitter
- **WHEN** a level has a splitter and the liquid reaches the first finish through its open side without passing the splitter
- **THEN** the level is lost at the first finish, and the level lost dialog says that a mandatory section was missed

### Requirement: Splitter is shown on the field and described
The playing field SHALL draw the splitter with its own pipe art, rotated to its orientation, on the tile tint used for sections placed by the level. The liquid SHALL be drawn running into the splitter's centre and then out along both other arms. A screen reader SHALL describe the splitter's cell as a splitter with its closed side, and whether the liquid has passed it.

#### Scenario: Splitter drawn on the board
- **WHEN** a level with a splitter closed at the top is shown
- **THEN** its cell shows the splitter art with the top side closed, on the tinted tile

#### Scenario: Screen reader description
- **WHEN** a screen reader focuses the splitter's cell before the liquid has reached it
- **THEN** it reads "Splitter, closed on the top, not passed yet"

### Requirement: Two heads in the HUD and the warnings
While two heads are flowing, the HUD SHALL show the speed of the faster head, the gap warning and the heartbeat SHALL follow the nearest gap of the two heads, and fast forward SHALL run both heads ten times as fast.

#### Scenario: HUD speed with two heads
- **WHEN** one head moves at 1.2 cells per second and the other at 1.1 cells per second
- **THEN** the HUD shows 1.2

#### Scenario: Fast forward with two heads
- **WHEN** fast forward is on while two heads flow
- **THEN** both heads move ten times as fast
