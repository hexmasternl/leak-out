## ADDED Requirements

### Requirement: Play opens a choice dialog
Tapping "Play" on the welcome screen SHALL open a modal dialog offering two actions, "Continue" and "Select level", instead of navigating directly.

#### Scenario: Tap Play
- **WHEN** the user taps "Play" on the welcome screen
- **THEN** a modal dialog is shown with the buttons "Continue" and "Select level"
- **AND** no navigation event is emitted yet

#### Scenario: Dismiss the dialog
- **WHEN** the dialog is open and the user taps outside it or presses back
- **THEN** the dialog closes and the welcome screen is shown unchanged

### Requirement: Continue starts the next level
Tapping "Continue" SHALL start the level following the highest completed level. A level is completed when a history record with at least one star exists. With no completed level, "Continue" SHALL start level 1.

#### Scenario: Progress exists
- **WHEN** the highest completed level is 7 and the user taps "Continue"
- **THEN** a navigation event to start level 8 is emitted and the dialog closes

#### Scenario: Gaps in history
- **WHEN** levels 1, 2 and 5 are completed
- **THEN** "Continue" starts level 6

#### Scenario: Fresh install
- **WHEN** no level is completed and the user taps "Continue"
- **THEN** a navigation event to start level 1 is emitted

#### Scenario: All levels completed
- **WHEN** the highest completed level is the last level
- **THEN** "Continue" starts the last level

#### Scenario: Next level is locked
- **WHEN** the next level is the first level of a page that is still locked by the star threshold
- **THEN** "Continue" opens the level library on the page of the highest completed level instead of starting a level

### Requirement: Select level opens the level library on the right page
Tapping "Select level" SHALL open the level library, showing 24 levels per page that the player can leaf through, on the page containing the next level to be played (the same level "Continue" would target).

#### Scenario: Default page
- **WHEN** the highest completed level is 30 and the user taps "Select level"
- **THEN** the level library opens on page 2 (levels 25-48), because the next level is 31

#### Scenario: Boundary
- **WHEN** the highest completed level is 24 and page 2 is accessible
- **THEN** the level library opens on page 2, because the next level is 25

#### Scenario: Fresh install
- **WHEN** no level is completed
- **THEN** the level library opens on page 1

#### Scenario: Free paging
- **WHEN** the level library is opened from the dialog
- **THEN** the player can move to other accessible pages as defined by the `level-library` capability
