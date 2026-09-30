## 1. Setup

- [x] 1.1 Check that `app-game-play-stack`, `app-game-play-hud`, `app-game-play-playing-field`, `app-game-play-points`, `app-game-play-fast-forward` and `app-game-play-level-lost` are merged into `development` (`PipeStack`, `Board`/`Cell`, `Placement.check`, `ScoreSheet`, `PipeSection.exitFor`, `RouteTracer`, `FlowStep`/`ChannelUse`, and `GameSession` with `liquidExited` and `liquidLeaked`). If not, stop and merge them first.
- [x] 1.2 Run `./gradlew :core:test :app:testDebugUnitTest` and note the passing baseline.

## 2. Core: state and commands

- [x] 2.1 Create the package `nl.hexmaster.leakout.core.engine` with `Outcome` (`Playing`, `Won(atMs)`, `Lost(at, side, atMs)`), `Award(position, points, atMs)` with `VISIBLE_MS = 1000`, and `FlowState` (`Waiting`, `Flowing`, `Ended`).
- [x] 2.2 Add `GameState` with the fields from design decision 2 and the derived `score`, `stars`, `flowSpeed`, `flowTimeScale` and `isStopped`.
- [x] 2.3 Add `GameCommand` (`Start`, `Place`, `Pause`, `Resume`, `Restart`, `FastForward`, `Tick`).
- [x] 2.4 Add `GameEngine(random)` with `start(level, previous)`, building the fresh state and the attempt number.
- [x] 2.5 Implement `handle` for `Start`, `Restart`, `Pause`, `Resume` and `FastForward`, returning the same instance for every refused command.
- [x] 2.6 Implement `canPlace` and `handle(Place)` on one shared private check (`isStopped`, `fastForward`, `Placement.check`), with the stack advance, the replace lock after a replacement and the `RouteTracer` check.
- [x] 2.7 Unit test start and restart: fresh board, stack of the visible size, countdown at the start delay, score 0, attempt 1 then 2; restart after a loss, while paused and after a win clears everything; two engines with the same seed and commands give equal states.
- [x] 2.8 Unit test pause and resume: tick while paused returns the same instance; place and fast forward are refused while paused; pause is refused after a win and after a loss; resume continues the countdown from where it stopped.
- [x] 2.9 Unit test placement: place on empty, replace an unentered section, refuse a filled section, a fixed tile, a tile outside the grid, during the replace lock, while fast forward is on and after an outcome; `canPlace` equals the result of `Place` for every tile of a scripted board; a closing placement sets `routeComplete`; fast forward is refused on an open route.

## 3. Core: flow simulation

- [x] 3.1 Add the start tile's synthetic channel and a speed function (base × ramp × reservoir factor).
- [x] 3.2 Add `FlowSimulation.advance(state, flowDtMs)` with the border-walking loop from design decision 4: countdown, move, leave tile (fill channel, award, `FlowStep.after`), enter next tile with `markLiquidEntered`, win, leak, drop the rest after an end.
- [x] 3.3 Implement `handle(Tick)`: refuse negative ticks and paused states, advance `clockMs`, drop old awards, and run `advance` with `dt * flowTimeScale` only while playing.
- [x] 3.4 Unit test the countdown: partial ticks, the leftover of the tick that ends the countdown, 10x countdown under fast forward.
- [x] 3.5 Unit test the head: progress within a tile, overshoot carried to the next tile, the ramp after 4 sections, the reservoir factor, `flowSpeed` without the reservoir factor, a section counts as entered as soon as the head enters it.
- [x] 3.6 Unit test leaving a tile: points before a leak, leak straight from the start with score 0, win at the finish with the last section's points, cross second pass gives 50, cross used channel leaks, the leak reports the tile and side left.
- [x] 3.7 Unit test large ticks: one 5,000 ms tick through 3 sections awards in route order and wins; a long tick into a leak stops at the right tile; 1 ms ticks and one long tick give the same outcome and score.
- [x] 3.8 Unit test that every scripted complete-route board is won by the engine, and that the clock keeps running after an outcome while the score and flow do not change.
- [x] 3.9 Add the headless tests: play a bundled level from `start` to `Won` with scripted placements and ticks, and one to `Lost`.
- [x] 3.10 Run `./gradlew :core:test` and check that everything passes.

## 4. App: game session

- [x] 4.1 Rewrite `GameSession(level, engine)` to hold `state` in `mutableStateOf`, with `send(command): Boolean`, `canPlace(at)`, `showPauseDialog` and `showLostDialog`.
- [x] 4.2 Remove `liquidExited`, `liquidLeaked`, `pauseMenuOpen` and the stored score, board, outcome, route and fast forward fields; make `skipLeakEffect()` send a `Tick` for the rest of the leak effect.
- [x] 4.3 Point `PopupTiming.TOTAL_MS` at `Award.VISIBLE_MS` and read popups from `state.awards` and `state.clockMs`.
- [x] 4.4 Move the `GameSession` unit tests that called `liquidExited` or `liquidLeaked` to engine-driven tests; add tests that `send` returns false for refused commands and that the dialogs are derived correctly.

## 5. App: views render from the state

- [x] 5.1 In `GameScreen`, pass `state.stack`, `state.board`, `state.flow`, `state.awards`, `state.outcome` and the clock to the views, and wire the drop target, cog wheel, fast forward button, dialogs, `BackHandler` and `ON_STOP` to `send(...)`.
- [x] 5.2 Update `GameHud`, `StackPanel`, `PlayingField`, `FastForwardButton`, `PauseDialog` and `LevelLostDialog` call sites so none of them reads game data from anywhere but `GameState`.
- [x] 5.3 Change the frame loop to `LaunchedEffect(state.paused)` calling `send(Tick(dt))`, with a first delta of 0.
- [x] 5.4 Wrap the stack column and the playing field in `key(state.attempt)` and add `LaunchedEffect(state.attempt) { dragState.cancel() }`.
- [x] 5.5 Pass the head position and filled channels to `PlayingField` for a later liquid drawing; draw nothing new yet.

## 6. Tests and docs

- [x] 6.1 Instrumented test: place a route, tick until the flow ends, and check that the HUD score and the popups follow the game state and the outcome is won.
- [x] 6.2 Instrumented test: zoom and pan the field, place sections, open the pause dialog and tap Restart; check that the field is back at its starting fit, the board is empty, the HUD shows 0 and three grey stars, no popup is shown and the dialog is closed.
- [x] 6.3 Instrumented test: start a drag, then restart from code; check that the drag is cancelled and the stack shows the new sections.
- [x] 6.4 Instrumented test: pause with the cog wheel and with `ON_STOP`, tick, and check that nothing moves until Resume.
- [x] 6.5 Update `docs/project-plan.md` sections 2.5 and 2.6 to the engine as built (drag to place, `Side`-based model, the commands, `Outcome`, awards).
- [x] 6.6 Run `./gradlew :core:test :app:testDebugUnitTest :app:connectedDebugAndroidTest` on the emulator and check that everything passes.
