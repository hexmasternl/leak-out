# Spec Delta

## ADDED Requirements

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
