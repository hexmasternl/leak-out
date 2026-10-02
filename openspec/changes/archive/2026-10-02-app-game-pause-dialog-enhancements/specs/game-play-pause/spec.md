## MODIFIED Requirements

### Requirement: Pause dialog content
The pause dialog SHALL be a modal dialog shown above the game screen. It SHALL show, from top to bottom: the title **Paused**, a **Resume** button, a **Restart** button, a separator line and an **Exit** button. The game screen behind the dialog SHALL NOT be visible (see "Game screen is hidden while paused").

#### Scenario: Dialog layout
- **WHEN** the pause dialog is open
- **THEN** it shows the title Paused, then Resume, Restart, a separator and Exit, in that order from top to bottom, on the opaque pause backdrop

### Requirement: Dialog closes only through its actions or Back
Tapping outside the pause dialog's panel SHALL NOT close it. Pressing the system Back button while the dialog is open SHALL work the same as tapping **Resume**.

#### Scenario: Tapping outside the dialog
- **WHEN** the pause dialog is open and the player taps the backdrop outside the panel
- **THEN** the dialog stays open and the game stays paused

#### Scenario: Back closes the dialog
- **WHEN** the pause dialog is open and the player presses Back
- **THEN** the dialog closes and the game continues

## ADDED Requirements

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
