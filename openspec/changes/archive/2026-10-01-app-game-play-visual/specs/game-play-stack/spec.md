## ADDED Requirements

### Requirement: Stack sections use the pipe art
Every section in the stack column, and the copy of a dragged section under the finger, SHALL be drawn with the pipe tile: the section's SVG asset with its glass overlay, scaled to the slot or to the copy, without liquid. The stack SHALL NOT draw pipes from any other drawing.

#### Scenario: Stack shows the pipe art
- **WHEN** the game screen shows a stack that holds a reservoir and a cross
- **THEN** their slots show the reservoir and cross pipe tiles, each with its glass overlay

#### Scenario: Dragged copy shows the pipe art
- **WHEN** the player drags the next section, a left-bottom corner
- **THEN** the copy under the finger is the left-bottom corner pipe tile with its glass overlay
