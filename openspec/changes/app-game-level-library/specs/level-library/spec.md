## ADDED Requirements

### Requirement: Level library shows a paged 6x4 grid
The system SHALL provide a level library screen that shows levels in a grid of 6 columns and 4 rows, so 24 levels per page, in ascending level order. Page `p` (starting at 1) SHALL contain levels `(p-1)*24+1` through `p*24`. Each tile SHALL show its level number.

#### Scenario: First page layout
- **WHEN** the level library opens on page 1
- **THEN** a grid of 6 columns and 4 rows is shown containing levels 1 to 24 in order

#### Scenario: Last page is partial
- **WHEN** there are 30 levels and the player is on page 2
- **THEN** levels 25 to 30 are shown and the remaining grid cells are empty

### Requirement: Tiles show level state
The system SHALL show each tile as locked, unlocked or completed. A completed tile SHALL show the number of stars (1-3) of the player's best result. A locked tile SHALL be visually distinct and show a lock indicator.

#### Scenario: Completed level shows stars
- **WHEN** level 5 has a history record with 2 stars
- **THEN** its tile is shown as completed with 2 of 3 stars filled

#### Scenario: Unlocked unfinished level
- **WHEN** level 6 is unlocked and has no history record
- **THEN** its tile shows the number without stars and without a lock

#### Scenario: Locked level
- **WHEN** a level is locked
- **THEN** its tile shows a lock indicator and is dimmed

### Requirement: Levels unlock sequentially
The system SHALL treat level 1 as always unlocked. A level `n > 1` SHALL be unlocked when level `n-1` has been finished. A finished level SHALL always be unlocked.

#### Scenario: Finishing a level unlocks the next
- **WHEN** level 2 is finished and level 3 was locked
- **THEN** level 3 is shown as unlocked

#### Scenario: Skipping is not possible
- **WHEN** levels 1 and 2 are finished and level 3 is not
- **THEN** level 4 is shown as locked

#### Scenario: Fresh install
- **WHEN** no level has been finished
- **THEN** only level 1 is unlocked

### Requirement: Next page unlocks by earned stars
The system SHALL unlock the first level of page `p+1` only when the total stars earned over the levels of page `p` is at least `{x}`, the page unlock star threshold. Unplayed levels SHALL count as 0 stars. The remaining levels of page `p+1` SHALL then follow the sequential unlock rule.

#### Scenario: Threshold reached
- **WHEN** the stars on page 1 total `{x}` or more
- **THEN** level 25 is unlocked

#### Scenario: Threshold not reached
- **WHEN** the stars on page 1 total less than `{x}` and level 24 is not finished
- **THEN** level 25 is locked and page 2 is not accessible

#### Scenario: Improving a result raises the page total
- **WHEN** replaying a level on page 1 raises its stars from 1 to 3 and this lifts the page total to `{x}`
- **THEN** level 25 becomes unlocked

### Requirement: Page unlock threshold is configurable
The system SHALL read the page unlock star threshold `{x}` from a single configuration value and SHALL NOT hard-code it in unlock logic or UI. A provisional default SHALL be used until the final value is defined.

#### Scenario: Threshold changed
- **WHEN** the configured threshold is changed from 36 to 20
- **THEN** page 2 unlocks once page 1 totals 20 stars, without other code changes

### Requirement: Paging is limited to accessible pages
The system SHALL let the player move between pages with previous and next controls and by swiping, but only to pages that contain at least one unlocked level. Pages without an unlocked level SHALL NOT be reachable.

#### Scenario: Navigate to an unlocked page
- **WHEN** page 2 contains an unlocked level and the player selects next on page 1
- **THEN** page 2 is shown

#### Scenario: Next page is locked
- **WHEN** page 2 has no unlocked level and the player is on page 1
- **THEN** the next control is disabled and swiping does not reach page 2

#### Scenario: Locked-page hint
- **WHEN** the next page exists but is locked
- **THEN** the screen shows the earned stars on the current page against `{x}`

#### Scenario: First page has no previous
- **WHEN** the player is on page 1
- **THEN** the previous control is disabled

### Requirement: Selecting a level starts it
The system SHALL raise a navigation event with the level id when the player taps an unlocked or completed tile. Tapping a locked tile SHALL NOT raise a navigation event.

#### Scenario: Tap unlocked level
- **WHEN** the player taps the tile of unlocked level 7
- **THEN** a navigation event to start level 7 is raised

#### Scenario: Tap locked level
- **WHEN** the player taps the tile of a locked level
- **THEN** no navigation event is raised and brief locked feedback is shown

### Requirement: Library opens on a useful page
The system SHALL open the level library on the page containing the lowest unlocked level that has not been finished, or on the last accessible page if every unlocked level is finished.

#### Scenario: Returning after progress
- **WHEN** levels 1 to 24 are finished, page 2 is accessible and level 25 is unfinished
- **THEN** the library opens on page 2
