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
The Settings screen SHALL show an Audio panel and a Feel panel in the left column, and a Play Games panel, a Progress panel and an About panel in the right column. Every interactive control SHALL have a touch target of at least 48 dp, and each column SHALL scroll when its content does not fit.

#### Scenario: Panels shown
- **WHEN** the Settings screen is displayed
- **THEN** the Audio, Feel, Play Games, Progress and About panels are visible with their titles

#### Scenario: Content does not fit
- **WHEN** a column's content is taller than the available height (for example at 130 % font scale)
- **THEN** that column scrolls vertically and no control is clipped out of reach

### Requirement: Audio panel is complete but disabled
The Audio panel SHALL show a "Sound effects" switch with a volume slider and a "Music" switch with a volume slider. Each control SHALL show the stored value. The Sound effects switch and slider SHALL be enabled and SHALL change the stored `soundEnabled` and `soundVolume` settings, taking effect for the next sound effect without leaving the screen. When the user releases the Sound effects slider while sound effects are on, a section placed sound SHALL play once at the new volume as a preview. The Music switch and slider SHALL be enabled and SHALL change the stored `musicEnabled` and `musicVolume` settings, taking effect on the playing music immediately; while the Music slider is dragged the music SHALL follow the slider, and the volume SHALL be stored when the slider is released. Neither row SHALL show a "Coming soon" note.

#### Scenario: Audio controls show stored values
- **WHEN** the stored settings are sound on at volume 0.8 and music off at volume 0.6
- **THEN** the Sound effects switch is on with its slider at 0.8, and the Music switch is off with its slider at 0.6

#### Scenario: Sound effects switch changes the setting
- **WHEN** sound effects are stored as on and the user taps the Sound effects switch
- **THEN** the switch shows off and sound effects are stored as off

#### Scenario: Sound effects slider changes the setting
- **WHEN** the user drags the Sound effects slider to 0.3 and releases it
- **THEN** the slider shows 0.3 and the sound volume is stored as 0.3

#### Scenario: Volume preview
- **WHEN** sound effects are on and the user releases the Sound effects slider at 0.5
- **THEN** a section placed sound plays once at volume 0.5

#### Scenario: No preview while off
- **WHEN** sound effects are off and the user releases the Sound effects slider
- **THEN** no sound plays

#### Scenario: Music switch changes the setting
- **WHEN** music is stored as on and the user taps the Music switch
- **THEN** the switch shows off and music is stored as off

#### Scenario: Music slider changes the setting
- **WHEN** the user drags the Music slider to 0.3 and releases it
- **THEN** the slider shows 0.3 and the music volume is stored as 0.3

#### Scenario: Music follows the slider while dragging
- **WHEN** the user drags the Music slider without releasing it
- **THEN** the playing music takes each new volume and nothing is stored until the slider is released

#### Scenario: Audio controls report their state
- **WHEN** an accessibility service inspects the audio controls
- **THEN** the Sound effects and Music switches and sliders are reported as enabled

#### Scenario: No coming soon note
- **WHEN** the Audio panel is displayed
- **THEN** no "Coming soon" text is visible on the Sound effects row or the Music row

### Requirement: Vibration switch
The Feel panel SHALL show a "Vibration" switch that reflects and changes the stored vibration setting. The stored setting turns the game's haptic feedback on and off, as described in `game-haptics`.

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

### Requirement: Show tips again
The Progress panel SHALL show a "Show tips again" button below the progress summary. When no tip is seen the button SHALL be disabled. Tapping it SHALL reset the seen tips, after which the button SHALL be disabled and the panel SHALL show "Tips will show again when you play".

#### Scenario: Reset the tips
- **WHEN** the intro tour and the cross tip are seen and the user taps "Show tips again"
- **THEN** no tip is seen, the button is disabled and "Tips will show again when you play" is shown

#### Scenario: Nothing to reset
- **WHEN** no tip is seen
- **THEN** the "Show tips again" button is disabled

#### Scenario: Tour shown after reset
- **WHEN** the user has tapped "Show tips again" and then starts a level
- **THEN** the intro tour is shown

#### Scenario: Button is accessible
- **WHEN** an accessibility service inspects the Progress panel while a tip is seen
- **THEN** "Show tips again" is reported as an enabled button

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
