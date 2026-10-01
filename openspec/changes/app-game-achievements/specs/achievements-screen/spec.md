## ADDED Requirements

### Requirement: Achievements screen opens from settings
Tapping "Achievements" on the settings screen SHALL open the achievements screen. Its back button and system Back SHALL return to the settings screen.

#### Scenario: Open achievements
- **WHEN** the user taps Achievements on the settings screen
- **THEN** the achievements screen is shown with the title "Achievements"

#### Scenario: Back to settings
- **WHEN** the user presses system Back on the achievements screen
- **THEN** the settings screen is shown

### Requirement: All achievements are listed
The achievements screen SHALL list every achievement in the catalogue, in catalogue order, from the local data, signed in or not, and SHALL show the number unlocked out of the number in the catalogue. The list SHALL scroll when it does not fit.

#### Scenario: Count
- **WHEN** the catalogue holds 25 achievements and 7 are unlocked
- **THEN** the screen shows "7 / 25"

#### Scenario: Works signed out
- **WHEN** the player is not signed in to Play Games and opens the achievements screen
- **THEN** every achievement in the catalogue is listed

### Requirement: Achievement states are shown
An unlocked achievement SHALL show its icon in full colour, its name, its description and its unlock date. A locked achievement that is not hidden SHALL show its icon dimmed, its name and its description; an incremental one SHALL also show a progress bar and its progress as "<progress> / <steps>". A locked hidden achievement SHALL show a generic icon, the name "Hidden achievement" and the description "Keep playing to find out." An unlocked hidden achievement SHALL be shown like any other unlocked achievement.

#### Scenario: Incremental progress
- **WHEN** Journeyman is locked with progress 370
- **THEN** its card shows a progress bar and "370 / 1000"

#### Scenario: Hidden locked
- **WHEN** Photo Finish is locked
- **THEN** its card shows "Hidden achievement" and does not show the name "Photo Finish"

#### Scenario: Hidden unlocked
- **WHEN** Photo Finish is unlocked
- **THEN** its card shows the name "Photo Finish", its description and the unlock date

### Requirement: Open in Play Games
While the player is signed in, the achievements screen SHALL show a "Play Games" button that opens the Play Games achievements UI. When not signed in, the button SHALL NOT be shown.

#### Scenario: Signed in
- **WHEN** the player is signed in and taps Play Games
- **THEN** the Play Games achievements UI is opened

#### Scenario: Signed out
- **WHEN** the player is not signed in
- **THEN** no Play Games button is shown

### Requirement: Achievements screen is accessible
Every card SHALL be read by a screen reader as one item with its name, whether it is unlocked, and its progress if it is incremental. Interactive controls SHALL have a touch target of at least 48 dp.

#### Scenario: Screen reader reads a card
- **WHEN** a screen reader focuses the Journeyman card with progress 370
- **THEN** it reads "Journeyman", "locked" and "370 of 1000"
