# Spec Delta

## MODIFIED Requirements

### Requirement: Level lost dialog appears after the leak
One second after the level is lost, the system SHALL show the level lost dialog: a modal dialog above the dimmed game screen. The dialog SHALL show the level number, a title that clearly says the level is lost ("Leaked!"), a short message, the score of the attempt, and two buttons: **Restart** and **Exit**. When the top 10 is available for the level (see `level-top-scores`), the dialog SHALL also show a **Top 10** button. The message SHALL be "The pipe is leaking. Level lost." after a leak, and "The liquid missed a mandatory section. Level lost." when a mandatory section was missed. The dialog SHALL NOT show a Resume button. When the loss unlocked achievements, the dialog SHALL also show an "Achievement unlocked" row below the score with the names of the unlocked achievements: at most two names, followed by "+N more" when there are more.

#### Scenario: Dialog content
- **WHEN** level 7 is lost by a leak with a score of 85 and 1 second has passed
- **THEN** the level lost dialog shows "Level 7", "Leaked!", the message "The pipe is leaking. Level lost.", a score of 85, and the buttons Restart and Exit

#### Scenario: Top 10 button
- **WHEN** a level from the level library is lost and the score server is configured
- **THEN** the level lost dialog also shows a Top 10 button

#### Scenario: No Top 10 button for a test play
- **WHEN** a test play from the level designer is lost
- **THEN** the level lost dialog shows no Top 10 button

#### Scenario: Dialog after a missed mandatory section
- **WHEN** the level is lost because a mandatory section was missed and 1 second has passed
- **THEN** the level lost dialog shows "Leaked!" and the message "The liquid missed a mandatory section. Level lost."

#### Scenario: Dialog waits for the leak effect
- **WHEN** the level has been lost for less than 1 second
- **THEN** the level lost dialog is not shown yet

#### Scenario: First loss unlocks Wet Feet
- **WHEN** the player loses for the first time and the dialog is shown
- **THEN** the dialog shows "Achievement unlocked" with "Wet Feet"
