# app-updates Specification

## Purpose
TBD - created by archiving change app-required-updates. Update Purpose after archive.

## Requirements

### Requirement: The app checks Google Play for a newer version
When the app starts and each time it returns to the foreground, it SHALL ask Google Play whether a newer version is available and, if so, that version's in-app update priority (0–5). The check SHALL run in the background: the splash and welcome screens SHALL NOT wait for it.

#### Scenario: Check at start
- **WHEN** the app is started
- **THEN** it asks Google Play for an available update, and the welcome screen appears without waiting for the answer

#### Scenario: Check on return to the foreground
- **WHEN** the app was in the background and the player returns to it
- **THEN** it asks Google Play again

### Requirement: Updates are classified by priority
An available update with an in-app update priority of 4 or higher SHALL be treated as required. An available update with a lower priority SHALL be treated as optional. When no update is available, or the check fails or is not possible (no network, the app was not installed from Google Play, Google Play is missing, debug build), the app SHALL behave as if no update is available.

#### Scenario: Priority 5
- **WHEN** Google Play reports an available update with priority 5
- **THEN** the update is required

#### Scenario: Priority 0
- **WHEN** Google Play reports an available update with priority 0
- **THEN** the update is optional

#### Scenario: Check fails
- **WHEN** the update check fails or cannot run
- **THEN** the player can start and play levels as normal, and no update screen or prompt is shown

### Requirement: A required update blocks play
While a required update is known and not installed, the app SHALL show a blocking update screen instead of the welcome screen and SHALL NOT start a level. The screen SHALL offer only to update, which opens the Google Play update flow, and to close the app. Back SHALL close the app rather than dismiss the screen. If the player cancels or the update fails, the blocking screen SHALL stay.

#### Scenario: Required update at start
- **WHEN** the app starts and a required update is available
- **THEN** the blocking update screen is shown and no level can be started

#### Scenario: Player cancels the update
- **WHEN** the player taps update and then cancels the Google Play update flow
- **THEN** the blocking update screen is shown again

#### Scenario: Back on the blocking screen
- **WHEN** the blocking update screen is shown and the player presses Back
- **THEN** the app closes

### Requirement: A level in progress is finished before a required update
When a required update becomes known while the player is in a level, the level SHALL continue undisturbed. When that level ends, won or lost, the level complete or level lost dialog SHALL be shown as usual, but any of its buttons that would start a level (next level, restart) SHALL lead to the blocking update screen instead.

#### Scenario: Required update during a level
- **WHEN** a required update becomes known while a level is being played
- **THEN** play continues without any update screen or prompt until the level ends

#### Scenario: Next level after a required update
- **WHEN** the player completes that level and taps next level
- **THEN** the blocking update screen is shown instead of the next level

#### Scenario: Restart after a required update
- **WHEN** the player loses that level and taps restart
- **THEN** the blocking update screen is shown instead of the level

### Requirement: An optional update is offered at start
When the app starts and an optional update is available, the app SHALL offer it once on the welcome screen with a prompt the player can dismiss. Dismissing it SHALL leave the app fully playable. If the player accepts, the update SHALL download in the background while they keep playing, and once it is downloaded the app SHALL offer to restart and install it, but never while a level is being played.

#### Scenario: Optional update offered
- **WHEN** the app starts and an optional update is available
- **THEN** the welcome screen shows a dismissible update prompt

#### Scenario: Optional update dismissed
- **WHEN** the player dismisses the prompt
- **THEN** the prompt closes and the player can start levels as normal

#### Scenario: Downloaded during a level
- **WHEN** an accepted optional update finishes downloading while a level is being played
- **THEN** nothing is shown until the player is back outside the level, where the app offers to restart and install it

### Requirement: Update screens are localized and accessible
The blocking update screen and the optional update prompts SHALL use strings in every supported language, follow the design system, and expose their buttons to accessibility services with clear labels.

#### Scenario: Translated update screen
- **WHEN** the app language is Dutch and a required update is shown
- **THEN** all text on the blocking update screen is Dutch
