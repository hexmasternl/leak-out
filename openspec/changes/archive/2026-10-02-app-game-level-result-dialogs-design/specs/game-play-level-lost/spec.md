## MODIFIED Requirements

### Requirement: Level lost dialog appears after the leak
One second after the level is lost, the system SHALL show the level lost dialog: a modal dialog above the dimmed game screen. The dialog SHALL show the level number, a title that clearly says the level is lost ("Leaked!"), a short message ("The pipe is leaking. Level lost."), the score of the attempt, and two buttons: **Restart** and **Exit**. The dialog SHALL NOT show a Resume button.

#### Scenario: Dialog content
- **WHEN** level 7 is lost with a score of 85 and 1 second has passed
- **THEN** the level lost dialog shows "Level 7", "Leaked!", the message that the level is lost, a score of 85, and the buttons Restart and Exit

#### Scenario: Dialog waits for the leak effect
- **WHEN** the level has been lost for less than 1 second
- **THEN** the level lost dialog is not shown yet

## ADDED Requirements

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
