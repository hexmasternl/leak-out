## ADDED Requirements

### Requirement: Startup version label
When the welcome screen is first displayed after the app process starts, it SHALL show the installed version as "v<versionName> (<versionCode>)" in its lower-right corner, inside the safe area, without overlapping the Play, Settings and Exit buttons. The label SHALL stay fully visible for 5 seconds and then fade out over 300 ms. Once it has faded out it SHALL NOT be shown again while the process runs, including when the player returns to the welcome screen from another screen. The label SHALL NOT receive touches. With the system "remove animations" setting on, the fade SHALL take 160 ms instead of 300 ms.

#### Scenario: Version shown at start
- **WHEN** the app starts with version name "1.4.0" and version code 57 and the welcome screen is displayed
- **THEN** the lower-right corner shows "v1.4.0 (57)" fully opaque

#### Scenario: Version fades out
- **WHEN** 5 seconds have passed since the welcome screen was first displayed
- **THEN** the label fades out over 300 ms
- **AND** after the fade the label is no longer shown

#### Scenario: Not shown again in the same session
- **WHEN** the label has faded out and the player opens Settings and returns to the welcome screen
- **THEN** no version label is shown

#### Scenario: Leaving before the fade ends
- **WHEN** the player taps Play within the first 5 seconds
- **THEN** the label is not shown again when the player later returns to the welcome screen

#### Scenario: Label does not block buttons
- **WHEN** the label is visible
- **THEN** the Play, Settings and Exit buttons are in the same position as without the label and respond to taps

#### Scenario: Reduced motion
- **WHEN** the system "remove animations" setting is on and 5 seconds have passed
- **THEN** the label fades out over 160 ms
