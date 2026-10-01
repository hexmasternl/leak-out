## MODIFIED Requirements

### Requirement: Level lost dialog appears after the leak
One second after the level is lost, the system SHALL show the level lost dialog: a modal dialog above the dimmed game screen. The dialog SHALL show a title that clearly says the level is lost ("Leaked!"), a short message ("The pipe is leaking. Level lost."), the score of the attempt, and two buttons: **Restart** and **Exit**. The dialog SHALL NOT show a Resume button. When the loss unlocked achievements, the dialog SHALL also show an "Achievement unlocked" row below the score with the names of the unlocked achievements: at most two names, followed by "+N more" when there are more.

#### Scenario: Dialog content
- **WHEN** the level is lost with a score of 85 and 1 second has passed
- **THEN** the level lost dialog shows "Leaked!", the message that the level is lost, a score of 85, and the buttons Restart and Exit

#### Scenario: Dialog waits for the leak effect
- **WHEN** the level has been lost for less than 1 second
- **THEN** the level lost dialog is not shown yet

#### Scenario: First loss unlocks Wet Feet
- **WHEN** the player loses for the first time and the dialog is shown
- **THEN** the dialog shows "Achievement unlocked" with "Wet Feet"

### Requirement: Level lost dialog is accessible
Each button in the level lost dialog SHALL have a touch target of at least 48 dp high and SHALL be read by its label. When the dialog opens, a screen reader SHALL announce that the level is lost. The achievement row SHALL be read as "Achievement unlocked" followed by the names of all unlocked achievements.

#### Scenario: Screen reader announces the loss
- **WHEN** the level lost dialog opens while a screen reader is on
- **THEN** the screen reader announces that the level is lost and can move through the score, Restart and Exit

#### Scenario: Screen reader reads the achievements
- **WHEN** the loss unlocked Wet Feet and Instant Flood and a screen reader focuses the achievement row
- **THEN** it reads "Achievement unlocked", "Wet Feet" and "Instant Flood"
