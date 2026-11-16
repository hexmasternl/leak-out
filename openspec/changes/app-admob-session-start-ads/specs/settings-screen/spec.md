## ADDED Requirements

### Requirement: Privacy options row
The Settings screen SHALL show a "Privacy options" row that opens the ad consent form, only while the UMP consent rules require such an entry point (see `ad-consent`). The row SHALL follow the design system's settings row style and be fully usable with TalkBack.

#### Scenario: Required
- **WHEN** consent rules require a privacy options entry point
- **THEN** the row is visible, has a content description, and opens the consent form when tapped

#### Scenario: Not required
- **WHEN** they do not
- **THEN** the row is not in the Settings list
