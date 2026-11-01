## MODIFIED Requirements

### Requirement: Menu buttons below the title
The welcome screen SHALL show the buttons labelled "Play", "Settings", and "Exit", positioned below the title, in that order. When the player is signed in with Google Play Games, the welcome screen SHALL also show a "Daily Pipe" button directly below "Play"; otherwise that button SHALL NOT be shown (see the `daily-pipe` capability).

#### Scenario: Menu layout
- **WHEN** the welcome screen is displayed and the player is not signed in with Play Games
- **THEN** the buttons Play, Settings, and Exit are visible below the title
- **AND** each button has a touch target of at least 48dp

#### Scenario: Menu layout when signed in
- **WHEN** the welcome screen is displayed and the player is signed in with Play Games
- **THEN** the buttons Play, Daily Pipe, Settings, and Exit are visible below the title, in that order
- **AND** each button has a touch target of at least 48dp

#### Scenario: Button pressed feedback
- **WHEN** the user presses and holds a menu button
- **THEN** the button visibly compresses downward following the design-system pressed state
