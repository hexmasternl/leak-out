## ADDED Requirements

### Requirement: Tile renders a section at any size
The app SHALL provide a pipe tile that draws a pipe section from its asset scaled to the tile size, so the same asset can be used for the playing field, the stack and previews. A tile without liquid SHALL show only the artwork.

#### Scenario: Scaled to the cell
- **WHEN** a section is drawn in a tile of 48 dp and in a tile of 96 dp
- **THEN** both show the whole section filling the tile, scaled from the same asset

### Requirement: Liquid per channel with progress and entry side
The tile SHALL take, for each channel of the section, an optional liquid fill made of a progress between 0 and 1 and the side the liquid entered through. The entry side SHALL be one of the two sides of that channel. The liquid SHALL start at the entry side and cover the first `progress` part of the channel's length, measured from the entry side. Progress 0 SHALL show no liquid and progress 1 SHALL show the whole channel filled. A progress outside 0 to 1 SHALL be clamped.

#### Scenario: Half filled from the left
- **WHEN** an `H` tile is drawn with its channel at progress 0.5, entered from the left
- **THEN** the left half of the bore is filled and the right half is empty

#### Scenario: Entry side not on the channel
- **WHEN** a fill is resolved for a channel with an entry side that is not one of the channel's sides
- **THEN** resolving the fill fails

### Requirement: Liquid flows in both directions
Every channel SHALL be fillable from either of its two sides. Filling from the other side SHALL show the liquid starting at that other side and moving the opposite way along the same path.

#### Scenario: Straight from the right
- **WHEN** an `H` tile is drawn at progress 0.25, entered from the right
- **THEN** the right quarter of the bore is filled

#### Scenario: Corner in both directions
- **WHEN** an `LT` tile is drawn at progress 0.5, once entered from the left and once entered from the top
- **THEN** the first shows the left leg filled up to the centre and the second shows the top leg filled up to the centre

### Requirement: Cross channels are independent
The cross SHALL have a separate fill for its horizontal and its vertical channel. Each channel SHALL have its own progress and entry side, both SHALL be able to show liquid at the same time, and the liquid of one channel SHALL never be drawn in the other channel. The horizontal liquid SHALL be drawn above the vertical liquid, matching the artwork.

#### Scenario: Only the horizontal channel
- **WHEN** a cross is drawn with the horizontal channel filled and no vertical fill
- **THEN** only the horizontal bore shows liquid

#### Scenario: Both channels in opposite directions
- **WHEN** a cross is drawn with the horizontal channel at progress 1 entered from the right and the vertical channel at progress 0.5 entered from the bottom
- **THEN** the horizontal bore is filled completely and the lower half of the vertical bore is filled

### Requirement: Reservoir fills its tank
For the reservoir, progress 0 to 0.1 SHALL fill the inlet stub from the entry side to the tank, progress 0.1 to 0.9 SHALL fill the tank from the bottom up (clipped to the tank shape, with the level rising linearly), and progress 0.9 to 1 SHALL fill the outlet stub from the tank to the exit side. This SHALL work for liquid entering from the left and from the right. The liquid SHALL keep the liquid colour; only the tank is violet.

#### Scenario: Tank half full
- **WHEN** a reservoir entered from the left is drawn at progress 0.5
- **THEN** the left stub is full, the tank is filled to half its height, and the right stub is empty

#### Scenario: Reservoir from the right
- **WHEN** a reservoir entered from the right is drawn at progress 0.05
- **THEN** only the outer half of the right stub is filled and the tank is empty

### Requirement: Liquid look
The liquid SHALL be drawn with the design system's liquid gradient (`liquid.300` → `liquid.500` → `liquid.700`), 0.21 of the cell wide, with a rounded head in `liquid.glow` at its leading edge while progress is between 0 and 1. A channel that is filling SHALL have a `liquid.glow` outer glow; a completely filled channel SHALL be drawn without the glow.

#### Scenario: Head only while filling
- **WHEN** a channel is drawn at progress 0.4 and at progress 1
- **THEN** the first shows a glowing head at 40 % of the channel and the second shows no head

### Requirement: Progress from time and speed
The app SHALL compute a channel's progress from the time the liquid has been in the section, the flow speed in cells per second, and the section: `progress = elapsedSeconds × cellsPerSecond × factor`, clamped to 0 to 1, where `factor` is the reservoir speed factor for `RES` and 1 for every other section. The time a section takes to fill SHALL therefore be `1 / (cellsPerSecond × factor)` seconds. A flow speed or reservoir speed factor of 0 or less SHALL be refused. The progress SHALL be linear in time; the renderer SHALL NOT ease it.

#### Scenario: Straight at 2 cells per second
- **WHEN** liquid has been in an `H` section for 250 ms at 2 cells per second
- **THEN** its progress is 0.5

#### Scenario: Reservoir takes longer
- **WHEN** liquid has been in a reservoir for 1 s at 1 cell per second with a reservoir speed factor of 0.25
- **THEN** its progress is 0.25, and the reservoir is full after 4 s

#### Scenario: Invalid speed
- **WHEN** the progress is asked for a flow speed of 0
- **THEN** the request is refused
