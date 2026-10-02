# game-play-points-popup Specification

## Purpose
TBD - created by syncing change app-game-play-points. Update Purpose after archive.
## Requirements
### Requirement: Points are shown on the tile
Each time points are awarded for a section, the system SHALL show the number of points on the tile of that section, as a plus sign followed by the number, such as "+10", "+15", "+25" or "+50". The text SHALL be centred on the tile, SHALL be drawn above the section and the liquid, and SHALL be sized to the tile so it stays readable at every zoom level. The glyphs SHALL be filled solid with the design system's `surface.deep` colour, edged with a thin `liquid.glow` stroke, over a soft `liquid.glow` glow, as described in the design system. No thick dark outline SHALL be drawn. The fill, stroke and glow SHALL fade together with the popup.

#### Scenario: Popup for a corner
- **WHEN** the liquid exits a corner section
- **THEN** "+15" appears centred on the tile of that corner

#### Scenario: Popup for the second pass of a cross
- **WHEN** the liquid exits a cross for the second time
- **THEN** "+50" appears on the tile of that cross

#### Scenario: Popup look
- **WHEN** a popup is fully visible over a filled straight section
- **THEN** its digits are filled with opaque `surface.deep`, edged with a thin `liquid.glow` stroke and surrounded by a soft `liquid.glow` glow, and the pipe and liquid do not show through the digits

#### Scenario: Look fades with the popup
- **WHEN** a popup has been shown for 850 ms, halfway through its fade-out
- **THEN** its fill, stroke and glow are all drawn at the popup's current alpha, so none of them stays visible after the popup is removed

### Requirement: Popup fades in, holds and fades out
A popup SHALL fade in from fully transparent to fully visible in 150 ms, stay fully visible for 500 ms, and fade out to fully transparent in 350 ms, so that it is shown for 1,000 ms in total. While it is shown it SHALL move up by a quarter of a tile. During the fade-in it SHALL grow from 80 % to 100 % of its size. After 1,000 ms the popup SHALL be removed.

#### Scenario: Popup appears
- **WHEN** a popup has been shown for 150 ms
- **THEN** it is fully visible at its full size

#### Scenario: Popup disappears
- **WHEN** a popup has been shown for 1,000 ms
- **THEN** it is fully transparent and no longer shown

### Requirement: Popups for sections close together
Each award SHALL get its own popup. When the liquid runs quickly through several sections, the popups of those sections SHALL be shown at the same time, each on its own tile and each with its own timing. When a cross scores twice, the second popup SHALL be shown on the same tile, also when the first one is still visible.

#### Scenario: Two sections in quick succession
- **WHEN** the liquid exits a straight section and, 300 ms later, the next corner section
- **THEN** "+10" and "+15" are both visible on their own tiles, and the "+10" disappears 300 ms before the "+15"

### Requirement: Popup follows the tile
A popup SHALL stay on its tile when the player pans or zooms the playing field. A popup on a tile that is outside the visible part of the field SHALL NOT be drawn, but its timing SHALL continue.

#### Scenario: Pan while a popup is shown
- **WHEN** a popup is shown and the player pans the field
- **THEN** the popup moves with its tile

### Requirement: Popups follow the game clock
The popup timing SHALL use the game clock. While the game is paused, popups SHALL NOT fade or move and SHALL continue from the same point when the game is resumed. Restarting the level SHALL remove all popups.

#### Scenario: Pause during a popup
- **WHEN** a popup has been shown for 400 ms and the player opens the pause menu, waits and taps Resume
- **THEN** the popup continues from 400 ms and is removed after 600 ms more of play

#### Scenario: Restart removes popups
- **WHEN** popups are shown and the player restarts the level
- **THEN** no popups are shown

### Requirement: Popups do not block input
Popups SHALL NOT take touch input. A tile with a popup SHALL accept panning, zooming and the hover highlight in the same way as without a popup.

#### Scenario: Pan starting on a popup
- **WHEN** the player presses on a tile that shows a popup and drags
- **THEN** the field pans
