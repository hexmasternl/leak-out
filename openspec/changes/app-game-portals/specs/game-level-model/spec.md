## MODIFIED Requirements

### Requirement: Level is described by a JSON document
The system SHALL read a level from a JSON document. The fields `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow.initialDelayMs`, `flow.cellsPerSecond` and `stars` SHALL be required. The fields `flow.reservoirSpeedFactor`, `flow.speedRampPerCell`, `stack`, `rules`, `mandatory` and `portals`, and every field inside `stack` and `rules`, SHALL be optional.

#### Scenario: Complete level is parsed
- **WHEN** a JSON document containing every field with valid values is parsed
- **THEN** the system produces a level whose fields equal the values in the document

#### Scenario: Missing required field is reported
- **WHEN** a JSON document omits a required field such as `flow.cellsPerSecond`
- **THEN** parsing fails with an error that names the missing field

#### Scenario: Unknown field is rejected
- **WHEN** a JSON document contains a key that is not part of the level format
- **THEN** parsing fails with an error that names the unknown key

#### Scenario: Malformed JSON is rejected
- **WHEN** the input is not valid JSON
- **THEN** parsing fails with an error and no level is produced

#### Scenario: Level with portals is parsed
- **WHEN** a JSON document with a `portals` array holding one valid pair is parsed
- **THEN** the system produces a level with that portal pair

### Requirement: Start and finish are placed so that they can be connected
The system SHALL accept a start and a finish only when all of the following hold. Each lies inside the grid. They are different cells. Neither lies on a blocked cell or a portal. Each facing cell lies inside the grid, is not blocked and is not a portal. The start's facing cell is not the finish, and the finish's facing cell is not the start. The start and the finish MAY share a facing cell.

#### Scenario: Position outside the grid
- **WHEN** the start or the finish has an `x` that is negative or at least the column count, or a `y` that is negative or at least the row count
- **THEN** validation fails with an error that names the start or finish and the offending coordinate

#### Scenario: Start and finish on the same cell
- **WHEN** the start and the finish have the same `x` and `y`
- **THEN** validation fails with an error stating that start and finish must be different cells

#### Scenario: Start opens onto the grid edge
- **WHEN** the start is at x 0 with open side `LEFT`
- **THEN** validation fails with an error stating that the start's open side faces the edge of the grid

#### Scenario: Finish opens onto the grid edge
- **WHEN** the finish is in the last row with open side `BOTTOM`
- **THEN** validation fails with an error stating that the finish's open side faces the edge of the grid

#### Scenario: Facing cell is blocked
- **WHEN** the facing cell of the start or of the finish is a blocked cell
- **THEN** validation fails with an error stating that the start or finish is walled off

#### Scenario: Start and finish face each other
- **WHEN** the start is at x 1, y 1 with open side `RIGHT` and the finish is at x 2, y 1 with open side `LEFT`
- **THEN** validation fails with an error stating that the start and finish must not face each other directly

#### Scenario: Start faces the closed side of the finish
- **WHEN** the start is at x 1, y 1 with open side `RIGHT` and the finish is at x 2, y 1 with open side `TOP`
- **THEN** validation fails with an error stating that the start's facing cell must not be the finish

#### Scenario: Shared facing cell
- **WHEN** the start is at x 1, y 1 with open side `RIGHT` and the finish is at x 2, y 2 with open side `TOP`, so that both face x 2, y 1
- **THEN** the level is accepted

#### Scenario: Start faces a portal
- **WHEN** the start's facing cell holds a portal
- **THEN** validation fails with an error stating that the start's facing cell must not be a portal

### Requirement: Level lists blocked cells
The system SHALL define `blocked` as an array of cells, each with an `x` and a `y`, on which no pipe section can be placed. An empty array SHALL mean the level has no blocked cells. Every blocked cell SHALL lie inside the grid, SHALL NOT coincide with the start, the finish or a portal, and SHALL NOT appear more than once.

#### Scenario: Level without blocked cells
- **WHEN** a level has an empty `blocked` array
- **THEN** the level is accepted with no blocked cells

#### Scenario: Valid blocked cells
- **WHEN** a level on a 7 by 9 grid has a blocked cell at x 3, y 4
- **THEN** the level is accepted and reports that cell as blocked

#### Scenario: Blocked cell outside the grid
- **WHEN** a blocked cell lies outside the grid
- **THEN** validation fails with an error that names the blocked cell and its coordinates

#### Scenario: Blocked cell on start or finish
- **WHEN** a blocked cell has the same coordinates as the start or the finish
- **THEN** validation fails with an error stating that the start and finish cannot be blocked

#### Scenario: Duplicate blocked cell
- **WHEN** the same cell appears twice in `blocked`
- **THEN** validation fails with an error that names the duplicated cell

#### Scenario: Blocked cell on a portal
- **WHEN** a blocked cell has the same coordinates as a portal
- **THEN** validation fails with an error that names the portal and states that the cell is blocked

### Requirement: Mandatory sections lie on free cells
Every mandatory section SHALL lie inside the grid, SHALL NOT lie on a blocked cell, the start, the finish or a portal, and SHALL NOT share its cell with another mandatory section.

#### Scenario: Mandatory section outside the grid
- **WHEN** a mandatory section has an `x` that is at least the column count
- **THEN** validation fails with an error that names the mandatory entry and its coordinates

#### Scenario: Mandatory section on a blocked cell
- **WHEN** a mandatory section has the same coordinates as a blocked cell
- **THEN** validation fails with an error that names the mandatory entry and states that the cell is blocked

#### Scenario: Mandatory section on the start or finish
- **WHEN** a mandatory section has the same coordinates as the start or the finish
- **THEN** validation fails with an error that names the mandatory entry and states that the start and finish cannot hold a section

#### Scenario: Two mandatory sections on one cell
- **WHEN** two mandatory entries have the same coordinates
- **THEN** validation fails with an error that names the duplicated cell

#### Scenario: Mandatory section on a portal
- **WHEN** a mandatory section has the same coordinates as a portal
- **THEN** validation fails with an error that names the portal and states that the cell holds a mandatory section
