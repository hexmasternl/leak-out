# game-engine Specification

## Purpose
TBD - created by syncing change app-game-play-game-engine. Update Purpose after archive.
## Requirements
### Requirement: One game state per attempt
The system SHALL keep all data of the running attempt in one game state owned by the game engine. The game state SHALL hold the level, the board with the start, finish, blocked tiles and every placed pipe section, the stack, the flow state, the score and the points awarded so far, the replace lock, the game clock, whether the game is paused, whether fast forward is on, whether the route is complete, the outcome of the attempt (playing, won or lost) and an attempt number. The game state SHALL NOT be changed in place: every change SHALL produce a new game state.

#### Scenario: State after a placement
- **WHEN** the player places an `H` section on an empty tile
- **THEN** a new game state is produced whose board holds the `H` on that tile, and the previous game state still has an empty tile there

#### Scenario: Same commands give the same state
- **WHEN** two engines with the same random source start the same level and receive the same commands
- **THEN** both end with equal game states

### Requirement: Every change goes through a command
The system SHALL change the game state only by sending a command to the game engine. The commands SHALL be: start a level, place the next section on a tile, pause, resume, restart, turn fast forward on, go back to normal speed, and tick (advance the game clock by a number of milliseconds). A command that is not allowed in the current state SHALL leave the game state unchanged.

#### Scenario: Refused command
- **WHEN** the game is paused and a place command is sent
- **THEN** the game state is unchanged

#### Scenario: Negative tick
- **WHEN** a tick of -16 ms is sent
- **THEN** the game state is unchanged

### Requirement: Starting a level builds a fresh state
When a level is started, the engine SHALL build a new game state from that level: a board with only the start, finish and blocked tiles, a newly drawn stack of the level's visible size, the flow waiting with the countdown at the level's start delay, score 0 with no points awarded, no replace lock, the game clock at 0, not paused, fast forward off, the route not complete and the outcome playing. The attempt number SHALL be one higher than that of the state it replaces, or 1 for the first attempt.

#### Scenario: Level start
- **WHEN** level 3 with a start delay of 12,000 ms and a visible stack size of 5 is started
- **THEN** the board has no placed sections, the stack holds 5 sections, the countdown shows 12,000 ms, the score is 0 and the game is playing and not paused

### Requirement: Restart resets the whole attempt
When restart is sent, the engine SHALL replace the whole game state with a fresh state for the same level, exactly as when the level is started, with a new stack drawn from the random source and the attempt number increased by one. Restart SHALL be accepted while playing, while paused, after a win and after a loss. Nothing of the previous attempt SHALL remain: no placed section, used channel, awarded points, replace lock, fast forward or outcome.

#### Scenario: Restart after a loss
- **WHEN** the level is lost with 4 placed sections and a score of 55, and restart is sent
- **THEN** the board has no placed sections, the score is 0, the flow is waiting at the full start delay, fast forward is off, the outcome is playing and the attempt number is one higher

#### Scenario: Restart from the pause dialog
- **WHEN** the game is paused and restart is sent
- **THEN** the new attempt is not paused

### Requirement: Pause freezes the game
When pause is sent while the outcome is playing, the engine SHALL mark the game as paused. While paused, a tick SHALL change nothing: the game clock, the countdown, the liquid, the score, the points popups and the replace lock SHALL stay where they are, and a place or fast forward command SHALL be refused. Pause SHALL be refused after a win or a loss. Sending pause while already paused SHALL change nothing.

#### Scenario: Tick while paused
- **WHEN** the liquid is halfway through a tile, the game is paused and ticks for 5,000 ms are sent
- **THEN** the liquid is still halfway through that tile and the game clock has not moved

#### Scenario: Pause after a loss
- **WHEN** the level is lost and pause is sent
- **THEN** the game is not paused and the state is unchanged

### Requirement: Resume continues where the game stopped
When resume is sent while paused, the engine SHALL clear the paused flag, and the next tick SHALL continue from exactly the state the game had when it was paused. Resume SHALL change nothing when the game is not paused.

#### Scenario: Resume
- **WHEN** the countdown is at 7,000 ms, the game is paused, then resumed, and a tick of 1,000 ms is sent
- **THEN** the countdown is at 6,000 ms

### Requirement: Placement is decided by the engine
When a place command for a tile is sent, the engine SHALL decide whether the stack's next section may go there, using the placement rule (empty tile: place; placed section the liquid has not entered and replacing allowed: replace; anything else: refuse), the replace lock, pause, fast forward and the outcome. On acceptance the engine SHALL put the section on the tile, move the stack down and draw a new section, start the replace lock after a replacement, and check whether the route is now complete. The engine SHALL also answer, without changing the state, whether a tile can take the next section, and this answer SHALL be the same as the result of placing there.

#### Scenario: Place on an empty tile
- **WHEN** the next section is `LT` and a place command for an empty tile is sent
- **THEN** the tile holds `LT`, the section that was second from the bottom is now the next section, and a new section is at the top of the stack

#### Scenario: Place on a filled section
- **WHEN** a place command is sent for a tile whose section the liquid has entered
- **THEN** the game state is unchanged

#### Scenario: Query agrees with placing
- **WHEN** the engine answers that a tile cannot take the next section
- **THEN** a place command for that tile leaves the game state unchanged

#### Scenario: Placement completes the route
- **WHEN** a placement closes the route from the start to the finish
- **THEN** the route is marked complete in the new state

### Requirement: Fast forward through the engine
When fast forward is sent while the route is complete, the outcome is playing and the game is not paused, the engine SHALL turn fast forward on. While fast forward is on, place commands SHALL be refused. When normal speed is sent while fast forward is on, the outcome is playing and the game is not paused, the engine SHALL turn fast forward off, which unlocks placing again (the slow button of the fast-forward change). Pause and resume SHALL keep fast forward as it is; a new attempt SHALL start with it off. Fast forward SHALL be refused when the route is not complete.

#### Scenario: Fast forward on a complete route
- **WHEN** the route is complete and fast forward is sent
- **THEN** fast forward is on and a following place command is refused

#### Scenario: Fast forward on an open route
- **WHEN** the route is not complete and fast forward is sent
- **THEN** fast forward stays off

#### Scenario: Back to normal speed
- **WHEN** fast forward is on and normal speed is sent
- **THEN** fast forward is off and a place command on an empty tile is accepted

### Requirement: The game clock advances with ticks
A tick of `dt` milliseconds SHALL advance the game clock by `dt` while the game is not paused. The replace lock and the points popups SHALL use the game clock. After a win or a loss the game clock SHALL keep advancing, so the end effects can play, but the flow, the countdown and the score SHALL NOT change.

#### Scenario: Replace lock runs out
- **WHEN** a section is replaced with a replace penalty of 500 ms, and ticks for 500 ms are sent
- **THEN** a place command on an empty tile is accepted

#### Scenario: Clock after a loss
- **WHEN** the level is lost and a tick of 300 ms is sent
- **THEN** the game clock is 300 ms further and the score and the flow are unchanged

### Requirement: The outcome ends the attempt once
The outcome SHALL start as playing. It SHALL become won when the liquid enters the finish, and lost when the liquid leaks. Once won or lost, the outcome SHALL NOT change again until the next attempt starts, and place, pause, resume, fast forward and normal speed commands SHALL be refused.

#### Scenario: Win
- **WHEN** the liquid enters the finish through its open side
- **THEN** the outcome is won and a following place command is refused

#### Scenario: No second outcome
- **WHEN** the outcome is won and further ticks are sent
- **THEN** the outcome stays won

### Requirement: Views render from the game state
The stack, the HUD, the playing field, the points popups, the fast forward button, the pause dialog and the level lost dialog SHALL show only data read from the current game state. They SHALL keep no game data of their own. When a new game state is produced, every view SHALL show the new state in the next frame. User actions on the game screen (drop a section, tap the cog wheel, Back, Resume, Restart, fast forward) SHALL reach the game only as commands to the engine.

#### Scenario: HUD follows the score
- **WHEN** the liquid leaves a corner section and the score in the game state goes from 40 to 55
- **THEN** the HUD shows 55

#### Scenario: Stack follows the state
- **WHEN** a placement is accepted
- **THEN** the stack column shows the sections of the new game state's stack

#### Scenario: Drop goes to the engine
- **WHEN** the player drops the next section on a tile
- **THEN** a place command for that tile is sent to the engine and the drop is accepted exactly when the engine accepts it

### Requirement: Views reset on a new attempt
When a new attempt starts (restart, or starting another level), every view SHALL drop the screen state of the previous attempt. The stack SHALL show the new sections without a slide animation from the old ones. The HUD SHALL show score 0 and three grey stars. The playing field SHALL show the empty board at the zoom and position it uses when a level starts, with no points popups and no leak effect. A drag that is going on SHALL be cancelled and the section SHALL go back to its slot. The pause dialog and the level lost dialog SHALL be closed.

#### Scenario: Field after restart
- **WHEN** the player has zoomed in and panned to a corner of the level and taps Restart in the pause dialog
- **THEN** the field shows the empty board at its starting zoom and position

#### Scenario: HUD after restart
- **WHEN** the score is 120 with two stars earned and the player restarts
- **THEN** the HUD shows score 0 and three grey stars

#### Scenario: Popups cleared on restart
- **WHEN** a "+15" popup is showing and the player restarts
- **THEN** no popup is shown on the field

### Requirement: Pause dialog follows the engine
The pause dialog SHALL be shown exactly while the game state is paused and the outcome is playing. Tapping the cog wheel, pressing Back and the app going to the background SHALL send pause. Resume in the dialog and Back while the dialog is open SHALL send resume. Restart in the dialog SHALL send restart.

#### Scenario: Cog wheel
- **WHEN** the player taps the cog wheel while playing
- **THEN** pause is sent, the game state is paused and the pause dialog is shown

#### Scenario: Background
- **WHEN** the app goes to the background during a level and the player comes back
- **THEN** the game state is paused and the pause dialog is shown
