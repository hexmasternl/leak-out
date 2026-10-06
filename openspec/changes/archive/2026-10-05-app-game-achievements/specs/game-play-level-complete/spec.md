## MODIFIED Requirements

### Requirement: Level complete dialog appears after the finish flash
One second after the level is won, the system SHALL show the level complete dialog: a modal dialog above the dimmed game screen. The dialog SHALL show the level number, a title that celebrates the win ("Level Complete!") with a short cheerful line, three stars, the score rows **Points** (the points earned during play), **Penalty** (the unused-section penalty) and **Score** (the total), and three buttons: **Next Level**, **Restart** and **Exit**. When the win unlocked achievements, the dialog SHALL also show an "Achievement unlocked" row below the score rows with the names of the unlocked achievements: at most two names, followed by "+N more" when there are more. The row SHALL appear once the score tally is done and SHALL NOT delay enabling the buttons.

#### Scenario: Dialog content
- **WHEN** level 7 is won and 1 second has passed
- **THEN** the level complete dialog shows "Level 7", "Level Complete!", three stars, the rows Points, Penalty and Score, and the buttons Next Level, Restart and Exit

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

### Requirement: Level complete dialog is accessible
Each button in the level complete dialog SHALL have a touch target of at least 48 dp high and SHALL be read by its label and whether it is enabled. When the dialog opens, a screen reader SHALL announce that the level is complete. Each score row SHALL be read as one item with its label and value ("Points 120", "Penalty minus 12", "Score 108"), and the stars SHALL be read as the number of stars earned out of 3. The achievement row SHALL be read as "Achievement unlocked" followed by the names of all unlocked achievements.

#### Scenario: Screen reader announces the win
- **WHEN** the level complete dialog opens while a screen reader is on
- **THEN** the screen reader announces that the level is complete and can move through the stars, the score rows, Next Level, Restart and Exit

#### Scenario: Score rows are read with the new labels
- **WHEN** the tally is done with 120 earned points and a penalty of 12, and a screen reader moves through the score rows
- **THEN** it reads "Points 120", "Penalty minus 12" and "Score 108"

#### Scenario: Screen reader reads the achievements
- **WHEN** the win unlocked three achievements and a screen reader focuses the achievement row
- **THEN** it reads "Achievement unlocked" and all three names
