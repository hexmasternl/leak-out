## MODIFIED Requirements

### Requirement: Level is described by a JSON document
The system SHALL read a level from a JSON document. The fields `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow.initialDelayMs`, `flow.cellsPerSecond` and `stars` SHALL be required. The fields `flow.reservoirSpeedFactor`, `flow.speedRampPerCell`, `stack`, `rules`, `mandatory`, `portals` and `leaky`, and every field inside `stack` and `rules`, SHALL be optional.

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

#### Scenario: Level without leaky tiles
- **WHEN** a JSON document has no `leaky` field
- **THEN** the parsed level has no leaky tiles

#### Scenario: Level with portals is parsed
- **WHEN** a JSON document with a `portals` array holding one valid pair is parsed
- **THEN** the system produces a level with that portal pair

## ADDED Requirements

### Requirement: Level lists leaky tiles
The system SHALL define `leaky` as an array of cells, each with an `x` and a `y`, on which a section can be placed but which drain the liquid that passes. An absent or empty array SHALL mean the level has no leaky tiles. The parsed level SHALL keep the cells in authored order.

#### Scenario: Valid leaky tiles
- **WHEN** a level on a 7 by 9 grid lists leaky tiles at x 3, y 4 and x 5, y 2
- **THEN** the level is accepted and reports both cells as leaky and every other cell as not leaky

#### Scenario: Missing coordinate
- **WHEN** a leaky entry omits `y`
- **THEN** parsing fails with an error that names `leaky[0].y`

#### Scenario: Unknown key in an entry
- **WHEN** a leaky entry contains a key other than `x` and `y`
- **THEN** parsing fails with an error that names the unknown key

### Requirement: Leaky tiles lie on free cells
Every leaky tile SHALL lie inside the grid, SHALL NOT lie on a blocked cell, the start, the finish, a mandatory section or a portal, and SHALL NOT appear more than once. Every problem SHALL be reported with the `leaky[i]` entry it concerns.

#### Scenario: Leaky tile outside the grid
- **WHEN** a leaky tile has a `y` that is at least the row count
- **THEN** validation fails with an error that names the leaky entry and its coordinates

#### Scenario: Leaky tile on a blocked cell
- **WHEN** a leaky tile has the same coordinates as a blocked cell
- **THEN** validation fails with an error that names the leaky entry and states that the cell is blocked

#### Scenario: Leaky tile on the start or finish
- **WHEN** a leaky tile has the same coordinates as the start or the finish
- **THEN** validation fails with an error that names the leaky entry and states that the start and finish cannot be leaky

#### Scenario: Leaky tile under a mandatory section
- **WHEN** a leaky tile has the same coordinates as a mandatory section
- **THEN** validation fails with an error that names the leaky entry and states that a mandatory section cannot lie on a leaky tile

#### Scenario: Leaky tile on a portal
- **WHEN** a leaky tile has the same coordinates as a portal
- **THEN** validation fails with an error that names the leaky entry and states that a portal cannot lie on a leaky tile

#### Scenario: Duplicate leaky tile
- **WHEN** the same cell appears twice in `leaky`
- **THEN** validation fails with an error that names the duplicated cell
