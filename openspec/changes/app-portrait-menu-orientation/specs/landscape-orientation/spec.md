## REMOVED Requirements

### Requirement: Landscape only
**Reason**: The app-wide landscape lock makes Google Play imply a required landscape screen feature, which blocks production review (#179). Only playing needs landscape.
**Migration**: Replaced by "Menu follows the device orientation" and "Landscape while playing".

## ADDED Requirements

### Requirement: Menu follows the device orientation
The splash screen and the welcome screen, including the dialogs opened from it, SHALL follow the device orientation, in landscape and in portrait, and SHALL respect the user's rotation lock.

#### Scenario: Launch with device held in portrait
- **WHEN** the user launches the app while holding the device in portrait with auto-rotate on
- **THEN** the splash screen and then the welcome screen are displayed in portrait

#### Scenario: Rotate on the welcome screen
- **WHEN** the user rotates the device between portrait and landscape while the welcome screen is shown
- **THEN** the welcome screen re-lays out for the new orientation
- **AND** the splash is not replayed and any open dialog stays open

#### Scenario: Rotation lock on
- **WHEN** the user's rotation lock is on and the device is held in portrait
- **THEN** the welcome screen is displayed in the orientation the system rotation lock dictates

### Requirement: Session-start ad in the current orientation
The session-start ad SHALL be shown in the orientation the app is in when the player starts a level from a menu screen. The landscape lock for playing SHALL only apply once the ad has closed or has been skipped.

#### Scenario: Start a level in portrait
- **WHEN** the player taps "Continue" on the play menu while the welcome screen is in portrait and an ad is shown
- **THEN** the ad is displayed in portrait
- **AND** the level opens in landscape after the ad is closed

#### Scenario: No ad available
- **WHEN** the player starts a level in portrait and no ad is shown
- **THEN** the level opens directly in landscape

### Requirement: Landscape while playing
Every screen other than the splash and the welcome screen SHALL be displayed in landscape and MUST NOT rotate to portrait, regardless of device rotation lock settings or physical device orientation. This covers a level, the Daily Pipe, Select level, Settings, Achievements, the level designer and its test play.

#### Scenario: Level opens while the device is in portrait
- **WHEN** a level is opened while the device is held in portrait
- **THEN** the playing field is displayed in landscape
- **AND** the level, its stack and its timer are not reset by the orientation switch

#### Scenario: Device rotated while playing
- **WHEN** the user physically rotates the device to portrait while a level is on screen
- **THEN** the app stays in landscape

#### Scenario: Back to the welcome screen
- **WHEN** the player leaves a level and returns to the welcome screen
- **THEN** the welcome screen follows the device orientation again
