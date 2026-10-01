# settings-screen Specification

## Purpose
TBD - created by archiving change app-settings-page. Update Purpose after archive.
## Requirements
### Requirement: Settings screen opens from the welcome screen
Tapping "Settings" on the welcome screen SHALL open the Settings screen.

#### Scenario: Open settings
- **WHEN** the user taps "Settings" on the welcome screen
- **THEN** the Settings screen is displayed with the title "SETTINGS"

### Requirement: Leaving the settings screen
The Settings screen SHALL return to the welcome screen through its back button and through system Back.

#### Scenario: Back button
- **WHEN** the user taps the back button in the top bar
- **THEN** the welcome screen is displayed

#### Scenario: System Back
- **WHEN** the user presses system Back on the Settings screen and no dialog is open
- **THEN** the welcome screen is displayed

### Requirement: Settings layout
The Settings screen SHALL show an Audio panel and a Feel panel in the left column, and a Progress panel and an About panel in the right column. Every interactive control SHALL have a touch target of at least 48 dp, and each column SHALL scroll when its content does not fit.

#### Scenario: Panels shown
- **WHEN** the Settings screen is displayed
- **THEN** the Audio, Feel, Progress and About panels are visible with their titles

#### Scenario: Content does not fit
- **WHEN** a column's content is taller than the available height (for example at 130 % font scale)
- **THEN** that column scrolls vertically and no control is clipped out of reach

### Requirement: Audio panel is complete but disabled
The Audio panel SHALL show a "Sound effects" switch with a volume slider and a "Music" switch with a volume slider. Each control SHALL show the stored value. All four controls SHALL be disabled, and the panel SHALL show the note "Coming soon".

#### Scenario: Audio controls show stored values
- **WHEN** the stored settings are sound on at volume 0.8 and music off at volume 0.6
- **THEN** the Sound effects switch is on with its slider at 0.8, and the Music switch is off with its slider at 0.6

#### Scenario: Audio controls cannot be changed
- **WHEN** the user taps an audio switch or drags an audio slider
- **THEN** the control does not change and no setting is written

#### Scenario: Audio controls are reported as disabled
- **WHEN** an accessibility service inspects the audio controls
- **THEN** each control is reported as disabled

#### Scenario: Coming soon note
- **WHEN** the Audio panel is displayed
- **THEN** the text "Coming soon" is visible in the panel

### Requirement: Vibration switch
The Feel panel SHALL show a "Vibration" switch that reflects and changes the stored vibration setting. The switch only stores the preference; this change makes nothing vibrate.

#### Scenario: Switch reflects the stored value
- **WHEN** vibration is stored as on
- **THEN** the Vibration switch is shown on

#### Scenario: Turning vibration off
- **WHEN** the user taps the Vibration switch while it is on
- **THEN** the switch shows off and vibration is stored as off

#### Scenario: Value survives leaving the screen
- **WHEN** the user turns vibration off, leaves the Settings screen and opens it again
- **THEN** the Vibration switch is shown off

### Requirement: Progress summary
The Progress panel SHALL summarise the level history as the number of finished levels and the total stars earned. With no finished levels it SHALL show "No levels finished yet".

#### Scenario: Progress exists
- **WHEN** the history holds 12 finished levels with 31 stars in total
- **THEN** the Progress panel shows 12 levels finished and 31 stars

#### Scenario: No progress
- **WHEN** the history is empty
- **THEN** the Progress panel shows "No levels finished yet"

### Requirement: Reset progress
The Progress panel SHALL show a destructive "Reset progress" button. When the history is empty, the button SHALL be disabled. Otherwise, tapping it SHALL open a confirm dialog with the title "Reset progress?", a body stating that all finished levels, highscores and stars are deleted and that this cannot be undone, a "Cancel" button and a destructive "Reset" button.

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

#### Scenario: Nothing to reset
- **WHEN** the history is empty
- **THEN** the "Reset progress" button is disabled and tapping it opens no dialog

#### Scenario: Levels relock after reset
- **WHEN** the user confirms a reset and then opens the level library
- **THEN** the library shows no finished levels and only level 1 is unlocked

### Requirement: About section
The About panel SHALL show the app name "Leak Out", the installed version as "Version <versionName> (<versionCode>)", the maker credit and a privacy line saying that no account is used and the data stays on the device.

#### Scenario: Version shown
- **WHEN** the installed app has version name "0.1.0" and version code 1
- **THEN** the About panel shows "Version 0.1.0 (1)"

#### Scenario: About content
- **WHEN** the About panel is displayed
- **THEN** it shows "Leak Out", the maker credit and the privacy line

