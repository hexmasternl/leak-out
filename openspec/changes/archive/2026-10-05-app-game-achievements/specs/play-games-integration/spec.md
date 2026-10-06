## ADDED Requirements

### Requirement: Silent sign-in at start
When the app starts and Play Games is configured, the system SHALL initialise the Play Games SDK and check whether the player is signed in without showing any UI. The sign-in state SHALL be one of: unavailable, signed out, signing in, or signed in with the player's display name.

#### Scenario: Already signed in
- **WHEN** the app starts on a device where the player is signed in to Play Games
- **THEN** the sign-in state becomes signed in with the player's display name and no sign-in UI is shown

#### Scenario: Not signed in
- **WHEN** the app starts and the silent sign-in fails
- **THEN** the sign-in state becomes signed out and no sign-in UI is shown

### Requirement: Manual sign-in
The system SHALL let the player sign in to Play Games from the settings screen when the state is signed out. While signing in, the state SHALL be signing in. When the sign-in fails or is cancelled, the state SHALL return to signed out and the app SHALL keep working.

#### Scenario: Sign in succeeds
- **WHEN** the state is signed out and the player taps Sign in and completes the Play Games sign-in
- **THEN** the state becomes signed in with the player's display name

#### Scenario: Sign in cancelled
- **WHEN** the player cancels the Play Games sign-in
- **THEN** the state is signed out and the settings screen is shown

### Requirement: Achievements are synced to Play Games
While signed in, the system SHALL push every locally unlocked standard achievement with an unlock call, and the progress of every incremental achievement with a set-steps call, never with an increment call. It SHALL push all achievements when the state becomes signed in and again whenever the local achievement data changes. Failed pushes SHALL NOT change local data and SHALL be retried by the next push.

#### Scenario: Sync on sign-in
- **WHEN** Zero Waste is unlocked locally, Apprentice has progress 40, and the player signs in
- **THEN** Play Games receives an unlock for Zero Waste and a set-steps of 40 for Apprentice

#### Scenario: Sync on new unlock
- **WHEN** the player is signed in and Wet Feet is unlocked
- **THEN** Play Games receives an unlock for Wet Feet

#### Scenario: Repeated sync is harmless
- **WHEN** the same local state is pushed twice
- **THEN** Play Games receives the same unlock and set-steps calls twice and the progress shown in Play Games does not change

#### Scenario: Offline push
- **WHEN** a push fails because the device is offline
- **THEN** local achievements are unchanged and the next push sends them again

### Requirement: Every achievement has a Play Games id
Every achievement in the catalogue SHALL have a Play Games id resource named `achievement_<key>`.

#### Scenario: Ids for the catalogue
- **WHEN** the catalogue is checked against the app resources
- **THEN** every catalogue key has a matching `achievement_<key>` string resource

### Requirement: Works without Play Games
When the Play Games app id is not configured (not a positive number), the system SHALL NOT initialise the Play Games SDK, the sign-in state SHALL be unavailable, and achievements SHALL still be decided, stored and shown locally.

#### Scenario: Placeholder configuration
- **WHEN** the app runs with the placeholder Play Games app id and a level is won with no unused sections
- **THEN** the sign-in state is unavailable, no Play Games call is made, and Zero Waste is unlocked locally
