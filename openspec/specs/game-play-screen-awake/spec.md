# game-play-screen-awake Specification

## Purpose
Keeps the display on while the player is in a level, so the device's screen timeout never dims or locks the screen in the middle of play, and leaves the system timeout alone everywhere else.
## Requirements
### Requirement: Screen stays on while the game screen is open
While the game screen is shown, the app SHALL keep the display on, whatever the state of the level: the countdown, the flowing liquid, fast forward, the pause dialog, the leak effect, the finish flash, the level lost dialog and the level complete dialog. The display SHALL NOT dim or turn off through the screen timeout while the game screen is shown.

#### Scenario: Playing
- **WHEN** a level is open and the liquid is flowing
- **THEN** the screen is kept on

#### Scenario: Paused
- **WHEN** the player pauses the level and the pause dialog is shown
- **THEN** the screen is kept on

#### Scenario: Result dialog
- **WHEN** the level complete or level lost dialog is shown
- **THEN** the screen is kept on

#### Scenario: Next level
- **WHEN** the player taps Next level on the level complete dialog
- **THEN** the screen is still kept on in the next level

### Requirement: Normal screen timeout outside the game screen
When the game screen is closed, the app SHALL stop keeping the display on, so the device's screen timeout applies again. The main menu, the level library and the settings screen SHALL never keep the display on.

#### Scenario: Exit to the main menu
- **WHEN** the player exits a level to the main menu
- **THEN** the screen is no longer kept on

#### Scenario: Menus
- **WHEN** the main menu, the level library or the settings screen is shown
- **THEN** the screen is not kept on

### Requirement: Nothing is held in the background
The app SHALL NOT keep the device or its display awake while it is not visible, and SHALL NOT need the wake lock permission to keep the screen on.

#### Scenario: App goes to the background during a level
- **WHEN** the player presses Home during a level
- **THEN** the level pauses as before and the device follows its normal screen and sleep behaviour

#### Scenario: Back to the game
- **WHEN** the player returns to the paused level
- **THEN** the screen is kept on again
