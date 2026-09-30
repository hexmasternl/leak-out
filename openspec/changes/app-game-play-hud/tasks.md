## 1. Setup

- [ ] 1.1 Check that `app-game-play-stack` is merged into `development` (`GameScreen`, `StackPanel`, `SectionDragState`, `PlaceholderBoard` exist). If not, stop and merge it first.
- [x] 1.2 Add string resources: `hud_level` ("Level %1$d"), `hud_speed_description` ("Flow speed %1$s cells per second"), `hud_stars_description` ("%1$d of 3 stars"), `hud_score_description` ("Score %1$s"), `hud_pause_menu` ("Pause menu"), `pause_title` ("Paused"), `pause_resume`, `pause_restart`, `pause_exit`.
- [x] 1.3 Add the `ic_cog.xml` cog wheel vector drawable, the `ic_star.xml` filled star vector (24 dp grid, design system section 8) and a small speed icon vector to `res/drawable`.
- [x] 1.4 Add `Brass100`, `Brass300`, `Brass500` and `TextMuted` to `LeakOutColors` from `docs/design-system.md` sections 2.4 and 2.7, next to the existing Deep Flow tokens.

## 2. Game session state

- [ ] 2.1 Add `GameSession(level, random)` in `app/.../ui/game/`, holding `stack`, `placed`, `score`, `flowSpeed`, `elapsedMs`, `paused` and `pauseMenuOpen` in Compose state, and a derived `stars` = `level.stars.starsFor(score)`. Start with score 0 and `flowSpeed = level.flow.cellsPerSecond`.
- [ ] 2.2 Add `pause()` (idempotent), `resume()`, `restart()` (new stack, empty `placed`, score 0, speed and clock reset, not paused), `tick(dtMs)` (no change while paused) and `place(...)` (refused while paused).
- [ ] 2.3 Unit test `GameSession`: `tick` does not move `elapsedMs` while paused; resume keeps stack, placed sections, score and clock; restart resets all of them and unpauses; `place` is refused while paused; pausing twice is the same as once.
- [ ] 2.4 Unit test `GameSession.stars` with thresholds 100, 200 and 350: 0 stars at 0 and 99 points, 1 at 100 and 199, 2 at 200, 3 at 350 and above; 0 again after restart.

## 3. HUD

- [x] 3.1 Add `HudFormat` with `level(id)`, `speed(cellsPerSecond, locale)` (one decimal) and `score(value, locale)` (digit grouping). Unit test with the English and Dutch locales (0.8 / 0,8, 1,250 / 1.250).
- [ ] 3.2 Add `HudLayout.HEIGHT` (48 dp) and a `StackLayoutTest` case: `visibleSize` 1 to 10 fits in 360 dp minus the HUD height.
- [x] 3.3 Add `HudStars(earned, modifier)`: a `Row` of three 20 dp `ic_star` icons 4 dp apart; not-earned stars in `TextMuted` without glow, earned stars in `Brass300` with a static radial `Brass500` glow at 45 % drawn behind them; a pop (scale 1.3 to 1 with the `motion.pop` spring) when a star becomes earned, `snap()` when `ANIMATOR_DURATION_SCALE == 0`; one merged content description "%1$d of 3 stars"; test tags `hud_star_1` to `hud_star_3` and a `StarEarned` semantics property per star.
- [x] 3.4 Add `GameHud(levelId, flowSpeed, stars, score, onPause, modifier)`: a full-width `Row` of fixed height with the level, the speed, a weighted spacer, `HudStars`, the end-aligned score and a 48 dp cog wheel `IconButton`, with content descriptions and test tags.

## 4. Pause dialog

- [x] 4.1 Add `PauseDialog(onResume, onRestart, onExit)`: a `Dialog` with `dismissOnClickOutside = false` and `onDismissRequest = onResume`, the design-system card with the "Paused" title and a `paneTitle` of "Pause menu", Resume and Restart `CartoonButton`s, a `HorizontalDivider`, and an Exit `CartoonButton` in the error-red style. Every button is at least 48 dp high.

## 5. Game screen wiring

- [ ] 5.1 Change `GameScreen` to `GameScreen(level, onExit, random)`: create `GameSession` with `remember(level)`, lay out `Column { GameHud; Row { StackPanel; PlaceholderBoard } }`, and read the stack, placed sections, score and stars from the session.
- [ ] 5.2 Add the frame loop: `LaunchedEffect(session.paused)` with `withFrameNanos` that calls `session.tick(dt)` while not paused, with a first delta of 0.
- [ ] 5.3 Add `SectionDragState.cancel()` (slide back without `tryPlace`), an `enabled` flag on `StackPanel`, and cancel any drag when the session pauses.
- [ ] 5.4 Show `PauseDialog` while `session.pauseMenuOpen`: Resume calls `session.resume()`, Restart calls `session.restart()`, Exit calls `onExit()`.
- [ ] 5.5 Add `BackHandler(enabled = !session.pauseMenuOpen) { session.pause() }` and `LifecycleEventEffect(ON_STOP) { session.pause() }`.
- [ ] 5.6 In `MainActivity`, replace the one-line Play wiring with a `Welcome` / `Game(level)` screen state; Play shows the game with level 1, and `onExit` goes back to `Welcome`.

## 6. Instrumented tests (emulator)

- [ ] 6.1 HUD: level 1 shows "Level 1", the level's speed, three not-earned stars and a score of 0; the level is left of the speed, the stars are left of the score, the score is left of the cog wheel, and the cog wheel is at the right edge; content descriptions are as in the spec, including "0 of 3 stars"; with `visibleSize` 10 on a 360 dp tall screen all sections and the HUD are visible.
- [ ] 6.2 Stars: with a level whose thresholds are 100, 200 and 350, setting the session score to 100, 199, 200 and 350 marks stars 1, 1, 2 and 3 as earned (`StarEarned` on `hud_star_n`) and the content description reads "1 of 3 stars", "1 of 3 stars", "2 of 3 stars" and "3 of 3 stars"; restart marks all three as not earned again; with the main clock advanced past the pop the earned star's scale is 1.
- [ ] 6.3 Cog wheel opens the dialog with Resume, Restart, a separator and Exit in that order, and the session is paused.
- [ ] 6.4 While paused, dragging the next section does nothing; a drag that is going on when the game pauses returns to its slot and the stack does not change.
- [ ] 6.5 Resume closes the dialog and keeps the placed sections, stack, score and stars; the game clock runs again.
- [ ] 6.6 Restart after placing a section clears the board, draws a new stack, shows a score of 0 with three not-earned stars and leaves the game running.
- [ ] 6.7 Exit shows the welcome screen.
- [ ] 6.8 Back on the game screen opens the dialog; Back in the dialog resumes; a tap outside the dialog keeps it open.
- [ ] 6.9 Moving the activity to `STOPPED` and back (`ActivityScenario.moveToState`) leaves the game paused with the dialog shown and the clock unchanged.
- [ ] 6.10 Run `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` on the emulator and record the results.

## 7. Wrap up

- [x] 7.1 Update `docs/project-plan.md` section 1.11: the game screen has a HUD at the top (level, flow speed, stars, score, cog wheel) and the pause menu has Resume, Restart and Exit to the main menu. Update `docs/design-system.md` section 5.3 item 3: the score has three stars to its left (grey in `text.muted`, earned in `brass.300` with a static brass glow and a `motion.pop`) instead of one small brass star.
- [ ] 7.2 Run `openspec validate app-game-play-hud` and fix any issues.
