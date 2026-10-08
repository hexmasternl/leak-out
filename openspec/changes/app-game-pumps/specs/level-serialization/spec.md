## MODIFIED Requirements

### Requirement: Written levels round-trip through the parser
For every valid level, parsing the output of `LevelWriter` with `LevelParser` SHALL succeed and produce a level equal to the one written.

#### Scenario: Round trip of a bundled level
- **WHEN** each bundled `level_NNN.json` is parsed, written and parsed again
- **THEN** the second parse succeeds and equals the first

#### Scenario: Round trip with every optional field set
- **WHEN** a level with non-default flow, stack, rules and blocked cells is written and parsed
- **THEN** the parsed level equals the written level

#### Scenario: Round trip with pumps
- **WHEN** a level with a pump weight of 2, a pump speed factor of 2.5 and a mandatory pump is written and parsed
- **THEN** the parsed level equals the written level

#### Scenario: Default pump settings are left out
- **WHEN** a level with a pump weight of 0 and a pump speed factor of 1.5 is written
- **THEN** the JSON has no `pumpSpeedFactor` key and no `PUMP` weight
