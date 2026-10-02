# play-menu-dialog Specification

## Purpose
TBD - created by syncing change app-welcome-play-button. Update Purpose after archive.
## Requirements
### Requirement: Play opens a choice dialog
Tapping "Play" on the welcome screen SHALL open a modal dialog offering two actions, "Continue" and "Select Level", instead of navigating directly.

#### Scenario: Tap Play
- **WHEN** the user taps "Play" on the welcome screen
- **THEN** a modal dialog is shown with the buttons "Continue" and "Select Level"
- **AND** no navigation event is emitted yet

#### Scenario: Dismiss the dialog
- **WHEN** the dialog is open and the user taps the scrim outside the panel or presses back
- **THEN** the dialog closes and the welcome screen is shown unchanged

#### Scenario: Tap inside the panel
- **WHEN** the dialog is open and the user taps inside the panel but not on a button
- **THEN** the dialog stays open and no navigation event is emitted

### Requirement: Continue starts the next level
Tapping "Continue" SHALL start the level following the highest completed level. A level is completed when a history record exists; a 0-star win counts, as in the `level-library` rules. With no completed level, "Continue" SHALL start level 1.

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

### Requirement: Play menu follows the design system
The play menu dialog SHALL use the Deep Flow dialog look of `docs/design-system.md` §5.6: a level 3 floating panel (`radius.lg`, `surface.base`, a 1 dp `line.highlight` top edge, no outline, 24 dp padding, at most 360 dp wide) centred with a 16 dp gutter over a see-through `surface.scrim` drawn by the dialog (platform dim off). The title SHALL be "Play" in Title Case in `display.lg` `text.primary`. "Continue" SHALL be a brass Primary button and "Select Level" a glass Secondary button (§5.1), both Medium (48 dp face + 4 dp lip), full panel width and 12 dp apart, in that order below the title.

#### Scenario: Look of the dialog
- **WHEN** the play menu is open
- **THEN** the title reads "Play" (not "PLAY")
- **AND** "Continue" is a Primary and "Select Level" a Secondary `FlowButton`, in that order below the title
- **AND** the panel has no outline and is at most 360 dp wide

#### Scenario: Scrim
- **WHEN** the play menu is open
- **THEN** the whole window is covered by the `surface.scrim` colour (`#07111C` at 72 %) and the welcome screen stays visible through it

#### Scenario: Smallest screen
- **WHEN** the play menu is open on a 640 × 360 dp screen
- **THEN** the title and both buttons are fully displayed inside the panel, with at least 16 dp between the panel and the window edge

### Requirement: Play menu entry motion
The play menu SHALL enter as the other dialogs do (§7.2): the scrim fades in from 0 to its 72 % over `motion.quick` (160 ms) while the panel scales from 0.92 to 1 with `motion.pop`. With reduced motion (§7.4) the panel SHALL fade in over 160 ms without scaling. Closing SHALL be immediate.

#### Scenario: Entry with animations on
- **WHEN** the play menu opens with animations enabled
- **THEN** the panel starts at scale 0.92 and settles at scale 1

#### Scenario: Entry with reduced motion
- **WHEN** the play menu opens with the system "remove animations" setting on
- **THEN** the panel is shown at scale 1 throughout and fades in over 160 ms

#### Scenario: Closing
- **WHEN** the user taps "Continue", "Select Level", the scrim or Back
- **THEN** the dialog disappears at once, without an exit animation
