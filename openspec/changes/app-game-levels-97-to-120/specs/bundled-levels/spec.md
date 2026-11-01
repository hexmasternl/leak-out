## ADDED Requirements

### Requirement: The app bundles 120 levels
The app SHALL bundle levels with the ids 1 to 120, without gaps, so the level library shows five full pages of 24 levels. Every bundled level SHALL parse, pass validation and be solvable.

#### Scenario: Fifth page is full
- **WHEN** the bundled levels are loaded
- **THEN** there are 120 levels with ids 1 to 120 and library page 5 holds levels 97 to 120

#### Scenario: Every new level is valid and solvable
- **WHEN** the solver runs over levels 97 to 120
- **THEN** every level validates and the solver finds a route for each

### Requirement: Levels 97 to 120 are a fast-flow pack
Levels 97 to 120 SHALL have small grids of 6 to 12 columns and 5 to 8 rows, an initial delay between 28000 and 32000 milliseconds, a speed ramp of 0, and a base speed (`flow.cellsPerSecond`) that is higher than that of level 96, starts at 0.5 on level 97, reaches 0.9 on level 120 and never decreases from one level to the next. Each of these levels SHALL use a reservoir speed factor of 0.1.

#### Scenario: Small grids and a head start
- **WHEN** levels 97 to 120 are read
- **THEN** each has 6 to 12 columns, 5 to 8 rows and an initial delay between 28000 and 32000 milliseconds

#### Scenario: Speed rises through the pack
- **WHEN** the base speeds of levels 97 to 120 are compared in order
- **THEN** level 97 runs at 0.5, level 120 at 0.9, and no level runs slower than the level before it

#### Scenario: Strongest reservoir slow-down
- **WHEN** levels 97 to 120 are read
- **THEN** each has a reservoir speed factor of 0.1

### Requirement: Levels 97 to 120 are doable
For each of levels 97 to 120, with `L` the length in sections of the route found by `LevelSolver` (the shortest route, or with mandatory sections the solver's greedy route), `L` SHALL be at most 30 and the initial delay in seconds plus `L` divided by the base speed SHALL be at least 1.5 times `L`, so a player who places one section every 1.5 seconds stays ahead of the liquid along that route.

#### Scenario: Route can be built in time
- **WHEN** a level has a shortest route of 20 sections, an initial delay of 30000 milliseconds and a base speed of 0.9
- **THEN** 30 + 20 / 0.9 ≈ 52.2 seconds is at least 1.5 × 20 = 30 seconds and the level counts as doable

#### Scenario: Route too long for the head start
- **WHEN** a level has a shortest route of 30 sections, an initial delay of 0 milliseconds and a base speed of 0.9
- **THEN** 30 / 0.9 ≈ 33.3 seconds is less than 45 seconds and the bundled level test fails for that level
