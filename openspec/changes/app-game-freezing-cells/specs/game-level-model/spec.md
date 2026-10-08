# Spec Delta

## MODIFIED Requirements

### Requirement: Level is described by a JSON document
The system SHALL read a level from a JSON document. The fields `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow.initialDelayMs`, `flow.cellsPerSecond` and `stars` SHALL be required. The fields `flow.reservoirSpeedFactor`, `flow.speedRampPerCell`, `stack`, `rules`, `mandatory` and `freezing`, and every field inside `stack` and `rules`, SHALL be optional.

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

#### Scenario: Existing level without freezing cells
- **WHEN** a bundled level from 1 to 96, which has no `freezing` field, is parsed
- **THEN** the level is accepted with no freezing cells and is otherwise unchanged

## ADDED Requirements

### Requirement: Level lists freezing cells
The system SHALL define `freezing` as an array of cells, each with an `x` and a `y`. An absent or empty array SHALL mean the level has no freezing cells. The parsed level SHALL keep the cells in authored order.

#### Scenario: Level without freezing cells
- **WHEN** a level has no `freezing` field
- **THEN** the level is accepted with no freezing cells

#### Scenario: Valid freezing cells
- **WHEN** a level on a 7 by 9 grid lists freezing cells at x 3, y 4 and x 5, y 2
- **THEN** the level is accepted and reports both cells as freezing

#### Scenario: Missing coordinate
- **WHEN** a freezing entry omits `y`
- **THEN** parsing fails with an error that names `freezing[0].y`

#### Scenario: Unknown key in an entry
- **WHEN** a freezing entry contains a key other than `x` and `y`
- **THEN** parsing fails with an error that names the unknown key

### Requirement: Freezing cells lie on free cells
Every freezing cell SHALL lie inside the grid, SHALL NOT lie on a blocked cell, the start or the finish, and SHALL NOT appear more than once. A freezing cell MAY hold a mandatory reservoir. A freezing cell SHALL NOT hold a mandatory cross, because the liquid could never pass both of its channels.

#### Scenario: Freezing cell outside the grid
- **WHEN** a freezing cell has a `y` that is at least the row count
- **THEN** validation fails with an error that names the freezing entry and its coordinates

#### Scenario: Freezing cell on a blocked cell
- **WHEN** a freezing cell has the same coordinates as a blocked cell
- **THEN** validation fails with an error that names the freezing entry and states that the cell is blocked

#### Scenario: Freezing cell on the start or finish
- **WHEN** a freezing cell has the same coordinates as the start or the finish
- **THEN** validation fails with an error that names the freezing entry and states that the start and finish cannot freeze

#### Scenario: Duplicate freezing cell
- **WHEN** the same cell appears twice in `freezing`
- **THEN** validation fails with an error that names the duplicated cell

#### Scenario: Mandatory cross on a freezing cell
- **WHEN** a level has a mandatory `X` at x 3, y 4 and a freezing cell at x 3, y 4
- **THEN** validation fails with an error that names the freezing entry and states that a mandatory cross cannot lie on a freezing cell

#### Scenario: Mandatory reservoir on a freezing cell
- **WHEN** a level has a mandatory `RES` at x 5, y 2 and a freezing cell at x 5, y 2
- **THEN** the level is accepted
