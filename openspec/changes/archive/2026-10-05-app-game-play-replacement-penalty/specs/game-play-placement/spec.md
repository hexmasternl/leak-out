## MODIFIED Requirements

### Requirement: Placement rule per tile
The system SHALL decide for every tile whether a dragged section can be dropped on it, using one rule:
- an empty tile SHALL accept a section;
- a blocked tile, the start tile and the finish tile SHALL never accept a section;
- a tile with a placed section SHALL accept a section only when the liquid has not entered that placed section and the level's `rules.allowReplace` is true;
- no tile SHALL accept a section while a replacement sequence runs or while the game is paused.

#### Scenario: Empty tile accepts
- **WHEN** a section is dragged to an empty tile
- **THEN** the tile accepts the section

#### Scenario: Blocked, start and finish tiles refuse
- **WHEN** a section is dragged to a blocked tile, the start tile or the finish tile
- **THEN** the tile does not accept the section

#### Scenario: Placed section without liquid accepts
- **WHEN** a section is dragged to a tile with a placed section that the liquid has not entered, in a level that allows replacing
- **THEN** the tile accepts the section

#### Scenario: Placed section with liquid refuses
- **WHEN** a section is dragged to a tile with a placed section that the liquid has entered, also when the liquid is still flowing through it
- **THEN** the tile does not accept the section

#### Scenario: Level without replacing
- **WHEN** a section is dragged to a tile with a placed section in a level where `rules.allowReplace` is false
- **THEN** the tile does not accept the section

#### Scenario: Replacement sequence running
- **WHEN** a section is dragged to an empty tile while a replacement sequence runs
- **THEN** the tile does not accept the section

### Requirement: Replacing a placed section
When a section is dropped on a tile that holds a placed section and the tile accepts it, the new section SHALL take the place of the old one through a replacement sequence that lasts the level's `rules.replacePenaltyMs` (see `game-play-replacement-penalty`). The old section SHALL leave the board when the sequence starts and SHALL NOT go back to the stack. The new section SHALL land on the tile when the sequence ends. The sequence SHALL use the game clock, so it does not run out while the game is paused. A drop on an empty tile SHALL NOT start a replacement sequence.

#### Scenario: Replacing an unfilled section
- **WHEN** the player drops `V` on a tile holding an `H` that the liquid has not entered, and the level's `rules.replacePenaltyMs` of game time passes
- **THEN** the tile holds `V`, the `H` is gone, and the stack moved down at the drop

#### Scenario: Replacement sequence blocks the next drop
- **WHEN** the player replaces a section and drops another section within the level's `rules.replacePenaltyMs`
- **THEN** no tile is highlighted and the second drop is rejected

#### Scenario: Replacement sequence ends
- **WHEN** the level's `rules.replacePenaltyMs` of game time has passed after a replacement
- **THEN** the new section is on the tile, and tiles are highlighted and drops are accepted again by the placement rule

#### Scenario: Placing on an empty tile is instant
- **WHEN** the player drops a section on an empty tile
- **THEN** the section is on the tile right away and no replacement sequence runs
