## MODIFIED Requirements

### Requirement: Level complete dialog appears after the finish flash
One second after the level is won, the system SHALL show the level complete dialog: a modal dialog above the dimmed game screen. The dialog SHALL show the level number, a title that celebrates the win ("Level Complete!") with a short cheerful line, three stars, the score rows **Points** (the points earned during play), **Penalty** (the unused-section penalty) and **Score** (the total), and three buttons: **Next Level**, **Restart** and **Exit**.

#### Scenario: Dialog content
- **WHEN** level 7 is won and 1 second has passed
- **THEN** the level complete dialog shows "Level 7", "Level Complete!", three stars, the rows Points, Penalty and Score, and the buttons Next Level, Restart and Exit

#### Scenario: Row values after the tally
- **WHEN** the level complete dialog has finished its tally with 120 earned points and 4 unused sections
- **THEN** Points shows 120, Penalty shows −12 and Score shows 108

#### Scenario: Dialog waits for the finish flash
- **WHEN** the level has been won for less than 1 second
- **THEN** the level complete dialog is not shown yet

### Requirement: Level complete dialog is accessible
Each button in the level complete dialog SHALL have a touch target of at least 48 dp high and SHALL be read by its label and whether it is enabled. When the dialog opens, a screen reader SHALL announce that the level is complete. Each score row SHALL be read as one item with its label and value ("Points 120", "Penalty minus 12", "Score 108"), and the stars SHALL be read as the number of stars earned out of 3.

#### Scenario: Screen reader announces the win
- **WHEN** the level complete dialog opens while a screen reader is on
- **THEN** the screen reader announces that the level is complete and can move through the stars, the score rows, Next Level, Restart and Exit

#### Scenario: Score rows are read with the new labels
- **WHEN** the tally is done with 120 earned points and a penalty of 12, and a screen reader moves through the score rows
- **THEN** it reads "Points 120", "Penalty minus 12" and "Score 108"

## ADDED Requirements

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
