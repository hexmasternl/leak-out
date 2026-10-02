## MODIFIED Requirements

### Requirement: Stack holds the level's visible number of sections
When a level starts, the system SHALL create a stack that holds exactly `stack.visibleSize + 10` pipe sections of that level. The stack SHALL hold that many sections for the whole game: every section that is removed SHALL be replaced in the same step.

#### Scenario: Stack is filled when a level starts
- **WHEN** a level with a `stack.visibleSize` of 5 starts
- **THEN** the stack holds 15 sections

#### Scenario: Largest stack
- **WHEN** a level with a `stack.visibleSize` of 10 starts
- **THEN** the stack holds 20 sections

#### Scenario: Stack keeps its size after a placement
- **WHEN** a section from a stack of 15 is placed on the board
- **THEN** the stack holds 15 sections again

### Requirement: New sections enter at the top and the next section is at the bottom
The stack SHALL be ordered from top to bottom. The bottom section SHALL be the next section, the one that is placed next. A new section SHALL always be added at the top. When the next section is removed, every other section SHALL move down one slot. Every section SHALL keep the same identity while it moves down, so that it can be followed from slot to slot.

#### Scenario: Placement moves the stack down
- **WHEN** the stack holds, from top to bottom, `H`, `V`, `LT`, `LB`, `RT`, the `RT` is placed, and `X` is drawn
- **THEN** the stack holds, from top to bottom, `X`, `H`, `V`, `LT`, `LB`, and `LB` is the next section

#### Scenario: Sections keep their identity
- **WHEN** the section second from the bottom has a given identity and the next section is placed
- **THEN** the new next section has that same identity, and the new top section has an identity no other section in the stack has

### Requirement: Stack is a vertical column on the left of the game screen
The system SHALL draw the stack as one vertical column along the left edge of the game screen, in landscape, with the next section at the bottom of the column and the top of the stack above it. The next section SHALL be highlighted and drawn larger than the preview sections. The slot sizes SHALL be chosen so that the bottom `stack.visibleSize` sections fit the column height, and those sections SHALL always be fully visible. The other sections SHALL continue upwards at the preview size as far as the column has room. A section that does not fit SHALL be clipped at the top edge of the column: a partly fitting section shows only its lower part and a section above the edge is not shown. The column SHALL NOT scroll.

#### Scenario: Column layout
- **WHEN** the game screen is shown for a level with a `stack.visibleSize` of 5
- **THEN** the left edge of the screen shows a column with the highlighted next section at the bottom and at least 5 fully visible sections

#### Scenario: Largest stack fits
- **WHEN** the game screen is shown on a 360 dp tall landscape screen for a level with a `stack.visibleSize` of 10
- **THEN** at least the bottom 10 sections are fully visible and the column does not scroll

#### Scenario: Sections that do not fit are clipped off the top
- **WHEN** the game screen is shown on a 360 dp tall landscape screen for a level with a `stack.visibleSize` of 10 and a stack of 20
- **THEN** the sections that do not fit are hidden above the top edge of the column, the section at the edge is cut off at that edge, and nothing is drawn outside the column

#### Scenario: Taller screen shows more sections
- **WHEN** the same level is shown on a taller screen
- **THEN** the column shows at least as many fully visible sections as on the smaller screen, at the same slot size

### Requirement: Stack sections are accessible
Each fully visible section in the stack SHALL have a content description that names the section and says whether it is the next section. A section that is clipped, partly or entirely, SHALL NOT be exposed to accessibility services.

#### Scenario: Next section is described
- **WHEN** the next section is a left-top corner
- **THEN** its content description names the left-top corner and says that it is the next section

#### Scenario: Clipped section is not announced
- **WHEN** a section is cut off at the top edge of the column
- **THEN** no accessibility node describes that section

## ADDED Requirements

### Requirement: Sections fall into place with gravity
When the board accepts a placement, the system SHALL animate every remaining section from its old slot down to its new slot as if it falls under gravity: it SHALL start slowly and speed up during the fall, land with one small bounce of at most 4 dp and come to rest. The section nearest the bottom SHALL start falling first and each section above it SHALL start a little later. The section that becomes the next section SHALL grow to the next-section size while it falls. The new section SHALL fall in from above the top edge of the column into the top slot. A fall of one slot SHALL take no longer than 400 ms from the placement until the section rests.

#### Scenario: Sections drop after a placement
- **WHEN** the board accepts a placement from the stack
- **THEN** every remaining section falls one slot down, accelerating, bounces once and rests in its new slot

#### Scenario: Cascade from the bottom up
- **WHEN** the sections fall after a placement
- **THEN** the section that becomes the next section starts falling before the sections above it

#### Scenario: New section falls in on top
- **WHEN** a new section is added to the stack
- **THEN** it falls into the top slot from above the top edge of the column

#### Scenario: Fall time
- **WHEN** a section falls one slot
- **THEN** it rests in its new slot no later than 400 ms after the placement

### Requirement: Placements during a fall continue from the current position
When the board accepts a placement while sections are still falling, the system SHALL make each section continue from its current position towards its new slot. A section SHALL never jump back up or snap to a slot.

#### Scenario: Two quick placements
- **WHEN** the player places a section and places the next one before the stack has come to rest
- **THEN** each section keeps falling from where it was to the slot two places below where it started, without moving up

### Requirement: The next section can be dragged while it falls
The next section SHALL be draggable as soon as the board accepts the previous placement, also while it is still falling. When a drag starts during the fall, its slot SHALL show empty at once and the copy SHALL follow the finger as for any drag. The fall SHALL NOT change any game state.

#### Scenario: Drag during the fall
- **WHEN** the player starts dragging the next section while it is still falling into the bottom slot
- **THEN** a drag of that section starts, its slot shows empty and the copy follows the finger

### Requirement: Stack motion respects reduced motion
When the system setting to remove animations is on, the system SHALL NOT accelerate, bounce or stagger the stack sections. Every section SHALL instead move to its new slot over 160 ms with an ease-out curve, and the new top section SHALL fade in in its slot over 160 ms.

#### Scenario: Reduced motion
- **WHEN** animations are turned off and the board accepts a placement
- **THEN** all sections move down together over 160 ms ease-out without a bounce and the new top section fades in
