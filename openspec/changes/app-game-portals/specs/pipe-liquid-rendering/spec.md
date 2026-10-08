## MODIFIED Requirements

### Requirement: Progress from time and speed
The app SHALL compute a channel's progress from the time the liquid has been in the section, the flow speed in cells per second, and the section: `progress = elapsedSeconds × cellsPerSecond × factor`, clamped to 0 to 1, where `factor` is the reservoir speed factor for `RES` and 1 for every other section and for a portal pair. The time a section takes to fill SHALL therefore be `1 / (cellsPerSecond × factor)` seconds. A flow speed or reservoir speed factor of 0 or less SHALL be refused. The progress SHALL be linear in time; the renderer SHALL NOT ease it. For a portal pair the progress SHALL be split over its two cells: progress 0 to 0.5 fills the entry portal's stub and progress 0.5 to 1 fills the exit portal's stub, each from 0 to full.

#### Scenario: Straight at 2 cells per second
- **WHEN** liquid has been in an `H` section for 250 ms at 2 cells per second
- **THEN** its progress is 0.5

#### Scenario: Reservoir takes longer
- **WHEN** liquid has been in a reservoir for 1 s at 1 cell per second with a reservoir speed factor of 0.25
- **THEN** its progress is 0.25, and the reservoir is full after 4 s

#### Scenario: Invalid speed
- **WHEN** the progress is asked for a flow speed of 0
- **THEN** the request is refused

#### Scenario: Portal pair at 1 cell per second
- **WHEN** liquid has been in a portal pair for 750 ms at 1 cell per second
- **THEN** the pair's progress is 0.75, the entry portal's stub is full and the exit portal's stub is half filled from its ring towards its open side
