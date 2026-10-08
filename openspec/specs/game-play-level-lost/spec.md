# game-play-level-lost Specification

## Purpose
TBD - created by syncing change app-game-play-level-lost. Update Purpose after archive.

## Requirements
### Requirement: A leak loses the level
When the liquid leaks, the system SHALL mark the current attempt as **lost**. A level SHALL be lost at most once per attempt. When the liquid leaks from a section, the points for that section SHALL be awarded before the level is lost.

#### Scenario: Liquid leaks
- **WHEN** the liquid leaves a section into an empty tile
- **THEN** the level is lost

#### Scenario: Points before the loss
- **WHEN** the liquid leaves a corner section into an empty tile with a score of 40
- **THEN** the score is 55 and the level is lost

### Requirement: The game stops when the level is lost
From the moment the level is lost, the flow SHALL NOT move, the countdown SHALL NOT count down and the score SHALL NOT change. The player SHALL NOT be able to drag a section, place a section or replace a section. A drag that is going on SHALL be cancelled and the section SHALL go back to its slot. The cog wheel and the fast forward button SHALL do nothing. Panning and zooming the field SHALL still work while the leak is shown.

#### Scenario: No placing after a loss
- **WHEN** the level is lost and the player tries to drag the next section from the stack
- **THEN** no drag starts and the board and stack stay the same

#### Scenario: Drag cancelled by the loss
- **WHEN** the player is dragging a section and the level is lost
- **THEN** the drag ends, the section goes back to its slot and the stack stays the same

#### Scenario: Cog wheel after a loss
- **WHEN** the level is lost and the player taps the cog wheel
- **THEN** the pause dialog does not open

### Requirement: The leak is shown on the field
When the level is lost, the playing field SHALL show liquid spurting out of the leaking tile on the side where it leaked, for 1 second. The effect SHALL move with the tile when the field is panned or zoomed.

#### Scenario: Leak effect
- **WHEN** the liquid leaks out of the right side of a tile
- **THEN** liquid spurts out of the right side of that tile for 1 second

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

### Requirement: Restart starts the level again
Tapping **Restart** in the level lost dialog SHALL close the dialog and start the same level again from the beginning: the board SHALL hold no placed sections, a new stack SHALL be drawn, the score SHALL be 0, fast forward SHALL be off, the flow speed SHALL be the level's `flow.cellsPerSecond` and the countdown SHALL start again from the level's `flow.initialDelayMs`. The attempt SHALL no longer be lost and the player SHALL be able to place sections again.

#### Scenario: Restarting after a loss
- **WHEN** the level lost dialog is shown and the player taps Restart
- **THEN** the dialog closes, the board holds no placed sections, the stack is full, the HUD shows the same level and a score of 0, the countdown runs and the player can drag the next section

### Requirement: Exit returns to the main menu
Tapping **Exit** in the level lost dialog SHALL close the dialog, end the level and show the main menu (the welcome screen).

#### Scenario: Exiting after a loss
- **WHEN** the level lost dialog is shown and the player taps Exit
- **THEN** the game screen closes and the welcome screen is shown

### Requirement: Dialog closes only through its buttons or Back
Tapping outside the level lost dialog SHALL NOT close it. Pressing the system Back button while the dialog is shown SHALL work the same as tapping **Exit**. Pressing Back during the leak effect, before the dialog is shown, SHALL do nothing.

#### Scenario: Tapping outside the dialog
- **WHEN** the level lost dialog is shown and the player taps the dimmed game screen
- **THEN** the dialog stays open

#### Scenario: Back on the dialog
- **WHEN** the level lost dialog is shown and the player presses Back
- **THEN** the welcome screen is shown

#### Scenario: Back during the leak effect
- **WHEN** the level was lost less than 1 second ago and the player presses Back
- **THEN** the game screen stays open and the pause dialog does not open

### Requirement: A lost level stays lost in the background
When the app goes to the background after the level is lost, the game SHALL NOT open the pause dialog. When the player comes back, the level lost dialog SHALL be shown.

#### Scenario: Coming back after a loss
- **WHEN** the level is lost, the player leaves the app and comes back
- **THEN** the level lost dialog is shown and the pause dialog is not

### Requirement: Level lost dialog is accessible
Each button in the level lost dialog SHALL have a touch target of at least 48 dp high and SHALL be read by its label. When the dialog opens, a screen reader SHALL announce that the level is lost. The achievement row SHALL be read as "Achievement unlocked" followed by the names of all unlocked achievements.

#### Scenario: Screen reader announces the loss
- **WHEN** the level lost dialog opens while a screen reader is on
- **THEN** the screen reader announces that the level is lost and can move through the score, Restart and Exit

#### Scenario: Screen reader reads the achievements
- **WHEN** the loss unlocked Wet Feet and Instant Flood and a screen reader focuses the achievement row
- **THEN** it reads "Achievement unlocked", "Wet Feet" and "Instant Flood"

### Requirement: Level lost dialog follows the design system
The level lost dialog SHALL look as the design system's leak dialog prescribes: a floating panel in the base surface colour with large rounded corners and no outline over the 72 % scrim, the level number as an eyebrow in the leak colour, a splash that animates once (a still image when animations are turned off), the headline in the large display style, the message in body text, the score, a brass primary **Restart** button and a text-only **Exit** button. The scrim SHALL keep the field and the leak visible behind the dialog.

#### Scenario: Leak dialog styling
- **WHEN** the level lost dialog is shown
- **THEN** the panel has no outline, Restart is the brass primary button and Exit the text button

#### Scenario: Splash with animations off
- **WHEN** the level lost dialog is shown while the system has animations turned off
- **THEN** the splash is shown as a still image

### Requirement: Level lost dialog fits the screen
The level lost dialog SHALL show all its content and buttons without clipping. When the one-column layout fits the available height, the dialog SHALL use it. When it does not fit, the dialog SHALL use a two-column layout with the level, splash, headline, message and score in the left column and the buttons stacked in the right column. If the left column still does not fit, it SHALL scroll while the buttons stay fully visible.

#### Scenario: Short landscape phone
- **WHEN** the level lost dialog is shown on a 640 × 360 dp screen
- **THEN** it uses the two-column layout, with the score on the left and Restart and Exit stacked on the right, all fully visible

#### Scenario: Tall screen
- **WHEN** the level lost dialog is shown on a 1280 × 800 dp screen
- **THEN** it uses the one-column layout with the buttons below the score

#### Scenario: Large font on a short screen
- **WHEN** the level lost dialog is shown on a 640 × 360 dp screen with a 130 % font scale
- **THEN** Restart and Exit are fully visible and the score can be reached

### Requirement: Reaching the finish with an unused mandatory section loses the level
When the liquid leaves a section into the finish tile through the finish's open side while at least one mandatory section of the level is not used, the finish SHALL refuse the liquid and the system SHALL mark the current attempt as **lost** because a mandatory section was missed. The points for the section the liquid just left SHALL be awarded before the level is lost. The game SHALL stop exactly as for a leak.

#### Scenario: Mandatory reservoir skipped
- **WHEN** the liquid enters the finish through its open side and has never run through the level's mandatory reservoir
- **THEN** the level is lost because a mandatory section was missed, and it is not won

#### Scenario: Mandatory cross used on one channel
- **WHEN** the liquid enters the finish through its open side after passing the level's mandatory cross from left to right only
- **THEN** the level is lost because a mandatory section was missed

#### Scenario: Points before the loss
- **WHEN** the liquid leaves a corner section into the finish with a score of 40 and a mandatory section unused
- **THEN** the score is 55 and the level is lost

### Requirement: Missed mandatory sections are shown on the field
When the level is lost because a mandatory section was missed, the playing field SHALL show liquid spurting out of the finish tile on its open side for 1 second, and SHALL make every unused mandatory section pulse in the `leak` colour during that second. The effect SHALL move with the tiles when the field is panned or zoomed.

#### Scenario: Missed section effect
- **WHEN** the level is lost because the mandatory cross at x 3, y 2 was not used
- **THEN** liquid spurts out of the finish's open side and the cross at x 3, y 2 pulses red for 1 second
