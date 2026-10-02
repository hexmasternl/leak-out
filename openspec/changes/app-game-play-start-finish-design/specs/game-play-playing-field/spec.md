## MODIFIED Requirements

### Requirement: Field draws the level cells
The field SHALL draw the grid, the start cell with its open side, the finish cell with its open side, every blocked cell and every placed section. Each kind of cell SHALL look different from the others. Placed sections SHALL be drawn with the same pipe art as the stack: the section's SVG asset with its glass overlay, scaled to the cell and following pan and zoom. The start SHALL be drawn as the start valve and the finish as the finish flag, as described in the design system, so the player can tell at a glance where the liquid comes from and where it has to go.

#### Scenario: Level cells are drawn
- **WHEN** a level with blocked cells starts
- **THEN** the field shows the grid, the start valve and the finish flag with their openings, and the blocked cells, all different from empty cells

#### Scenario: Placed section is drawn
- **WHEN** a section is placed on a cell
- **THEN** the field draws that section's pipe art with its glass overlay on the cell

#### Scenario: Pipe art follows zoom
- **WHEN** the player zooms in on a placed section
- **THEN** the section's pipe art is drawn at the new cell size, filling the cell

## ADDED Requirements

### Requirement: Start valve
The start cell SHALL be drawn on the empty-cell tile, without a coloured tint, as a pipe stub and a valve wheel. The stub SHALL be the half of the straight section's pipe art (horizontal for a left or right open side, vertical for a top or bottom open side) between the centre of the cell and the open side, including the flange and the glass overlay, so it joins a section placed against the open side the same way two sections join. On top of the stub, centred in the cell, the field SHALL draw a brass valve wheel (radius 0.24 of the cell, four spokes, a hub) and, on the stub between the wheel and the open side, a brass chevron pointing out of the open side. The start SHALL use no success or leak colour.

#### Scenario: Start valve faces its open side
- **WHEN** a level starts whose start opens to the right
- **THEN** the start cell shows the brass valve wheel in its centre, a horizontal pipe stub from the centre to the right edge, and a chevron on the stub pointing right

#### Scenario: Stub joins a placed section
- **WHEN** the player places a horizontal straight directly right of a start that opens to the right
- **THEN** the stub's flange and bore line up with the section's flange and bore on the shared edge

#### Scenario: Start valve follows zoom
- **WHEN** the player zooms in on the start
- **THEN** the stub, wheel and chevron are drawn at the new cell size, keeping their proportions

### Requirement: Finish flag
The finish cell SHALL be drawn on the empty-cell tile, without a coloured tint, as a pipe stub and a finish ring with a checkered flag. The stub SHALL be the half of the straight section's pipe art between the open side and the centre of the cell, including the flange and the glass overlay. On top of the stub, centred in the cell, the field SHALL draw a dark disc with a ring in the success colour (radius 0.24 of the cell) and, inside it, a checkered flag: a light pole and a waving cloth of 4 × 3 squares alternating light and dark. The finish SHALL use no brass colour.

#### Scenario: Finish shows a checkered flag
- **WHEN** a level starts whose finish opens to the left
- **THEN** the finish cell shows a horizontal pipe stub from the left edge to the centre and, in the centre, a success-coloured ring holding a checkered flag

#### Scenario: Start and finish are told apart
- **WHEN** a level starts
- **THEN** the start shows a brass valve wheel and the finish a success-coloured ring with a checkered flag, and neither shows the other's symbol

### Requirement: Liquid leaves the start valve
While the liquid head is in the start cell, the field SHALL draw the liquid in the start's stub, from the centre of the cell towards the open side, up to the head's progress, the same way the liquid is drawn in a section's channel. Once the head has left the start, the stub SHALL stay filled for the rest of the attempt. Before the liquid starts, the stub SHALL be dry. The finish's stub SHALL stay dry; reaching the finish is shown by the finish flash.

#### Scenario: Liquid comes out of the valve
- **WHEN** the countdown ends and the head is halfway through the start cell
- **THEN** the start's stub is filled from the centre half way to the open side

#### Scenario: Stub stays filled
- **WHEN** the head has moved on into the first placed section
- **THEN** the start's stub is completely filled

#### Scenario: Restart empties the stub
- **WHEN** the player restarts the level
- **THEN** the start's stub is dry again

### Requirement: Endpoint glows
While the countdown runs, the valve wheel SHALL have a brass glow that pulses with a period of 1.6 seconds. Once the liquid starts, the valve's glow SHALL stop pulsing and stay at a steady 60 %. Until the level is won, the finish ring SHALL have a success glow that pulses with a period of 2 seconds. When the level is won, the finish flash SHALL replace the pulse and the ring SHALL stay solid afterwards. The pulses SHALL follow game time, so they freeze while the game is paused. When the system setting to remove animations is on, both glows SHALL be steady at 60 % and SHALL NOT pulse.

#### Scenario: Valve pulses during the countdown
- **WHEN** a level starts and the countdown runs
- **THEN** the valve's glow grows and fades with a 1.6 second period

#### Scenario: Valve settles when the liquid starts
- **WHEN** the countdown ends
- **THEN** the valve's glow stays steady at 60 %

#### Scenario: Finish pulses until the win
- **WHEN** the liquid flows and the level is not yet won
- **THEN** the finish ring's glow grows and fades with a 2 second period

#### Scenario: Pulses freeze on pause
- **WHEN** the player pauses the game
- **THEN** the valve and finish glows stay as they were until the game resumes

#### Scenario: Reduced motion
- **WHEN** the system setting to remove animations is on and a level starts
- **THEN** the valve and the finish ring have steady glows at 60 % and do not pulse
