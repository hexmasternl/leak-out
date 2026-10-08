## MODIFIED Requirements

### Requirement: Single tips are dismissed with "Got it"
The cross, reservoir, pump and mandatory section tips SHALL each offer one "Got it" button. Tapping it SHALL close the tip, store it as seen and continue the game from where it froze.

#### Scenario: Continue after a tip
- **WHEN** the cross tip is shown while the liquid is halfway through a tile and the player taps "Got it"
- **THEN** the tip closes, the cross tip is stored as seen and the liquid continues from halfway through that tile

### Requirement: One tip at a time in a fixed order
When more than one tip is due at the same moment, the game SHALL show them one after the other in this order: intro tour, mandatory section tip, cross tip, reservoir tip, pump tip. The game SHALL stay frozen until the last of them is closed.

#### Scenario: Everything due on the first level
- **WHEN** no tip has been seen and the first level played has a mandatory section and an `X` and a `RES` in fully visible slots of its starting stack
- **THEN** the intro tour, the mandatory section tip, the cross tip and the reservoir tip are shown in that order, and the countdown only starts after the reservoir tip is closed

#### Scenario: Pump tip comes last
- **WHEN** no tip has been seen and the first level played has a mandatory pump and a `RES` in a fully visible slot of its starting stack
- **THEN** the intro tour, the mandatory section tip, the reservoir tip and the pump tip are shown in that order, and the countdown only starts after the pump tip is closed

#### Scenario: Pump tip dismissed
- **WHEN** the pump tip is shown and the player taps "Got it"
- **THEN** the tip closes, the pump tip is stored as seen and the game continues from where it froze
