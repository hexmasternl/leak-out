## ADDED Requirements

### Requirement: One-way valve tip
The first time the player starts a level with at least one valve, the game SHALL freeze before the countdown runs and show the one-way valve tip, highlighting every valve on the board. The tip SHALL explain that a valve is already placed and cannot be replaced or removed, that the liquid can pass it only in the direction of its arrow, that liquid reaching it from any other side leaks, and that it scores like a straight section. The tip text SHALL be in every supported language.

#### Scenario: First level with valves
- **WHEN** the valve tip has not been seen and a level with two valves starts
- **THEN** the game is frozen, both valves are highlighted and the valve tip is shown

#### Scenario: Valve tip shown once
- **WHEN** the valve tip has been seen and a level with valves starts
- **THEN** no valve tip is shown

#### Scenario: Level without valves
- **WHEN** the valve tip has not been seen and a level without valves starts
- **THEN** no valve tip is shown

## MODIFIED Requirements

### Requirement: Single tips are dismissed with "Got it"
The cross, reservoir, mandatory section and one-way valve tips SHALL each offer one "Got it" button. Tapping it SHALL close the tip, store it as seen and continue the game from where it froze.

#### Scenario: Continue after a tip
- **WHEN** the cross tip is shown while the liquid is halfway through a tile and the player taps "Got it"
- **THEN** the tip closes, the cross tip is stored as seen and the liquid continues from halfway through that tile

#### Scenario: Continue after the valve tip
- **WHEN** the valve tip is shown before the countdown and the player taps "Got it"
- **THEN** the tip closes, the valve tip is stored as seen and the countdown starts

### Requirement: One tip at a time in a fixed order
When more than one tip is due at the same moment, the game SHALL show them one after the other in this order: intro tour, mandatory section tip, one-way valve tip, cross tip, reservoir tip. The game SHALL stay frozen until the last of them is closed.

#### Scenario: Everything due on the first level
- **WHEN** no tip has been seen and the first level played has a mandatory section and an `X` and a `RES` in fully visible slots of its starting stack
- **THEN** the intro tour, the mandatory section tip, the cross tip and the reservoir tip are shown in that order, and the countdown only starts after the reservoir tip is closed

#### Scenario: Valve tip after the mandatory section tip
- **WHEN** neither the mandatory section tip nor the valve tip has been seen and a level with a mandatory section and a valve starts
- **THEN** the mandatory section tip is shown first and the valve tip after it
