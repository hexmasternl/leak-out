# Spec Delta

## MODIFIED Requirements

### Requirement: Level is described by a JSON document
The system SHALL read a level from a JSON document. The fields `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow.initialDelayMs`, `flow.cellsPerSecond` and `stars` SHALL be required. The fields `flow.reservoirSpeedFactor`, `flow.speedRampPerCell`, `stack`, `rules`, `mandatory`, `secondFinish` and `splitter`, and every field inside `stack` and `rules`, SHALL be optional.

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

#### Scenario: Level without a splitter
- **WHEN** a level has no `secondFinish` and no `splitter` field
- **THEN** the level is accepted with one finish and no splitter, exactly as before

## ADDED Requirements

### Requirement: Level can have a second finish
The system SHALL define an optional `secondFinish` with an `x`, a `y` and an `openSide`, read like `finish`. It SHALL follow every rule for placing a finish: inside the grid, not blocked, its facing cell inside the grid and not blocked, not the start's cell or the start's facing cell, and the start's facing cell is not it. It SHALL NOT share its cell with `finish`. It MAY share a facing cell with `finish` or the start.

#### Scenario: Valid second finish
- **WHEN** a level on a 9 by 7 grid has a start at x 0, y 3 open to the right, a finish at x 8, y 1 open to the left and a second finish at x 8, y 5 open to the left
- **THEN** the level is accepted with two finishes

#### Scenario: Second finish on the first finish
- **WHEN** `secondFinish` has the same coordinates as `finish`
- **THEN** validation fails with an error that names `secondFinish` and states that the finishes must be different cells

#### Scenario: Second finish opens onto the grid edge
- **WHEN** `secondFinish` is in the last row with open side `BOTTOM`
- **THEN** validation fails with an error that names `secondFinish` and states that its open side faces the edge of the grid

### Requirement: Level can have one splitter
The system SHALL define an optional `splitter` with an `x`, a `y` and a `closedSide` of `LEFT`, `RIGHT`, `TOP` or `BOTTOM`. The splitter SHALL lie inside the grid and SHALL NOT lie on a blocked cell, the start, a finish or a mandatory section. Each of its three open sides SHALL face a cell inside the grid that is not blocked. The splitter's section ids SHALL NOT be stack weight keys.

#### Scenario: Valid splitter
- **WHEN** a level has a `splitter` at x 4, y 3 with `closedSide` `LEFT` and a valid `secondFinish`
- **THEN** the level is accepted with that splitter

#### Scenario: Unknown closed side
- **WHEN** a `splitter` has a `closedSide` that is not `LEFT`, `RIGHT`, `TOP` or `BOTTOM`
- **THEN** parsing fails with an error that names `splitter.closedSide`

#### Scenario: Splitter on a blocked cell
- **WHEN** the `splitter` has the same coordinates as a blocked cell
- **THEN** validation fails with an error that names `splitter` and states that the cell is blocked

#### Scenario: Open side faces the grid edge
- **WHEN** a splitter at x 0 has `closedSide` `RIGHT`
- **THEN** validation fails with an error that names `splitter` and states that an open side faces the edge of the grid

#### Scenario: Splitter weight in the stack
- **WHEN** `stack.weights` contains a splitter section id
- **THEN** parsing fails with an error that names the unknown key

### Requirement: A splitter and a second finish come together
The system SHALL accept a `splitter` only together with a `secondFinish`, and a `secondFinish` only together with a `splitter`.

#### Scenario: Second finish without a splitter
- **WHEN** a level has a `secondFinish` and no `splitter`
- **THEN** validation fails with an error that names `secondFinish` and states that a second finish needs a splitter

#### Scenario: Splitter without a second finish
- **WHEN** a level has a `splitter` and no `secondFinish`
- **THEN** validation fails with an error that names `splitter` and states that a splitter needs a second finish
