## MODIFIED Requirements

### Requirement: HUD bar at the top of the game screen
The game screen SHALL show a HUD bar along the top edge, across the full width of the screen, in landscape. The HUD SHALL only keep clear of a display cutout on a side where that cutout overlaps the HUD's vertical band; a cutout beside the stack column or the board SHALL NOT narrow the HUD. The stack column and the board SHALL be placed below the HUD, SHALL NOT be covered by it, and SHALL keep clear of the display cutout. The HUD SHALL have a fixed height, so that the space for the stack and the board does not change while playing.

#### Scenario: HUD is shown when a level starts
- **WHEN** the player starts a level
- **THEN** the HUD bar is shown at the top of the game screen, and the stack column and the board are shown below it

#### Scenario: Largest stack still fits below the HUD
- **WHEN** the game screen is shown on a 360 dp tall landscape screen for a level with a `stack.visibleSize` of 10
- **THEN** the HUD and all 10 stack sections are fully visible, and nothing scrolls or overlaps

#### Scenario: Cutout beside the field
- **WHEN** the game screen is shown in landscape on a device whose camera cutout sits in the middle of the right short edge, below the HUD band
- **THEN** the HUD spans from the left edge to the right edge of the screen, and the playing field stays clear of the cutout

#### Scenario: Cutout beside the stack
- **WHEN** the device is turned to the other landscape side, so the same cutout sits in the middle of the left short edge
- **THEN** the HUD spans from the left edge to the right edge of the screen, and the stack column stays clear of the cutout

#### Scenario: Cutout in the HUD band
- **WHEN** the game screen is shown on a device whose cutout overlaps the HUD's vertical band on the left
- **THEN** the HUD content starts to the right of the cutout, and the right side of the HUD still reaches the right edge of the screen
