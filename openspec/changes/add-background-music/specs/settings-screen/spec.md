## MODIFIED Requirements

### Requirement: Audio panel is complete but disabled
The Audio panel SHALL show a "Sound effects" switch with a volume slider and a "Music" switch with a volume slider. Each control SHALL show the stored value. The Music switch and slider SHALL be enabled and SHALL change the stored `musicEnabled` and `musicVolume` settings. The Sound effects switch and slider SHALL stay disabled, and the Sound effects row SHALL show the note "Coming soon".

#### Scenario: Audio controls show stored values
- **WHEN** the stored settings are sound on at volume 0.8 and music off at volume 0.6
- **THEN** the Sound effects switch is on with its slider at 0.8, and the Music switch is off with its slider at 0.6

#### Scenario: Music switch changes the setting
- **WHEN** music is stored as on and the user taps the Music switch
- **THEN** the switch shows off and music is stored as off

#### Scenario: Music slider changes the setting
- **WHEN** the user drags the Music slider to 0.3 and releases it
- **THEN** the slider shows 0.3 and the music volume is stored as 0.3

#### Scenario: Sound effects controls cannot be changed
- **WHEN** the user taps the Sound effects switch or drags its slider
- **THEN** the control does not change and no setting is written

#### Scenario: Audio controls report their state
- **WHEN** an accessibility service inspects the audio controls
- **THEN** the Music controls are reported as enabled and the Sound effects controls as disabled

#### Scenario: Coming soon note
- **WHEN** the Audio panel is displayed
- **THEN** the text "Coming soon" is visible on the Sound effects row
