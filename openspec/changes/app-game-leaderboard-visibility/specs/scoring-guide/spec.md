## ADDED Requirements

### Requirement: How scoring works dialog content
The How scoring works dialog SHALL explain, in this order: the points per section type (straight 10, corner 15, reservoir 25, cross 10 for the first pass and 50 for the second pass through the other channel), that unused sections cost 3 points each when the level is won, that the stars follow from the total score, and, when the top 10 is available, that every level from the level library has a global Top 10 in which only the player's best score counts. The point values SHALL be taken from the game's scoring rules. The text SHALL be translatable and shown in every supported language.

#### Scenario: Point values shown
- **WHEN** the dialog is open
- **THEN** it shows 10 for a straight, 15 for a corner, 25 for a reservoir, 10 and 50 for a cross and a penalty of 3 per unused section

#### Scenario: Top 10 mentioned when available
- **WHEN** the dialog is open in a build with the score server configured
- **THEN** it says that every library level has a global Top 10 where the player's best score counts

#### Scenario: Top 10 not mentioned when unavailable
- **WHEN** the dialog is open in a build with a blank score server URL
- **THEN** it does not mention the Top 10

### Requirement: Scoring guide opens from the pause dialog
The pause dialog SHALL show a **Scoring** button. Tapping it SHALL open the How scoring works dialog while the game stays paused. Closing the guide SHALL return to the unchanged pause dialog.

#### Scenario: Open while paused
- **WHEN** the player pauses a level and taps Scoring
- **THEN** the How scoring works dialog is shown and the liquid, the timer and the score do not advance

#### Scenario: Back to the pause dialog
- **WHEN** the guide was opened from the pause dialog and the player taps Close
- **THEN** the pause dialog is shown again and the game is still paused

### Requirement: Scoring guide shown once after the first win
The first time the player wins a level from the level library, the How scoring works dialog SHALL be shown once the score and star animations of the level complete dialog are done. Closing it SHALL return to the level complete dialog with its buttons enabled and mark the guide as seen. The seen flag SHALL be stored on the device; once it is set, a win SHALL NOT show the guide by itself again. A test play from the level designer SHALL NOT show it.

#### Scenario: First win
- **WHEN** the player wins a library level for the first time and the tally is done
- **THEN** the How scoring works dialog is shown above the level complete dialog

#### Scenario: Later win
- **WHEN** the player has closed the guide after a win and wins another level
- **THEN** the guide is not shown

#### Scenario: Survives an app restart
- **WHEN** the guide has been seen and the app is restarted
- **THEN** the next win does not show the guide

### Requirement: Closing the scoring guide
The How scoring works dialog SHALL have a **Close** button. Tapping Close or pressing the system Back button SHALL close it and return to the dialog it was opened from. Tapping outside the dialog SHALL NOT close it.

#### Scenario: Back closes the guide
- **WHEN** the guide is open and the player presses Back
- **THEN** the guide closes and the dialog it was opened from is shown

### Requirement: Scoring guide is accessible and follows the design system
The guide SHALL follow the Deep Flow dialog look of `docs/design-system.md` and fit a 640 × 360 dp screen with the Close button always fully visible; when the content does not fit, it SHALL scroll. Each scoring rule SHALL be read by a screen reader as one item, and Close SHALL have a touch target of at least 48 dp.

#### Scenario: Small screen with large font
- **WHEN** the guide is open on a 640 × 360 dp screen at a 130 % font scale
- **THEN** Close is fully visible and every rule can be reached by scrolling
