# Spec Delta

## Purpose

One-time guided tips during play that teach a new player the start, the finish, the stack, placing, zoom and pan, and the special cross, reservoir and mandatory sections the first time they meet them.

## ADDED Requirements

### Requirement: Intro tour on the first level played
When the player starts a level and the intro tour has not been seen, the game SHALL freeze before the countdown runs and show the intro tour. This SHALL apply to whichever level the player starts first, not only to level 1.

#### Scenario: First level played is level 4
- **WHEN** no tip has been seen and the player starts level 4 from the level library
- **THEN** the intro tour is shown, the countdown is still at the level's full start delay and does not move

#### Scenario: Tour is not shown again
- **WHEN** the intro tour has been seen and the player starts any level
- **THEN** no intro tour is shown and the countdown runs at once

#### Scenario: Restart does not show the tour again
- **WHEN** the player finishes the intro tour and later restarts the same level
- **THEN** no intro tour is shown

### Requirement: Intro tour steps
The intro tour SHALL show these steps in this order, one at a time, each highlighting what it explains and dimming the rest of the screen: the start tile (the liquid comes out here when the countdown ends), the finish tile (lead the liquid here to win), the stack (the sections you get, with the next one at the bottom), placing (drag the next section onto an empty cell) and zoom and pan (pinch to zoom, drag the field to move around).

#### Scenario: Step order
- **WHEN** the intro tour is shown and the player taps "Next" four times
- **THEN** the steps for the start, the finish, the stack, placing and zoom and pan are shown in that order, each with a step counter such as "2 / 5"

#### Scenario: Start step highlights the start tile
- **WHEN** the start step is shown
- **THEN** the start tile is highlighted and the rest of the screen is dimmed

#### Scenario: Placing step shows a drag hint
- **WHEN** the placing step is shown
- **THEN** the next section in the stack is highlighted and an animated hint moves from it to an empty cell on the field

#### Scenario: Highlighted tile outside the view
- **WHEN** the finish tile of a large level is outside the visible field and the finish step is shown
- **THEN** the field pans so that the finish tile is visible and highlighted

#### Scenario: View after the tour
- **WHEN** the intro tour ends on a large level
- **THEN** the field shows the start cell as it does when a level starts

### Requirement: Moving through and skipping the intro tour
Every step except the last SHALL offer "Next" and "Skip tour"; the last step SHALL offer "Let's go". Finishing or skipping the tour SHALL mark it as seen, close it and start the countdown.

#### Scenario: Finishing the tour
- **WHEN** the zoom and pan step is shown and the player taps "Let's go"
- **THEN** the tour closes, the countdown starts running and the intro tour is stored as seen

#### Scenario: Skipping the tour
- **WHEN** the stack step is shown and the player taps "Skip tour"
- **THEN** the tour closes without showing the remaining steps, the countdown starts running and the intro tour is stored as seen

#### Scenario: Tour interrupted by closing the app
- **WHEN** the app is closed while the intro tour is shown and the player later starts a level
- **THEN** the intro tour is shown again from its first step

### Requirement: Cross section tip
The first time a cross (`X`) in the stack is fully visible on screen during play, the game SHALL freeze and show the cross tip, highlighting the cross in the stack. A section is fully visible when its whole stack slot lies within the visible area of the screen; on a small screen the top of the stack can be cut off, and an `X` in a slot that is partly or entirely cut off SHALL NOT trigger the tip. The tip SHALL explain that a cross has two separate channels the liquid passes straight through, that the first pass scores 10 points and the second pass through the other channel 50 points, and that each channel can be used only once.

#### Scenario: Cross drawn onto the stack
- **WHEN** the cross tip has not been seen and a placement draws an `X` onto the top of the stack, where its slot is fully visible
- **THEN** the game freezes, the `X` in the stack is highlighted and the cross tip is shown with its explanation of the two channels and the 10 and 50 points

#### Scenario: Cross drawn onto a cut-off top of the stack
- **WHEN** the cross tip has not been seen and a placement draws an `X` onto the top of the stack, where its slot is partly outside the visible area
- **THEN** no tip is shown and the game keeps running

#### Scenario: Cross moves into view
- **WHEN** the cross tip has not been seen, an `X` is in a cut-off slot of the stack and a later placement moves it down into a fully visible slot
- **THEN** the game freezes, the `X` is highlighted in that slot and the cross tip is shown

#### Scenario: Cross in the starting stack
- **WHEN** the cross tip has not been seen and a level starts with an `X` in a fully visible slot of its stack
- **THEN** the cross tip is shown before the countdown runs, after the intro tour when that is shown too

#### Scenario: Cross in a cut-off part of the starting stack
- **WHEN** the cross tip has not been seen and a level starts with an `X` only in slots of its stack that are cut off
- **THEN** no cross tip is shown before the countdown, and the cross tip is shown once a placement moves the `X` into a fully visible slot

#### Scenario: Cross tip shown once
- **WHEN** the cross tip has been seen and an `X` is drawn onto the stack
- **THEN** no tip is shown and the game keeps running

### Requirement: Reservoir tip
The first time a reservoir (`RES`) in the stack is fully visible on screen during play, the game SHALL freeze and show the reservoir tip, highlighting the reservoir in the stack. The tip SHALL explain that a reservoir only connects left and right, that the liquid slows down to the level's reservoir speed while it fills the reservoir, shown as a percentage of normal speed, and that it scores 25 points. Full visibility SHALL be judged as for the cross tip: a `RES` in a slot that is partly or entirely cut off SHALL NOT trigger the tip.

#### Scenario: Reservoir drawn onto the stack
- **WHEN** the reservoir tip has not been seen, the level's reservoir speed factor is 0.3 and a placement draws a `RES` onto the stack, where its slot is fully visible
- **THEN** the game freezes, the `RES` in the stack is highlighted and the tip says the liquid flows at 30 % speed in a reservoir and that it scores 25 points

#### Scenario: Reservoir waits until it is in view
- **WHEN** the reservoir tip has not been seen, a `RES` is drawn onto a cut-off top of the stack and two placements later it has moved down into a fully visible slot
- **THEN** no tip is shown on the first placement and the reservoir tip is shown after the placement that makes the `RES` fully visible

#### Scenario: Reservoir tip shown once
- **WHEN** the reservoir tip has been seen and a `RES` is drawn onto the stack
- **THEN** no tip is shown and the game keeps running

### Requirement: Mandatory section tip
The first time the player starts a level with mandatory sections, the game SHALL freeze before the countdown runs and show the mandatory section tip, highlighting every mandatory section on the board. The tip SHALL explain that these sections are already placed and cannot be removed or replaced, and that the liquid must pass through every channel of each of them before it reaches the finish or the level is lost.

#### Scenario: First level with mandatory sections
- **WHEN** the mandatory section tip has not been seen and a level with one mandatory cross and one mandatory reservoir starts
- **THEN** the game is frozen, both mandatory sections are highlighted and the mandatory section tip is shown

#### Scenario: Mandatory section tip shown once
- **WHEN** the mandatory section tip has been seen and a level with mandatory sections starts
- **THEN** no mandatory section tip is shown

### Requirement: Single tips are dismissed with "Got it"
The cross, reservoir and mandatory section tips SHALL each offer one "Got it" button. Tapping it SHALL close the tip, store it as seen and continue the game from where it froze.

#### Scenario: Continue after a tip
- **WHEN** the cross tip is shown while the liquid is halfway through a tile and the player taps "Got it"
- **THEN** the tip closes, the cross tip is stored as seen and the liquid continues from halfway through that tile

### Requirement: One tip at a time in a fixed order
When more than one tip is due at the same moment, the game SHALL show them one after the other in this order: intro tour, mandatory section tip, cross tip, reservoir tip. The game SHALL stay frozen until the last of them is closed.

#### Scenario: Everything due on the first level
- **WHEN** no tip has been seen and the first level played has a mandatory section and an `X` and a `RES` in fully visible slots of its starting stack
- **THEN** the intro tour, the mandatory section tip, the cross tip and the reservoir tip are shown in that order, and the countdown only starts after the reservoir tip is closed

### Requirement: The game is frozen while a tip is shown
While a tip is shown, the countdown and the flow SHALL not move, the player SHALL not be able to drag a section or zoom and pan the field, a drag in progress SHALL be cancelled as when the game pauses, and the pause dialog SHALL not open. Fast forward SHALL keep its state after the tip.

#### Scenario: Countdown stops during a tip
- **WHEN** the countdown shows 8 seconds, a tip is shown for 20 seconds and then closed
- **THEN** the countdown still shows 8 seconds when the tip closes

#### Scenario: No placing during a tip
- **WHEN** a tip is shown and the player tries to drag the next section
- **THEN** no drag starts and the board and stack do not change

#### Scenario: No pause dialog for a tip
- **WHEN** a tip is shown
- **THEN** the pause dialog is not shown

### Requirement: Back and backgrounding during a tip
While a tip is shown, system Back SHALL act as "Skip tour" during the intro tour and as "Got it" on a single tip. When the app goes to the background while a tip is shown, the pause dialog SHALL be shown over the tip on return, and Resume SHALL close the dialog and leave the tip shown with the game still frozen.

#### Scenario: Back skips the tour
- **WHEN** the finish step of the intro tour is shown and the player presses Back
- **THEN** the tour closes, the intro tour is stored as seen and the game screen stays open

#### Scenario: Coming back to the app during a tip
- **WHEN** the player leaves the app while the reservoir tip is shown, comes back and taps Resume
- **THEN** the reservoir tip is still shown and the countdown and flow have not moved

### Requirement: Tips are accessible
A shown tip SHALL be announced by a screen reader with its title and explanation, its buttons SHALL be reachable in reading order, and the highlighted element SHALL be named in the announcement. With reduced motion the highlight SHALL appear without animation and the drag hint SHALL be shown as a static arrow.

#### Scenario: Screen reader announces a tip
- **WHEN** the start step of the intro tour is shown while a screen reader is on
- **THEN** the screen reader announces the step title, its explanation and "Step 1 of 5", and can move to "Next" and "Skip tour"

#### Scenario: Reduced motion drag hint
- **WHEN** animations are turned off on the device and the placing step is shown
- **THEN** a static arrow from the next section to an empty cell is shown and nothing moves
