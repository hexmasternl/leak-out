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
One second after the level is lost, the system SHALL show the level lost dialog: a modal dialog above the dimmed game screen. The dialog SHALL show a title that clearly says the level is lost ("Leaked!"), a short message ("The pipe is leaking. Level lost."), the score of the attempt, and two buttons: **Restart** and **Exit**. The dialog SHALL NOT show a Resume button.

#### Scenario: Dialog content
- **WHEN** the level is lost with a score of 85 and 1 second has passed
- **THEN** the level lost dialog shows "Leaked!", the message that the level is lost, a score of 85, and the buttons Restart and Exit

#### Scenario: Dialog waits for the leak effect
- **WHEN** the level has been lost for less than 1 second
- **THEN** the level lost dialog is not shown yet

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
Each button in the level lost dialog SHALL have a touch target of at least 48 dp high and SHALL be read by its label. When the dialog opens, a screen reader SHALL announce that the level is lost.

#### Scenario: Screen reader announces the loss
- **WHEN** the level lost dialog opens while a screen reader is on
- **THEN** the screen reader announces that the level is lost and can move through the score, Restart and Exit
