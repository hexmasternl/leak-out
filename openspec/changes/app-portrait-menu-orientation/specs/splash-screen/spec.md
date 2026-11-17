## MODIFIED Requirements

### Requirement: In-app splash shows the two-line wordmark
After the system splash is dismissed, the app SHALL show a splash screen with a `surface.deep` background. It SHALL display the app name as two lines, "LEAK" on the first line and "OUT" on the second. Each line SHALL be horizontally centred, and the two-line block SHALL be vertically centred on the screen.

#### Scenario: Wordmark layout
- **WHEN** the in-app splash screen is displayed on any screen size, in landscape or in portrait
- **THEN** "LEAK" appears on the first line and "OUT" directly below it, both centred horizontally, with the block centred vertically

#### Scenario: Seamless handoff from the system splash
- **WHEN** the system splash is dismissed and the in-app splash appears
- **THEN** the background colour stays `#0C1A2A`, with no flash of another colour in between

### Requirement: Splash duration and transition to the welcome screen
The in-app splash SHALL stay visible for at least 1 200 ms from the moment it first appears. It SHALL then cross-fade into the welcome screen over 320 ms. The welcome screen SHALL be the destination, and the splash SHALL NOT come back when the player presses back or rotates the device.

#### Scenario: Normal launch
- **WHEN** the app launches and the in-app splash appears
- **THEN** after at least 1 200 ms the splash cross-fades into the welcome screen

#### Scenario: Back from the welcome screen
- **WHEN** the player presses back on the welcome screen
- **THEN** the app exits and the splash screen is not shown again

#### Scenario: Configuration change
- **WHEN** the device rotates between portrait and landscape, or between the two landscape sides, after the splash has finished
- **THEN** the welcome screen stays visible and the splash is not replayed

#### Scenario: Rotation during the splash
- **WHEN** the device rotates while the in-app splash is visible
- **THEN** the splash re-lays out for the new orientation and still hands over to the welcome screen once, without restarting its minimum duration

#### Scenario: Hot start
- **WHEN** the app is brought back to the foreground while its activity is still alive
- **THEN** the current screen is shown directly, with no splash
