# game-level-model Specification

## Purpose
TBD - created by archiving change app-game-level-model. Update Purpose after archive.
## Requirements
### Requirement: Level is described by a JSON document
The system SHALL read a level from a JSON document. The fields `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow.initialDelayMs`, `flow.cellsPerSecond` and `stars` SHALL be required. The fields `flow.reservoirSpeedFactor`, `flow.speedRampPerCell`, `stack` and `rules`, and every field inside `stack` and `rules`, SHALL be optional.

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

### Requirement: Engine settings have defaults that a level can override
The system SHALL keep one central set of defaults: `flow.reservoirSpeedFactor` 0.3, `flow.speedRampPerCell` 0.0, `stack.visibleSize` 5, stack weights `H` 3, `V` 3, `LT` 2, `LB` 2, `RT` 2, `RB` 2, `X` 0 and `RES` 0, `rules.allowReplace` true and `rules.replacePenaltyMs` 500. When a level leaves out an optional field, the parsed level SHALL hold the default value for it. When a level provides an optional field, the parsed level SHALL hold the provided value, and all other optional fields SHALL keep their defaults.

#### Scenario: Minimal level gets all defaults
- **WHEN** a level contains only the required fields
- **THEN** the parsed level has a reservoir speed factor of 0.3, a speed ramp of 0.0, a visible stack size of 5, the default stack weights, replacing allowed and a replace penalty of 500 milliseconds

#### Scenario: Single field is overridden
- **WHEN** a level provides `flow.reservoirSpeedFactor` 0.5 and no other optional field
- **THEN** the parsed level has a reservoir speed factor of 0.5 and every other optional field at its default

#### Scenario: Single stack weight is overridden
- **WHEN** a level provides `stack.weights` containing only `X` 1
- **THEN** the parsed level has a cross weight of 1 and every other weight at its default

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

### Requirement: Level has a display name
The system SHALL define a `name` for each level: a string that is not empty after trimming. The parsed level SHALL hold the trimmed name. Names do not need to be unique.

#### Scenario: Valid name
- **WHEN** a level has `name` "First Crossing"
- **THEN** the level is accepted with the name "First Crossing"

#### Scenario: Surrounding whitespace is trimmed
- **WHEN** a level has `name` "  First Crossing "
- **THEN** the level is accepted with the name "First Crossing"

#### Scenario: Blank name
- **WHEN** a level has a `name` that is empty or contains only whitespace
- **THEN** validation fails with an error that names `name`

### Requirement: Level defines a grid size between 4 by 4 and 50 by 50
The system SHALL define the grid of a level with an integer number of `columns` and an integer number of `rows`, each between 4 and 50 inclusive.

#### Scenario: Valid grid
- **WHEN** a level has 7 columns and 9 rows
- **THEN** the level is accepted with a 7 by 9 grid

#### Scenario: Smallest and largest grids
- **WHEN** a level has a 4 by 4 grid, or a 50 by 50 grid
- **THEN** the level is accepted

#### Scenario: Grid too small
- **WHEN** a level has 3 columns or 3 rows
- **THEN** validation fails with an error that names the offending dimension

#### Scenario: Grid too large
- **WHEN** a level has 51 columns or 51 rows
- **THEN** validation fails with an error that names the offending dimension

### Requirement: Start and finish have a cell and an open side
The system SHALL define the start and the finish each with an `x` column, a `y` row and an `openSide`. Coordinates SHALL be zero-based with the origin at the top-left cell and `y` increasing downward. An open side SHALL be one of `LEFT`, `RIGHT`, `TOP` or `BOTTOM`. For the start, the open side is the side through which liquid leaves. For the finish, the open side is the side through which liquid must enter. The neighbouring cell on the open side is the endpoint's facing cell.

#### Scenario: Valid start and finish
- **WHEN** a level on a 7 by 9 grid has a start at x 0, y 1, open side `RIGHT` and a finish at x 6, y 7, open side `LEFT`
- **THEN** the level is accepted

#### Scenario: Unknown open side
- **WHEN** a start or finish has an `openSide` value that is not `LEFT`, `RIGHT`, `TOP` or `BOTTOM`
- **THEN** parsing fails with an error that names the invalid value

### Requirement: Start and finish are placed so that they can be connected
The system SHALL accept a start and a finish only when all of the following hold. Each lies inside the grid. They are different cells. Neither lies on a blocked cell. Each facing cell lies inside the grid and is not blocked. The start's facing cell is not the finish, and the finish's facing cell is not the start. The start and the finish MAY share a facing cell.

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

### Requirement: Level has flow settings
The system SHALL define `flow` with an `initialDelayMs`, a `cellsPerSecond`, a `reservoirSpeedFactor` and a `speedRampPerCell`. `initialDelayMs` is the integer number of milliseconds between the start of the level and the moment liquid begins to flow, and SHALL be zero or greater. `cellsPerSecond` is the number of cell lengths the liquid front advances per second at base speed, and SHALL be greater than 0 and at most 10. `reservoirSpeedFactor` multiplies the speed while liquid passes a reservoir, and SHALL be greater than 0 and at most 1. `speedRampPerCell` is the speed increase per filled cell, and SHALL be zero or greater.

#### Scenario: Valid flow settings
- **WHEN** a level has `initialDelayMs` 12000, `cellsPerSecond` 0.6, `reservoirSpeedFactor` 0.3 and `speedRampPerCell` 0.02
- **THEN** the level is accepted with those values

#### Scenario: Zero delay
- **WHEN** a level has `initialDelayMs` 0
- **THEN** the level is accepted and liquid begins to flow immediately

#### Scenario: Negative delay
- **WHEN** a level has a negative `initialDelayMs`
- **THEN** validation fails with an error that names `flow.initialDelayMs`

#### Scenario: Non-positive speed
- **WHEN** a level has a `cellsPerSecond` of 0 or less
- **THEN** validation fails with an error that names `flow.cellsPerSecond`

#### Scenario: Speed above the upper bound
- **WHEN** a level has a `cellsPerSecond` of 60
- **THEN** validation fails with an error that names `flow.cellsPerSecond`

#### Scenario: Reservoir speed factor out of range
- **WHEN** a level has a `reservoirSpeedFactor` of 0, a negative value or a value above 1
- **THEN** validation fails with an error that names `flow.reservoirSpeedFactor`

#### Scenario: Negative speed ramp
- **WHEN** a level has a negative `speedRampPerCell`
- **THEN** validation fails with an error that names `flow.speedRampPerCell`

### Requirement: Level has stack settings with a weight per pipe section
The system SHALL define `stack` with a `visibleSize` and `weights`. `visibleSize` SHALL be between 1 and 10 inclusive. `weights` holds one non-negative integer per pipe section, keyed by the section ids `H`, `V`, `LT`, `LB`, `RT`, `RB`, `X` and `RES`. The weights of `H`, `V`, `LT`, `LB`, `RT` and `RB` SHALL be at least 1. A section with weight 0 SHALL never appear on the stack. The level SHALL report a section as allowed when its weight is greater than 0.

#### Scenario: Cross and reservoir enabled
- **WHEN** a level has stack weights with `X` 1 and `RES` 2
- **THEN** the level reports the cross and the reservoir as allowed

#### Scenario: Cross and reservoir disabled by default
- **WHEN** a level has no `stack` field
- **THEN** the level reports the cross and the reservoir as not allowed, and all straight and corner sections as allowed

#### Scenario: Mandatory section with zero weight
- **WHEN** a level has a stack weight of 0 for `LT`
- **THEN** validation fails with an error that names `stack.weights.LT`

#### Scenario: Negative weight
- **WHEN** a level has a negative stack weight for `X`
- **THEN** validation fails with an error that names `stack.weights.X`

#### Scenario: Unknown section id
- **WHEN** `stack.weights` contains a key that is not a section id, such as `Z`
- **THEN** parsing fails with an error that names the unknown key

#### Scenario: Visible size out of range
- **WHEN** a level has a `visibleSize` of 0 or 11
- **THEN** validation fails with an error that names `stack.visibleSize`

### Requirement: Level has placement rules
The system SHALL define `rules` with a boolean `allowReplace`, which says whether an unfilled pipe may be replaced, and an integer `replacePenaltyMs`, which is the time after a replacement during which no new placement is accepted. `replacePenaltyMs` SHALL be zero or greater.

#### Scenario: Replacing disabled
- **WHEN** a level has `rules.allowReplace` false
- **THEN** the level reports that replacing is not allowed

#### Scenario: Negative replace penalty
- **WHEN** a level has a negative `replacePenaltyMs`
- **THEN** validation fails with an error that names `rules.replacePenaltyMs`

#### Scenario: Non-boolean replace flag
- **WHEN** `rules.allowReplace` is a string or number instead of a boolean
- **THEN** parsing fails with an error that names `rules.allowReplace`

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

### Requirement: Level defines star benchmarks
The system SHALL define `stars` with three point thresholds, `one`, `two` and `three`, that are the minimum points needed to earn one, two and three stars when the level is won. Each threshold SHALL be a positive integer, and the thresholds SHALL be strictly increasing. The system SHALL NOT check whether a threshold can be reached.

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

### Requirement: Stars are derived from points, and a win may earn 0 stars
The system SHALL provide the number of stars earned for a given point total: 0 when the total is below `stars.one`, 1 when it is at least `stars.one` and below `stars.two`, 2 when it is at least `stars.two` and below `stars.three`, and 3 when it is at least `stars.three`. Earning 0 stars SHALL NOT make a won level lost.

#### Scenario: Won below the first threshold
- **WHEN** the thresholds are 100, 200 and 350 and a won level scored 99 points
- **THEN** the stars earned are 0 and the level is still won

#### Scenario: Exactly on a threshold
- **WHEN** the thresholds are 100, 200 and 350 and the points are 200
- **THEN** the stars earned are 2

#### Scenario: Above the last threshold
- **WHEN** the thresholds are 100, 200 and 350 and the points are 1000
- **THEN** the stars earned are 3

### Requirement: All problems in a level are reported together
The system SHALL report every validation problem found in a level in one result, each with the name of the field it concerns, rather than stopping at the first problem.

#### Scenario: Several problems at once
- **WHEN** a level has a `cellsPerSecond` of 60 and a blocked cell outside the grid
- **THEN** the validation result contains an error for `flow.cellsPerSecond` and an error for the blocked cell

