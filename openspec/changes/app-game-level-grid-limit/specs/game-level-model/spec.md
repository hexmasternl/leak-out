## RENAMED Requirements

- FROM: `### Requirement: Level defines a grid size between 4 by 4 and 50 by 50`
- TO: `### Requirement: Level defines a grid size between 4 by 4 and 25 by 15`

## MODIFIED Requirements

### Requirement: Level defines a grid size between 4 by 4 and 25 by 15
The system SHALL define the grid of a level with an integer number of `columns` between 4 and 25 inclusive and an integer number of `rows` between 4 and 15 inclusive.

#### Scenario: Valid grid
- **WHEN** a level has 7 columns and 9 rows
- **THEN** the level is accepted with a 7 by 9 grid

#### Scenario: Smallest and largest grids
- **WHEN** a level has a 4 by 4 grid, or a grid of 25 columns by 15 rows
- **THEN** the level is accepted

#### Scenario: Grid too small
- **WHEN** a level has 3 columns or 3 rows
- **THEN** validation fails with an error that names the offending dimension

#### Scenario: Grid too large
- **WHEN** a level has 26 columns and 15 rows
- **THEN** validation fails with an error that names `grid.columns`

#### Scenario: Too many rows
- **WHEN** a level has 25 columns and 16 rows
- **THEN** validation fails with an error that names `grid.rows`
