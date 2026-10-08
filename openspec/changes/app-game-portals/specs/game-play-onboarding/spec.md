## ADDED Requirements

### Requirement: Portal tip
The first time the player starts a level with portal pairs, the game SHALL freeze before the countdown runs and show the portal tip, highlighting every portal on the board. The tip SHALL explain that liquid entering a portal through its open side comes out of the portal with the same colour and symbol, through that portal's open side, that pairs work both ways, and that portals cannot be replaced and score no points. The tip text SHALL be translatable and shown in every supported language.

#### Scenario: First level with portals
- **WHEN** the portal tip has not been seen and a level with two portal pairs starts
- **THEN** the game is frozen, all four portals are highlighted and the portal tip is shown

#### Scenario: Portal tip shown once
- **WHEN** the portal tip has been seen and a level with portals starts
- **THEN** no portal tip is shown

#### Scenario: Portal tip in Dutch
- **WHEN** the app language is Dutch and the portal tip is shown
- **THEN** its title, explanation and button are shown in Dutch

## MODIFIED Requirements

### Requirement: Single tips are dismissed with "Got it"
The cross, reservoir, mandatory section and portal tips SHALL each offer one "Got it" button. Tapping it SHALL close the tip, store it as seen and continue the game from where it froze.

#### Scenario: Continue after a tip
- **WHEN** the cross tip is shown while the liquid is halfway through a tile and the player taps "Got it"
- **THEN** the tip closes, the cross tip is stored as seen and the liquid continues from halfway through that tile

#### Scenario: Continue after the portal tip
- **WHEN** the portal tip is shown before the countdown and the player taps "Got it"
- **THEN** the tip closes, the portal tip is stored as seen and the countdown starts running

### Requirement: One tip at a time in a fixed order
When more than one tip is due at the same moment, the game SHALL show them one after the other in this order: intro tour, mandatory section tip, portal tip, cross tip, reservoir tip. The game SHALL stay frozen until the last of them is closed.

#### Scenario: Everything due on the first level
- **WHEN** no tip has been seen and the first level played has a mandatory section and an `X` and a `RES` in fully visible slots of its starting stack
- **THEN** the intro tour, the mandatory section tip, the cross tip and the reservoir tip are shown in that order, and the countdown only starts after the reservoir tip is closed

#### Scenario: Portal tip after the intro tour
- **WHEN** no tip has been seen and the first level played has a portal pair and an `X` in a fully visible slot of its starting stack
- **THEN** the intro tour, the portal tip and the cross tip are shown in that order
