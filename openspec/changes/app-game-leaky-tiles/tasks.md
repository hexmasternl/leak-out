# Tasks

## 1. Level format, validation and writing

- [x] 1.1 Add `Level.leaky: List<Position>` (default empty) with `isLeaky(position)` and an optional `leaky` array of `{x, y}` in `LevelDto`/`LevelParser`. Cover in `LevelParserTest`: absent field, valid entries in authored order, a missing `leaky[0].y` and an unknown key inside an entry. Run `./gradlew :core:test` and confirm it passes.
- [x] 1.2 Add the leaky checks to `LevelValidator`: inside the grid, not blocked, not start or finish, not a mandatory section, not a portal, no duplicates, each error naming `leaky[i]`. Cover every `game-level-model` leaky scenario in `LevelValidatorTest`, including several errors reported together. Run `./gradlew :core:test` and confirm it passes.
- [x] 1.3 Write `leaky` in `LevelWriter` only when non-empty, in level order. Cover in `LevelWriterTest` the minimal level without a `leaky` key, a level with two leaky tiles, and a round trip with every optional field set; confirm the bundled-levels round-trip test still passes unchanged.
- [x] 1.4 Document the field in `docs/level-format.md`: add `leaky` to the full example, a "Leaky tiles" section with the validation rules and the score effect, and the leak-free note under Solvability. Add a `LevelParserTest` case that parses the documented example and confirm it passes.

## 2. Scoring and flow in core

- [ ] 2.1 Add `Points.LEAKY_TILE_PENALTY = 20` and a `leaky` parameter to `ScoreSheet.award` that still counts the pass and returns `-20` instead of the section's points. Cover in `ScoreSheetTest`: a straight, a corner and a reservoir on a leaky tile, a cross on a leaky tile passed twice (−20 twice, no 10 or 50), and a total that goes below 0.
- [ ] 2.2 Pass `level.isLeaky(position)` from `FlowSimulation.leaveTile` and record the negative `Award`. Cover in `FlowSimulationTest` and `GameEngineTest`: the penalty before a leak, an unchanged speed on a leaky tile, a won route over two leaky tiles with two penalties, a replaced section on a leaky tile that is still leaky, and a restart. Add leaky boards to `FlowStepRouteAgreementTest` and confirm the flow and the route check still agree.
- [ ] 2.3 Add a `FinalScoreTest` case with negative earned points (total 0, 0 stars) and an achievement-rules test with a negative award, and check every use of `Award.points` and `ScoreSheet.total` in core and app for a positive-only assumption. Add a `HeadlessPlayTest` case that plays a level with leaky tiles end to end. Run `./gradlew :core:test` and confirm it passes.

## 3. Solver

- [ ] 3.1 Treat leaky tiles as free in `LevelSolver` and add `leakFree: Boolean` to `SolverResult.Solvable`, computed by a second solve with leaky tiles treated as blocked. Cover every `level-solvability` leaky scenario in `LevelSolverTest` (corridor through a leaky tile, detour, every route leaky, no leaky tiles) and confirm the 50 by 50 timing guard still passes with leaky tiles added. Run `./gradlew :core:test` and confirm it passes.

## 4. Rendering, popup, HUD and accessibility

- [ ] 4.1 Draw leaky tiles in `PlayingField.drawCell`: cracks, damp blob and drip badge under and around the pipe art, following pan and zoom. Add the "Leaky tile" row to `docs/design-system.md` 6.2 and the drip badge to 6.4. Cover in `PlayingFieldTest` that an empty and a placed leaky tile are drawn, and that a tap and a drop on a leaky tile place the next section.
- [ ] 4.2 Add the drip effect on the game clock while the head is in a section on a leaky tile, and the wet stain afterwards; with reduced motion only the stain. Document it in `docs/design-system.md` 6.5 and 7.4. Cover the timing and reduced-motion branches in a JVM unit test of the drip model.
- [ ] 4.3 Show a negative award as "−20" with `leak.500` stroke and glow in the points popup, and let the HUD show a signed score with the bar fill clamped at 0. Document the penalty popup in `docs/design-system.md`. Cover in `PointsPopupTest` and `HudBarsTest` the "−20" popup and a score of −20, and show signed earned points in the level complete dialog with a `LevelCompleteTest` case.
- [ ] 4.4 Add the leaky texts ("Leaky tile, empty", "… on a leaky tile") to the default string resources and to `CellSemantics`. Cover in `PlayingFieldTest` both descriptions, and run the app module's lint check to confirm no hard-coded text.
- [ ] 4.5 Run `./gradlew :app:testDebugUnitTest` and confirm it passes.

## 5. First-time tip

- [ ] 5.1 Add `TipId.LEAKY_TILE` with its seen flag, the leaky tile tip that freezes the game on level start and highlights every leaky tile, and its title and text in the default string resources (the source for every supported language). Put it between the portal tip and the cross tip. Cover in the onboarding unit tests the tip on the first level with leaky tiles, not again once seen, not on a level without leaky tiles, "Got it" storing it as seen, and the new tip order.
- [ ] 5.2 Describe the leaky tile tip in `docs/design-system.md` 5.9 if it needs anything beyond the existing single-tip layout, and run `./gradlew :app:testDebugUnitTest` to confirm it passes.

## 6. Level designer

- [ ] 6.1 Add leaky tiles to `LevelDraft`: the toggle and drag rules, a blocked cell becoming leaky, Block, Start, Finish and Portal clearing the leaky flag, mandatory tools refusing a leaky tile, the Leaky tile tool skipping portals, grid shrink removing leaky tiles outside the grid, Clear leaky, and `Level.scoringCells` for the star thresholds. Cover each `level-designer` leaky scenario in `LevelDraftTest`, including the thresholds 350, 440 and 590.
- [ ] 6.2 Add the Leaky tile tool and the Clear leaky action to the designer panel, draw leaky tiles on the designer grid, highlight leaky validation errors, show the "every route crosses a leaky tile" note from `leakFree`, and keep leaky tiles in copy, export, test play and saved state. Cover in the designer's unit tests export round trip and saved-state restore. Run `./gradlew :app:testDebugUnitTest` and confirm it passes.

## 7. Leaky tiles in levels 25 to 96

- [ ] 7.1 Add a `BundledLevelsTest` check: levels 1 to 24 have no leaky tiles, level 25 has a leaky tile and a leak-free route, fewer than half of levels 25 to 96 have leaky tiles, and at least four levels from 49 without mandatory sections have a shortest route over a leaky tile that is shorter than their leak-free route.
- [ ] 7.2 Update level 25 and about five more levels spread over 26 to 48 with one or two leaky tiles each, keeping a leak-free route on every one. Keep ids, names, endpoints and flow; retune the star thresholds as described in design.md.
- [ ] 7.3 Update about twelve levels spread over 49 to 96, mostly without portals, with leaky tiles; design at least four of them as a shortcut over a leaky tile next to a longer leak-free way around. List the chosen levels in the PR description.
- [ ] 7.4 Run `./gradlew :core:test` and confirm `BundledLevelsTest`, `LevelSolvabilityTest`, `HeadlessPlayTest`, `ThreeStarReachabilityTest` and the round-trip test pass, and that `git diff --stat app/src/app/src/main/assets/levels` only lists the updated levels (levels 1 to 24 and every level without leaky tiles byte-identical).

## 8. Instrumented tests and integration

- [ ] 8.1 Add a `LeakyTilesShowcaseTest` instrumented test on level 25: the leaky tiles are shown and described, a section can be dropped on one, the liquid passes it with drops and a "−20" popup, the score goes down by 20, and the leaky tile tip is shown once.
- [ ] 8.2 Run `./gradlew :app:connectedDebugAndroidTest` for the affected instrumented tests only (`LeakyTilesShowcaseTest`, `PlayingFieldTest`, `PointsPopupTest`, `HudBarsTest`, `LevelCompleteTest` and the onboarding tests) on an emulator and confirm they pass.
- [ ] 8.3 Run `openspec validate app-game-leaky-tiles --strict` and confirm the change is valid.
