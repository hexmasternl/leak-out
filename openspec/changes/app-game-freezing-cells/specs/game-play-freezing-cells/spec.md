# Spec Delta

## ADDED Requirements

### Requirement: A freezing cell takes any section
A freezing cell SHALL be a free cell of the board: the player SHALL be able to place any section on it, and replace a section on it before the liquid enters it, under the same placement rules as any other free cell. A freezing cell SHALL start every attempt unfrozen.

#### Scenario: Place a cross on a freezing cell
- **WHEN** the next section is a cross and the player drops it on an empty freezing cell
- **THEN** the cross is placed on that cell

#### Scenario: Replace before the liquid arrives
- **WHEN** replacing is allowed, a freezing cell holds an `H` the liquid has not entered and the player drops a `V` on it
- **THEN** the replacement runs as on any other cell and the cell ends up holding the `V`

#### Scenario: Restart unfreezes
- **WHEN** a section on a freezing cell has frozen and the player restarts the level
- **THEN** the freezing cell is empty and unfrozen

### Requirement: The section on a freezing cell freezes once the liquid has left it
When the liquid leaves the section on a freezing cell, that section SHALL freeze, for the rest of the attempt. While the liquid is still inside the section, it SHALL NOT be frozen. Ice SHALL NOT melt during an attempt.

#### Scenario: Freezes when the liquid leaves
- **WHEN** the liquid leaves an `H` on a freezing cell
- **THEN** that section is frozen

#### Scenario: Not frozen while the liquid is inside
- **WHEN** the liquid head is at progress 0.6 in a cross on a freezing cell
- **THEN** the cross is not frozen yet

#### Scenario: Stays frozen
- **WHEN** a section on a freezing cell has frozen and the liquid flows on for 20 more seconds
- **THEN** the section is still frozen

### Requirement: A frozen section refuses the liquid
Liquid that reaches a frozen section SHALL NOT enter it, through any side or channel. The liquid SHALL leak at the tile it left, through the side facing the frozen section, and the level SHALL be lost as with any other leak.

#### Scenario: Cross on a freezing cell freezes after its first pass
- **WHEN** a cross lies on a freezing cell, the liquid passes it from left to right and later reaches it from the top
- **THEN** the liquid leaks at the tile above the cross through its bottom side, the level is lost, and the cross scored 10 points for its only pass

#### Scenario: Cross on a normal cell is unaffected
- **WHEN** a cross lies on a cell that is not a freezing cell, the liquid passes it from left to right and later enters it from the top
- **THEN** the liquid passes it to the bottom and 50 points are awarded when it leaves

### Requirement: Freezing cells and ice are drawn on the field
The field SHALL draw a freezing cell with the design system's frost tile: a tile tinted with `ice.tint` and edged in `ice.300`, with or without a section on it, so the player can tell a freezing cell apart from an empty cell, a blocked cell and a mandatory section before placing on it. A section on a freezing cell SHALL be drawn with its normal pipe art on that tile. When the section freezes, its liquid SHALL turn to ice, spreading from the exit side to the entry side over 400 ms of game time, and the tile SHALL then show a frosted glass overlay. With reduced motion the liquid SHALL turn to ice at once. The frost tile SHALL follow pan and zoom.

#### Scenario: Empty freezing cell is drawn
- **WHEN** a level with a freezing cell starts
- **THEN** the field draws that cell with the frost tile, different from empty and blocked cells

#### Scenario: Liquid turns to ice
- **WHEN** the liquid leaves an `H` on a freezing cell
- **THEN** the liquid in that `H` turns to ice from its exit side to its entry side within 400 ms, and the tile shows the frosted overlay

#### Scenario: Reduced motion
- **WHEN** the system setting to remove animations is on and the liquid leaves a section on a freezing cell
- **THEN** the liquid in that section is drawn as ice at once

### Requirement: Freezing cells are described to a screen reader
A screen reader SHALL describe a freezing cell as a freezing cell together with what it holds, and a frozen section as frozen. The texts SHALL come from string resources, like every other cell description.

#### Scenario: Empty freezing cell
- **WHEN** a screen reader reaches an empty freezing cell
- **THEN** it reads "Freezing cell, empty"

#### Scenario: Section on a freezing cell
- **WHEN** a screen reader reaches a dry cross on a freezing cell
- **THEN** it reads "Placed Cross section on a freezing cell"

#### Scenario: Frozen section
- **WHEN** a screen reader reaches a cross on a freezing cell that the liquid has left
- **THEN** it reads "Placed Cross section, frozen"
