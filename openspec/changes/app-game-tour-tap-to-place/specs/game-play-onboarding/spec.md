# Spec Delta

## MODIFIED Requirements

### Requirement: Intro tour steps
The intro tour SHALL show these steps in this order, one at a time, each highlighting what it explains and dimming the rest of the screen: the start tile (the liquid comes out here when the countdown ends), the finish tile (lead the liquid here to win), the stack (the sections you get, with the next one at the bottom), placing (drag the next section onto an empty cell), tap to place (a pro tip: tapping an empty cell places the next section there, faster than dragging) and zoom and pan (pinch to zoom, drag the field to move around).

#### Scenario: Step order
- **WHEN** the intro tour is shown and the player taps "Next" five times
- **THEN** the steps for the start, the finish, the stack, placing, tap to place and zoom and pan are shown in that order, each with a step counter such as "2 / 6"

#### Scenario: Start step highlights the start tile
- **WHEN** the start step is shown
- **THEN** the start tile is highlighted and the rest of the screen is dimmed

#### Scenario: Placing step shows a drag hint
- **WHEN** the placing step is shown
- **THEN** the next section in the stack is highlighted and an animated hint moves from it to an empty cell on the field

#### Scenario: Tap to place step shows a tap hint
- **WHEN** the tap to place step is shown
- **THEN** the empty cell the placing step's hint ended on is highlighted, an animated tap hint pulses on that cell, no drag hint is shown, and the step says that tapping an empty cell places the next section there without dragging

#### Scenario: Highlighted tile outside the view
- **WHEN** the finish tile of a large level is outside the visible field and the finish step is shown
- **THEN** the field pans so that the finish tile is visible and highlighted

#### Scenario: View after the tour
- **WHEN** the intro tour ends on a large level
- **THEN** the field shows the start cell as it does when a level starts

### Requirement: Tips are accessible
A shown tip SHALL be announced by a screen reader with its title and explanation, its buttons SHALL be reachable in reading order, and the highlighted element SHALL be named in the announcement. With reduced motion the highlight SHALL appear without animation, the drag hint SHALL be shown as a static arrow and the tap hint SHALL be shown as a static ring on the cell.

#### Scenario: Screen reader announces a tip
- **WHEN** the start step of the intro tour is shown while a screen reader is on
- **THEN** the screen reader announces the step title, its explanation and "Step 1 of 6", and can move to "Next" and "Skip tour"

#### Scenario: Reduced motion drag hint
- **WHEN** animations are turned off on the device and the placing step is shown
- **THEN** a static arrow from the next section to an empty cell is shown and nothing moves

#### Scenario: Reduced motion tap hint
- **WHEN** animations are turned off on the device and the tap to place step is shown
- **THEN** a static ring is shown on the highlighted cell and nothing moves
