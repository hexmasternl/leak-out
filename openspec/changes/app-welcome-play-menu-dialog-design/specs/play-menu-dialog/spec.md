## MODIFIED Requirements

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

## ADDED Requirements

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
