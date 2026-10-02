# Spec Delta

## ADDED Requirements

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
