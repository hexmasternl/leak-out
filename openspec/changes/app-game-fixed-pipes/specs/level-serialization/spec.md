# Spec Delta

## MODIFIED Requirements

### Requirement: Level is written as level-format JSON
The system SHALL provide a `LevelWriter` in `core` that turns a `Level` into a JSON document in the level file format. The document SHALL contain every required field. It SHALL contain an optional field only when its value differs from the default in `LevelDefaults`, and SHALL leave out the `flow` optional fields, the `stack` object, `stack.weights` entries, the `rules` object, and the `mandatory` and `fixed` arrays when they hold only defaults. Blocked cells, mandatory sections and fixed pipes SHALL be written in the level's order. The output SHALL be pretty-printed with two-space indentation and end with a newline.

#### Scenario: Minimal level
- **WHEN** a level whose optional fields all hold their defaults is written
- **THEN** the JSON contains `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow` with only `initialDelayMs` and `cellsPerSecond`, and `stars`, and no `stack`, `rules`, `mandatory` or `fixed` key

#### Scenario: Only changed weights are written
- **WHEN** a level has the default stack except a cross weight of 1
- **THEN** the JSON has `"stack": { "weights": { "X": 1 } }` and no other stack key

#### Scenario: Empty blocked list
- **WHEN** a level has no blocked cells
- **THEN** the JSON has `"blocked": []`

#### Scenario: Fixed pipes are written in order
- **WHEN** a level has a fixed `LT` at x 3, y 4 and then a fixed `H` at x 1, y 1
- **THEN** the JSON has a `fixed` array with those two entries, each with `x`, `y` and `section`, in that order

### Requirement: Written levels round-trip through the parser
For every valid level, parsing the output of `LevelWriter` with `LevelParser` SHALL succeed and produce a level equal to the one written.

#### Scenario: Round trip of a bundled level
- **WHEN** each bundled `level_NNN.json` is parsed, written and parsed again
- **THEN** the second parse succeeds and equals the first

#### Scenario: Round trip with every optional field set
- **WHEN** a level with non-default flow, stack, rules, blocked cells, mandatory sections and fixed pipes is written and parsed
- **THEN** the parsed level equals the written level
