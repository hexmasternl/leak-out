## ADDED Requirements

### Requirement: Stack holds the level's visible number of sections
When a level starts, the system SHALL create a stack that holds exactly `stack.visibleSize` pipe sections of that level. The stack SHALL hold that many sections for the whole game: every section that is removed SHALL be replaced in the same step.

#### Scenario: Stack is filled when a level starts
- **WHEN** a level with a `stack.visibleSize` of 5 starts
- **THEN** the stack holds 5 sections

#### Scenario: Stack keeps its size after a placement
- **WHEN** a section from a stack of 5 is placed on the board
- **THEN** the stack holds 5 sections again

### Requirement: Sections are drawn by the level's weights
The system SHALL draw every new section at random from the level's stack weights. The chance of each section SHALL be its weight divided by the sum of all weights. A section with weight 0 SHALL never be drawn. The level holds no seed, so each attempt SHALL get a new random order. The random source SHALL be replaceable, so that tests can get a repeatable order.

#### Scenario: Section with weight 0 is never drawn
- **WHEN** a level with an `X` weight of 0 draws 10,000 sections
- **THEN** none of the drawn sections is `X`

#### Scenario: Draws follow the weights
- **WHEN** a level with the weights `H` 3, `V` 3, `LT` 2, `LB` 2, `RT` 2, `RB` 2, `X` 1 and `RES` 1 draws 100,000 sections
- **THEN** each section's share of the draws is within 1 percentage point of its weight divided by 16

#### Scenario: Same random source gives the same stack
- **WHEN** two stacks are created for the same level with random sources that use the same seed
- **THEN** both stacks hold the same sections in the same order, and they stay the same after the same number of placements

### Requirement: New sections enter at the top and the next section is at the bottom
The stack SHALL be ordered from top to bottom. The bottom section SHALL be the next section, the one that is placed next. A new section SHALL always be added at the top. When the next section is removed, every other section SHALL move down one slot.

#### Scenario: Placement moves the stack down
- **WHEN** the stack holds, from top to bottom, `H`, `V`, `LT`, `LB`, `RT`, the `RT` is placed, and `X` is drawn
- **THEN** the stack holds, from top to bottom, `X`, `H`, `V`, `LT`, `LB`, and `LB` is the next section

### Requirement: Only the next section can be dragged
The system SHALL let the player drag only the next section, the bottom one. The other sections SHALL be a preview only: pressing or dragging them SHALL do nothing. Dragging SHALL start when the player presses the next section and moves their finger. No long press SHALL be needed.

#### Scenario: Dragging the next section
- **WHEN** the player presses the bottom section and moves their finger
- **THEN** a drag of that section starts

#### Scenario: Preview section cannot be dragged
- **WHEN** the player presses a section that is not the bottom one and moves their finger
- **THEN** no drag starts and the stack stays the same

### Requirement: Dragged section follows the finger
While a drag is going on, the system SHALL draw a copy of the dragged section under the player's finger. The section's own slot in the stack SHALL look empty while it is dragged. Only one drag SHALL be possible at a time.

#### Scenario: Copy follows the finger
- **WHEN** the player drags the next section across the screen
- **THEN** a copy of that section moves with the finger, and the bottom slot of the stack looks empty

#### Scenario: Second finger does not start a second drag
- **WHEN** a drag is going on and the player presses the stack with a second finger
- **THEN** no second drag starts

### Requirement: Stack changes only when the board accepts the placement
When the player lets go of a dragged section, the system SHALL ask the board to place that section at the release point. The board decides whether it accepts the placement. Only when the board accepts it SHALL the stack remove the next section, move the others down and add a new section at the top. The stack SHALL change at most once per drop.

#### Scenario: Accepted placement
- **WHEN** the player drops the next section on a board cell that accepts it
- **THEN** the section is placed on that cell, it is removed from the stack, the other sections move down and a new section is added at the top

#### Scenario: Placement cannot be counted twice
- **WHEN** the board accepts one drop
- **THEN** the stack changes exactly once

### Requirement: Rejected or cancelled drags leave the stack unchanged
When a drag ends without an accepted placement, the system SHALL leave the stack unchanged. The dragged section SHALL move back into its slot. A drag ends without a placement when the section is dropped outside the board, when the board rejects the drop, or when the system cancels the gesture.

#### Scenario: Dropped outside the board
- **WHEN** the player drops the next section outside the board
- **THEN** the section moves back to the bottom slot and the stack stays the same

#### Scenario: Board rejects the drop
- **WHEN** the player drops the next section on a cell that cannot take a pipe, such as a blocked cell
- **THEN** the section moves back to the bottom slot and the stack stays the same

#### Scenario: Gesture is cancelled
- **WHEN** the system cancels the drag gesture before the player lets go
- **THEN** the section moves back to the bottom slot and the stack stays the same

### Requirement: Stack is a vertical column on the left of the game screen
The system SHALL draw the stack as one vertical column along the left edge of the game screen, in landscape. The column SHALL show every section in the stack. The top of the column SHALL be the top of the stack, and the next section SHALL be at the bottom of the column. The next section SHALL be highlighted and drawn larger than the preview sections. The column SHALL fit the screen height without scrolling for every allowed `stack.visibleSize` (1 to 10).

#### Scenario: Column layout
- **WHEN** the game screen is shown for a level with a `stack.visibleSize` of 5
- **THEN** the left edge of the screen shows a column of 5 sections with the highlighted next section at the bottom

#### Scenario: Largest stack fits
- **WHEN** the game screen is shown on a 360 dp tall landscape screen for a level with a `stack.visibleSize` of 10
- **THEN** all 10 sections are fully visible and the column does not scroll

### Requirement: Stack sections are accessible
Each section in the stack SHALL have a content description that names the section and says whether it is the next section.

#### Scenario: Next section is described
- **WHEN** the next section is a left-top corner
- **THEN** its content description names the left-top corner and says that it is the next section
