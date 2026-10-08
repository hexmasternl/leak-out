# Tasks

Tracks [#91](https://github.com/hexmasternl/leak-out/issues/91).

## 1. Idle clock after the end effects

- [x] 1.1 Add `GameSession.needsClock` (design decision 1) with JVM tests in `GameSessionTest`/`GameSessionWinTest`: true while playing; after a loss true until `LeakTiming.EFFECT_MS` and while awards are visible, false after; after a win true until the end of the tally and while awards are visible, false after; true again after a restart. Verify with `./gradlew :app:testDebugUnitTest --tests '*GameSession*'`
- [x] 1.2 Make `EndpointGlow.finish` steady at 60 % after a loss (decision 2) and pass the lost state from `PlayingField`. Extend `EndpointsTest` (lost → `STEADY` at any `nowMs`; won → 0; playing → pulse). Verify with `./gradlew :app:testDebugUnitTest --tests '*Endpoints*'`
- [x] 1.3 Key `GameClock` on `needsClock` so it returns without looping when it is false. Add an instrumented test (in `GameEngineScreenTest` or a new `GameClockIdleTest`) with `autoAdvance = false`: lose a level, advance past the leak effect, check that `session.state.clockMs` no longer changes over further frames and that the lost dialog is shown. Do the same for a win after the tally. Restart and check that the clock runs again. Verify by running only that test class on the emulator

## 2. Tick cap

- [x] 2.1 Add the pure `TickPacer` (decision 3, `MIN_TICK_MS = 12`) with JVM tests: at 60 Hz every frame ticks; at 120 Hz every second frame ticks (60/s); at 90 and 144 Hz the expected rates (50/s and 72/s); the sum of the deltas always equals the elapsed whole milliseconds (no time lost); the first frame after a (re)start sends 0. Verify with `./gradlew :app:testDebugUnitTest --tests '*TickPacer*'`
- [ ] 2.2 Use `TickPacer` in `GameClock` and keep pause/resume (no time leaks in from a pause). Verify by rerunning the existing instrumented game tests that drive the clock (`GameEngineScreenTest`, `GamePlayHudTest`, `GapHeartbeatTest`, `GapWarningScreenTest`, `HudBarsTest`, `GameTipsTest`) on the emulator

## 3. Easy allocation cuts

- [ ] 3.1 In `core` `GameEngine.advance`, keep the same `awards` list when it is empty or its oldest award is still visible. Add `GameEngineTest` cases: same instance kept while every award is visible, expired awards still dropped. Verify with `./gradlew :core:test`
- [ ] 3.2 Cache popup `TextLayoutResult`s by text in `PlayingField.drawPopups` (decision 4). Verify that the points popup instrumented tests still pass, running only the affected test class
- [ ] 3.3 Split `FieldLiquid` into settled fills and head fill and cache the settled part per `(board, used)` identity in `PlayingField`. Add `FieldLiquidTest` cases showing settled + head equals the old `fills` result for straight, cross and reservoir tiles, with and without the head. Verify with `./gradlew :app:testDebugUnitTest --tests '*FieldLiquid*'`

## 4. Integration

- [ ] 4.1 Run `./gradlew :core:test :app:testDebugUnitTest` and verify that everything passes
- [ ] 4.2 On the emulator or a device, open the level lost and level complete dialogs, wait 30 s, and compare `adb shell dumpsys gfxinfo nl.hexmaster.leakout` frame counts before and after the change. Expect close to 0 new frames while idle. Record the numbers on #91
