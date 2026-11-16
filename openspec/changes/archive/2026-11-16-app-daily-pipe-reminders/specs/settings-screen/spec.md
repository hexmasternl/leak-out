# Spec Delta

## ADDED Requirements

### Requirement: Reminders panel
The Settings screen SHALL show a Reminders panel with a "Daily reminders" switch that shows and changes the stored `remindersEnabled` setting. Switching it off SHALL cancel all pending reminders; switching it on SHALL schedule the reminder window again. The control SHALL have a touch target of at least 48 dp.

#### Scenario: Switch off
- **WHEN** the player switches "Daily reminders" off
- **THEN** the stored setting is false and no reminder is pending

#### Scenario: Switch on
- **WHEN** the player switches "Daily reminders" on and the permission is granted
- **THEN** the stored setting is true and the reminder window is scheduled
