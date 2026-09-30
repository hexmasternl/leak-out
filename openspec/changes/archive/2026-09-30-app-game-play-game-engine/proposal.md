## Why

The game screen is built from separate pieces: the stack, the HUD, the playing field, points, fast forward and the leak. Each open change added its own bit of state to the `GameSession` state holder in `app` and left the real rules to "the engine" or "the flow change" later. So the game state is spread over Compose state, nothing moves the liquid, and a restart has to reset every field by hand. The plan (section 2.6) calls for one `GameEngine` that owns the whole game. Now that all the parts are specified, this is the right time to bring them together in one engine that can be tested on the JVM.

## What Changes

- Add a **game engine** in `core`: the single control system of the game domain model. It holds one immutable `GameState` for the running attempt, and every change to the game goes through a command.
- The `GameState` holds everything about the attempt: the level, the board with the placed pipe sections, the stack, the flow state (countdown, liquid head, used channels), the score sheet and the points awarded, the replace lock, the game clock, paused, fast forward, and the outcome (playing, won or lost).
- **Commands**: start a level, place the next section on a tile, pause, resume, restart, turn fast forward on, go back to normal speed, and tick (advance the game clock). The engine validates every command. A command that is not allowed in the current state (placing while paused, resuming a lost level) leaves the state unchanged.
- **Loading a level**: starting a level builds a fresh state from the `Level`: an empty board with the start, finish and blocked tiles, a newly drawn stack, the countdown at the level's start delay, score 0, not paused and fast forward off.
- **Pausing**: opening the pause dialog with the cog wheel (or Back, or going to the background) sends pause. While paused, a tick does nothing: the game clock, the countdown, the liquid, the points and the replace lock stay where they are. Resume continues from exactly that point.
- **The flow runs inside the engine**: tick counts down the start delay and then moves the liquid head through the placed sections at the level's speed, with the speed ramp per cell, the reservoir slow-down and the fast forward time scale. When the head leaves a tile it awards the points (`app-game-play-points`), takes the next step (`FlowStep` from `app-game-play-level-lost`) and ends in a win at the finish or a loss at a leak. Large ticks are split so that no tile is skipped.
- **The views render from the engine**: the stack, the HUD, the playing field, the points popups, the fast forward button and the dialogs read only from `GameState`. They keep no game data of their own. Drag and drop asks the engine whether a tile can take the section and sends the place command on a drop.
- **A (re)start resets everything**: restart and starting another level replace the whole `GameState` with a fresh one and start a new attempt. The views follow: the stack shows the new sections, the HUD shows score 0 and grey stars, and the playing field shows an empty board at its starting zoom and position, with no popups, leak effect or drag left over.
- `GameSession` in `app` becomes a thin holder of the current `GameState`. It sends commands to the engine and drives the frame loop. **BREAKING** (internal only): the entry points that the open changes added for a future flow (`liquidExited`, `liquidLeaked`, the direct `score` and `board` fields) are removed; the engine does this work itself.

## Capabilities

### New Capabilities
- `game-engine`: The engine and its state: what the game state holds, the commands and when each is accepted, starting and restarting a level (full reset), pause and resume, placement through the engine, the game clock, the outcome, and the rule that every view renders from the game state.
- `game-play-flow`: The liquid flow as run by the engine: the countdown, how the head moves and how fast (base speed, ramp, reservoir, fast forward), what happens when it leaves a tile (points, next tile, win, leak), the cross's two channels, and splitting large ticks.

### Modified Capabilities

None. The stack, HUD, pause, playing-field, placement, points, fast-forward, leak and level-lost specs are all still in open changes and not in `openspec/specs/`. Their requirements stay the same: this change moves the state they describe into the engine and makes the flow, which they all wait for, actually run.

## Impact

- New pure Kotlin code in `core` (`nl.hexmaster.leakout.core.engine`): `GameState`, `GameCommand`, `GameEngine`, `Outcome` and `FlowState`, and a `FlowSimulation` that uses `FlowStep`, `ScoreSheet`, `Placement.check`, `RouteTracer` and `PipeStack`. JVM unit tests, including a headless test that plays a level from start to win and one from start to leak (the milestone 1 exit criterion in the plan).
- `app` (`ui/game/`): `GameSession` is rewritten as a wrapper around `GameState` with a `send(command)` method. `GameScreen`, `GameHud`, `StackPanel`, `PlayingField`, `FastForwardButton`, `PauseDialog` and `LevelLostDialog` read from `GameState`. Their remembered UI state (viewport, drag, stack slide animation) is keyed on the attempt, so it resets on restart. Unit and instrumented tests.
- This change builds on `app-game-play-stack`, `app-game-play-hud`, `app-game-play-playing-field`, `app-game-play-points`, `app-game-play-fast-forward` and `app-game-play-level-lost` and must merge after all of them. `app-game-play-level-complete` shows the win that this engine reports and can merge before or after it.
- `docs/project-plan.md` sections 2.5 and 2.6 are updated to the engine as built (drag to place, the `Side`-based model, the commands and the outcome).
