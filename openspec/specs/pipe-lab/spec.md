# pipe-lab Specification

## Purpose
TBD - created by archiving change app-game-play-pipe-assets. Update Purpose after archive.
## Requirements
### Requirement: Pipe Lab in debug builds only
Debug builds SHALL include a Pipe Lab screen with its own launcher entry named "Pipe Lab". Release builds SHALL NOT contain the Pipe Lab screen or its launcher entry. The screen SHALL use the game's theme and landscape orientation.

#### Scenario: Debug launcher entry
- **WHEN** the debug build is installed
- **THEN** the launcher shows a "Pipe Lab" entry next to the game, and opening it shows the Pipe Lab

#### Scenario: Not in release
- **WHEN** the release build is assembled
- **THEN** its merged manifest has no Pipe Lab activity

### Requirement: Shows every section
The Pipe Lab SHALL show one tile for every pipe section (`H`, `V`, `LT`, `LB`, `RT`, `RB`, `X`, `RES`), each labelled with its section id, drawn with the pipe tile renderer.

#### Scenario: All eight tiles
- **WHEN** the Pipe Lab opens
- **THEN** it shows eight labelled tiles, one per section, without liquid

### Requirement: Direction per channel
For each section the Pipe Lab SHALL offer a control to choose the entry side of each of its channels. The cross SHALL have a separate direction control for its horizontal and its vertical channel. The default entry side SHALL be the channel's `data-from` side. Changing a direction SHALL empty the liquid of that tile.

#### Scenario: Reverse a straight
- **WHEN** the developer switches the direction of `H` and starts the animation
- **THEN** the liquid in `H` enters from the right and flows to the left

#### Scenario: Cross directions separately
- **WHEN** the developer sets the cross's horizontal channel to enter from the right and its vertical channel to enter from the bottom, and starts the animation
- **THEN** the horizontal liquid flows right to left and the vertical liquid flows bottom to top

### Requirement: Flow speed and reservoir speed controls
The Pipe Lab SHALL have a flow speed slider from 0.1 to 4 cells per second (default 1) and a reservoir speed factor slider from 0.1 to 1 (default 0.3), each showing its current value. Changes SHALL take effect immediately, also while the animation runs, without making the liquid jump.

#### Scenario: Faster flow
- **WHEN** the developer sets the flow speed to 2 cells per second and starts the animation
- **THEN** a straight section fills in 0.5 s

#### Scenario: Slower reservoir
- **WHEN** the flow speed is 1 cell per second and the reservoir speed factor is 0.25
- **THEN** after starting, the reservoir takes 4 s to fill while a straight takes 1 s

#### Scenario: Changing speed while running
- **WHEN** the developer changes the flow speed while liquid is half way through a section
- **THEN** the liquid continues from where it was at the new speed

### Requirement: Start, pause and reset
The Pipe Lab SHALL have a Start button that starts the fluid animation in all tiles at once, a Pause button that freezes the liquid where it is (Start then continues it), and a Reset button that empties every tile and stops the animation. A full tile SHALL stay full until reset. The cross SHALL fill its horizontal channel first and then its vertical channel, so both channels are shown flowing on their own.

#### Scenario: Start
- **WHEN** the developer presses Start
- **THEN** liquid enters every tile from its chosen entry side and flows at the chosen speed

#### Scenario: Pause and continue
- **WHEN** the developer presses Pause while the liquid is flowing, and later presses Start
- **THEN** the liquid stops where it is, and continues from there

#### Scenario: Reset
- **WHEN** the developer presses Reset
- **THEN** every tile is empty and the animation is stopped

#### Scenario: Cross fills one channel after the other
- **WHEN** the animation runs at 1 cell per second
- **THEN** the cross's horizontal channel fills in the first second and its vertical channel in the second second

