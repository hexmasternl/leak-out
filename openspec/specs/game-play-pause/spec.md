# game-play-pause Specification

## Purpose
TBD - created by archiving change app-game-play-hud. Update Purpose after archive.
## Requirements
### Requirement: Pause dialog content
The pause dialog SHALL be a modal dialog shown above the game screen. It SHALL show, from top to bottom: the title **Paused**, a **Resume** button, a **Restart** button, a separator line and an **Exit** button. The game screen behind the dialog SHALL NOT be visible (see "Game screen is hidden while paused").

#### Scenario: Dialog layout
- **WHEN** the pause dialog is open
- **THEN** it shows the title Paused, then Resume, Restart, a separator and Exit, in that order from top to bottom, on the opaque pause backdrop

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
Tapping outside the pause dialog's panel SHALL NOT close it. Pressing the system Back button while the dialog is open SHALL work the same as tapping **Resume**.

#### Scenario: Tapping outside the dialog
- **WHEN** the pause dialog is open and the player taps the backdrop outside the panel
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

### Requirement: Game screen is hidden while paused
While the pause dialog is open, an opaque backdrop SHALL cover the whole screen, including the HUD, the playing field, the stack and the areas behind the system bars and any display cutout, so that no part of the game can be seen. The backdrop SHALL be a diagonal linear gradient from the `surface.abyss` colour in the top-left corner to the `surface.deep` colour in the bottom-right corner, as defined in the design system. The backdrop SHALL fade in over 160 ms when the dialog opens, or appear at once when the system's animations are turned off. When the dialog closes the backdrop SHALL disappear at once together with it.

#### Scenario: Backdrop covers the game
- **WHEN** the player pauses a level
- **THEN** the backdrop covers the full screen and no cell, section, stack slot or HUD element of the game can be seen through it

#### Scenario: Backdrop gradient
- **WHEN** the pause dialog is open
- **THEN** the backdrop's top-left corner has the `surface.abyss` colour (`#07111C`), its bottom-right corner the `surface.deep` colour (`#0C1A2A`), and the colour changes gradually along the diagonal between them

#### Scenario: Hidden after coming back from the background
- **WHEN** the player leaves the app during a level and comes back
- **THEN** the pause dialog is shown on the opaque backdrop and the game cannot be seen

#### Scenario: Game shown again on Resume
- **WHEN** the player taps Resume
- **THEN** the backdrop is gone at once and the game is visible and running

### Requirement: Pause dialog follows the design system
The pause dialog's panel SHALL follow the design system's dialog style: a floating `surface.base` panel with 24 dp rounded corners, 24 dp padding, a maximum width of 360 dp, a deep shadow and no outline, centred on the backdrop. The title SHALL read "Paused" in Title Case in the `display.lg` style and the `text.primary` colour. **Resume** SHALL be the brass primary button, **Restart** the glass secondary button and **Exit** the destructive button, each with the design system's bottom lip and press feedback; the separator between Restart and Exit SHALL be a `line.subtle` line. The whole panel SHALL fit on a 360 dp high landscape screen without scrolling. When the dialog opens the panel SHALL scale from 0.92 to 1 with the design system's pop motion, or fade in over 160 ms without scaling when the system's animations are turned off.

#### Scenario: Panel style
- **WHEN** the pause dialog is open
- **THEN** the panel is a dark `surface.base` panel with rounded corners and no outline, and the title reads "Paused" (not "PAUSED")

#### Scenario: Button styles
- **WHEN** the pause dialog is open
- **THEN** Resume is drawn in the brass primary style, Restart in the glass secondary style and Exit in the destructive style

#### Scenario: Fits a small landscape screen
- **WHEN** the pause dialog is open on a 640 × 360 dp landscape screen
- **THEN** the title and all three buttons are fully visible without scrolling

#### Scenario: Reduced motion
- **WHEN** the system's animations are turned off and the player pauses
- **THEN** the backdrop is shown at once and the panel appears without scaling
