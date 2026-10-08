# Tasks

## 1. Level format, validation and writing

- [ ] 1.1 Add `Level.freezing: List<Position>` (default empty) with `isFreezing(position)`, and an optional `freezing` array of `{x, y}` in `LevelDto`. Cover the parse cases in `LevelParserTest`: absent field, valid cells, a missing `freezing[0].y` and an unknown key inside an entry.
- [ ] 1.2 Add the validation to `LevelValidator`: inside the grid, not blocked, not on the start or finish, no duplicate cells, and no mandatory `X` on a freezing cell, each error naming `freezing[i]`. A mandatory `RES` on a freezing cell is accepted. Cover every scenario of "Freezing cells lie on free cells" in `LevelValidatorTest`, including several errors reported together.
- [ ] 1.3 Make `LevelWriter` write `freezing` in the level's order only when it is not empty, and make sure `mandatory` is left out when empty. Cover in `LevelWriterTest` the minimal level, the freezing scenario and a round trip with every optional field set, including freezing cells.
- [ ] 1.4 Document the `freezing` field in `docs/level-format.md`: add it to the full example, add a "Freezing cells" section with an example, the validation rules and the freeze rule, and note under Solvability that a route never passes a freezing cell twice. Verify that the documented example JSON parses in a `LevelParserTest` case.

## 2. Board and the freeze rule in the flow

- [ ] 2.1 Add `freezing: Set<Position>` to `Board`, filled by `Board.from(level)` and part of `equals` and `hashCode`. Cover in `BoardTest` that a level's board holds its freezing cells and that placing on and replacing on a freezing cell work as on any other free cell (`PlacementTest`).
- [ ] 2.2 Add `FreezeRule.isFrozen(board, position, used)` in `core/flow` and use it in `FlowStep.after` so that a frozen section gives a leak at the tile the liquid left. Cover in a new `FreezeRuleTest`: not frozen while the liquid is inside, frozen after it left, a cell that is not freezing never freezes.
- [ ] 2.3 Cover the flow in `FlowSimulationTest` and `GameEngineTest`: a cross on a freezing cell freezes after its first pass and the second pass leaks at the tile before it (10 points for the cross, no 50); a cross on a normal cell still scores 50; the section before the frozen one still scores; a restart unfreezes the cell; a long tick stops at the frozen cell.
- [ ] 2.4 Use the same rule in `RouteTracer.walk` with a new `Route.Reason.FROZEN`. Cover in `RouteTracerTest` a route through a freezing cell once (complete) and a route back to a cross on a freezing cell (not complete). Add freezing-cell boards to `FlowStepRouteAgreementTest`, so the flow and the route check still agree.
- [ ] 2.5 Add a `HeadlessPlayTest` case that plays a level with freezing cells end to end, once winning through a freezing cell and once losing at a frozen cross. Run `./gradlew :core:test` and confirm it passes.

## 3. Solver

- [ ] 3.1 Make `LevelSolver` treat freezing cells as free, report a mandatory cross on a freezing cell as unsolvable by name, and never return a route that passes a freezing cell twice. Cover the new `level-solvability` scenarios in `LevelSolverTest`, plus a property-style test over generated levels with freezing cells that no returned route repeats a freezing cell.
- [ ] 3.2 Run `./gradlew :core:test`, including `LevelSolvabilityTest` and `BundledLevelsTest`, and confirm it passes.

## 4. Rendering and accessibility

- [ ] 4.1 Add the `ice.100`, `ice.300`, `ice.500` and `ice.tint` tokens to `LeakOutColors`, and document them, the "Freezing cell" tile state, the ice look and the freeze animation in `docs/design-system.md`.
- [ ] 4.2 Draw freezing cells in `PlayingField.drawCell` with the frost tile (with or without a section, and together with the mandatory edge on a mandatory reservoir), following pan and zoom.
- [ ] 4.3 Add the per-channel frozen progress to the pipe tile renderer: ice gradient on the liquid path, no head, no glow, spreading from the exit side. Drive it from the game time of the cell's award over 400 ms of game clock, at once with reduced motion. Cover the `pipe-liquid-rendering` scenarios in its unit tests.
- [ ] 4.4 Add the freezing and frozen texts to `CellDescription` as string resources with translator comments, and cover the screen-reader scenarios in `CellDescriptionTest`.
- [ ] 4.5 Run `./gradlew :app:testDebugUnitTest` and confirm it passes.

## 5. Level designer

- [ ] 5.1 Add freezing cells to `LevelDraft`: toggle, drag-apply like the Block tool, refused on blocked cells and endpoints, removed when a cell is blocked or falls outside a resized grid, kept when copying a bundled level. Cover in `LevelDraftTest`.
- [ ] 5.2 Add the Freezing cell tool to `LevelDesignerScreen` and `LevelDesignerViewModel`, draw freezing cells in `DesignerGrid` with the frost tile, highlight freezing-cell validation errors on the grid, and keep freezing cells across configuration changes and process death. Make sure export and test play carry the freezing cells.
- [ ] 5.3 Cover in `LevelDesignerViewModelTest` the tool, the live validation of a mandatory cross on a freezing cell, export round trip and saved-state restore.

## 6. First-time tip

- [ ] 6.1 Add `TipId.Freezing("freezing")` after `Mandatory`, and make `TipScheduler` show it at level start for a level with freezing cells, highlighting every freezing cell and panning to the one nearest the start. Cover in `TipSchedulerTest`: shown once, not for a level without freezing cells, and the order intro, mandatory, freezing, cross, reservoir.
- [ ] 6.2 Add `tip_freezing_title`, `tip_freezing_text` and `tip_freezing_target` to `res/values/strings.xml` (translated by Play Console into every supported language), and the tip row to the tip table in `docs/design-system.md`. Check the tip under the `en-XA` pseudo-locale at 640 × 360 so 40 % longer text still fits.

## 7. Chapter of 24 levels

- [ ] 7.1 Add 24 new `level_NNN.json` files with the next free page of ids (97 to 120 if no other #102 chapter has landed), following the curve in design.md. Design them in the level designer and test play each one. Leave levels 1 to 96 untouched.
- [ ] 7.2 Add a test that levels 1 to 96 parse with no freezing cells and round-trip through `LevelWriter` unchanged, and that the new levels all have freezing cells. Run `./gradlew :core:test` and confirm `BundledLevelsTest` and `LevelSolvabilityTest` pass for every new level.
- [ ] 7.3 Check that the level library shows the new page, that it unlocks with the existing page star rule, and that existing level history and achievements still load (`LevelLibraryTest`, `LevelHistory` tests).

## 8. Instrumented tests and wrap-up

- [ ] 8.1 Add instrumented tests: a freezing cell is drawn and described on the board (`PlayingFieldTest`), the liquid passes a section on a freezing cell and it is described as frozen afterwards, a second pass through a frozen cross loses the level (`GameScreenTest`), and the freezing cell tip is shown once (`OnboardingTest`).
- [ ] 8.2 Run `./gradlew :app:testDebugUnitTest`, then only the affected instrumented tests with `./gradlew :app:connectedDebugAndroidTest` on the emulator, and confirm they pass.
- [ ] 8.3 Run `openspec validate app-game-freezing-cells --strict` and confirm it reports the change as valid.
