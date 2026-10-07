# Spec Delta

## Purpose

Lets the player see the global top 10 scores of a level, from the Leak Out score server, while playing it or after winning or losing it, so they can compare their score with other players.

## ADDED Requirements

### Requirement: Top 10 availability
The top 10 SHALL be available for a level from the level library when the score server is configured in the build. It SHALL NOT be available for a test play from the level designer, or when the score server is not configured. When it is not available, no Top 10 button SHALL be shown anywhere.

#### Scenario: Library level
- **WHEN** the player plays level 7 from the level library in a build with the score server configured
- **THEN** the top 10 is available for level 7

#### Scenario: Test play
- **WHEN** the player test plays a design from the level designer
- **THEN** the top 10 is not available and no Top 10 button is shown

#### Scenario: Score server not configured
- **WHEN** the build has a blank score server URL
- **THEN** no Top 10 button is shown

### Requirement: Top 10 opens from the pause and result dialogs
Tapping **Top 10** in the pause dialog, the level complete dialog or the level lost dialog SHALL open the top 10 dialog for the level being played. Opening it from the pause dialog SHALL keep the game paused: the liquid, the timer and the score SHALL NOT advance while the top 10 dialog is open.

#### Scenario: Open during play
- **WHEN** the player pauses level 7 and taps Top 10
- **THEN** the top 10 dialog for level 7 is shown and the game stays frozen

#### Scenario: Open after a win
- **WHEN** the level complete dialog of level 7 has its buttons enabled and the player taps Top 10
- **THEN** the top 10 dialog for level 7 is shown

#### Scenario: Open after a loss
- **WHEN** the level lost dialog of level 7 is shown and the player taps Top 10
- **THEN** the top 10 dialog for level 7 is shown

### Requirement: Top 10 dialog backdrop
The top 10 dialog SHALL be a modal dialog shown on the opaque backdrop of the design system (the diagonal gradient from `surface.abyss` to `surface.deep`), covering the whole screen, so that neither the game nor the dialog it was opened from can be seen behind it.

#### Scenario: Opaque backdrop
- **WHEN** the top 10 dialog is open
- **THEN** no cell, section, stack slot, HUD element or other dialog can be seen through its backdrop

### Requirement: Top 10 dialog content
The top 10 dialog SHALL show a title, the level number and at most 10 entries in ranking order, highest score first. Each entry SHALL show its rank, the player's display name and the score, as returned by the score server. The entry of the signed-in player SHALL be highlighted.

#### Scenario: Ranked list
- **WHEN** the score server returns 10 entries for level 7
- **THEN** the dialog shows "Level 7" and the 10 entries from rank 1 to rank 10, each with its rank, display name and score

#### Scenario: Fewer than 10 scores
- **WHEN** the score server returns 3 entries for the level
- **THEN** the dialog shows exactly those 3 entries

#### Scenario: Own entry highlighted
- **WHEN** the signed-in player is ranked 4th
- **THEN** the 4th entry is highlighted and no other entry is

### Requirement: Top 10 dialog states
While the scores load, the dialog SHALL show a loading indicator. When the level has no scores, it SHALL say that there are no scores yet. When the player is not signed in to Google Play Games, it SHALL say that signing in is needed and offer the manual sign-in. When the scores cannot be loaded, it SHALL show an error message and a **Try again** action that loads them again.

#### Scenario: Loading
- **WHEN** the dialog is opened and the score server has not answered yet
- **THEN** the dialog shows a loading indicator and no entries

#### Scenario: No scores yet
- **WHEN** the score server returns no entries for the level
- **THEN** the dialog says that there are no scores for this level yet

#### Scenario: Signed out
- **WHEN** the player is not signed in to Google Play Games and opens the top 10
- **THEN** the dialog says that signing in to Google Play Games is needed to see the top 10, offers a sign-in action, and no request is sent to the score server

#### Scenario: Signing in from the dialog
- **WHEN** the signed-out dialog is shown and the player signs in successfully with its sign-in action
- **THEN** the dialog loads and shows the top 10

#### Scenario: Offline
- **WHEN** the device is offline and the player opens the top 10
- **THEN** the dialog shows an error message and a Try again action

#### Scenario: Try again
- **WHEN** the error message is shown, the device is back online and the player taps Try again
- **THEN** the dialog shows the loading indicator and then the top 10

### Requirement: Top 10 is loaded from the score server
The system SHALL load the top 10 with `GET /api/levels/{level}/scores/top` for the signed-in player, with the same score server session as score posting. When the server answers 401, the system SHALL sign in to the score server again once and repeat the request. The top 10 SHALL be loaded each time the dialog opens and SHALL NOT be stored on the device.

#### Scenario: Request for the level
- **WHEN** the top 10 dialog opens for level 7
- **THEN** one request for the top scores of level 7 is sent with the player's access token

#### Scenario: Expired token
- **WHEN** the server answers the request with 401
- **THEN** the system signs in to the score server again and repeats the request once

#### Scenario: Fresh on every opening
- **WHEN** the player closes the top 10 dialog and opens it again
- **THEN** the top 10 is loaded again from the score server

### Requirement: Closing the top 10 dialog
The top 10 dialog SHALL have a **Close** button. Tapping Close or pressing the system Back button SHALL close it and return to the dialog it was opened from, which is unchanged. Tapping the backdrop SHALL NOT close it.

#### Scenario: Back to the pause dialog
- **WHEN** the top 10 was opened from the pause dialog and the player taps Close
- **THEN** the pause dialog is shown again and the game is still paused

#### Scenario: Back to the level complete dialog
- **WHEN** the top 10 was opened from the level complete dialog and the player presses Back
- **THEN** the level complete dialog is shown again with its final score and enabled buttons

#### Scenario: Back to the level lost dialog
- **WHEN** the top 10 was opened from the level lost dialog and the player taps Close
- **THEN** the level lost dialog is shown again

#### Scenario: Tapping the backdrop
- **WHEN** the top 10 dialog is open and the player taps the backdrop
- **THEN** the dialog stays open

### Requirement: Top 10 dialog is accessible
Each entry SHALL be read by a screen reader as one item with its rank, name and score, in ranking order, and the player's own entry SHALL be announced as theirs. The Close, Try again and sign-in actions SHALL be buttons with readable labels and a touch target of at least 48 dp.

#### Scenario: Entry read as one item
- **WHEN** TalkBack focuses the entry ranked 2nd of "Ada" with 1450 points
- **THEN** it reads the rank 2, "Ada" and 1450 as one item

#### Scenario: Own entry announced
- **WHEN** TalkBack focuses the signed-in player's entry
- **THEN** it says that this is the player's own score

### Requirement: Top 10 dialog follows the design system and fits the screen
The top 10 dialog SHALL follow the approved Top 10 dialog design in `docs/design-system.md`. On a 640 × 360 dp landscape screen, the title and the Close button SHALL be fully visible; when the 10 entries do not fit, the list SHALL scroll while the title and the Close button stay in place.

#### Scenario: Small landscape screen
- **WHEN** the top 10 dialog shows 10 entries on a 640 × 360 dp screen
- **THEN** the title and Close are fully visible and every entry can be reached by scrolling the list

#### Scenario: Large font
- **WHEN** the top 10 dialog shows 10 entries on a 640 × 360 dp screen with a 130 % font scale
- **THEN** Close is fully visible and every entry can be reached
