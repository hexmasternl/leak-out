## MODIFIED Requirements

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

### Requirement: About section
The About panel SHALL show the app name "Leak Out", the installed version as "Version <versionName> (<versionCode>)", the maker credit and a privacy line saying that no account is used and the data stays on the device. Below these it SHALL show a "Music" section that lists every background track as its title and artist, in playlist order, read from the bundled `audio_credits.json`. If the credits cannot be read, the Music section SHALL be left out.

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
