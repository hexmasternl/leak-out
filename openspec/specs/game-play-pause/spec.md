# game-play-pause Specification

## Purpose
TBD - created by archiving change app-game-play-hud. Update Purpose after archive.
## Requirements
### Requirement: Pause dialog content
The pause dialog SHALL be a modal dialog shown above the game screen. It SHALL show, from top to bottom: a **Resume** button, a **Restart** button, a separator line and an **Exit** button. The game screen behind the dialog SHALL stay visible but dimmed.

#### Scenario: Dialog layout
- **WHEN** the pause dialog is open
- **THEN** it shows Resume, Restart, a separator and Exit, in that order from top to bottom, above the dimmed game screen

### Requirement: Game is frozen while paused
While the game is paused, the system SHALL stop the game clock: the flow SHALL NOT move, the countdown before the flow starts SHALL NOT count down, and the score SHALL NOT change. The player SHALL NOT be able to drag a section or place one on the board. When the game pauses during a drag, the drag SHALL be cancelled, the section SHALL go back to its slot and the stack SHALL stay the same.

#### Scenario: Countdown stops while paused
- **WHEN** the countdown shows 8 seconds, the game is paused for 5 seconds and then resumed
- **THEN** the countdown still shows 8 seconds when the game resumes

#### Scenario: No dragging while paused
- **WHEN** the game is paused
- **THEN** the player cannot start a drag of the next section, and the board and stack do not change

#### Scenario: Drag is cancelled when the game pauses
- **WHEN** the player is dragging a section and the game pauses
- **THEN** the drag ends, the section goes back to the bottom slot and the stack stays the same

### Requirement: Resume continues the game
Tapping **Resume** SHALL close the pause dialog and continue the game in exactly the state it had when it was paused: the same board, stack, score, flow position and countdown.

#### Scenario: Resuming
- **WHEN** the player taps Resume
- **THEN** the dialog closes and the game continues from where it was paused

### Requirement: Restart starts the level again
Tapping **Restart** SHALL close the pause dialog and start the current level again from the beginning: the board SHALL hold no placed sections, a new stack SHALL be drawn, the score SHALL be 0, the flow speed SHALL be the level's `flow.cellsPerSecond` and the countdown SHALL start again from the level's `flow.initialDelayMs`. The game SHALL NOT be paused after a restart.

#### Scenario: Restarting a level
- **WHEN** the player has placed sections and scored points, and taps Restart
- **THEN** the dialog closes, the board holds no placed sections, the stack is full again, the HUD shows the same level and a score of 0, and the game runs

### Requirement: Exit returns to the main menu
Tapping **Exit** SHALL close the pause dialog, end the current level and show the main menu (the welcome screen). The attempt SHALL NOT count as won or lost.

#### Scenario: Exiting a level
- **WHEN** the player taps Exit
- **THEN** the game screen closes and the welcome screen is shown

### Requirement: Dialog closes only through its actions or Back
Tapping outside the pause dialog SHALL NOT close it. Pressing the system Back button while the dialog is open SHALL work the same as tapping **Resume**.

#### Scenario: Tapping outside the dialog
- **WHEN** the pause dialog is open and the player taps the dimmed game screen
- **THEN** the dialog stays open and the game stays paused

#### Scenario: Back closes the dialog
- **WHEN** the pause dialog is open and the player presses Back
- **THEN** the dialog closes and the game continues

### Requirement: Back on the game screen opens the pause dialog
Pressing the system Back button on the game screen while the dialog is closed SHALL pause the game and open the pause dialog. It SHALL NOT leave the game screen.

#### Scenario: Back during play
- **WHEN** the player presses Back during a level
- **THEN** the game pauses and the pause dialog opens, and the game screen stays open

### Requirement: Game pauses when the app goes to the background
When the app is no longer visible during a level (for example the player goes to the home screen or takes a call), the system SHALL pause the game. When the player comes back, the game SHALL still be paused and the pause dialog SHALL be shown.

#### Scenario: Coming back to the app
- **WHEN** the player leaves the app during a level and comes back later
- **THEN** the game is paused, the countdown and flow have not moved, and the pause dialog is shown

### Requirement: Pause dialog is accessible
Each button in the pause dialog SHALL have a touch target of at least 48 dp high and SHALL be read by its label. When the dialog opens, a screen reader SHALL announce it as the pause menu.

#### Scenario: Screen reader announces the dialog
- **WHEN** the pause dialog opens while a screen reader is on
- **THEN** the screen reader announces the pause menu and can move through Resume, Restart and Exit

