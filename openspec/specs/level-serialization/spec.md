# level-serialization Specification

## Purpose
TBD - created by archiving change app-game-level-designer. Update Purpose after archive.

## Requirements

### Requirement: Level is written as level-format JSON
The system SHALL provide a `LevelWriter` in `core` that turns a `Level` into a JSON document in the level file format. The document SHALL contain every required field. It SHALL contain an optional field only when its value differs from the default in `LevelDefaults`, and SHALL leave out the `flow` optional fields, the `stack` object, `stack.weights` entries and the `rules` object when they hold only defaults. The `leaky` array SHALL be written only when the level has leaky tiles. Blocked cells and leaky tiles SHALL be written in the level's order. Portal pairs SHALL be written as a `portals` array in the level's order, and the `portals` key SHALL be left out when the level has none. The output SHALL be pretty-printed with two-space indentation and end with a newline.

#### Scenario: Minimal level
- **WHEN** a level whose optional fields all hold their defaults is written
- **THEN** the JSON contains `id`, `name`, `grid`, `start`, `finish`, `blocked`, `flow` with only `initialDelayMs` and `cellsPerSecond`, and `stars`, and no `stack`, `rules` or `leaky` key

#### Scenario: Only changed weights are written
- **WHEN** a level has the default stack except a cross weight of 1
- **THEN** the JSON has `"stack": { "weights": { "X": 1 } }` and no other stack key

#### Scenario: Empty blocked list
- **WHEN** a level has no blocked cells
- **THEN** the JSON has `"blocked": []`

#### Scenario: Leaky tiles are written
- **WHEN** a level with leaky tiles at x 2, y 1 and x 4, y 3 is written
- **THEN** the JSON has a `leaky` array with those two cells in that order

#### Scenario: Portal pairs are written
- **WHEN** a level with two portal pairs is written
- **THEN** the JSON has a `portals` array with both pairs in the level's order, each with its `a` and `b` portal and their `x`, `y` and `openSide`

### Requirement: Written levels round-trip through the parser
For every valid level, parsing the output of `LevelWriter` with `LevelParser` SHALL succeed and produce a level equal to the one written.

#### Scenario: Round trip of a bundled level
- **WHEN** each bundled `level_NNN.json` is parsed, written and parsed again
- **THEN** the second parse succeeds and equals the first

#### Scenario: Round trip with every optional field set
- **WHEN** a level with non-default flow, stack, rules, blocked cells, mandatory sections and leaky tiles is written and parsed
- **THEN** the parsed level equals the written level
