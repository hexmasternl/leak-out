## MODIFIED Requirements

### Requirement: Settings layout
The Settings screen SHALL show an Audio panel and a Feel panel in the left column, and a Play Games panel, a Progress panel and an About panel in the right column. Every interactive control SHALL have a touch target of at least 48 dp, and each column SHALL scroll when its content does not fit.

#### Scenario: Panels shown
- **WHEN** the Settings screen is displayed
- **THEN** the Audio, Feel, Play Games, Progress and About panels are visible with their titles

#### Scenario: Content does not fit
- **WHEN** a column's content is taller than the available height (for example at 130 % font scale)
- **THEN** that column scrolls vertically and no control is clipped out of reach

### Requirement: Reset progress
The Progress panel SHALL show a destructive "Reset progress" button. When the history is empty, the button SHALL be disabled. Otherwise, tapping it SHALL open a confirm dialog with the title "Reset progress?", a body stating that all finished levels, highscores and stars are deleted, that achievements are kept, and that this cannot be undone, a "Cancel" button and a destructive "Reset" button.

#### Scenario: Open the confirm dialog
- **WHEN** the history is not empty and the user taps "Reset progress"
- **THEN** the confirm dialog is displayed and the history is unchanged

#### Scenario: Confirm reset
- **WHEN** the user taps "Reset" in the confirm dialog
- **THEN** every level history record is deleted, the dialog closes and the Progress panel shows "No levels finished yet"

#### Scenario: Cancel reset
- **WHEN** the user taps "Cancel", taps outside the dialog or presses system Back while the confirm dialog is open
- **THEN** the dialog closes, the history is unchanged and the Settings screen stays open

#### Scenario: Settings survive a reset
- **WHEN** vibration is stored as off and the user confirms a reset
- **THEN** vibration is still stored as off and every audio setting keeps its value

#### Scenario: Achievements survive a reset
- **WHEN** 9 achievements are unlocked and the user confirms a reset
- **THEN** 9 achievements are still unlocked

#### Scenario: Nothing to reset
- **WHEN** the history is empty
- **THEN** the "Reset progress" button is disabled and tapping it opens no dialog

#### Scenario: Levels relock after reset
- **WHEN** the user confirms a reset and then opens the level library
- **THEN** the library shows no finished levels and only level 1 is unlocked

### Requirement: About section
The About panel SHALL show the app name "Leak Out", the installed version as "Version <versionName> (<versionCode>)", the maker credit and a privacy line saying that no account is needed and that achievements are shared with Google Play only when the player signs in to Google Play Games. Below these it SHALL show a "Music" section that lists every background track as its title and artist, in playlist order, read from the bundled `audio_credits.json`. If the credits cannot be read, the Music section SHALL be left out.

#### Scenario: Version shown
- **WHEN** the installed app has version name "0.1.0" and version code 1
- **THEN** the About panel shows "Version 0.1.0 (1)"

#### Scenario: About content
- **WHEN** the About panel is displayed
- **THEN** it shows "Leak Out", the maker credit and the privacy line

#### Scenario: Music credits listed
- **WHEN** the About panel is displayed with the bundled credits
- **THEN** a "Music" section shows ten entries in playlist order, the first being "Ebb and Flow" by Fabian Measures and the last "Endless Garden" by Unheard Music Concepts

#### Scenario: Credits unavailable
- **WHEN** the credits file cannot be read
- **THEN** the About panel shows no Music section and the rest of the panel is unchanged

## ADDED Requirements

### Requirement: Play Games panel
The Play Games panel SHALL show the Play Games sign-in state. When signed in, it SHALL show "Signed in as <display name>". When signed out, it SHALL show "Not signed in" and a "Sign in" button that starts the Play Games sign-in. While signing in, the Sign in button SHALL be disabled. When Play Games is unavailable, it SHALL show "Play Games is not available" and no Sign in button.

#### Scenario: Signed out
- **WHEN** the sign-in state is signed out
- **THEN** the panel shows "Not signed in" and an enabled "Sign in" button

#### Scenario: Tap sign in
- **WHEN** the user taps "Sign in"
- **THEN** the Play Games sign-in starts and the button is disabled until it finishes

#### Scenario: Signed in
- **WHEN** the sign-in state is signed in as "Eduard"
- **THEN** the panel shows "Signed in as Eduard" and no Sign in button

#### Scenario: Unavailable
- **WHEN** Play Games is not configured
- **THEN** the panel shows "Play Games is not available" and no Sign in button

### Requirement: Achievements button
The Play Games panel SHALL always show an "Achievements" button with the number of unlocked achievements out of the number in the catalogue, whatever the sign-in state. Tapping it SHALL open the achievements screen.

#### Scenario: Button shows the count
- **WHEN** the catalogue holds 25 achievements and 12 are unlocked
- **THEN** the Achievements button shows "12 / 25"

#### Scenario: Open while signed out
- **WHEN** the player is not signed in and taps Achievements
- **THEN** the achievements screen opens
