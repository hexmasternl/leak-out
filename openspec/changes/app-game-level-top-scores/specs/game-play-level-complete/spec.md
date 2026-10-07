# Spec Delta

## MODIFIED Requirements

### Requirement: Level complete dialog appears after the finish flash
One second after the level is won, the system SHALL show the level complete dialog: a modal dialog above the dimmed game screen. The dialog SHALL show the level number, a title that celebrates the win ("Level Complete!") with a short cheerful line, three stars, the score rows **Points** (the points earned during play), **Penalty** (the unused-section penalty) and **Score** (the total), and three buttons: **Next Level**, **Restart** and **Exit**. When the top 10 is available for the level (see `level-top-scores`), the dialog SHALL also show a **Top 10** button, which is enabled together with the other buttons. When the win unlocked achievements, the dialog SHALL also show an "Achievement unlocked" row below the score rows with the names of the unlocked achievements: at most two names, followed by "+N more" when there are more. The row SHALL appear once the score tally is done and SHALL NOT delay enabling the buttons.

#### Scenario: Dialog content
- **WHEN** level 7 is won and 1 second has passed
- **THEN** the level complete dialog shows "Level 7", "Level Complete!", three stars, the rows Points, Penalty and Score, and the buttons Next Level, Restart and Exit

#### Scenario: Top 10 button
- **WHEN** a level from the level library is won, the score server is configured and the dialog's buttons are enabled
- **THEN** the level complete dialog also shows an enabled Top 10 button

#### Scenario: No Top 10 button for a test play
- **WHEN** a test play from the level designer is won
- **THEN** the level complete dialog shows no Top 10 button

#### Scenario: Row values after the tally
- **WHEN** the level complete dialog has finished its tally with 120 earned points and 4 unused sections
- **THEN** Points shows 120, Penalty shows −12 and Score shows 108

#### Scenario: Dialog waits for the finish flash
- **WHEN** the level has been won for less than 1 second
- **THEN** the level complete dialog is not shown yet

#### Scenario: Achievement unlocked by the win
- **WHEN** the win unlocks Zero Waste and the score tally is done
- **THEN** the dialog shows "Achievement unlocked" with "Zero Waste"

#### Scenario: Many achievements
- **WHEN** the win unlocks Zero Waste, Measure Twice, Steady Hands and Personal Best
- **THEN** the row shows "Zero Waste · Measure Twice +2 more"

#### Scenario: No achievement
- **WHEN** the win unlocks no achievement
- **THEN** the dialog shows no achievement row
