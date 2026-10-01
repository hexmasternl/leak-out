## MODIFIED Requirements

### Requirement: Audio panel is complete but disabled
The Audio panel SHALL show a "Sound effects" switch with a volume slider and a "Music" switch with a volume slider. Each control SHALL show the stored value. The Sound effects switch and slider SHALL be enabled and SHALL change the stored `soundEnabled` and `soundVolume` settings, taking effect for the next sound effect without leaving the screen. When the user releases the Sound effects slider while sound effects are on, a section placed sound SHALL play once at the new volume as a preview. The Music switch and slider SHALL be enabled when background music is available, and SHALL then change `musicEnabled` and `musicVolume`; while background music is not available they SHALL be disabled and the Music row SHALL show the note "Coming soon". The Sound effects row SHALL NOT show a "Coming soon" note.

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

#### Scenario: Sound effects controls report their state
- **WHEN** an accessibility service inspects the audio controls
- **THEN** the Sound effects switch and slider are reported as enabled

#### Scenario: Music controls without background music
- **WHEN** background music is not available and the Audio panel is displayed
- **THEN** the Music switch and slider are disabled, the text "Coming soon" is visible on the Music row, and no "Coming soon" text is on the Sound effects row
