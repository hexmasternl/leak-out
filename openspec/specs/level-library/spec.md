# level-library Specification

## Purpose
TBD - created by archiving change app-game-level-library. Update Purpose after archive.
## Requirements
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

### Requirement: Next page unlocks by earned stars
The system SHALL open page `p+1` only when page `p` is open and the total stars earned over the levels of page `p` is at least `{x}`, the page unlock star threshold. Page 1 SHALL always be open. Unplayed levels SHALL count as 0 stars, and a page SHALL hold at most 72 stars. Finishing levels on page `p` SHALL NOT open page `p+1` by itself. When page `p+1` opens, its first three levels SHALL be unlocked and its other levels SHALL follow the unlock window rule.

#### Scenario: Threshold reached
- **WHEN** the stars on page 1 total `{x}` or more
- **THEN** page 2 is open and levels 25, 26 and 27 are unlocked

#### Scenario: Threshold not reached
- **WHEN** the stars on page 1 total less than `{x}`, even with level 24 finished
- **THEN** level 25 is locked and page 2 is not accessible

#### Scenario: Improving a result raises the page total
- **WHEN** replaying a level on page 1 raises its stars from 1 to 3 and this lifts the page total to `{x}`
- **THEN** page 2 opens and level 25 becomes unlocked

#### Scenario: Pages open in order
- **WHEN** page 1 is below `{x}` stars and page 2 holds `{x}` or more stars from earlier results
- **THEN** pages 2 and 3 are both closed

### Requirement: Page unlock threshold is configurable
The system SHALL read the page unlock star threshold `{x}` from a single configuration value and SHALL NOT hard-code it in unlock logic or UI. The default threshold SHALL be 50 of the 72 stars a page can hold. The configuration SHALL reject a threshold below 0 or above 72.

#### Scenario: Default threshold
- **WHEN** the library uses the default configuration and page 1 totals 49 stars
- **THEN** page 2 is closed
- **AND** once page 1 totals 50 stars, page 2 is open

#### Scenario: Threshold changed
- **WHEN** the configured threshold is changed from 50 to 20
- **THEN** page 2 unlocks once page 1 totals 20 stars, without other code changes

#### Scenario: Invalid threshold
- **WHEN** a configuration with a threshold of 73 or -1 is created
- **THEN** it is rejected

### Requirement: Paging is limited to accessible pages
The system SHALL let the player move between pages with previous and next controls and by swiping, but only to open pages. A page that is not open SHALL NOT be reachable, even if it contains a finished level.

#### Scenario: Navigate to an open page
- **WHEN** page 2 is open and the player selects next on page 1
- **THEN** page 2 is shown

#### Scenario: Next page is closed
- **WHEN** page 2 is not open and the player is on page 1
- **THEN** the next control is disabled and swiping does not reach page 2

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
The system SHALL open the level library on the page of the level after the highest finished level when that level is unlocked, otherwise on the last open page. With no finished level it SHALL open on page 1.

#### Scenario: Returning after progress
- **WHEN** levels 1 to 24 are finished with at least `{x}` stars and level 25 is unfinished
- **THEN** the library opens on page 2

#### Scenario: A skipped level does not pull the library back
- **WHEN** level 2 was skipped, page 2 is open and levels 25 to 30 are finished
- **THEN** the library opens on page 2, not page 1

#### Scenario: Next level is on a closed page
- **WHEN** level 24 is the highest finished level and page 1 totals fewer than `{x}` stars
- **THEN** the library opens on page 1

### Requirement: Levels unlock in a window of three
The system SHALL let the player start any of the three levels that follow a finished level, so levels can be played ahead or skipped. On an open page, a level `n` SHALL be unlocked when it is finished, when it is one of the first three levels of that page, or when any of the levels `n-1`, `n-2` or `n-3` is finished. A finished level SHALL always be unlocked. No level on a page that is not open SHALL be unlocked, except a finished level. The window size of three SHALL be read from the library configuration.

#### Scenario: Fresh install
- **WHEN** no level has been finished
- **THEN** levels 1, 2 and 3 are unlocked
- **AND** level 4 is locked

#### Scenario: Finishing a level extends the window
- **WHEN** level 1 is finished and no other level is
- **THEN** levels 2, 3 and 4 are unlocked
- **AND** level 5 is locked

#### Scenario: Skipping levels
- **WHEN** levels 1 and 4 are finished and levels 2 and 3 are not
- **THEN** levels 2, 3, 5, 6 and 7 are unlocked
- **AND** level 8 is locked

#### Scenario: At most two levels in a row can be skipped
- **WHEN** level 1 is finished and levels 2, 3 and 4 are not
- **THEN** level 5 is locked

#### Scenario: Window does not cross into a closed page
- **WHEN** levels 22, 23 and 24 are finished and page 1 totals fewer stars than the page unlock threshold
- **THEN** levels 25, 26 and 27 are locked

#### Scenario: Window of one restores sequential unlocking
- **WHEN** the configured unlock window is 1 and levels 1 and 2 are finished
- **THEN** level 3 is unlocked and level 4 is locked

### Requirement: Star progress is shown as a progress bar
The system SHALL show, on every page that has a next page, a progress bar of the stars earned on that page towards `{x}`, labelled with the earned stars and the threshold (for example "34 / 50"). The bar SHALL be filled by `min(earned / {x}, 1)`. When the threshold is reached the bar SHALL be full and shown in the success style. On the last page no progress bar SHALL be shown. The bar SHALL fit below the 6x4 grid without scrolling.

#### Scenario: Progress below the threshold
- **WHEN** the player is on page 1, page 2 exists and page 1 totals 34 stars with a threshold of 50
- **THEN** the progress bar is filled to 68% and labelled "34 / 50"

#### Scenario: Threshold reached
- **WHEN** page 1 totals 55 stars with a threshold of 50
- **THEN** the progress bar is full, shown in the success style and labelled "55 / 50"
- **AND** the next control is enabled

#### Scenario: Last page
- **WHEN** the player is on the last page
- **THEN** no progress bar is shown

#### Scenario: Progress follows the visible page
- **WHEN** the player moves from page 1 with 60 stars to page 2 with 12 stars
- **THEN** the progress bar shows "12 / 50"

#### Scenario: Locked tap message
- **WHEN** the player taps a locked tile
- **THEN** the locked message is shown briefly in the progress bar's row and the bar returns afterwards

