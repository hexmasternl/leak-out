# Spec Delta

## MODIFIED Requirements

### Requirement: Level is described by a JSON document
The system SHALL read a level from a JSON document. The fields `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow.initialDelayMs`, `flow.cellsPerSecond` and `stars` SHALL be required. The fields `flow.reservoirSpeedFactor`, `flow.speedRampPerCell`, `stack`, `rules`, `mandatory` and `fixed`, and every field inside `stack` and `rules`, SHALL be optional.

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

#### Scenario: Level without fixed pipes
- **WHEN** a JSON document has no `fixed` field
- **THEN** the level is parsed with no fixed pipes

## ADDED Requirements

### Requirement: Level lists fixed pipes
The system SHALL define `fixed` as an array of entries, each with an `x`, a `y` and a `section`, where `section` is any of the section ids `H`, `V`, `LT`, `LB`, `RT`, `RB`, `X` and `RES`. An absent or empty array SHALL mean the level has no fixed pipes. The parsed level SHALL keep the entries in authored order. A fixed pipe's `section` SHALL NOT depend on its stack weight.

#### Scenario: Valid fixed pipes
- **WHEN** a level on a 7 by 9 grid lists a fixed `LT` at x 3, y 4 and a fixed `RES` at x 5, y 2, and its stack weight for `RES` is 0
- **THEN** the level is accepted with those two fixed pipes in that order

#### Scenario: Missing field in an entry
- **WHEN** a fixed entry omits `section`
- **THEN** parsing fails with an error that names `fixed[0].section`

#### Scenario: Unknown section id
- **WHEN** a fixed entry has a `section` that is not a section id, such as `Z`
- **THEN** parsing fails with an error that names the invalid value

#### Scenario: Unknown key in an entry
- **WHEN** a fixed entry contains a key other than `x`, `y` and `section`
- **THEN** parsing fails with an error that names the unknown key

### Requirement: Fixed pipes lie on free cells
Every fixed pipe SHALL lie inside the grid, and SHALL NOT lie on a blocked cell, the start, the finish or a mandatory section's cell. No two fixed pipes SHALL share a cell. Each problem SHALL be reported with the name of the fixed entry, together with every other problem in the level.

#### Scenario: Fixed pipe outside the grid
- **WHEN** a fixed pipe has a `y` that is at least the row count
- **THEN** validation fails with an error that names the fixed entry and its coordinates

#### Scenario: Fixed pipe on a blocked cell
- **WHEN** a fixed pipe has the same coordinates as a blocked cell
- **THEN** validation fails with an error that names the fixed entry and states that the cell is blocked

#### Scenario: Fixed pipe on the start or finish
- **WHEN** a fixed pipe has the same coordinates as the start or the finish
- **THEN** validation fails with an error that names the fixed entry and states that the start and finish cannot hold a section

#### Scenario: Fixed pipe on a mandatory section
- **WHEN** a fixed pipe has the same coordinates as a mandatory section
- **THEN** validation fails with an error that names the fixed entry and states that the cell already holds a mandatory section

#### Scenario: Two fixed pipes on one cell
- **WHEN** two fixed entries have the same coordinates
- **THEN** validation fails with an error that names the duplicated cell

#### Scenario: Fixed pipe on the start's facing cell
- **WHEN** a fixed pipe lies on the cell the start's open side faces
- **THEN** the level format accepts it, and whether a route exists is left to the solver
