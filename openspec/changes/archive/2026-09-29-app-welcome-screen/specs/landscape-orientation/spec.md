## ADDED Requirements

### Requirement: Landscape only
The app SHALL always be displayed in landscape orientation and MUST NOT rotate to portrait, regardless of device rotation lock settings or physical device orientation.

#### Scenario: Launch with device held in portrait
- **WHEN** the user launches the app while holding the device in portrait
- **THEN** the app is displayed in landscape from the first frame

#### Scenario: Device rotated while running
- **WHEN** the user physically rotates the device to portrait while the app is open
- **THEN** the app stays in landscape

### Requirement: Both landscape directions
The app SHALL support both landscape directions and follow the device sensor between them.

#### Scenario: Flip between landscape sides
- **WHEN** the user rotates the device by 180° while in landscape
- **THEN** the app re-orients to the opposite landscape direction without leaving landscape
