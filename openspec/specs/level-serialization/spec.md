# level-serialization Specification

## Purpose
TBD - created by archiving change app-game-level-designer. Update Purpose after archive.

## Requirements

### Requirement: Level is written as level-format JSON
The system SHALL provide a `LevelWriter` in `core` that turns a `Level` into a JSON document in the level file format. The document SHALL contain every required field. It SHALL contain an optional field only when its value differs from the default in `LevelDefaults`, and SHALL leave out the `flow` optional fields, the `stack` object, `stack.weights` entries and the `rules` object when they hold only defaults. Blocked cells SHALL be written in the level's order. The output SHALL be pretty-printed with two-space indentation and end with a newline.

#### Scenario: Minimal level
- **WHEN** a level whose optional fields all hold their defaults is written
- **THEN** the JSON contains `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow` with only `initialDelayMs` and `cellsPerSecond`, and `stars`, and no `stack` or `rules` key

#### Scenario: Only changed weights are written
- **WHEN** a level has the default stack except a cross weight of 1
- **THEN** the JSON has `"stack": { "weights": { "X": 1 } }` and no other stack key

#### Scenario: Empty blocked list
- **WHEN** a level has no blocked cells
- **THEN** the JSON has `"blocked": []`

### Requirement: Written levels round-trip through the parser
For every valid level, parsing the output of `LevelWriter` with `LevelParser` SHALL succeed and produce a level equal to the one written.

#### Scenario: Round trip of a bundled level
- **WHEN** each bundled `level_NNN.json` is parsed, written and parsed again
- **THEN** the second parse succeeds and equals the first

#### Scenario: Round trip with every optional field set
- **WHEN** a level with non-default flow, stack, rules and blocked cells is written and parsed
- **THEN** the parsed level equals the written level
