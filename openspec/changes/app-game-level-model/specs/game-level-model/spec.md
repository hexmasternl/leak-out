## ADDED Requirements

### Requirement: Level is described by a JSON document
The system SHALL read a level from a JSON document containing an id, a grid size, a start, a finish, an initial delay, a flow speed, allowed pipes, blocked cells and star benchmarks. All of these fields SHALL be required.

#### Scenario: Complete level is parsed
- **WHEN** a JSON document containing every required field with valid values is parsed
- **THEN** the system produces a level whose fields equal the values in the document

#### Scenario: Missing field is reported
- **WHEN** a JSON document omits a required field such as `flowSpeed`
- **THEN** parsing fails with an error that names the missing field

#### Scenario: Unknown field is rejected
- **WHEN** a JSON document contains a key that is not part of the level format
- **THEN** parsing fails with an error that names the unknown key

#### Scenario: Malformed JSON is rejected
- **WHEN** the input is not valid JSON
- **THEN** parsing fails with an error and no level is produced

### Requirement: Level has a unique numeric id
The system SHALL identify each level by a positive integer id. The system SHALL reject a set of levels in which two levels share an id.

#### Scenario: Valid id
- **WHEN** a level has `id` 3
- **THEN** the level is accepted with id 3

#### Scenario: Non-positive id
- **WHEN** a level has an `id` of 0 or a negative number
- **THEN** validation fails with an error that names `id`

#### Scenario: Duplicate ids
- **WHEN** two levels in a set both have `id` 2
- **THEN** validation of the set fails with an error that names the duplicated id

### Requirement: Level defines a grid size
The system SHALL define the grid of a level with a positive integer number of `columns` and a positive integer number of `rows`.

#### Scenario: Valid grid
- **WHEN** a level has 7 columns and 9 rows
- **THEN** the level is accepted with a 7 by 9 grid

#### Scenario: Zero or negative dimension
- **WHEN** a level has 0 columns or a negative number of rows
- **THEN** validation fails with an error that names the offending dimension

### Requirement: Start and finish positions have coordinates and a direction
The system SHALL define the start and the finish each with an `x` column, a `y` row and a `direction`. Coordinates SHALL be zero-based with the origin at the top-left cell and `y` increasing downward. A direction SHALL be one of `LEFT`, `RIGHT`, `TOP` or `BOTTOM`. For the start, the direction is the side through which liquid leaves. For the finish, the direction is the side through which liquid must enter.

#### Scenario: Valid start and finish
- **WHEN** a level on a 7 by 9 grid has a start at x 0, y 1, direction `RIGHT` and a finish at x 6, y 7, direction `LEFT`
- **THEN** the level is accepted

#### Scenario: Position outside the grid
- **WHEN** the start or the finish has an `x` that is negative or at least the column count, or a `y` that is negative or at least the row count
- **THEN** validation fails with an error that names the start or finish and the offending coordinate

#### Scenario: Unknown direction
- **WHEN** a start or finish has a direction value that is not `LEFT`, `RIGHT`, `TOP` or `BOTTOM`
- **THEN** parsing fails with an error that names the invalid value

#### Scenario: Start and finish on the same cell
- **WHEN** the start and the finish have the same `x` and `y`
- **THEN** validation fails with an error stating that start and finish must be different cells

#### Scenario: Start faces outside the grid
- **WHEN** the start direction points to a neighbouring cell that lies outside the grid
- **THEN** validation fails with an error stating that the start exits the grid

#### Scenario: Finish faces outside the grid
- **WHEN** the finish direction points to a neighbouring cell that lies outside the grid
- **THEN** validation fails with an error stating that the finish cannot be reached from outside the grid

### Requirement: Level has an initial delay
The system SHALL define an `initialDelayMs`, an integer number of milliseconds, that is the time between the start of the level and the moment liquid begins to flow from the start position. The value SHALL be zero or greater.

#### Scenario: Valid delay
- **WHEN** a level has `initialDelayMs` 12000
- **THEN** the level is accepted with a 12000 millisecond initial delay

#### Scenario: Zero delay
- **WHEN** a level has `initialDelayMs` 0
- **THEN** the level is accepted and liquid begins to flow immediately

#### Scenario: Negative delay
- **WHEN** a level has a negative `initialDelayMs`
- **THEN** validation fails with an error that names `initialDelayMs`

### Requirement: Level has a flow speed between zero and one
The system SHALL define a `flowSpeed` as a floating point number greater than 0 and at most 1. A higher value SHALL mean faster liquid flow. The system SHALL store the value as authored and SHALL NOT convert it to a physical unit.

#### Scenario: Valid flow speed
- **WHEN** a level has `flowSpeed` 0.4
- **THEN** the level is accepted with flow speed 0.4

#### Scenario: Upper bound is inclusive
- **WHEN** a level has `flowSpeed` 1
- **THEN** the level is accepted

#### Scenario: Zero flow speed
- **WHEN** a level has `flowSpeed` 0
- **THEN** validation fails with an error that names `flowSpeed`

#### Scenario: Flow speed above one
- **WHEN** a level has `flowSpeed` 1.5
- **THEN** validation fails with an error that names `flowSpeed`

#### Scenario: Negative flow speed
- **WHEN** a level has a negative `flowSpeed`
- **THEN** validation fails with an error that names `flowSpeed`

### Requirement: Level declares which optional pipe sections are allowed
The system SHALL define `allowedPipes` with two required boolean values, `cross` and `reservoir`, that state whether the cross section and the reservoir section may appear on the pipe stack of the level. Straight and corner sections SHALL always be allowed and SHALL NOT be configurable.

#### Scenario: Both optional sections allowed
- **WHEN** a level has `allowedPipes` with `cross` true and `reservoir` true
- **THEN** the level reports both sections as allowed

#### Scenario: Both optional sections disallowed
- **WHEN** a level has `allowedPipes` with `cross` false and `reservoir` false
- **THEN** the level reports both sections as not allowed and straight and corner sections remain allowed

#### Scenario: Missing pipe flag
- **WHEN** `allowedPipes` omits `reservoir`
- **THEN** parsing fails with an error that names `allowedPipes.reservoir`

#### Scenario: Non-boolean pipe flag
- **WHEN** `allowedPipes.cross` is a string or number instead of a boolean
- **THEN** parsing fails with an error that names `allowedPipes.cross`

### Requirement: Level lists blocked cells
The system SHALL define `blocked` as an array of cells, each with an `x` and a `y`, on which no pipe section can be placed. An empty array SHALL mean the level has no blocked cells. Every blocked cell SHALL lie inside the grid, SHALL NOT coincide with the start or the finish, and SHALL NOT appear more than once.

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

#### Scenario: Liquid path blocked at the ends
- **WHEN** the cell that the start exits into, or the cell that the finish is entered from, is a blocked cell
- **THEN** validation fails with an error stating that the start or finish is walled off

### Requirement: Level defines star benchmarks
The system SHALL define `stars` with three point thresholds, `one`, `two` and `three`, that are the minimum points needed to earn one, two and three stars on successful completion. Each threshold SHALL be a positive integer, and the thresholds SHALL be strictly increasing.

#### Scenario: Valid benchmarks
- **WHEN** a level has `stars` with `one` 100, `two` 200 and `three` 350
- **THEN** the level is accepted with those thresholds

#### Scenario: Thresholds not increasing
- **WHEN** `stars.two` is less than or equal to `stars.one`, or `stars.three` is less than or equal to `stars.two`
- **THEN** validation fails with an error stating that star thresholds must be strictly increasing

#### Scenario: Non-positive threshold
- **WHEN** any star threshold is 0 or negative
- **THEN** validation fails with an error that names the threshold

#### Scenario: Missing threshold
- **WHEN** `stars` omits `three`
- **THEN** parsing fails with an error that names `stars.three`

### Requirement: Stars are derived from points
The system SHALL provide the number of stars earned for a given point total: 0 when the total is below `stars.one`, 1 when it is at least `stars.one` and below `stars.two`, 2 when it is at least `stars.two` and below `stars.three`, and 3 when it is at least `stars.three`.

#### Scenario: Below the first threshold
- **WHEN** the thresholds are 100, 200 and 350 and the points are 99
- **THEN** the stars earned are 0

#### Scenario: Exactly on a threshold
- **WHEN** the thresholds are 100, 200 and 350 and the points are 200
- **THEN** the stars earned are 2

#### Scenario: Above the last threshold
- **WHEN** the thresholds are 100, 200 and 350 and the points are 1000
- **THEN** the stars earned are 3

### Requirement: All problems in a level are reported together
The system SHALL report every validation problem found in a level in one result, each with the name of the field it concerns, rather than stopping at the first problem.

#### Scenario: Several problems at once
- **WHEN** a level has a `flowSpeed` of 2 and a blocked cell outside the grid
- **THEN** the validation result contains an error for `flowSpeed` and an error for the blocked cell
