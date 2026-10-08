# Spec Delta

## Purpose

One-way valves: straight sections that a level puts on the board with a direction, which the liquid can pass only that way. Covers how a level lists them, where they may lie, how the liquid passes or leaks at them, and the chapter of levels that introduces them.

## ADDED Requirements

### Requirement: Level lists one-way valves
The system SHALL define `valves` as an optional array of at most 8 entries, each with an `x`, a `y` and a `direction`. `direction` SHALL be `LEFT`, `RIGHT`, `UP` or `DOWN`: the way the liquid must flow through the valve. An absent or empty array SHALL mean the level has no valves. The parsed level SHALL keep the entries in authored order.

#### Scenario: Level without valves
- **WHEN** a level has no `valves` field
- **THEN** the level is accepted with no valves

#### Scenario: Valid valves
- **WHEN** a level on a 7 by 9 grid lists a valve at x 3, y 4 with direction `RIGHT` and one at x 5, y 2 with direction `UP`
- **THEN** the level is accepted with those two valves in that order

#### Scenario: Missing direction
- **WHEN** a valve entry omits `direction`
- **THEN** parsing fails with an error that names `valves[0].direction`

#### Scenario: Unknown direction
- **WHEN** a valve entry has the direction `TOP`
- **THEN** parsing fails with an error that names the invalid value

#### Scenario: Too many valves
- **WHEN** a level lists 9 valves
- **THEN** validation fails with an error that names `valves`

### Requirement: Valves lie on free cells
Every valve SHALL lie inside the grid, SHALL NOT lie on a blocked cell, the start, the finish or a mandatory section, and SHALL NOT share its cell with another valve.

#### Scenario: Valve outside the grid
- **WHEN** a valve has a `y` that is at least the row count
- **THEN** validation fails with an error that names the valve entry and its coordinates

#### Scenario: Valve on a mandatory section
- **WHEN** a valve has the same coordinates as a mandatory section
- **THEN** validation fails with an error that names the valve entry and states that the cell holds a mandatory section

#### Scenario: Two valves on one cell
- **WHEN** two valve entries have the same coordinates
- **THEN** validation fails with an error that names the second valve entry

### Requirement: A valve can be passed
A valve's inlet neighbour and outlet neighbour SHALL each be able to carry the route: a cell inside the grid that is not blocked, or the start when the start faces the valve's inlet, or the finish when the finish faces the valve's outlet. The inlet is the side opposite the valve's direction and the outlet is the side it points to.

#### Scenario: Outlet against the grid edge
- **WHEN** a valve with direction `RIGHT` lies in the last column
- **THEN** validation fails with an error that names the valve entry and states that it can never be passed

#### Scenario: Valve fed by the start
- **WHEN** the start at x 0, y 2 opens to the right and a valve with direction `RIGHT` lies at x 1, y 2
- **THEN** the valve is accepted

#### Scenario: Valve facing the start the wrong way
- **WHEN** the start at x 0, y 2 opens to the right and a valve with direction `LEFT` lies at x 1, y 2
- **THEN** validation fails with an error that names the valve entry

### Requirement: Valves are on the board from the start
At the start of every attempt, and after a restart, each valve SHALL be on its cell without liquid. A valve with direction `LEFT` or `RIGHT` SHALL be a horizontal straight and one with direction `UP` or `DOWN` a vertical straight. Valves SHALL NOT be dealt from the stack and SHALL NOT change the stack.

#### Scenario: Valve on the board
- **WHEN** a level with a valve at x 3, y 2 with direction `DOWN` starts
- **THEN** that cell holds a vertical valve that points down and has no liquid

#### Scenario: Stack is unchanged
- **WHEN** a level with valves starts
- **THEN** the stack is dealt from the level's stack weights exactly as it would be without the valves

### Requirement: Liquid passes a valve in its direction
Liquid that enters a valve through its inlet SHALL leave it through its outlet, and the valve SHALL score as a straight section.

#### Scenario: Passing a valve
- **WHEN** the liquid leaves a tile to the right into a valve with direction `RIGHT`
- **THEN** the liquid enters the valve, leaves it to the right and 10 points are awarded

### Requirement: Liquid against a valve leaks
Liquid that reaches a valve through any side other than its inlet SHALL NOT enter it. The liquid SHALL leak and the level SHALL be lost as for any other leak, at the tile the liquid left and the side it left through.

#### Scenario: Against the direction
- **WHEN** the liquid leaves a tile to the left into a valve with direction `RIGHT`
- **THEN** the liquid leaks at the tile it left through its left side and the level is lost

#### Scenario: From the side
- **WHEN** the liquid leaves a tile downwards into a valve with direction `RIGHT`
- **THEN** the liquid leaks and the level is lost

### Requirement: Valves are never replaced or removed
The player SHALL NOT be able to replace, remove or move a valve, whatever `rules.allowReplace` says. When the unused placed sections are removed after a win, every valve SHALL stay on the board.

#### Scenario: Drop on a valve
- **WHEN** the player drags a section onto a valve the liquid has not entered, in a level that allows replacing
- **THEN** the tile is not highlighted, the drop is rejected and the valve stays

#### Scenario: Valves stay after a win
- **WHEN** a level with an unused valve is won and the unused placed sections are removed
- **THEN** the valve stays on the board

### Requirement: The route does not have to pass a valve
A level SHALL be won when the liquid reaches the finish without passing a valve, as long as every mandatory section is used.

#### Scenario: Route around a valve
- **WHEN** the liquid reaches the finish through its open side without having entered the level's only valve
- **THEN** the level is won

### Requirement: Valves are described for accessibility
The accessibility description of a valve cell SHALL say that it is a one-way valve, the direction the liquid flows through it, and whether the liquid has entered it.

#### Scenario: Description of a dry valve
- **WHEN** a screen reader focuses a valve with direction `UP` that the liquid has not entered
- **THEN** the description says that it is a one-way valve that flows up

#### Scenario: Description of a filled valve
- **WHEN** a screen reader focuses a valve the liquid has passed
- **THEN** the description says that it is a one-way valve, its direction and that it is filled

### Requirement: A chapter of levels introduces valves
The app SHALL bundle a chapter of 24 new levels, one full level library page, that introduces valves. Its first level SHALL have at least one valve, and every level of the chapter SHALL pass the solver check. The chapter's page SHALL unlock by the existing page star rule.

#### Scenario: Chapter is one page
- **WHEN** the level library is opened on the valve chapter's page
- **THEN** it shows 24 levels and every one of them has at least one valve

#### Scenario: Chapter levels are solvable
- **WHEN** the headless solvability suite runs over the bundled levels
- **THEN** every level of the valve chapter is reported as solvable

### Requirement: Existing levels and progress are unaffected
Levels 1 to 96 SHALL keep their files, ids, routes, scores and star thresholds. Saved progress, best scores, stars and seen tips SHALL be kept when the app is updated to a version with valves.

#### Scenario: Existing level unchanged
- **WHEN** level 1 is parsed by a version with valves
- **THEN** it has no valves and equals the level parsed before this change

#### Scenario: Progress kept on update
- **WHEN** a player who has finished levels 1 to 96 updates the app
- **THEN** their stars and best scores for those levels are unchanged and the valve chapter's page is open when the previous page has enough stars
