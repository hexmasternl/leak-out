# Spec Delta

## MODIFIED Requirements

### Requirement: Level is described by a JSON document
The system SHALL read a level from a JSON document. The fields `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow.initialDelayMs`, `flow.cellsPerSecond` and `stars` SHALL be required. The fields `flow.reservoirSpeedFactor`, `flow.speedRampPerCell`, `stack`, `rules` and `mandatory`, and every field inside `stack` and `rules`, SHALL be optional.

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

## ADDED Requirements

### Requirement: Level lists mandatory sections
The system SHALL define `mandatory` as an array of at most 6 entries, each with an `x`, a `y` and a `section`. An absent or empty array SHALL mean the level has no mandatory sections. The parsed level SHALL keep the entries in authored order. A mandatory section's `section` SHALL NOT depend on its stack weight, so a level MAY list a mandatory cross while `stack.weights.X` is 0.

#### Scenario: Level without mandatory sections
- **WHEN** a level has no `mandatory` field
- **THEN** the level is accepted with no mandatory sections

#### Scenario: Valid mandatory sections
- **WHEN** a level on a 7 by 9 grid lists a mandatory `X` at x 3, y 4 and a mandatory `RES` at x 5, y 2, and its stack weights for `X` and `RES` are 0
- **THEN** the level is accepted with those two mandatory sections

#### Scenario: Missing field in an entry
- **WHEN** a mandatory entry omits `section`
- **THEN** parsing fails with an error that names `mandatory[0].section`

#### Scenario: Unknown section id
- **WHEN** a mandatory entry has a `section` that is not a section id, such as `Z`
- **THEN** parsing fails with an error that names the invalid value

#### Scenario: Too many mandatory sections
- **WHEN** a level lists 7 mandatory sections
- **THEN** validation fails with an error that names `mandatory`

### Requirement: Only a cross or a reservoir can be mandatory
The system SHALL accept a mandatory entry only when its `section` is `X` or `RES`.

#### Scenario: Mandatory straight
- **WHEN** the second mandatory entry has `section` `H`
- **THEN** validation fails with an error that names `mandatory[1].section` and states that only `X` and `RES` can be mandatory

#### Scenario: Mandatory reservoir
- **WHEN** a mandatory entry has `section` `RES`
- **THEN** the entry is accepted

### Requirement: Mandatory sections lie on free cells
Every mandatory section SHALL lie inside the grid, SHALL NOT lie on a blocked cell, the start or the finish, and SHALL NOT share its cell with another mandatory section.

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
