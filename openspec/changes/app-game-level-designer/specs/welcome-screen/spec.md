## ADDED Requirements

### Requirement: Level designer entry button
When the `levelDesigner` feature flag is on, the welcome screen SHALL show a small level designer button in its top-right corner, inside the safe area and outside the Play / Settings / Exit row, with a touch target of at least 48dp and a content description "Level designer". Tapping it SHALL signal the host to open the level designer exactly once. When the flag is off, the button SHALL NOT be shown.

#### Scenario: Button shown in debug
- **WHEN** the welcome screen is displayed with the `levelDesigner` flag on
- **THEN** the level designer button is visible in the top-right corner and the Play, Settings and Exit buttons are unchanged

#### Scenario: Tap the button
- **WHEN** the user taps the level designer button
- **THEN** the open level designer event is emitted exactly once

#### Scenario: Button hidden in release
- **WHEN** the welcome screen is displayed with the `levelDesigner` flag off
- **THEN** no level designer button is shown
