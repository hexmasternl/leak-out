## ADDED Requirements

### Requirement: The liquid's path is recorded
The engine SHALL record, in order, every exit of the liquid from a section: the tile and the section on it. A cross the liquid leaves twice SHALL be recorded twice. Sections that are removed from the board after a win SHALL NOT change the record, and restart SHALL clear it.

#### Scenario: Path in flow order
- **WHEN** the liquid leaves an `H`, then an `LT`, then a `V`
- **THEN** the recorded path is the `H`, the `LT` and the `V` on their tiles, in that order

#### Scenario: Cross passed twice
- **WHEN** the liquid leaves a cross horizontally and later leaves it again vertically
- **THEN** the cross appears twice in the recorded path

#### Scenario: Restart clears the path
- **WHEN** an attempt in which the liquid left 5 sections is restarted
- **THEN** the new attempt's recorded path is empty

### Requirement: Taps on a running replacement are counted
The engine SHALL accept a tap on a tile while a replacement runs. A tap on the tile being replaced SHALL add one to that replacement's tap count; a tap on any other tile, or with no replacement running, SHALL change nothing. The attempt SHALL keep the highest tap count of any of its replacements. A tap SHALL NOT place a section, change the stack, play a sound or end the replacement early, and restart SHALL clear the counts.

#### Scenario: Taps on the replaced tile
- **WHEN** a replacement runs on tile (3, 2) and the player taps (3, 2) four times
- **THEN** the replacement's tap count is 4, the tile is still empty and the stack is unchanged

#### Scenario: Highest count is kept
- **WHEN** one replacement gets 6 taps and a later one gets 2
- **THEN** the attempt's highest replacement tap count is 6

#### Scenario: Tap without a replacement
- **WHEN** no replacement runs and a tap on a tile is sent
- **THEN** the game state is unchanged
