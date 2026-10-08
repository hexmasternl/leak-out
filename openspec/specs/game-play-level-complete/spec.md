# game-play-level-complete Specification

## Purpose
TBD - created by syncing change app-game-play-level-complete. Update Purpose after archive.

## Requirements
### Requirement: Reaching the finish wins the level
When the liquid leaves a section and enters the finish tile through the finish's open side, and every mandatory section of the level is used, the system SHALL mark the current attempt as **won**. A level SHALL be won at most once per attempt, and a won attempt SHALL NOT become lost. The points for the section the liquid just left SHALL be awarded before the level is won.

#### Scenario: Liquid enters the finish
- **WHEN** the liquid leaves a section into the finish tile through the finish's open side, in a level without mandatory sections
- **THEN** the level is won

#### Scenario: Liquid enters the finish after every mandatory section
- **WHEN** the liquid has used every mandatory section and then leaves a section into the finish tile through the finish's open side
- **THEN** the level is won

#### Scenario: Points before the win
- **WHEN** the liquid leaves a corner section into the finish with a score of 40
- **THEN** the score is 55 and the level is won

#### Scenario: Won at most once
- **WHEN** the level is already won and the finish is reported as reached again
- **THEN** nothing changes

### Requirement: The game stops when the level is won
From the moment the level is won, the flow SHALL NOT move, the countdown SHALL NOT count down and no points SHALL be awarded. The player SHALL NOT be able to drag a section, place a section or replace a section. A drag that is going on SHALL be cancelled and the section SHALL go back to its slot. The cog wheel and the fast forward button SHALL do nothing and the pause dialog SHALL NOT open. Panning and zooming the field SHALL still work.

#### Scenario: No placing after a win
- **WHEN** the level is won and the player tries to drag the next section from the stack
- **THEN** no drag starts and the stack stays the same

#### Scenario: Drag cancelled by the win
- **WHEN** the player is dragging a section and the level is won
- **THEN** the drag ends, the section goes back to its slot and the stack stays the same

#### Scenario: Cog wheel after a win
- **WHEN** the level is won and the player taps the cog wheel
- **THEN** the pause dialog does not open

#### Scenario: No points after a win
- **WHEN** the level is won and the liquid is reported as leaving another section
- **THEN** the score does not change

### Requirement: The finish flashes on the win
When the level is won, the playing field SHALL show the finish tile flashing in the success colour for 1 second, and SHALL keep the finish marked afterwards. The effect SHALL move with the tile when the field is panned or zoomed.

#### Scenario: Finish flash
- **WHEN** the liquid enters the finish
- **THEN** the finish tile flashes in the success colour for 1 second

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

### Requirement: Buttons are disabled until the animations are done
The three buttons SHALL be visible from the moment the dialog opens and SHALL be disabled while the score and star animations run. When the score and star animations are done, **Restart** and **Exit** SHALL be enabled, and **Next level** SHALL be enabled when a next level exists.

#### Scenario: Buttons during the animation
- **WHEN** the level complete dialog is shown and the total is still counting up
- **THEN** Next level, Restart and Exit are shown and disabled

#### Scenario: Buttons after the animation
- **WHEN** the score and star animations are done and a next level exists
- **THEN** Next level, Restart and Exit are enabled

#### Scenario: No next level
- **WHEN** the score and star animations are done and the level is the last level
- **THEN** Restart and Exit are enabled and Next level stays disabled

### Requirement: Next level starts the next level
Tapping **Next level** SHALL close the dialog and start the level with the next id from the beginning: the board SHALL hold no placed sections, a new stack SHALL be drawn, the score SHALL be 0, fast forward SHALL be off and the countdown SHALL start from that level's `flow.initialDelayMs`. The HUD SHALL show the new level.

#### Scenario: Going to the next level
- **WHEN** the level complete dialog of level 3 is shown with its buttons enabled and the player taps Next level
- **THEN** the dialog closes, the HUD shows level 4 with a score of 0, the board holds no placed sections and the player can drag the next section

### Requirement: Restart starts the level again
Tapping **Restart** in the level complete dialog SHALL close the dialog and start the same level again from the beginning: the board SHALL hold no placed sections, a new stack SHALL be drawn, the score SHALL be 0, fast forward SHALL be off and the countdown SHALL start again from the level's `flow.initialDelayMs`. The attempt SHALL no longer be won and the player SHALL be able to place sections again.

#### Scenario: Restarting after a win
- **WHEN** the level complete dialog is shown with its buttons enabled and the player taps Restart
- **THEN** the dialog closes, the board holds no placed sections, the stack is full, the HUD shows the same level and a score of 0, the countdown runs and the player can drag the next section

### Requirement: Exit returns to the main menu
Tapping **Exit** in the level complete dialog SHALL close the dialog, end the level and show the main menu (the welcome screen).

#### Scenario: Exiting after a win
- **WHEN** the level complete dialog is shown with its buttons enabled and the player taps Exit
- **THEN** the game screen closes and the welcome screen is shown

### Requirement: Dialog closes only through its buttons or Back
Tapping outside the level complete dialog SHALL NOT close it. Pressing the system Back button during the finish flash or while the animations run SHALL do nothing. Pressing Back once the buttons are enabled SHALL work the same as tapping **Exit**.

#### Scenario: Tapping outside the dialog
- **WHEN** the level complete dialog is shown and the player taps the dimmed game screen
- **THEN** the dialog stays open

#### Scenario: Back during the animation
- **WHEN** the level complete dialog is shown, its buttons are disabled and the player presses Back
- **THEN** the dialog stays open and the pause dialog does not open

#### Scenario: Back after the animation
- **WHEN** the level complete dialog is shown with its buttons enabled and the player presses Back
- **THEN** the welcome screen is shown

#### Scenario: Back during the finish flash
- **WHEN** the level was won less than 1 second ago and the player presses Back
- **THEN** the game screen stays open and the pause dialog does not open

### Requirement: A won level stays won in the background
When the app goes to the background after the level is won, the game SHALL NOT open the pause dialog and SHALL skip the rest of the finish flash and the animations. When the player comes back, the level complete dialog SHALL be shown with the final score, all earned stars lit, all unused sections removed and the buttons enabled.

#### Scenario: Coming back after a win
- **WHEN** the level is won, the player leaves the app during the animations and comes back
- **THEN** the level complete dialog is shown with the final total, the earned stars lit and the buttons enabled, and the pause dialog is not

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

### Requirement: Level complete dialog follows the design system
The level complete dialog SHALL look as the design system's win dialog prescribes: a floating panel in the base surface colour with large rounded corners and no outline over the 72 % scrim, the level number as an eyebrow in the success colour, the headline in the large display style, earned stars in brass, the Penalty value in the leak colour and the Score value in brass, a success-coloured **Next Level** button, a glass **Restart** button and a text-only **Exit** button. The scrim SHALL keep the field visible behind the dialog.

#### Scenario: Win dialog styling
- **WHEN** the level complete dialog is shown
- **THEN** the panel has no outline, Next Level is the success button, Restart the glass button and Exit the text button

#### Scenario: Field visible during the tally
- **WHEN** the penalty phase removes an unused section
- **THEN** the removal can be seen on the field behind the dialog

### Requirement: Level complete dialog fits the screen
The level complete dialog SHALL show all its content and buttons without clipping. When the one-column layout fits the available height, the dialog SHALL use it. When it does not fit, the dialog SHALL use a two-column layout with the level, headline, stars and score rows in the left column and the buttons stacked in the right column. If the left column still does not fit, it SHALL scroll while the buttons stay fully visible.

#### Scenario: Short landscape phone
- **WHEN** the level complete dialog is shown on a 640 × 360 dp screen
- **THEN** it uses the two-column layout, with the score rows on the left and Next Level, Restart and Exit stacked on the right, all fully visible

#### Scenario: Tall screen
- **WHEN** the level complete dialog is shown on a 1280 × 800 dp screen
- **THEN** it uses the one-column layout with the buttons below the score rows

#### Scenario: Large font on a short screen
- **WHEN** the level complete dialog is shown on a 640 × 360 dp screen with a 130 % font scale
- **THEN** Next Level, Restart and Exit are fully visible and every score row can be reached

#### Scenario: Layout stays put during the tally
- **WHEN** the score counts up in the level complete dialog
- **THEN** the dialog keeps the layout it opened with
