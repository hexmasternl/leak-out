## ADDED Requirements

### Requirement: Leaky tile tip
The first time the player starts a level with leaky tiles, the game SHALL freeze before the countdown runs and show the leaky tile tip, highlighting every leaky tile on the board. The tip SHALL explain that any section can be placed on a leaky tile, that liquid passing it drains away, and that each pass scores no points and costs 20 points, so a detour can be worth more.

#### Scenario: First level with leaky tiles
- **WHEN** the leaky tile tip has not been seen and a level with two leaky tiles starts
- **THEN** the game is frozen, both leaky tiles are highlighted and the leaky tile tip is shown with its explanation of the 20-point cost

#### Scenario: Leaky tile tip shown once
- **WHEN** the leaky tile tip has been seen and a level with leaky tiles starts
- **THEN** no leaky tile tip is shown and the countdown runs

#### Scenario: Level without leaky tiles
- **WHEN** the leaky tile tip has not been seen and a level without leaky tiles starts
- **THEN** no leaky tile tip is shown and the tip stays unseen

## MODIFIED Requirements

### Requirement: Single tips are dismissed with "Got it"
The cross, reservoir, mandatory section and leaky tile tips SHALL each offer one "Got it" button. Tapping it SHALL close the tip, store it as seen and continue the game from where it froze.

#### Scenario: Continue after a tip
- **WHEN** the cross tip is shown while the liquid is halfway through a tile and the player taps "Got it"
- **THEN** the tip closes, the cross tip is stored as seen and the liquid continues from halfway through that tile

#### Scenario: Leaky tile tip dismissed
- **WHEN** the leaky tile tip is shown before the countdown and the player taps "Got it"
- **THEN** the tip closes, the leaky tile tip is stored as seen and the countdown starts running

### Requirement: One tip at a time in a fixed order
When more than one tip is due at the same moment, the game SHALL show them one after the other in this order: intro tour, mandatory section tip, leaky tile tip, cross tip, reservoir tip. The game SHALL stay frozen until the last of them is closed.

#### Scenario: Everything due on the first level
- **WHEN** no tip has been seen and the first level played has a mandatory section and an `X` and a `RES` in fully visible slots of its starting stack
- **THEN** the intro tour, the mandatory section tip, the cross tip and the reservoir tip are shown in that order, and the countdown only starts after the reservoir tip is closed

#### Scenario: Leaky tile tip after the mandatory section tip
- **WHEN** no tip but the intro tour has been seen and a level with a mandatory section, a leaky tile and an `X` in a fully visible slot of its starting stack starts
- **THEN** the mandatory section tip, the leaky tile tip and the cross tip are shown in that order, and the countdown only starts after the cross tip is closed
