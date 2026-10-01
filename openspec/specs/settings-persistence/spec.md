# settings-persistence Specification

## Purpose
TBD - created by archiving change app-settings-page. Update Purpose after archive.
## Requirements
### Requirement: Stored settings and defaults
The app SHALL store these settings on the device, each with a default used when no value has been stored: `soundEnabled` (default on), `soundVolume` (default 0.8), `musicEnabled` (default on), `musicVolume` (default 0.6) and `vibrationEnabled` (default on).

#### Scenario: First launch
- **WHEN** the settings are read and nothing has been stored yet
- **THEN** sound is on at 0.8, music is on at 0.6 and vibration is on

### Requirement: Settings survive restarts
A stored setting SHALL keep its value after the app process is stopped and started again.

#### Scenario: Restart keeps a value
- **WHEN** vibration is stored as off and the app is restarted
- **THEN** reading the settings gives vibration off

### Requirement: Settings are observable
The settings SHALL be readable as a stream that emits the current values and then the new values after every change.

#### Scenario: Change is observed
- **WHEN** a collector observes the settings and vibration is then stored as off
- **THEN** the collector receives settings with vibration off

### Requirement: Volume range
A stored volume SHALL always be in the range 0.0 to 1.0. A value outside that range SHALL be clamped to the nearest bound when written and when read.

#### Scenario: Volume above range
- **WHEN** a sound volume of 1.5 is written
- **THEN** reading the settings gives a sound volume of 1.0

#### Scenario: Volume below range
- **WHEN** a music volume of -0.2 is written
- **THEN** reading the settings gives a music volume of 0.0

### Requirement: Clearing level history
The level history SHALL support deleting every record in one operation, without touching the stored settings.

#### Scenario: Clear all records
- **WHEN** the history holds records for levels 1, 2 and 3 and all records are cleared
- **THEN** the history is empty and observers of the history receive an empty list

#### Scenario: Clearing leaves settings alone
- **WHEN** all level history records are cleared
- **THEN** every stored setting keeps its value

