## ADDED Requirements

### Requirement: Portal tip
The first time the player sees a portal pair, that is the first time a level with portal pairs starts, the game SHALL freeze before the countdown runs and show the portal tip. The tip SHALL identify both portals of one pair, pair 1: it SHALL highlight those two portals in the pair colour and mark them as belonging together, with a two-headed link between them. The tip SHALL explain that the pair works both ways: liquid entering either portal through its open side comes out of the other portal of the pair, through that portal's open side. It SHALL also say that portals with the same colour belong together, that a pair can be used once, and that portals are part of the level: they do not come from the stack and cannot be replaced. The tip text SHALL be translatable and shown in every supported language.

#### Scenario: First level with portals
- **WHEN** the portal tip has not been seen and level 49, with one portal pair, starts
- **THEN** the game is frozen, both portals of the pair are highlighted with a two-headed link between them, and the portal tip explains that liquid can enter either portal and comes out of the other

#### Scenario: Several pairs on the first level with portals
- **WHEN** the portal tip has not been seen and a level with two portal pairs starts
- **THEN** both portals of pair 1 are highlighted and linked, and the portals of pair 2 are not highlighted
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
