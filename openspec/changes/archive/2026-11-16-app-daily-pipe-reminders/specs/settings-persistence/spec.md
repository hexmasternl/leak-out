# Spec Delta

## ADDED Requirements

### Requirement: Reminders setting is stored
The app SHALL store a boolean `remindersEnabled` setting that defaults to true and survives app restarts.

#### Scenario: Default
- **WHEN** the app starts with no stored value
- **THEN** `remindersEnabled` is true

#### Scenario: Persisted
- **WHEN** the player switches reminders off and restarts the app
- **THEN** `remindersEnabled` is still false
