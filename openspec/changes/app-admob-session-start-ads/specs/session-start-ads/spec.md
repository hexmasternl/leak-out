## ADDED Requirements

### Requirement: One interstitial at session start
When the player starts a game session from the menu, the app SHALL show one full-screen interstitial ad before the level opens. A session starts when a level or the Daily Pipe opens from any screen other than a level, that is: "Continue", a level picked in "Select level", the Daily Pipe button, or a tapped Daily Pipe reminder. The level or Daily Pipe SHALL open once the ad is closed.

#### Scenario: Continue starts a session
- **WHEN** the player taps "Continue" and an ad is loaded
- **THEN** the interstitial is shown first
- **AND** the level opens after the player closes the ad

#### Scenario: Select level starts a session
- **WHEN** the player picks a level in the level library and an ad is loaded
- **THEN** the interstitial is shown before the level opens

#### Scenario: Daily Pipe starts a session
- **WHEN** the Daily Pipe has been downloaded after the player taps its button, or a tapped reminder opens it, and an ad is loaded
- **THEN** the interstitial is shown before the Daily Pipe opens

### Requirement: No ads between levels
Moving from one level to the next within a session, and restarting a level, SHALL NOT show an ad. An ad is shown again only when the player returns to the menu and starts a new session.

#### Scenario: Several levels in a row
- **WHEN** the player finishes level 5 and continues to levels 6 and 7 without leaving the game
- **THEN** no ad is shown for levels 6 and 7

#### Scenario: New session after the menu
- **WHEN** the player leaves a level for the welcome screen and then taps "Continue"
- **THEN** a new ad is shown

#### Scenario: Restart
- **WHEN** the player restarts the level they are playing
- **THEN** no ad is shown

### Requirement: Ads never block play
If no ad is loaded when a session starts, or the ad fails to show, the level SHALL open immediately. A failed ad SHALL NOT be retried for the same session start. After an ad is closed, fails or is skipped, the app SHALL preload the next one in the background.

#### Scenario: No ad ready
- **WHEN** the player taps "Continue" and no ad has loaded (offline, no fill, SDK not initialised, no consent)
- **THEN** the level opens at once with no delay and no error message

#### Scenario: Ad fails to show
- **WHEN** an ad is loaded but fails to show
- **THEN** the level opens at once

#### Scenario: Preload after use
- **WHEN** an ad has been shown and closed
- **THEN** the next ad starts loading in the background

### Requirement: Only session starts show ads
The app SHALL NOT show ads on any other screen or event: not on the welcome screen, in Settings, between levels, on level complete or game over, nor in the level designer's test play. There SHALL be no banner, rewarded or app-open ads.

#### Scenario: Test play in the designer
- **WHEN** a designer test play starts
- **THEN** no ad is shown

### Requirement: Test ads in debug builds
Debug builds SHALL use Google's test AdMob app ID and test interstitial ad unit ID. Release builds SHALL use the app ID and ad unit ID supplied through the build variables `LEAKOUT_ADMOB_APP_ID` and `LEAKOUT_ADMOB_INTERSTITIAL_ID`, and SHALL fall back to the test IDs when either is missing. A real ad unit ID SHALL NOT be committed to the repository.

#### Scenario: Debug build
- **WHEN** a debug build requests an ad
- **THEN** it uses the Google test ad unit ID

#### Scenario: Release build without variables
- **WHEN** a release build is made without `LEAKOUT_ADMOB_*` variables
- **THEN** it uses the test IDs

### Requirement: Session rule is pure
The decision whether to show an ad at a level start SHALL be made by a pure Kotlin rule in `core` that takes whether the start comes from the menu and whether an ad is ready, and has no Android or AdMob dependency.

#### Scenario: Gate decisions
- **WHEN** a menu start occurs with an ad ready
- **THEN** the rule says to show the ad
- **AND** a level-to-level start, or a start with no ad ready, says not to
