# Spec Delta

## ADDED Requirements

### Requirement: Freezing cell tip
The first time the player starts a level with freezing cells, the game SHALL freeze before the countdown runs and show the freezing cell tip, highlighting every freezing cell on the board, panning the field when needed so that at least the freezing cell nearest the start is visible. The tip SHALL explain that the liquid freezes in a section on a freezing cell once it has passed, so the route can cross such a cell only once, and that a cross on it can be used for one channel only. The title, the text and the name of the highlighted element SHALL come from string resources, so they are shown in every supported language.

#### Scenario: First level with freezing cells
- **WHEN** the freezing cell tip has not been seen and a level with three freezing cells starts
- **THEN** the game is frozen, the three freezing cells are highlighted and the freezing cell tip is shown

#### Scenario: Freezing cell tip shown once
- **WHEN** the freezing cell tip has been seen and a level with freezing cells starts
- **THEN** no freezing cell tip is shown

#### Scenario: Level without freezing cells
- **WHEN** the freezing cell tip has not been seen and a level without freezing cells starts
- **THEN** no freezing cell tip is shown and the tip stays unseen

#### Scenario: Tip in Dutch
- **WHEN** the app is shown in Dutch and the freezing cell tip is shown
- **THEN** the tip's title, text and button are shown in Dutch

## MODIFIED Requirements

### Requirement: Single tips are dismissed with "Got it"
The cross, reservoir, mandatory section and freezing cell tips SHALL each offer one "Got it" button. Tapping it SHALL close the tip, store it as seen and continue the game from where it froze.

#### Scenario: Continue after a tip
- **WHEN** the cross tip is shown while the liquid is halfway through a tile and the player taps "Got it"
- **THEN** the tip closes, the cross tip is stored as seen and the liquid continues from halfway through that tile

#### Scenario: Close the freezing cell tip
- **WHEN** the freezing cell tip is shown before the countdown and the player taps "Got it"
- **THEN** the tip closes, the freezing cell tip is stored as seen and the countdown starts running

### Requirement: One tip at a time in a fixed order
When more than one tip is due at the same moment, the game SHALL show them one after the other in this order: intro tour, mandatory section tip, freezing cell tip, cross tip, reservoir tip. The game SHALL stay frozen until the last of them is closed.

#### Scenario: Everything due on the first level
- **WHEN** no tip has been seen and the first level played has a mandatory section and an `X` and a `RES` in fully visible slots of its starting stack
- **THEN** the intro tour, the mandatory section tip, the cross tip and the reservoir tip are shown in that order, and the countdown only starts after the reservoir tip is closed

#### Scenario: Freezing cell tip with a cross on the stack
- **WHEN** only the intro tour has been seen and a level with freezing cells starts with an `X` in a fully visible slot of its stack
- **THEN** the freezing cell tip is shown first and the cross tip after it
