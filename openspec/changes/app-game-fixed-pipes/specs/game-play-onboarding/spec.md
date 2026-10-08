# Spec Delta

## ADDED Requirements

### Requirement: Fixed pipe tip
The first time the player starts a level with fixed pipes, the game SHALL freeze before the countdown runs and show the fixed pipe tip, highlighting every fixed pipe on the board. The tip SHALL explain that these pipes are bolted down by the level and cannot be removed or replaced, that the liquid can run through them like any other pipe, and that the route does not have to use them. The tip SHALL be shown in every supported language.

#### Scenario: First level with fixed pipes
- **WHEN** the fixed pipe tip has not been seen and a level with two fixed pipes starts
- **THEN** the game is frozen, both fixed pipes are highlighted and the fixed pipe tip is shown

#### Scenario: Fixed pipe tip shown once
- **WHEN** the fixed pipe tip has been seen and a level with fixed pipes starts
- **THEN** no fixed pipe tip is shown

#### Scenario: Level without fixed pipes
- **WHEN** the fixed pipe tip has not been seen and a level without fixed pipes starts
- **THEN** no fixed pipe tip is shown

#### Scenario: Fixed pipe outside the view
- **WHEN** the fixed pipe tip is shown on a large level and a fixed pipe is outside the visible field
- **THEN** the field pans so that the fixed pipe is visible and highlighted

#### Scenario: Tip in Dutch
- **WHEN** the app language is Dutch and the fixed pipe tip is shown
- **THEN** its title, explanation and button are in Dutch

## MODIFIED Requirements

### Requirement: Single tips are dismissed with "Got it"
The cross, reservoir, mandatory section and fixed pipe tips SHALL each offer one "Got it" button. Tapping it SHALL close the tip, store it as seen and continue the game from where it froze.

#### Scenario: Continue after a tip
- **WHEN** the cross tip is shown while the liquid is halfway through a tile and the player taps "Got it"
- **THEN** the tip closes, the cross tip is stored as seen and the liquid continues from halfway through that tile

#### Scenario: Continue after the fixed pipe tip
- **WHEN** the fixed pipe tip is shown before the countdown and the player taps "Got it"
- **THEN** the tip closes, the fixed pipe tip is stored as seen and the countdown starts running

### Requirement: One tip at a time in a fixed order
When more than one tip is due at the same moment, the game SHALL show them one after the other in this order: intro tour, mandatory section tip, fixed pipe tip, cross tip, reservoir tip. The game SHALL stay frozen until the last of them is closed.

#### Scenario: Everything due on the first level
- **WHEN** no tip has been seen and the first level played has a mandatory section and an `X` and a `RES` in fully visible slots of its starting stack
- **THEN** the intro tour, the mandatory section tip, the cross tip and the reservoir tip are shown in that order, and the countdown only starts after the reservoir tip is closed

#### Scenario: Fixed pipe tip after the mandatory section tip
- **WHEN** no tip but the intro tour has been seen and a level with a mandatory section, a fixed pipe and an `X` in a fully visible slot of its starting stack starts
- **THEN** the mandatory section tip, the fixed pipe tip and the cross tip are shown in that order
