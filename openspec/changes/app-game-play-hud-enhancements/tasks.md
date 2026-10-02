# Tasks

Tracking issue: [#38 HUD enhancements](https://github.com/hexmasternl/leak-out/issues/38).

## 1. Design system proposal (gate)

- [x] 1.1 Rewrite `docs/design-system.md` §5.3 HUD bar for the new panel, level, flow speed number, countdown bar and score bar (sizes, colours, tokens, warning colours, marker lines, score legibility), replacing the outdated countdown ring; verify every token named exists in §2–§4
- [x] 1.2 Add the countdown fade, score fill and threshold sparkle to §7.2 and their reduced-motion variants to §7.4; verify durations match the spec (600 ms fill, 600 ms sparkle, `motion.quick` fade, 160 ms reduced)
- [x] 1.3 Build a static Compose `@Preview` mock-up of the proposed HUD (waiting with countdown, mid-score, full bar past three stars) and attach screenshots to issue #38; verify the preview renders in Android Studio
- [x] 1.4 Get the product owner's approval of the design on issue #38 and record any requested changes back into the design system and spec; verify approval is noted on the issue before starting group 2

## 2. Pure HUD model

- [x] 2.1 Add `HudModel.speedNumber` with half-up rounding via `BigDecimal` and change `HudFormat.speed` to return the whole number; verify unit tests for 0.2→2, 0.8→8, 0.82→8, 0.85→9, 1.25→13 pass with `./gradlew :app:testDebugUnitTest`
- [x] 2.2 Add `countdownFraction`, `countdownWarning` and `countdownSeconds`; verify unit tests for 10,000/4,000→0.4, delay 0, 8,000→Calm, 5,000→Brass, 2,000 and 1,500→Red, 6,200→7 s pass
- [x] 2.3 Add `scoreFraction`, `markerFractions` and `thresholdsPassed`; verify unit tests for 175/350→0.5, 500/350→1.0, markers 2/7 and 4/7, 90→100 passes one, 190→360 passes two then three, 360→400 passes none

## 3. HUD components

- [x] 3.1 Restyle the `GameHud` container as the approved HUD panel and keep `HudLayout.HEIGHT` fixed; verify the existing "largest stack fits on a 360 dp screen" instrumented test still passes
- [x] 3.2 Show the flow speed as the whole number with the "Flow speed N" description and update strings; verify the HUD instrumented test reads "Flow speed 8" for 0.8
- [x] 3.3 Add the countdown bar composable (fill, warning colours, fade-out, hidden for delay 0, reduced motion) with test semantics for fraction and warning; verify instrumented tests for full at start, 40 % after 6 s, frozen while paused, refilled on restart, fades on flow start, absent for delay 0
- [x] 3.4 Add the score bar composable (fixed width, markers, centred grouped number, animated fill with snap on new attempt) with test semantics for the fill fraction; verify instrumented tests for empty start, half at 175, full and "1,250" past three stars, empty after restart
- [x] 3.5 Add the threshold sparkle driven by `thresholdsPassed` on the animated fill, once per threshold per attempt, off under reduced motion; verify instrumented tests that crossing `stars.one` starts one sparkle, 190→360 starts two, 360→400 none, and none with animations removed
- [x] 3.6 Remove `HudStars`/`HudStar` and the HUD star constants, keep `StarIcon` and `WinStars` for the level complete dialog; verify the level complete dialog instrumented tests still pass

## 4. Wiring and accessibility

- [x] 4.1 Pass `countdownRemainingMs`, `initialDelayMs`, the level's star thresholds and the score from `GameScreen` to `GameHud`, and lay out level, speed, countdown slot, score bar and cog wheel; verify the item-order instrumented test and that nothing moves when the countdown disappears
- [x] 4.2 Set the content descriptions ("Level 7", "Flow speed 8", "Flow starts in 7 seconds", "Score 150, 1 of 3 stars", "Pause menu") with the score bar as one merged node; verify the screen reader instrumented test, including that no countdown is read after the flow starts

## 5. Integration

- [x] 5.1 Run `./gradlew :app:testDebugUnitTest` and the affected HUD, game screen and level complete instrumented tests on the emulator; verify all pass and post HUD screenshots on issue #38
