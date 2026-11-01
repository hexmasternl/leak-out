## MODIFIED Requirements

### Requirement: Grid size
The designer SHALL let the developer set the number of columns between 4 and 25 and the number of rows between 4 and 15, with controls that cannot go outside those ranges. When the grid shrinks, blocked cells outside the new grid SHALL be removed, a start or finish outside the new grid SHALL be moved to the nearest cell inside it, keeping its open side, and a portal pair with a portal outside the new grid SHALL be removed. A design that is larger than 25 columns or 15 rows when it is restored (for example one kept from before an app update) SHALL be shrunk to fit with the same rules.

#### Scenario: Grow the grid
- **WHEN** the developer raises the columns from 7 to 10
- **THEN** the grid shows 10 columns and the start, finish and blocked cells keep their positions

#### Scenario: Shrink removes blocked cells
- **WHEN** a blocked cell is at x 8, y 1 and the developer lowers the columns from 10 to 8
- **THEN** that blocked cell is removed and the other blocked cells are kept

#### Scenario: Shrink moves the finish
- **WHEN** the finish is at x 9, y 2 and the developer lowers the columns to 6
- **THEN** the finish is at x 5, y 2 with its open side unchanged

#### Scenario: Range limits
- **WHEN** the columns are 4
- **THEN** the control to lower the columns is disabled, at 25 columns the control to raise the columns is disabled, and at 15 rows the control to raise the rows is disabled

#### Scenario: Shrink removes a portal pair
- **WHEN** a portal is at x 9, y 1, its partner is at x 2, y 3, and the developer lowers the columns from 10 to 8
- **THEN** both portals of that pair are removed and the other pairs are kept

#### Scenario: Oversized design is shrunk on restore
- **WHEN** the designer restores a design of 24 columns by 24 rows whose finish is at x 6, y 23
- **THEN** the design has 24 columns and 15 rows and the finish is at x 6, y 14 with its open side unchanged
