## ADDED Requirements

### Requirement: Level complete dialog shows the leaderboard place
When the top 10 is available for the level and the player is signed in, the level complete dialog SHALL, after the score of the win was accepted by the score server, load the player's place with `GET /api/levels/{level}/scores/around-me` and show it below the score rows as "You are #<position> on level <level>". While it loads, and when it cannot be loaded, the line SHALL NOT be shown. Loading the place SHALL NOT delay the animations or enabling the buttons.

#### Scenario: Place shown
- **WHEN** the score of level 7 was accepted by the server and the server returns position 23 for the player
- **THEN** the dialog shows "You are #23 on level 7"

#### Scenario: Offline
- **WHEN** the score could not be sent because the device is offline
- **THEN** no place line is shown and the dialog otherwise works as usual

#### Scenario: Test play
- **WHEN** the player wins a test play from the level designer
- **THEN** no place line is shown and no request is sent

### Requirement: Level complete dialog invites the player to the Top 10
When the top 10 is available for the level, the level complete dialog SHALL show a short line below the score rows that invites the player to build longer routes and reach the Top 10, and the Top 10 button SHALL be shown in the highlighted style of the design system. Without the top 10, neither SHALL be shown.

#### Scenario: Invitation shown
- **WHEN** the level complete dialog of a library level is shown in a build with the score server configured
- **THEN** it shows the invitation line and a highlighted Top 10 button

#### Scenario: No top 10
- **WHEN** the level complete dialog is shown in a build with a blank score server URL
- **THEN** no invitation line and no Top 10 button are shown

### Requirement: Level complete dialog offers the scoring guide
The level complete dialog SHALL show a help button next to the score rows with the content description "How scoring works" and a touch target of at least 48 dp. Once the buttons are enabled, tapping it SHALL open the How scoring works dialog (see `scoring-guide`); closing the guide SHALL return to the unchanged level complete dialog.

#### Scenario: Open the guide after a win
- **WHEN** the buttons of the level complete dialog are enabled and the player taps the help button
- **THEN** the How scoring works dialog is shown, and Close returns to the level complete dialog with its final score
