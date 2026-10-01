## MODIFIED Requirements

### Requirement: One game state per attempt
The system SHALL keep all data of the running attempt in one game state owned by the game engine. The game state SHALL hold the level, the board with the start, finish, blocked tiles and every placed pipe section, the stack, the flow state, the score and the points awarded so far, the replace lock, the game clock, whether the game is paused, whether fast forward is on, whether the route is complete, the outcome of the attempt (playing, won or lost), an attempt number, and the attempt facts: the number of accepted placements, the number of replacements, whether the game was ever paused, whether the route was completed before the flow started, and whether the route was completed with a photo finish. The game state SHALL NOT be changed in place: every change SHALL produce a new game state.

#### Scenario: State after a placement
- **WHEN** the player places an `H` section on an empty tile
- **THEN** a new game state is produced whose board holds the `H` on that tile, and the previous game state still has an empty tile there

#### Scenario: Same commands give the same state
- **WHEN** two engines with the same random source start the same level and receive the same commands
- **THEN** both end with equal game states

### Requirement: Restart resets the whole attempt
When restart is sent, the engine SHALL replace the whole game state with a fresh state for the same level, exactly as when the level is started, with a new stack drawn from the random source and the attempt number increased by one. Restart SHALL be accepted while playing, while paused, after a win and after a loss. Nothing of the previous attempt SHALL remain: no placed section, used channel, awarded points, replace lock, fast forward, outcome or attempt fact.

#### Scenario: Restart after a loss
- **WHEN** the level is lost with 4 placed sections and a score of 55, and restart is sent
- **THEN** the board has no placed sections, the score is 0, the flow is waiting at the full start delay, fast forward is off, the outcome is playing and the attempt number is one higher

#### Scenario: Restart from the pause dialog
- **WHEN** the game is paused and restart is sent
- **THEN** the new attempt is not paused

#### Scenario: Restart clears the attempt facts
- **WHEN** an attempt with 6 placements, 1 replacement and one pause is restarted
- **THEN** the new attempt has 0 placements, 0 replacements and was never paused

## ADDED Requirements

### Requirement: Attempt facts are recorded
The engine SHALL record the attempt facts as it handles commands:
- Every accepted place command SHALL add one to the placements; an accepted place command that replaces a section SHALL also add one to the replacements.
- An accepted pause command SHALL mark the attempt as ever paused, and resume SHALL NOT clear it.
- When a placement makes the route complete while the flow is still waiting, the route SHALL be marked as completed before the flow.
- When a placement makes the route complete while the liquid is flowing and the placed tile is the tile the liquid head's current channel exits into, the attempt SHALL be marked as a photo finish.
A refused command SHALL NOT change any attempt fact.

#### Scenario: Placements and replacements
- **WHEN** the player places sections on 3 empty tiles and then replaces one of them
- **THEN** the attempt has 4 placements and 1 replacement

#### Scenario: Refused placement
- **WHEN** a place command is refused because the tile's section is filled
- **THEN** the placements stay the same

#### Scenario: Pause is remembered
- **WHEN** the game is paused and then resumed
- **THEN** the attempt is marked as ever paused

#### Scenario: Route ready during the countdown
- **WHEN** a placement completes the route while the countdown is running
- **THEN** the route is marked as completed before the flow

#### Scenario: Photo finish
- **WHEN** the liquid head runs through a section whose exit points at an empty tile and a placement on that tile completes the route
- **THEN** the attempt is marked as a photo finish

#### Scenario: No photo finish further ahead
- **WHEN** a placement completes the route on a tile two or more tiles ahead of the liquid head
- **THEN** the attempt is not marked as a photo finish
