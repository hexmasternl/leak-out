## MODIFIED Requirements

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
