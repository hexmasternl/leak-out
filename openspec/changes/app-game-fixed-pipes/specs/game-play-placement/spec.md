# Spec Delta

## MODIFIED Requirements

### Requirement: Placement rule per tile
The system SHALL decide for every tile whether a dragged section can be dropped on it, using one rule:
- an empty tile SHALL accept a section;
- a blocked tile, the start tile, the finish tile, a tile with a mandatory section and a tile with a fixed pipe SHALL never accept a section;
- a tile with a placed section SHALL accept a section only when the liquid has not entered that placed section and the level's `rules.allowReplace` is true;
- no tile SHALL accept a section while a replacement sequence runs or while the game is paused.

#### Scenario: Empty tile accepts
- **WHEN** a section is dragged to an empty tile
- **THEN** the tile accepts the section

#### Scenario: Blocked, start and finish tiles refuse
- **WHEN** a section is dragged to a blocked tile, the start tile or the finish tile
- **THEN** the tile does not accept the section

#### Scenario: Mandatory section refuses
- **WHEN** a section is dragged to a tile with a mandatory section that the liquid has not entered, in a level that allows replacing
- **THEN** the tile does not accept the section and is not highlighted

#### Scenario: Fixed pipe refuses
- **WHEN** a section is dragged to a tile with a fixed pipe that the liquid has not entered, in a level that allows replacing
- **THEN** the tile does not accept the section and is not highlighted

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
