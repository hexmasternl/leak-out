## 1. Setup

- [x] 1.1 Check that `app-game-play-stack`, `app-game-play-hud`, `app-game-play-playing-field`, `app-game-play-fast-forward`, `app-game-play-points` and `app-game-play-level-lost` are merged into `development` (`GameScreen`, `GameSession` with `outcome`, `isStopped`, `liquidExited`, `liquidLeaked`, `skipLeakEffect` and `restart`, `Board` and `Cell` in `core.board`, `ScoreSheet` in `core.score`, `FlowStep` in `core.flow`, `HudStars`, `LevelLostDialog`, `PlayingField` with the leak effect). If not, stop and merge them first.

## 2. Core: final score

- [x] 2.1 Add `Points.UNUSED_SECTION_PENALTY = 3` to `core.score.Points`.
- [x] 2.2 Add `FinalScore(earned, unusedSections, total)` in `core.score` with `unusedCount`, `penalty` and `FinalScore.of(earned, board)`: collect every `Cell.Pipe` with `liquidEntered == false` in row-major order and set `total = (earned - penalty).coerceAtLeast(0)`.
- [x] 2.3 Unit test `FinalScore`: a board with four unused sections gives a penalty of 12 and a total of earned minus 12; no unused sections gives penalty 0 and total equal to earned; earned 10 with five unused sections gives total 0; a cross with `liquidEntered` is not unused; start, finish and blocked tiles are never unused; the unused positions come in row-major order.
- [x] 2.4 Unit test the stars from the total with `StarBenchmarks(70, 110, 160)`: totals 108, 160 and 0 give 1, 3 and 0 stars.
- [x] 2.5 Run `./gradlew :core:test` and check that everything passes.

## 3. App: win timeline

- [x] 3.1 Add `WinTiming` (`FINISH_EFFECT_MS = 1000`, `EARNED_HOLD_MS = 500`, `PENALTY_STEP_MS = 120`, `PENALTY_MAX_MS = 2000`, `TOTAL_COUNT_MS = 800`, `STAR_STAGGER_MS = 300`, `STAR_POP_MS = 320`), `WinFrame` and the pure `winFrame(dialogAgeMs, score, stars)` in `ui/game/`, plus a `WinTally.totalDurationMs(score, stars)` helper for the skip.
- [x] 3.2 Unit test `winFrame`: at 0 ms the frame shows the earned points, penalty 0, total 0, no sections removed, no star lit and buttons disabled; during the penalty phase `sectionsRemoved` and `penaltyShown` grow one step at a time to `unusedCount` and `penalty`; the penalty phase takes `unusedCount * 120 ms` for 4 sections and at most 2000 ms for 40; with no unused sections the total starts counting at 500 ms; `totalShown` is monotonic and equals `total` at the end of the total phase; the stars light up 300 ms apart with a scale that rises above 1 and returns to 1 within 320 ms; missed stars stay at scale 1 and unlit; `buttonsEnabled` is false one ms before the end and true at `totalDurationMs`; with 0 stars the buttons are enabled right after the total phase.
- [x] 3.3 Add the pure `FinishFlashFrame(ringAlpha, ringScale)` and `finishFlashFrame(ageMs)` and unit test it: alpha and scale rise from the start, the scale peaks at about 1.3 within the effect, and from 1000 ms on the frame is the steady solid ring.

## 4. Engine and game session

> Adapted to `app-game-play-game-engine`, which merged after this change was written: the engine already sets `Outcome.Won(atMs)` on `FlowStep.Finish`, and `isStopped`, `Pause` and `Place` already treat a win like a loss. `GameSession` holds no game data, so the final score and the section removal live in `GameState` and go through a command (design decision 11).

- [x] 4.1 Add `finalScore: FinalScore?` and `sectionsRemoved: Int` to `GameState` (`null` and `0` in `GameEngine.start`) and the derived `finalStars` (`level.stars.starsFor(finalScore.total)`, 0 without a final score). Make `FlowSimulation` set `finalScore = FinalScore.of(sheet.total, board)` on `Finish`, after the last section's points.
- [x] 4.2 Add `GameCommand.RemoveUnused(upTo)`: refused unless won and `upTo` is above `sectionsRemoved`; otherwise replaces the next positions of `finalScore.unusedSections` with `Cell.Empty` until `sectionsRemoved == min(upTo, unusedCount)`.
- [x] 4.3 Unit test the engine: a win sets `finalScore` with the right earned points and unused sections in row-major order, the route sections not unused; `finalStars` follows the total; `RemoveUnused` removes one section per step, is refused while playing or lost and for a count already reached, and is capped at `unusedCount`; restart clears `finalScore` and `sectionsRemoved`.
- [ ] 4.4 Add `winAgeMs`, `showWinDialog` (`winAgeMs >= FINISH_EFFECT_MS`) and `winFrame` to `GameSession`. After every accepted command while won, `send` also sends `RemoveUnused(winFrame.sectionsRemoved)`, so ticks remove the sections in step with the tally. Add `skipWinAnimation()`: tick through the rest of the finish effect and the tally, which removes every remaining unused section.
- [ ] 4.5 Unit test `GameSession`: a route played to the finish is won with the final score; after a win place and pause are refused and the pause dialog is not shown; `showWinDialog` is false at 999 ms and true at 1000 ms; ticking through the penalty phase removes the unused sections one per step in row-major order and leaves the entered sections; `skipWinAnimation()` removes all unused sections at once and gives an enabled frame; restart after a win gives `Playing`, score 0, no final score, an empty board and accepts placements again.

## 5. App: stars, button style and dialog

- [ ] 5.1 Extract `StarIcon(earned, scale, size, modifier)` from `HudStars` (tint, brass glow and `StarEarned` semantics) and keep `HudStars` behaviour and tests unchanged.
- [ ] 5.2 Add `WinStars(lit, scales)`: three 40 dp `StarIcon`s 12 dp apart with test tags `win_star_1` to `win_star_3` and the row content description "%1$d of 3 stars".
- [ ] 5.3 Add `LeakOutColors.SuccessGreenDark` and a success-green `CartoonButton` style, with the disabled look unchanged; add a unit test case for its metrics if `CartoonButtonMetricsTest` covers styles.
- [ ] 5.4 Add string resources: eyebrow "LEVEL COMPLETE", "Level complete!", "You kept the water flowing.", "Pipe points", "Unused pipes", "Total", "Next level", "Restart", "Exit", the pane title "Level complete" and the row content descriptions with placeholders.
- [ ] 5.5 Add `LevelCompleteDialog(levelId, score, stars, frame, hasNextLevel, onNextLevel, onRestart, onExit)` as a Compose `Dialog` with `dismissOnClickOutside = false`, `dismissOnBackPress = frame.buttonsEnabled` and `onDismissRequest = onExit`, in the design-system card: eyebrow and title, `WinStars`, the three score rows with `HudFormat.score` ("-0" for the penalty before it starts), a divider before the total, and Next level (success), Restart (primary) and Exit (error-red) `CartoonButton`s at least 48 dp high, enabled from `frame.buttonsEnabled` (and `hasNextLevel`), with `paneTitle` "Level complete".
- [ ] 5.6 Add `@Preview`s of `LevelCompleteDialog` at the start of the animation and at the end with 2 stars.

## 6. App: field and screen wiring

- [ ] 6.1 Pass a `() -> Long?` for `winAgeMs` to `PlayingField`, read it only inside the draw lambda, and draw the success-green finish ring from `finishFlashFrame` on the finish tile after the pipes and before the popups.
- [ ] 6.2 Add a `FinishFlash` semantics property to the cell overlay of the finish tile while won, for the instrumented tests.
- [ ] 6.3 In `GameScreen`: show `LevelCompleteDialog` while `showWinDialog` with `session.winFrame`; Restart sends `GameCommand.Restart`, Exit calls `onExit`, Next level calls `onNextLevel`; keep the drag cancel, `StackPanel` `enabled`, the cog wheel and the fast forward button on `isStopped`.
- [ ] 6.4 In `GameScreen`: widen the Back handling so Back does nothing while `outcome !is Playing` and the current dialog's buttons are not enabled, and make `ON_STOP` call `skipWinAnimation()` when won, `skipLeakEffect()` when lost and send `Pause` otherwise.
- [ ] 6.5 Add `onNextLevel: (() -> Unit)?` to `GameScreen` and, in `MainActivity`, resolve the bundled level with id `level.id + 1` (null when there is none) and switch `screen` to `Game(nextLevel)` on Next level.

## 7. Tests and docs

- [ ] 7.1 Instrumented test: place sections on and off the route, tick the flow through the route into the finish, and check that the finish flash is shown, that no dialog is shown before 1000 ms of game time, and that the level complete dialog with "Level complete!", the earned points, "-0", total 0, three grey stars and three disabled buttons is shown after it.
- [ ] 7.2 Instrumented test: tick through the animation and check that the penalty row and the field change in step (a removed section's cell is empty while an entered section stays), that the total ends on the final total, that the earned stars are lit with `StarEarned` and the others are not, and that the buttons become enabled at the end.
- [ ] 7.3 Instrumented test: while won, a drag of the next section does not start, the cog wheel does not open the pause dialog and the stack stays the same; a drag in progress when the finish is reached returns to its slot.
- [ ] 7.4 Instrumented test: with the buttons enabled, Restart closes the dialog, empties the board, shows score 0 and accepts a new drag; Next level shows the HUD of the next level with score 0; Exit shows the welcome screen; on the last level Next level stays disabled.
- [ ] 7.5 Instrumented test: tapping outside the dialog keeps it open; Back while the buttons are disabled keeps the dialog open without the pause dialog; Back with the buttons enabled shows the welcome screen; Back during the finish flash keeps the game screen open without the pause dialog.
- [ ] 7.6 Instrumented test: move the app to the background during the animation and back, and check that the dialog shows the final total, the lit stars and enabled buttons, that all unused sections are gone, and that the pause dialog is not shown.
- [ ] 7.7 Run `./gradlew test` and `./gradlew connectedAndroidTest` on the emulator and check that everything passes.
- [ ] 7.8 Update `docs/project-plan.md`: section 1.9 (reaching the finish wins, the finish flashes for 1 second, the final score is earned points minus 3 per unused section with a floor of 0, stars from the total, the animated tally and the level complete dialog) and section 1.11 item 5 (the complete overlay has Next level, Restart and Exit to the main menu).
