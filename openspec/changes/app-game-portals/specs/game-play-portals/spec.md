# Spec Delta

## Purpose

Lets a level link two cells as a portal pair, so liquid that enters one portal comes out of the other and routes can cross walls of blocked cells.

## ADDED Requirements

### Requirement: Level lists portal pairs
The system SHALL read an optional `portals` array from the level document. Each entry is a pair with the portals `a` and `b`, each with an `x`, a `y` and an `openSide` of `LEFT`, `RIGHT`, `TOP` or `BOTTOM`. An absent or empty array SHALL mean the level has no portals. The parsed level SHALL keep the pairs in authored order; the first pair is pair 1.

#### Scenario: Level without portals
- **WHEN** a level has no `portals` field
- **THEN** the level is accepted with no portal pairs

#### Scenario: Valid portal pair
- **WHEN** a level on a 9 by 7 grid lists a pair with `a` at x 2, y 1, open side `LEFT` and `b` at x 6, y 5, open side `RIGHT`
- **THEN** the level is accepted with one portal pair holding those two portals

#### Scenario: Missing portal
- **WHEN** a portal pair omits `b`
- **THEN** parsing fails with an error that names `portals[0].b`

#### Scenario: Unknown open side
- **WHEN** a portal has an `openSide` of `UP`
- **THEN** parsing fails with an error that names the invalid value

### Requirement: A level has at most three portal pairs
The system SHALL accept a level with at most 3 portal pairs.

#### Scenario: Three pairs
- **WHEN** a level lists 3 valid portal pairs
- **THEN** the level is accepted

#### Scenario: Four pairs
- **WHEN** a level lists 4 portal pairs
- **THEN** validation fails with an error that names `portals`

### Requirement: Portals lie on their own cells
Every portal SHALL lie inside the grid and SHALL NOT lie on a blocked cell, the start, the finish, a mandatory section or another portal. The two portals of a pair are therefore different cells.

#### Scenario: Portal outside the grid
- **WHEN** a portal has an `x` that is at least the column count
- **THEN** validation fails with an error that names the portal, such as `portals[0].a`, and its coordinates

#### Scenario: Portal on a blocked cell
- **WHEN** a portal has the same coordinates as a blocked cell
- **THEN** validation fails with an error that names the portal and states that the cell is blocked

#### Scenario: Portal on the start or finish
- **WHEN** a portal has the same coordinates as the start or the finish
- **THEN** validation fails with an error that names the portal and states that the start and finish cannot hold a portal

#### Scenario: Portal on a mandatory section
- **WHEN** a portal has the same coordinates as a mandatory section
- **THEN** validation fails with an error that names the portal and states that the cell holds a mandatory section

#### Scenario: Two portals on one cell
- **WHEN** the two portals of a pair, or portals of two different pairs, have the same coordinates
- **THEN** validation fails with an error that names the duplicated cell

### Requirement: Portals face a cell that can hold a pipe
The start and finish facing-cell rules SHALL apply to every portal. A portal's facing cell is the neighbouring cell on its open side. It SHALL lie inside the grid, SHALL NOT be blocked, and SHALL NOT be the start, the finish or a portal. Two portals, or a portal and the start or finish, MAY share a facing cell.

#### Scenario: Portal opens onto the grid edge
- **WHEN** a portal is at x 0 with open side `LEFT`
- **THEN** validation fails with an error stating that the portal's open side faces the edge of the grid

#### Scenario: Portal walled off
- **WHEN** a portal's facing cell is a blocked cell
- **THEN** validation fails with an error stating that the portal is walled off

#### Scenario: Portal faces the start
- **WHEN** a portal's facing cell is the start
- **THEN** validation fails with an error stating that a portal must not face the start, the finish or another portal

#### Scenario: Portal faces a mandatory section
- **WHEN** a portal's facing cell holds a mandatory reservoir
- **THEN** the level is accepted

### Requirement: Portals are on the board from the start and cannot be replaced
When an attempt starts or restarts, the board SHALL hold every portal of the level on its cell. Portals SHALL NOT come from the stack, SHALL NOT be replaced or removed, and SHALL stay on the board when the unused sections are removed after a win. A portal is not a placed section: it SHALL NOT count as an unused section.

#### Scenario: Level starts with its portals
- **WHEN** a level with one portal pair starts
- **THEN** both portals are on the board with no liquid and the stack is dealt as for any other level

#### Scenario: Removal of unused sections after a win
- **WHEN** a level with portals is won and the unused placed sections are removed
- **THEN** every portal stays on the board and the unused-section penalty counts no portal

### Requirement: Liquid passes a portal pair
Liquid that leaves a tile into a portal through the portal's open side SHALL enter the pair and come out of the other portal of the pair through that portal's open side, into the cell it faces. Pairs SHALL work both ways: liquid can enter either portal of a pair. Liquid that reaches a portal through any other side SHALL leak at the tile it left.

#### Scenario: Through a pair from a to b
- **WHEN** a corner section's exit faces portal `a` of a pair through `a`'s open side, and `b`'s facing cell holds a section open towards `b`
- **THEN** the liquid passes the pair and continues in that section, entering it from `b`'s side

#### Scenario: Through a pair from b to a
- **WHEN** the liquid enters portal `b` through its open side
- **THEN** it comes out of portal `a` through `a`'s open side

#### Scenario: Closed side of a portal
- **WHEN** a straight section's exit faces a portal on a side that is not the portal's open side
- **THEN** the level is lost by a leak at the straight section and its exit side

#### Scenario: Nothing in front of the exit portal
- **WHEN** the liquid passes a pair and the cell that the exit portal faces is empty
- **THEN** the level is lost by a leak at the exit portal and its open side

### Requirement: Passing a pair takes the time of one cell
Passing a portal pair SHALL take the time the liquid needs for one cell at the current speed. During the first half of that time the liquid SHALL run from the entry portal's open side into the entry portal; during the second half it SHALL run out of the exit portal to the exit portal's open side. The reservoir speed factor SHALL NOT apply.

#### Scenario: Pair at 1 cell per second
- **WHEN** the speed is 1 cell per second and the liquid enters a portal pair
- **THEN** after 400 ms it is still in the entry portal, after 600 ms it is in the exit portal, and after 1,000 ms it leaves the exit portal

### Requirement: A portal pair is passed at most once per attempt
Each portal pair SHALL count as one channel. Once the liquid has passed a pair, liquid that reaches either portal of that pair again SHALL leak, as at a used cross channel. Because each portal has a single open side, this only matters for the route check's and the flow's loop protection.

#### Scenario: Pair already used
- **WHEN** the liquid has passed a pair and later reaches one of its portals through the open side
- **THEN** the level is lost by a leak at the tile the liquid left

### Requirement: Portal pairs are told apart by colour and symbol
Both portals of a pair SHALL share a colour and a symbol, and every pair in a level SHALL have a colour and a symbol that no other pair has, so colour is never the only cue. Pair 1, 2 and 3 SHALL use the design system's first, second and third portal colour and symbol.

#### Scenario: Two pairs on the board
- **WHEN** a level with two portal pairs is shown
- **THEN** the two portals of pair 1 show the first portal colour and symbol, the two portals of pair 2 show the second, and the two pairs differ in both colour and symbol

### Requirement: Portals are drawn with their open side and the liquid passing them
A portal SHALL be drawn on the plain empty-cell tile as a stub of straight pipe from its open side to the centre, ending in the portal ring with its pair symbol, following the design system. While the liquid passes a pair, the entry portal's stub SHALL fill from its open side to the ring and the exit portal's stub SHALL fill from the ring to its open side, each over its half of the pass. Both stubs SHALL stay filled after the pass.

#### Scenario: Halfway through a pair
- **WHEN** the liquid is halfway through a portal pair
- **THEN** the entry portal's stub is full, the exit portal's stub is still empty, and the liquid starts coming out of the exit portal's ring

#### Scenario: Pair passed
- **WHEN** the liquid has left the exit portal
- **THEN** both portals of the pair are drawn with full stubs

#### Scenario: Reduced motion
- **WHEN** animations are turned off on the device
- **THEN** the portal rings do not swirl or pulse, and the liquid still fills the stubs

### Requirement: Portals are described for accessibility
The accessibility description of a portal cell SHALL say that it is a portal, its pair number and symbol name, its open side, the cell of the other portal of the pair, and whether the liquid has passed the pair.

#### Scenario: Description of a portal
- **WHEN** a screen reader focuses portal `a` of pair 1, open to the right, whose partner is at column 7, row 4, before the liquid has passed it
- **THEN** the description says that it is portal pair 1, names its symbol, says that it opens to the right and leads to column 7, row 4, and that the liquid has not passed it

### Requirement: A chapter of 24 levels introduces portals
The bundled levels SHALL include one chapter of 24 levels, filling one level library page, that introduces portal pairs. The chapter's first level SHALL have a single pair that the shortest route must use, and later levels SHALL add more pairs, up to three. Every level of the chapter SHALL pass the solver check, and at least one SHALL be solvable only through a portal pair. The page SHALL unlock by the existing page star rule.

#### Scenario: Chapter fills one page
- **WHEN** the level library shows the portal chapter's page
- **THEN** it holds 24 levels, every one with at least one portal pair

#### Scenario: Chapter passes the solver check
- **WHEN** the headless solvability suite runs over the bundled levels
- **THEN** every level of the portal chapter is reported solvable

### Requirement: Existing levels and progress are unaffected
Adding portals SHALL NOT change levels 1 to 96, their ids, scores and star thresholds, or any saved level history, achievement or setting. A level without `portals` SHALL play exactly as before.

#### Scenario: Old level files unchanged
- **WHEN** the change is applied
- **THEN** the files for levels 1 to 96 are byte-identical to before, and each parses to the same level as before

#### Scenario: Saved progress kept
- **WHEN** a player who has finished levels 1 to 30 updates the app
- **THEN** the level history still holds those 30 results with their stars, and the library opens the same pages as before
