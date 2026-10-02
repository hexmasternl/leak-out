# Tasks

## 1. Rename the existing "mandatory" stack flag

- [x] 1.1 Rename `PipeSection.mandatory` to `alwaysOnStack` in `core/level/PipeSection.kt`, `LevelValidator` and every test, and reword its KDoc and the weight error so they no longer say "mandatory". Verify that `./gradlew :core:test` passes unchanged.

## 2. Level format and validation

- [x] 2.1 Add `MandatorySection(position, section)` and `Level.mandatory: List<MandatorySection>` (default empty), plus an optional `mandatory` array of `{x, y, section}` in `LevelDto`. Cover the parse cases in `LevelParserTest`: absent field, a valid entry, a missing `mandatory[0].section`, an unknown section id and an unknown key inside an entry.
- [x] 2.2 Add the validation to `LevelValidator`: at most 6 entries, `section` only `X` or `RES`, inside the grid, not blocked, not on the start or finish, and no duplicate cells. Every error names its `mandatory[i]` field, and a mandatory `X`/`RES` is accepted with stack weight 0. Cover each spec scenario in `LevelValidatorTest`, including several errors reported together.
- [x] 2.3 Document the `mandatory` field in `docs/level-format.md`: the full example, a "Mandatory sections" section with the rules and the meaning of "used", and the solver's limits under Solvability. Verify that the documented example JSON parses in a `LevelParserTest` case.

## 3. Board, placement and the "used" rule

- [x] 3.1 Add `mandatory: Boolean = false` to `Cell.Pipe` and place the level's mandatory sections in `Board.from(level)`. Cover in `BoardTest` that a level's board holds them unfilled and that `markLiquidEntered` keeps the flag.
- [x] 3.2 Make `Placement.check` reject a mandatory pipe with `FIXED`, before the replace checks. Cover in `PlacementTest` a dry mandatory section with `allowReplace` true, and one the liquid has entered.
- [x] 3.3 Add the shared `MandatoryProgress` helper that returns the unused mandatory sections for a set of `ChannelUse`s: a cross needs both channels, a reservoir its single channel. Cover the spec's reservoir, one-channel cross and two-channel cross cases in a new `MandatoryProgressTest`.
- [x] 3.4 Make `FinalScore.of` leave mandatory pipes out of `unusedSections`. Cover in `FinalScoreTest` and `FinalScoreEngineTest` that `RemoveUnused` never takes a mandatory section off the board.

## 4. Route, win and loss in the engine

- [x] 4.1 Make `RouteTracer` return `Route.Open(finish, MANDATORY_MISSED)` when a route that reaches the finish missed a mandatory channel. Cover in `RouteTracerTest` the skipped reservoir, the one-channel cross and the complete route. Keep `FlowStepRouteAgreementTest` passing.
- [x] 4.2 Add `LossReason { LEAK, MANDATORY_MISSED }` to `Outcome.Lost`, defaulting to `LEAK`. In `FlowSimulation.leaveTile`, lose with `MANDATORY_MISSED` at the finish and its open side when a mandatory section is unused, after the points are awarded. Cover in `FlowSimulationTest` and `GameEngineTest`: win after every mandatory section is used, loss when one is skipped, points before the loss, liquid passing a mandatory cross and a mandatory reservoir at reservoir speed, a restart that resets them unfilled, and a refused drop on a mandatory tile with the stack unchanged.
- [x] 4.3 Add a `HeadlessPlayTest` case that plays a level with a mandatory cross and reservoir end to end through engine commands, once winning and once losing with `MANDATORY_MISSED`. Run `./gradlew :core:test` and confirm it passes.

## 5. Solver

- [x] 5.1 Extend `LevelSolver` with the ordered waypoint search from design.md. Pre-checks name a mandatory section whose entry neighbours cannot carry a pipe, and the returned path runs through every mandatory channel without reusing a cell other than a mandatory cross. Cover every new `level-solvability` scenario in `LevelSolverTest`, plus a test that checks each returned path is self-avoiding and passes each waypoint in an allowed direction.
- [x] 5.2 Add a timing guard test: a 50 by 50 level with 6 mandatory sections solves within the test's time budget. Run `./gradlew :core:test`, including `LevelSolvabilityTest` and `BundledLevelsTest`, and confirm it passes.

## 6. App rendering, dialog and accessibility

- [x] 6.1 Draw mandatory pipes in `PlayingField.drawCell` on a `LeakTint` tile with a `leak.500` edge (add a `LeakOutColors.Leak500` token if it is missing) under the normal pipe art. Add the "Mandatory section" tile state to `docs/design-system.md`. Cover in `PlayingFieldTest` that a mandatory tile is drawn and keeps its tint when filled.
- [x] 6.2 Describe mandatory cells in `CellSemantics` as "Mandatory <section>, used" or "Mandatory <section>, not used yet", using `MandatoryProgress` on the current flow's channels. Cover in `PlayingFieldTest` that a one-channel cross reads "not used yet".
- [x] 6.3 On a `MANDATORY_MISSED` loss, show the leak spurt at the finish's open side and pulse every unused mandatory tile in `leak.500` for 1 second. Show the "The liquid missed a mandatory section. Level lost." message in the level lost dialog. Cover in `LevelLostTest` both dialog messages and that the dialog waits 1 second.
- [x] 6.4 Run `./gradlew :app:testDebugUnitTest`, then only the affected instrumented tests (`PlayingFieldTest`, `LevelLostTest`, `GameScreenTest`) with `./gradlew :app:connectedDebugAndroidTest` on the emulator, and confirm they pass.

## 7. Showcase level and integration

- [x] 7.1 Add the next free `level_NNN.json` with a mandatory cross and a mandatory reservoir (leave the existing levels untouched) and register it in the level library if the library config needs that. Verify that `BundledLevelsTest` and `LevelSolvabilityTest` pass for it.
- [ ] 7.2 Play the showcase level on the emulator: the red tiles show, a drop on them is refused, skipping one loses with the missed-section message, and using both channels of the cross plus the reservoir wins. Run `openspec validate app-game-play-mandatory-sections --strict` and confirm it reports the change as valid.
