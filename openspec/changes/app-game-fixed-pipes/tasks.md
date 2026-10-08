# Tasks

Paths are relative to `app/src/`. The tasks follow the recommended defaults in design.md's Open Questions; if the maintainer decides otherwise, adjust the affected group before starting it.

## 1. Level format, validation and writing

- [ ] 1.1 Add `FixedPipe(position, section)` and `Level.fixed: List<FixedPipe>` (default empty), plus an optional `fixed` array of `{x, y, section}` in `LevelDto`. Cover in `LevelParserTest`: absent field, valid entries in authored order, a missing `fixed[0].section`, an unknown section id and an unknown key inside an entry.
- [ ] 1.2 Add the fixed pipe checks to `LevelValidator`: inside the grid, not blocked, not on the start or finish, not on a mandatory cell, no duplicates, each error naming `fixed[i]`; a fixed `RES` or `X` is accepted with stack weight 0, and a fixed pipe on the start's facing cell is accepted. Cover every `game-level-model` scenario in `LevelValidatorTest`, including several errors reported together.
- [ ] 1.3 Make `LevelWriter` write `fixed` in authored order only when it is not empty. Cover in `LevelWriterTest` the minimal level (no `fixed` key), the ordered entries and a round trip with every optional field set, and keep the bundled-level round-trip test passing.
- [ ] 1.4 Document `fixed` in `docs/level-format.md`: the full example, the defaults table, a "Fixed pipes" section with the rules and how they differ from mandatory sections, the errors list, and the designer section. Verify that the documented example JSON parses and validates in a `LevelParserTest` case. Run `./gradlew :core:test` and confirm it passes.

## 2. Board, placement and final score

- [ ] 2.1 Add `fixed: Boolean = false` and the `placedByLevel` property to `Cell.Pipe`, and place the level's fixed pipes unfilled in `Board.from(level)`. Cover in `BoardTest` that the board holds them unfilled, that `markLiquidEntered` keeps the flag, and that a level without `fixed` gives the same board as before.
- [ ] 2.2 Make `Placement.check` reject a `placedByLevel` pipe with `FIXED` before the replace checks. Cover in `PlacementTest` a dry fixed pipe with `allowReplace` true, one the liquid has entered, and in `GameEngineTest` that a drop and a tap on a fixed pipe are refused, start no replacement and leave the stack unchanged.
- [ ] 2.3 Make `FinalScore.of` leave `placedByLevel` pipes out of `unusedSections`. Cover in `FinalScoreTest` and `FinalScoreEngineTest` that an unreached fixed pipe costs nothing and is never removed by `RemoveUnused`. Run `./gradlew :core:test` and confirm it passes.

## 3. Flow, points and route check

- [ ] 3.1 Add `FlowSimulationTest` cases for fixed pipes: the liquid follows a fixed corner and earns 15 points, slows down in a fixed reservoir, passes a fixed cross on both channels for 10 and 50 points, leaks out of a fixed pipe into an empty tile, leaks at a placed section that faces a fixed pipe's closed side, and wins on a route that avoids every fixed pipe. Fix the engine only if a case fails.
- [ ] 3.2 Add `RouteTracerTest` and `GapDistanceTest` cases: a route through a fixed pipe completes (fast forward available), a fixed pipe's closed side is a gap, and a fixed pipe counts as one section in the gap distance. Keep `FlowStepRouteAgreementTest` passing with a board that holds fixed pipes.
- [ ] 3.3 Add a `HeadlessPlayTest` case that plays a level with fixed pipes end to end through engine commands, once using a fixed pipe and once avoiding it, and an `AttemptSummaryTest` case that a fixed pipe on the route counts in the route facts but not in `sectionsPlaced`. Run `./gradlew :core:test` and confirm it passes.

## 4. Solver

- [ ] 4.1 Change `LevelSolver`'s search to the node-based BFS from design.md: a free cell is one node, a fixed pipe one node per channel, entered only through an open side and left through `exitFor`. Segments may end in a fixed pipe whose exit enters the waypoint or the finish, and a fixed pipe on the start's facing cell without a matching opening gives a reason naming the start. Cover every new `level-solvability` scenario in `LevelSolverTest`, and extend the path check so every returned route is self-avoiding (a cross only once per channel) and enters every fixed pipe through an open side.
- [ ] 4.2 Add a timing guard test for a 50 by 50 level with many fixed pipes and 6 mandatory sections, and document the solver's fixed pipe rules and its possible false rejections under Solvability in `docs/level-format.md`. Run `./gradlew :core:test`, including `LevelSolvabilityTest` and `BundledLevelsTest`, and confirm levels 1 to 96 are still reported solvable.

## 5. Rendering and accessibility

- [ ] 5.1 Draw a fixed `Cell.Pipe` in `PlayingField.drawCell` on the bolted steel tile from design.md (fill `steel.700` at 35 %, 1.5 dp `steel.300` edge, four bolts), under the normal pipe art, liquid and glass. Add the "Fixed pipe" row to the cell states table in `docs/design-system.md`. Cover in a `PlayingField` unit or screenshot-free Compose test that a fixed tile is drawn differently from a placed and a mandatory tile and keeps its look when filled.
- [ ] 5.2 Describe fixed cells in `CellDescription` as "Fixed <section>, empty" or "Fixed <section>, filled" with new strings in `values/strings.xml`. Cover in `CellDescriptionTest` a dry fixed corner and a filled fixed cross. Run `./gradlew :app:testDebugUnitTest` and confirm it passes.

## 6. First-time tip

- [ ] 6.1 Add `TipId.Fixed("fixed")` between `Mandatory` and `Cross`, make `TipScheduler` queue it when a level with fixed pipes starts and the tip is unseen, and highlight every fixed cell in `TipOverlay` with the existing pan-to-target. Add the title, text and content description strings to `values/strings.xml` (Play's automatic translation covers the other supported languages) and the tip row to `docs/design-system.md`. Cover in `TipSchedulerTest` the first level with fixed pipes, the tip shown once, a level without fixed pipes and the order mandatory, fixed, cross. Run `./gradlew :app:testDebugUnitTest` and confirm it passes.

## 7. Level designer

- [ ] 7.1 Add the fixed pipe operations to `LevelDraft`: place, remove, change section, move, and the refusals on blocked cells, endpoints and mandatory cells; make the start, finish, block and mandatory operations skip fixed cells, grid shrinking drop fixed pipes outside the grid, and the star calculation count fixed cells as free. Cover every `level-designer` scenario that `LevelDraft` owns in `LevelDraftTest`. Run `./gradlew :core:test` and confirm it passes.
- [ ] 7.2 Add the Fixed pipe tool with its section picker to `LevelDesignerScreen` and `LevelDesignerViewModel`, drag-to-move in `DesignerGrid`, the bolted look in the designer grid, error highlighting on fixed cells, and the fixed list and chosen section in the saved state. Cover in `LevelDesignerViewModelTest` placing, moving, removing, export JSON with `fixed`, and state restored after recreation. Update the designer section of `docs/level-format.md`. Run `./gradlew :app:testDebugUnitTest` and confirm it passes.

## 8. The fixed pipe chapter

- [ ] 8.1 Add `level_097.json` to `level_120.json` (or the page decided in #102) following the curve in design.md, each with at least one fixed pipe and star thresholds from the designer's formula. Leave `level_001.json` to `level_096.json` untouched (verify with `git diff --stat` that no existing level file changed). Run `./gradlew :core:test` and confirm `BundledLevelsTest`, `LevelSolvabilityTest`, the round-trip test and `AchievementReachabilityTest` pass for all 120 levels.
- [ ] 8.2 Add a `LevelLibrary` test that page 5 holds levels 97 to 120 and opens by the existing star rule, and a test that a level history saved before this change still loads with the same results and open pages. Fix anything that hard-codes 96 levels. Run `./gradlew :core:test :app:testDebugUnitTest` and confirm both pass.

## 9. Instrumented tests and end-to-end check

- [ ] 9.1 Add instrumented tests: a level with fixed pipes shows them on the board with their accessibility description, a drop on a fixed pipe is refused with the stack unchanged, the liquid passes a fixed pipe and the level is won, and the fixed pipe tip is shown once and dismissed with "Got it". Run only the affected classes with `./gradlew :app:connectedDebugAndroidTest` on the emulator (or as the maintainer agrees) and confirm they pass.
- [ ] 9.2 Play levels 97 and 101 on the emulator: the bolted tiles show, a drop on them is refused, the tip appears once, and a route through and a route around a fixed pipe both win. Run `openspec validate app-game-fixed-pipes --strict` and confirm it reports the change as valid.
