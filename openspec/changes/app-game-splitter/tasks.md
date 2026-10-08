# Tasks

All Gradle commands run from `app/src/`. A task is only done once its tests have been run and pass.

## 1. Splitter sections and exits

- [ ] 1.1 Add `SPL_L`, `SPL_R`, `SPL_T`, `SPL_B` to `core/level/PipeSection.kt` with three open sides each and a `stackable` flag (false for the splitters), plus `PipeSection.stackSections`. Make `StackSettings`, `LevelDefaults.STACK_WEIGHTS`, `Dealer`, `PipeStack`, `LevelDto` weights and `LevelWriter` use `stackSections` only. Verify with `PipeSectionTest` (open sides per orientation) and that `DealerTest`, `PipeStackTest` and `LevelParserTest` pass unchanged, including `stack.weights.SPL_L` being rejected as an unknown key.
- [ ] 1.2 Add `PipeSection.exitsFor(entry): List<Side>` (empty for a closed side, two sides for a splitter, one otherwise) and keep `exitFor` for single-exit sections. Give the splitter `Channel.SINGLE`. Cover every orientation and entry side in `ExitForTest`.

## 2. Level format, validation and docs

- [ ] 2.1 Add the optional `secondFinish` (endpoint) and `splitter` (`x`, `y`, `closedSide`) to `LevelDto`, `Level` (with a derived `finishes` list and `splitterAt`) and `LevelParser`. Cover in `LevelParserTest`: both absent, both present, an unknown `splitter.closedSide`, and an unknown key inside `splitter`.
- [ ] 2.2 Add the validation from the `game-level-model` delta to `LevelValidator`: second finish placement rules, splitter placement and open-side rules, and the "splitter and second finish come together" rule, every error naming its field. Cover each scenario in `LevelValidatorTest`, including several errors reported together. Include the splitter cell in `Level.freeCells` like a mandatory cell, and the second finish as taken.
- [ ] 2.3 Write `secondFinish` and `splitter` in `LevelWriter` only when present. Cover the `level-serialization` scenarios in `LevelWriterTest`, and confirm the bundled-levels round trip still passes.
- [ ] 2.4 Document both fields in `docs/level-format.md`: the full example, a "Splitter and second finish" section with an example level, the validation rules and the play rules (two heads, finish, leak, collision, splitter must be passed), and the solver's splitter search under Solvability. Add a `LevelParserTest` case that parses the documented example. Run `./gradlew :core:test` and confirm it passes.

## 3. Board and placement

- [ ] 3.1 Make `Board.from(level)` place the splitter as an unfilled, level-placed `Cell.Pipe` and the second finish as a `Cell.Finish`. Cover in `BoardTest`.
- [ ] 3.2 Make `Placement.check` refuse the splitter's tile with `FIXED` and the second finish like the first; make `FinalScore.of` leave the splitter out of the unused sections. Cover in `PlacementTest` and `FinalScoreTest`, and that `RemoveUnused` never removes the splitter.
- [ ] 3.3 Count the splitter as a mandatory section in `MandatoryProgress` (used once its single channel is filled). Cover in `MandatoryProgressTest`.

## 4. Flow simulation with two heads

- [ ] 4.1 Change `FlowState.Flowing` to hold a list of heads (position, entry, channel, progress, sections left on the head's own path) plus the shared used channels and the reached finishes. Keep the one-head case behaving exactly as before; adapt `GameState.flowSpeed` to the faster head and `speedIn` to per-head speed. Verify that every existing `FlowSimulationTest`, `GameEngineTest`, `HeadlessPlayTest` and achievement test passes unchanged.
- [ ] 4.2 Implement the split in `FlowSimulation.leaveTile` and `FlowStep`: award the splitter once (30 points in `Points`), spawn two heads in the fixed side order, leak on a closed side or an empty exit. Cover in `FlowSimulationTest` and `PointsTest`.
- [ ] 4.3 Implement the multi-head `advance`: step to the nearest border of any head, leave tiles in the fixed order on a tie, drop time after a win or loss. Cover in `FlowSimulationTest`: two heads at different speeds, a long tick with two heads, and the per-branch speed ramp from the `game-play-flow` delta.
- [ ] 4.4 Implement finish, win, loss and collision rules: a head stops at its finish, the level is won when both finishes are reached and mandatory sections are used, lost as soon as either head leaks, lost with `MANDATORY_MISSED` at a finish reached before the splitter, and a head leaks into a filled or occupied channel (the unused cross channel still works). Cover in `FlowSimulationTest` and `GameEngineTest`: both branches finishing, one branch leaking (before and after the other finished), a branch running into the other branch's pipes, both heads reaching one channel at once, crossing on a cross, and routing around the splitter.
- [ ] 4.5 Add a `HeadlessPlayTest` case that plays a splitter level end to end through engine commands, once winning and once losing on one branch. Run `./gradlew :core:test` and confirm it passes.

## 5. Route check, gap distance and solver

- [ ] 5.1 Make `RouteTracer` fork at the splitter and report `Complete` only when both branches reach different finishes without sharing a channel and all mandatory sections are used. Cover the `game-play-fast-forward` scenarios in `RouteTracerTest`, and extend `FlowStepRouteAgreementTest` with two-finish boards so the flow and the route check still agree.
- [ ] 5.2 Make `GapDistance` take the nearest gap over the live heads, with no gap for a head that will reach its finish. Cover the `game-play-gap-warning` scenarios in `GapDistanceTest`.
- [ ] 5.3 Extend `LevelSolver` with the splitter search from design.md (route to an open side of the splitter, then two disjoint branches to the two finishes, combined with the mandatory waypoints, within `MAX_SEARCH_WORK`), naming the finish or the splitter it cannot reach. Cover every new `level-solvability` scenario in `LevelSolverTest`, plus a check that each returned path is self-avoiding across all three parts, and a timing guard for a 50 by 50 splitter level. Run `./gradlew :core:test` and confirm it passes.

## 6. Rendering, HUD and accessibility

- [ ] 6.1 Draw the four splitter SVGs and glass overlays in `app/src/main/assets/pipes/` following `docs/design-system.md` (pipe recipe, three bores marked, closed side capped), and register them with the pipe asset loader. Verify the asset loading tests and the Pipe lab show all four orientations.
- [ ] 6.2 Add the splitter liquid fill (entry arm to the centre, then both other arms) to the pipe tile drawing. Cover the `pipe-liquid-rendering` scenarios in the app's pipe rendering unit tests.
- [ ] 6.3 Draw the splitter on the level-placed tile tint and the second finish with the checkered finish and glow on the playing field, flash both finishes on a win, and drive the liquid of both heads from the flow state. Add the splitter tile and the two-finish board to `docs/design-system.md`.
- [ ] 6.4 Describe the splitter cell in `CellSemantics` ("Splitter, closed on the <side>, liquid has passed" / "not passed yet") and the second finish as a finish, using string resources. Show the faster head's speed in the HUD and let the gap warning and heartbeat use the nearest gap. Cover in the app's JVM unit tests and run `./gradlew :app:testDebugUnitTest`.

## 7. Level designer

- [ ] 7.1 Add the splitter and second finish to `LevelDraft` (place, move, turn, remove, copy from a bundled level, keep across process death). Cover in `LevelDraftTest`.
- [ ] 7.2 Add the Splitter and Second finish tools, the panel controls, cell highlighting for their errors and the two-branch solver route to the designer screen. Cover the `level-designer` scenarios in `LevelDesignerScreenTest` and `TestPlayScreenTest` (export round trip and test play with a splitter).

## 8. Splitter tip

- [ ] 8.1 Add the splitter tip to the onboarding flow: shown before the countdown on the first level with a splitter, highlighting the splitter and both finishes, in the order intro tour, mandatory, splitter, cross, reservoir, stored as seen through `SeenTipsStorage`. Put its title, text and button in the default string resources only (Play Console translates them into every supported language) and add the text to the tip table in `docs/design-system.md`. Cover the `game-play-onboarding` scenarios in `GameSessionTipsTest` and `SeenTipsTest` and confirm lint reports no hard-coded text.

## 9. Chapter of 24 levels

- [ ] 9.1 Design 24 splitter levels with the designer as one chapter (one level library page), using the next free ids in #102's chapter order, from short symmetric branches to levels with blocked cells, a reservoir on one branch, mandatory sections and a cross shared by both branches. Save them as `level_NNN.json` without touching levels 1 to 96. Verify that `BundledLevelsTest`, `LevelSolvabilityTest`, the writer round trip, `AchievementReachabilityTest` and `ThreeStarReachabilityTest` pass for all of them.
- [ ] 9.2 Confirm that levels 1 to 96 are unchanged (`git diff` shows no change under `levels/` for those files), that their parsed levels equal the ones before this change, and that existing level history and scores still load (`LevelHistoryDatabaseTest`, `LeakOutDatabaseMigrationTest`). Run `./gradlew :core:test :app:testDebugUnitTest` and confirm they pass.

## 10. Instrumented tests and integration

- [ ] 10.1 Add instrumented tests: the splitter is shown on the board with its description, a drop on it is refused, the liquid passes the splitter and fills both branches, the level is won when both finishes are reached and lost when one branch leaks, and the splitter tip is shown once. Run only the affected instrumented tests (`GameScreenTest`, `PlayingFieldTest`, `LevelCompleteTest`, `LevelLostTest`, `GameTipsTest` and `LevelDesignerScreenTest`) with `./gradlew :app:connectedDebugAndroidTest` on the emulator and confirm they pass.
- [ ] 10.2 Play the first and the last level of the new chapter on the emulator: both branches animate, fast forward appears only on a complete split route, the gap warning follows the nearest gap, and the result dialogs show. Run `openspec validate app-game-splitter --strict` and confirm it reports the change as valid.
