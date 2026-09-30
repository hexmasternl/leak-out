# game-play-placement Specification

## Purpose
TBD - created by syncing change app-game-play-playing-field. Update Purpose after archive.
## Requirements
### Requirement: Placement rule per tile
The system SHALL decide for every tile whether a dragged section can be dropped on it, using one rule:
- an empty tile SHALL accept a section;
- a blocked tile, the start tile and the finish tile SHALL never accept a section;
- a tile with a placed section SHALL accept a section only when the liquid has not entered that placed section and the level's `rules.allowReplace` is true;
- no tile SHALL accept a section while the replace lock is active or while the game is paused.

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

### Requirement: Hovered tile is highlighted when it accepts
While the player drags a section, the field SHALL highlight the tile under the drop point of the dragged section when that tile accepts the section. It SHALL highlight at most one tile at a time. It SHALL NOT highlight any tile when the drop point is over a tile that does not accept the section, outside the level or outside the visible part of the field. The highlight SHALL follow the drop point as it moves, and SHALL disappear when the drag ends, is cancelled or the game pauses.

#### Scenario: Hovering an empty tile
- **WHEN** the player drags a section over an empty tile
- **THEN** that tile is highlighted

#### Scenario: Hovering a blocked tile
- **WHEN** the player drags a section over a blocked tile
- **THEN** no tile is highlighted

#### Scenario: Hovering a placed section without liquid
- **WHEN** the player drags a section over a placed section the liquid has not entered
- **THEN** that tile is highlighted

#### Scenario: Hovering a placed section with liquid
- **WHEN** the player drags a section over a placed section the liquid has entered
- **THEN** no tile is highlighted

#### Scenario: Moving to the next tile
- **WHEN** the player drags a section from one empty tile to the tile next to it
- **THEN** only the new tile is highlighted

#### Scenario: Hovering outside the field
- **WHEN** the player drags a section over the stack, the HUD, or a part of the level that is outside the visible field
- **THEN** no tile is highlighted

#### Scenario: Highlight disappears on release
- **WHEN** the player lets go of the section
- **THEN** no tile is highlighted anymore

### Requirement: Drop result matches the highlight
When the player lets go of a dragged section, the field SHALL accept the drop exactly when the tile under the drop point accepts the section by the placement rule. A drop on a tile that was not highlighted SHALL be rejected, and the stack SHALL stay the same.

#### Scenario: Drop on a highlighted tile
- **WHEN** the player lets go while a tile is highlighted
- **THEN** the section is placed on that tile and the stack moves down

#### Scenario: Drop on a tile that is not highlighted
- **WHEN** the player lets go over a blocked tile
- **THEN** nothing is placed and the section goes back to the stack

### Requirement: Replacing a placed section
When a section is dropped on a tile that holds a placed section and the tile accepts it, the new section SHALL take the place of the old one. The old section SHALL be removed from the board and SHALL NOT go back to the stack. After a replacement, the system SHALL start the replace lock for the level's `rules.replacePenaltyMs`. The lock SHALL use the game clock, so it does not run out while the game is paused. A drop on an empty tile SHALL NOT start the replace lock.

#### Scenario: Replacing an unfilled section
- **WHEN** the player drops `V` on a tile holding an `H` that the liquid has not entered
- **THEN** the tile holds `V`, the `H` is gone, and the stack moves down

#### Scenario: Replace lock blocks the next drop
- **WHEN** the player replaces a section and drops another section within the level's `rules.replacePenaltyMs`
- **THEN** no tile is highlighted and the second drop is rejected

#### Scenario: Replace lock ends
- **WHEN** the level's `rules.replacePenaltyMs` of game time has passed after a replacement
- **THEN** tiles are highlighted and drops are accepted again by the placement rule

### Requirement: Sections the liquid has entered are permanent
Once the liquid has entered a placed section, the system SHALL keep that section on its tile for the rest of the attempt. It SHALL NOT be replaced, removed or moved by any action of the player. Only restarting the level SHALL clear it.

#### Scenario: Filled section stays
- **WHEN** the liquid has entered a placed section and the player drops a section on that tile
- **THEN** the drop is rejected and the tile still holds the original section

#### Scenario: Restart clears all sections
- **WHEN** the player restarts the level
- **THEN** the board holds no placed sections, also none that the liquid had entered
