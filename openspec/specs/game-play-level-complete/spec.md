# game-play-level-complete Specification

## Purpose
TBD - created by syncing change app-game-play-level-complete. Update Purpose after archive.
## Requirements
### Requirement: Reaching the finish wins the level
When the liquid leaves a section and enters the finish tile through the finish's open side, the system SHALL mark the current attempt as **won**. A level SHALL be won at most once per attempt, and a won attempt SHALL NOT become lost. The points for the section the liquid just left SHALL be awarded before the level is won.

#### Scenario: Liquid enters the finish
- **WHEN** the liquid leaves a section into the finish tile through the finish's open side
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
One second after the level is won, the system SHALL show the level complete dialog: a modal dialog above the dimmed game screen. The dialog SHALL show a title that celebrates the win ("Level complete!") with a short cheerful line, three stars, the score rows for the earned points, the unused-section penalty and the total, and three buttons: **Next level**, **Restart** and **Exit**.

#### Scenario: Dialog content
- **WHEN** the level is won and 1 second has passed
- **THEN** the level complete dialog shows "Level complete!", three stars, the rows for earned points, unused pipes and total, and the buttons Next level, Restart and Exit

#### Scenario: Dialog waits for the finish flash
- **WHEN** the level has been won for less than 1 second
- **THEN** the level complete dialog is not shown yet

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
Each button in the level complete dialog SHALL have a touch target of at least 48 dp high and SHALL be read by its label and whether it is enabled. When the dialog opens, a screen reader SHALL announce that the level is complete. Each score row SHALL be read with its label and value, and the stars SHALL be read as the number of stars earned out of 3.

#### Scenario: Screen reader announces the win
- **WHEN** the level complete dialog opens while a screen reader is on
- **THEN** the screen reader announces that the level is complete and can move through the stars, the score rows, Next level, Restart and Exit
