## ADDED Requirements

### Requirement: Consent before ads
On app start the app SHALL run Google's User Messaging Platform (UMP) consent flow, showing the consent form where the player's region requires it. The AdMob SDK SHALL be initialised and ads requested only when UMP reports that ads may be requested. When consent is refused or unknown, the game SHALL work exactly as without ads.

#### Scenario: Consent required
- **WHEN** a player in a region that requires consent starts the app for the first time
- **THEN** the consent form is shown and no ad is requested until the player has answered

#### Scenario: Consent refused
- **WHEN** ads may not be requested
- **THEN** no ad is requested and levels open without an ad

#### Scenario: No consent needed
- **WHEN** the player's region needs no consent form
- **THEN** no form is shown and ads are requested normally

### Requirement: Privacy options stay reachable
When UMP reports that a privacy options entry point is required, the Settings screen SHALL show a "Privacy options" row that reopens the consent form so the player can change their choice.

#### Scenario: Row shown when required
- **WHEN** privacy options are required and the player opens Settings
- **THEN** a "Privacy options" row is shown and tapping it opens the UMP privacy options form

#### Scenario: Row hidden when not required
- **WHEN** privacy options are not required
- **THEN** no "Privacy options" row is shown
