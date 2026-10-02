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

### Requirement: Seen tips are stored
The stored state SHALL hold which tips the player has seen: the intro tour, the mandatory section tip, the cross tip and the reservoir tip. Nothing SHALL be stored as seen on first launch. A tip stored as seen SHALL stay seen after a restart and SHALL be observable like the settings.

#### Scenario: First launch
- **WHEN** the seen tips are read and nothing has been stored yet
- **THEN** no tip is seen

#### Scenario: Seen tip survives a restart
- **WHEN** the cross tip is stored as seen and the app is restarted
- **THEN** reading the seen tips gives the cross tip as seen and the others as not seen

#### Scenario: Marking a tip seen twice
- **WHEN** the reservoir tip is stored as seen and then stored as seen again
- **THEN** the reservoir tip is seen and no other tip changes

### Requirement: Resetting seen tips
Resetting the seen tips SHALL make every tip not seen. It SHALL leave every other setting and the level history unchanged, and clearing the level history SHALL leave the seen tips unchanged.

#### Scenario: Reset tips
- **WHEN** the intro tour and the cross tip are stored as seen and the seen tips are reset
- **THEN** no tip is seen and observers receive that no tip is seen

#### Scenario: Reset tips leaves settings and history alone
- **WHEN** vibration is stored as off, the history holds a record for level 2 and the seen tips are reset
- **THEN** vibration is still off and the history still holds the record for level 2

#### Scenario: Clearing history leaves tips alone
- **WHEN** the intro tour is stored as seen and all level history records are cleared
- **THEN** the intro tour is still seen
