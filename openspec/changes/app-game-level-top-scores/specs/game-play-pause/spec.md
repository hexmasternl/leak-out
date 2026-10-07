# Spec Delta

## MODIFIED Requirements

### Requirement: Pause dialog content
The pause dialog SHALL be a modal dialog shown above the game screen. It SHALL show, from top to bottom: the title **Paused**, a **Resume** button, a **Restart** button, a **Top 10** button, a separator line and an **Exit** button. The **Top 10** button SHALL only be shown when the top 10 is available for the level (see `level-top-scores`). The game screen behind the dialog SHALL NOT be visible (see "Game screen is hidden while paused").

#### Scenario: Dialog layout
- **WHEN** the pause dialog is open for a level from the level library and the score server is configured
- **THEN** it shows the title Paused, then Resume, Restart, Top 10, a separator and Exit, in that order from top to bottom, on the opaque pause backdrop

#### Scenario: Dialog layout without the top 10
- **WHEN** the pause dialog is open during a test play from the level designer
- **THEN** it shows the title Paused, then Resume, Restart, a separator and Exit, and no Top 10 button
