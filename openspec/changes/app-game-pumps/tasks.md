# Tasks

Gradle commands run from `app/src/` (`.\gradlew.bat` on Windows). A task is only done once its tests have been run and pass.

## 1. Level format

- [ ] 1.1 Add `PipeSection.PUMP` (`alwaysOnStack = false`, open sides `LEFT` and `RIGHT`), `LevelDefaults.PUMP_SPEED_FACTOR = 1.5` and `STACK_WEIGHTS[PUMP] = 0`, and fix every exhaustive `when` over `PipeSection` in core and app (for example `Points`, `AttemptSummary`, cell descriptions, tip mockups, liquid spans, Pipe Lab) with a placeholder that the later groups replace; verify `./gradlew :core:compileKotlin :app:compileDebugKotlin` succeeds and `PipeSectionTest`/`ExitForTest` cover `PUMP` (exit right for entry left, left for right, `null` for top and bottom)
- [ ] 1.2 Add `flow.pumpSpeedFactor` to `LevelDto`/`LevelFlow` and `LevelParser`, and accept `PUMP` in `stack.weights` and in `mandatory[].section`; verify `LevelParserTest` cases for a level with `pumpSpeedFactor` 2.0 and `PUMP` weight 3, a minimal level getting 1.5 and weight 0, and `mandatory` with `PUMP`
- [ ] 1.3 In `LevelValidator`, reject `flow.pumpSpeedFactor` not greater than 1 or above 3, allow a `PUMP` weight of 0 or more, and accept `PUMP` as a mandatory section with the message "only `X`, `RES` and `PUMP` can be mandatory"; verify `LevelValidatorTest` cases for 1, 0.9, 3 (accepted), 3.1, a negative `PUMP` weight and a mandatory `PUMP` with weight 0
- [ ] 1.4 In `LevelWriter`, write `pumpSpeedFactor` and the `PUMP` weight only when they differ from the defaults; verify `LevelWriterTest` for both cases and a round trip of a level with pumps, a factor of 2.5 and a mandatory pump, and that the bundled-level round trip still passes
- [ ] 1.5 In `LevelSolver`, treat a mandatory `PUMP` as one left–right channel like `RES`; verify `LevelSolverTest` cases for a reachable mandatory pump (route runs left–right or right–left), a pump whose right neighbour is the grid edge (unsolvable, reason names the section), and a level that deals pumps without mandatory sections (solvable)
- [ ] 1.6 Document the pump in `docs/level-format.md`: `flow.pumpSpeedFactor` in the defaults table and the flow fields with its range, `PUMP` in the stack weights table, `PUMP` as a mandatory section, the speed rule with its cap, a full example level with a pump and the new validation errors; verify the example parses in a `LevelParserTest` case copied from the doc and `./gradlew :core:test` passes

## 2. Flow simulation and points

- [ ] 2.1 Add `Points.PUMP = 40` and use it in `Points.forExit`; verify `PointsTest` and a `game-play-points` route scenario with a pump
- [ ] 2.2 Add `GameState.pumpsLeft` (passes on tiles holding a `PUMP`) and change `flowSpeed` to the capped boost formula from design.md, keeping it identical to today when `pumpsLeft` is 0; verify `FlowSimulationTest`/`GameEngineTest` cases: speed inside a pump is unboosted, 1.5 after one pump, 2.25 after two, ramp and pump together (2.4), the cap (5.0 × 3² → 10), a ramped speed of 11 left unchanged, a reservoir after a pump (0.45), restart resets the boost, and fast forward on top of a pump moves 1.5 cells in a 100 ms tick at 1.5 cells per second
- [ ] 2.3 Make `MandatoryProgress` count a mandatory `PUMP` as used once its channel is passed, and confirm the route check (`RouteTracer`/`FlowStep`) passes a pump only left–right; verify `MandatoryProgressTest`, `RouteTracerTest` (pump entered from the bottom is not complete, route through a pump is complete) and `FlowStepRouteAgreementTest` extended with boards that hold pumps
- [ ] 2.4 Confirm the dealer never forces a pump; verify a `DealerTest` case with a `PUMP` weight of 1 and 200 draws without a pump, and a draw-share test with a `PUMP` weight
- [ ] 2.5 Add a regression test that plays scripted attempts on every bundled level 1 to 96 and asserts the score and the tick of the outcome match the engine before this change (golden values recorded from `development` before 2.2); verify it and the full `./gradlew :core:test` pass

## 3. Rendering, HUD and accessibility

- [ ] 3.1 Add the `pump.*` colour tokens to the theme and to `docs/design-system.md` (palette, pump recipe row next to `RES`, "fast" badge, HUD pulse, accessibility note that the pump is told apart by shape); verify the theme unit tests and that the doc's token table matches the theme
- [ ] 3.2 Draw `pump.svg` (horizontal pipe, impeller housing with blades, flanges left and right, "fast" badge in the top-right corner, `channel-lr` from left to right, `under`/`over` groups) and `pump-glass.svg`; verify the asset loader tests: view box `0 0 100 100`, one left–right channel, no `tank`, glass group present
- [ ] 3.3 Render the pump in the pipe tile with liquid filling its channel like `H` in both directions (progress factor 1); verify `PipeTileTest` (instrumented) and the liquid span JVM tests for a pump entered from the left and from the right at progress 0.5
- [ ] 3.4 Show the pump in the Pipe Lab; verify `PipeLabStateTest` and `PipeLabScreenTest` see nine tiles including `PUMP`
- [ ] 3.5 Add the spoken name "Pump" to the cell and stack descriptions (string resources); verify the cell description JVM tests read "Placed Pump section, filled" and "Mandatory Pump, not used yet", and a `StackPanelTest` case names the next section as a pump
- [ ] 3.6 Pulse the HUD flow speed number once in `pump.500` over about 600 ms when a pump boost changes it, without a pulse when animations are off; verify a `GamePlayHudTest` case: speed 10 → 15 after a pump, pulse shown, and no pulse with reduced motion

## 4. Level designer

- [ ] 4.1 Add the Pump speed factor field and the `PUMP` weight field to the designer panel; verify `LevelDesignerViewModel` JVM tests for setting both, and that 3.5 shows a `flow.pumpSpeedFactor` error in the live validation
- [ ] 4.2 Add the Mandatory pump tool to `LevelDraft` and the designer (place, remove by tapping again, change kind from `X`/`RES`, at most six, not on blocked cells or endpoints); verify `LevelDraftTest` cases and a `LevelDesignerScreenTest` case that places a mandatory pump and sees "valid and solvable"
- [ ] 4.3 Verify export and test play with pumps: `LevelDesignerViewModel` test that an exported design with a mandatory pump and a `PUMP` weight parses back equal, and a `TestPlayScreenTest` case that starts with the mandatory pump on the board

## 5. Pump tip

- [ ] 5.1 Add the pump tip to `TipScheduler` (due for a `PUMP` in a fully visible stack slot or a mandatory pump at level start; last in the order intro, mandatory, cross, reservoir, pump) and a "pump tip seen" key in `SettingsRepository`/`DataStoreSettingsRepository` (missing key = not seen); verify `TipScheduler` JVM tests for each trigger, the cut-off slot, the order and "shown once", and a settings repository test for the new key
- [ ] 5.2 Add the tip title "Pump" and the text with the percentage placeholder to the default string resources (with a placeholder comment; English is the only resource language and Play Console translates it), the tip mockup, and the tip text in `docs/design-system.md`; verify the lint check passes and a JVM test formats 1.5 as "50 %" and 1.25 as "25 %"
- [ ] 5.3 Verify in `GameTipsTest` (instrumented) that the pump tip freezes the game, highlights the pump, is dismissed with "Got it" and is not shown again

## 6. Pump chapter

- [ ] 6.1 Write the 24 pump levels as `level_NNN.json` on the next free level library page (97 to 120 if no other chapter has landed), with a curve from a mandatory pump on a straight route to dealt pumps mixed with reservoirs, factors from 1.25 to 2, and star thresholds from the designer's calculation adjusted by play-testing; verify `./gradlew :core:test` runs `LevelSolvabilityTest` and `BundledLevelsTest` green for every new file
- [ ] 6.2 Add a chapter test that asserts the page holds 24 levels and each deals a pump (`PUMP` weight > 0) or has a mandatory pump, and that levels 1 to 96 are byte-for-byte unchanged and deal no pump; verify it passes
- [ ] 6.3 Verify the level library and saved progress: `LevelLibraryRulesTest` cases that page 5 (or the chapter's page) opens by the existing star rule and that existing history and top scores for levels 1 to 96 are read unchanged

## 7. Instrumented tests and final check

- [ ] 7.1 Add a `PumpScreenTest` (instrumented) that starts a level with a mandatory pump, sees the pump on the board with its description, lets the liquid pass it and checks the pump's 40 points and the HUD's boosted speed; run only the affected instrumented tests (`PumpScreenTest`, `PipeTileTest`, `PipeLabScreenTest`, `StackPanelTest`, `GamePlayHudTest`, `GameTipsTest`, `LevelDesignerScreenTest`, `TestPlayScreenTest`) with `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...` and verify they pass
- [ ] 7.2 Run `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug` and verify everything passes
