# Spec Delta

## MODIFIED Requirements

### Requirement: Written levels round-trip through the parser
For every valid level, parsing the output of `LevelWriter` with `LevelParser` SHALL succeed and produce a level equal to the one written. A level without a splitter SHALL be written without `secondFinish` and `splitter` keys.

#### Scenario: Round trip of a bundled level
- **WHEN** each bundled `level_NNN.json` is parsed, written and parsed again
- **THEN** the second parse succeeds and equals the first

#### Scenario: Round trip with every optional field set
- **WHEN** a level with non-default flow, stack, rules and blocked cells is written and parsed
- **THEN** the parsed level equals the written level

#### Scenario: Round trip with a splitter
- **WHEN** a level with a splitter and a second finish is written and parsed
- **THEN** the JSON has `secondFinish` and `splitter` keys and the parsed level equals the written level

#### Scenario: No splitter keys for a level without one
- **WHEN** a level with one finish and no splitter is written
- **THEN** the JSON has no `secondFinish` and no `splitter` key
