## 1. Setup

- [ ] 1.1 Check that `app-game-play-stack`, `app-game-play-hud`, `app-game-play-playing-field`, `app-game-play-fast-forward` and `app-game-play-points` are merged into `development` (`GameScreen`, `GameSession` with `pause`, `restart`, `elapsedMs` and `liquidExited`, `Board` and `Cell` in `core.board`, `PipeSection.exitFor`, `RouteTracer`, `PlayingField` with popups). If not, stop and merge them first.

## 2. Core: leak rule

- [ ] 2.1 Create the package `nl.hexmaster.leakout.core.flow` with `ChannelUse(position, channel)` and a helper that gives the channel a section uses for an entry side (horizontal or vertical for a cross, the single channel for every other section).
- [ ] 2.2 Add `FlowStep.after(board, from, exit, usedChannels)` returning `Enter(next, entry)`, `Finish` or `Leak(at, side)`, as an exhaustive `when` over `Cell` without `else`.
- [ ] 2.3 Unit test `FlowStep`: enter `H` from the left; reach the finish through its open side; leak into an empty tile, a blocked tile and the start tile; leak over each of the four grid edges; leak into `V` from the left; leak into a reservoir from above; leak into the finish from the wrong side; leak into a used cross channel from the opposite side; enter a cross on its free channel; the leak is reported at the tile left and the side left through.
- [ ] 2.4 Unit test the start check: `after(board, start.position, start.openSide, emptySet())` leaks when the tile next to the start is empty and enters when it holds a matching section.
- [ ] 2.5 Unit test that `FlowStep` and `RouteTracer` agree: for each scripted route board, following `FlowStep` from the start ends in `Finish` exactly when `RouteTracer` says the route is complete.
- [ ] 2.6 Run `./gradlew :core:test` and check that everything passes.

## 3. App: game session

- [ ] 3.1 Add `Outcome` (`Playing`, `Lost(at, side, atMs)`) and `LeakTiming.EFFECT_MS = 1000`.
- [ ] 3.2 Give `GameSession` `outcome` in Compose state, a read-only `isStopped` (`paused || outcome is Lost`), `lossAgeMs` and `showLostDialog`.
- [ ] 3.3 Add `GameSession.liquidLeaked(at, side)`: ignored while paused or already lost; otherwise sets `Lost` with `atMs = elapsedMs`. Write the flow contract (`liquidExited` first, then `FlowStep.after`, then `liquidLeaked` on `Leak`) in its KDoc.
- [ ] 3.4 Replace the `paused` checks in `place`, `canPlace` and `liquidExited` with `isStopped`, make `pause()` do nothing while lost, add `skipLeakEffect()`, and make `restart()` set `outcome` back to `Playing`.
- [ ] 3.5 Unit test `GameSession`: a leak sets `Lost` at the given tile and side; a second leak changes nothing; a leak while paused changes nothing; after a loss `place` is refused, `liquidExited` adds no points and `pause()` does not open the pause menu; `showLostDialog` is false at 999 ms of ticks and true at 1,000 ms; `skipLeakEffect()` makes it true at once; exit from a corner then leak gives 15 more points and a loss; restart after a loss gives `Playing`, score 0, an empty board and accepts placements again.

## 4. App: leak effect

- [ ] 4.1 Add the pure `LeakFrame(length, spread, alpha)` and `leakFrame(ageMs)` that grows the spurt during the effect and keeps a small puddle from 1,000 ms on.
- [ ] 4.2 Unit test `leakFrame`: at 0 ms the length is 0; the length grows and never goes down during the effect; alpha stays within 0 and 1; from 1,000 ms on the frame is the fixed puddle.
- [ ] 4.3 Pass the lost tile, side and a `() -> Long` for `lossAgeMs` to `PlayingField`, read them only inside the draw lambda, and draw the droplets from the middle of the leaking side outwards in the liquid color, after the pipes and before the popups.
- [ ] 4.4 Add a `LeakAt` semantics property to the cell overlay of the leaking tile with its side, for the instrumented tests.

## 5. App: level lost dialog and screen wiring

- [ ] 5.1 Add string resources: "Leaked!", "The pipe is leaking. Level lost.", "Score %1$s", "Restart", "Exit" and the pane title "Level lost".
- [ ] 5.2 Add `LevelLostDialog(score, onRestart, onExit)` as a Compose `Dialog` with `dismissOnClickOutside = false` and `dismissOnBackPress = true` (`onDismissRequest = onExit`), in the design-system card: red Fredoka title, a small leaking pipe drawing, the message, the score, Restart as the primary `CartoonButton`, Exit in the error-red style, both at least 48 dp high, and `paneTitle` "Level lost".
- [ ] 5.3 In `GameScreen`: show `LevelLostDialog` while `showLostDialog`; Restart calls `session.restart()`, Exit calls `onExit`; cancel the drag when `isStopped` becomes true; pass `enabled = !session.isStopped` to `StackPanel`; disable the cog wheel and hide the fast forward button while lost.
- [ ] 5.4 In `GameScreen`: limit the pause `BackHandler` to `outcome is Playing`, add a `BackHandler` that does nothing during the leak effect, and make `ON_STOP` call `skipLeakEffect()` when lost and `pause()` otherwise.
- [ ] 5.5 Add a `@Preview` of `LevelLostDialog` with a score of 85.

## 6. Tests and docs

- [ ] 6.1 Instrumented test: place a section, call `session.liquidLeaked` on its tile, and check that the leak effect is shown, that no dialog is shown before 1,000 ms of game time, and that the level lost dialog with "Leaked!", the score, Restart and Exit is shown after it.
- [ ] 6.2 Instrumented test: while lost, a drag of the next section does not start, the cog wheel does not open the pause dialog and the stack stays the same; a drag in progress when the leak happens returns to its slot.
- [ ] 6.3 Instrumented test: Restart closes the dialog, empties the board, shows score 0 and accepts a new drag; Exit shows the welcome screen.
- [ ] 6.4 Instrumented test: tapping outside the dialog keeps it open; Back on the dialog shows the welcome screen; Back during the leak effect keeps the game screen open without the pause dialog.
- [ ] 6.5 Instrumented test: move the app to the background after a loss and back, and check that the level lost dialog is shown and the pause dialog is not.
- [ ] 6.6 Run `./gradlew test` and `./gradlew connectedAndroidTest` on the emulator and check that everything passes.
- [ ] 6.7 Update `docs/project-plan.md`: section 1.9 (a leak stops the game, shows the spurt for 1 second, then the level lost dialog with the score, Restart and Exit to the main menu) and section 1.11 item 5 (the failed overlay has Restart and Exit).
