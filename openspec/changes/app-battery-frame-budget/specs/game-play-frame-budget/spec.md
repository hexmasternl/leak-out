# Spec Delta

## Purpose

Keeps the game screen from doing more work than the player can see, to save battery: the game clock runs only while something on the screen still moves with it, and never faster than about 60 ticks a second.

## ADDED Requirements

### Requirement: The game clock idles once an ended attempt is still
After a win or a loss, the game screen SHALL keep advancing the game clock only until every end effect driven by it has finished: the leak effect and the missed-section pulse, the finish flash, the win tally, and the points popups. After that the game screen SHALL send no further ticks, so the game state and the playing field stay unchanged until the player restarts, moves on to another level or leaves the screen.

#### Scenario: Idle behind the level lost dialog
- **WHEN** the level is lost and the leak effect has finished
- **THEN** the level lost dialog is shown and the game clock no longer advances

#### Scenario: Idle behind the level complete dialog
- **WHEN** the level is won and the finish flash, the win tally and the last points popup have finished
- **THEN** the level complete dialog shows the final tally and the game clock no longer advances

#### Scenario: End effects still play in full
- **WHEN** the level is won
- **THEN** the finish flash and the win tally play from start to end, as before

#### Scenario: Restart runs the clock again
- **WHEN** the game clock is idle behind a result dialog and the player restarts the level
- **THEN** the countdown of the new attempt runs

### Requirement: Game ticks are capped at about 60 per second
While the game clock runs, the game screen SHALL advance it at most about 60 times a second, whatever the display's refresh rate. The time of frames without a tick SHALL be added to the next tick, so the game clock, the countdown and the liquid move at the same speed as without the cap. Dragging a section, panning and zooming SHALL still follow the finger on every display frame.

#### Scenario: 120 Hz display
- **WHEN** the game runs on a display that refreshes 120 times a second, for one second of play
- **THEN** the game clock advances by about 1,000 ms in about 60 ticks

#### Scenario: No time lost
- **WHEN** frames arrive faster than the cap allows
- **THEN** the game clock after any tick equals the time elapsed since the clock started, as it would without the cap

#### Scenario: 60 Hz display
- **WHEN** the game runs on a display that refreshes 60 times a second
- **THEN** every frame advances the game clock

### Requirement: Pause still stops the clock at once
While the game is paused, or while the level-start tips are open, the game screen SHALL send no ticks, as before. After resuming, the first tick SHALL NOT include the time spent paused.

#### Scenario: Resume after a long pause
- **WHEN** the player pauses for 10 seconds and resumes
- **THEN** the game clock continues from where it stopped and does not jump 10 seconds
