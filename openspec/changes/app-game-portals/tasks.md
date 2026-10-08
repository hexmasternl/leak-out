# Tasks

Paths are relative to `app/src/`; run Gradle from `app/src/` (`.\gradlew.bat` on Windows). A task is only done when its tests have been run and pass.

## 1. Level format and validation

- [x] 1.1 Add `PortalPair(a: Endpoint, b: Endpoint)` and `Level.portals` (default empty) with `portalAt` and `partnerOf` in `core/level/Level.kt`; exclude portal cells from `Level.freeCells`. Verify with new cases in `LevelModelTest` (free cells with and without portals).
- [x] 1.2 Add `portals` to `LevelDto` and `LevelParser`: optional array of `{a, b}` with `x`, `y`, `openSide`; field-path errors such as `portals[0].b` and unknown open sides. Verify with `LevelParserTest` cases for absent, valid, missing `b` and invalid `openSide`.
- [x] 1.3 Extend `LevelValidator`: at most 3 pairs; each portal inside the grid and not on a blocked, start, finish, mandatory or other portal cell; each portal's facing cell inside the grid, not blocked and not the start, finish or a portal; start and finish facing cells not a portal; blocked cells and mandatory sections not on a portal. Verify with one `LevelValidatorTest` case per scenario in the `game-play-portals` and `game-level-model` deltas, including several errors reported together.
- [x] 1.4 Write `portals` in `LevelWriter` only when non-empty, in authored order. Verify with `LevelWriterTest` (no `portals` key on a minimal level, two pairs written in order, round trip with every optional field set) and the bundled-level round trip.
- [x] 1.5 Document portals in `docs/level-format.md`: a "Portal pairs" section with a JSON example, the validation rules as a numbered list, how the liquid passes a pair, and an update to the Solvability section; add `portals` to the full example. Verify the example parses by adding it as a `LevelParserTest` fixture.
- [x] 1.6 Run `./gradlew :core:test` and confirm `BundledLevelsTest` still parses levels 1 to 96 to the same levels and that no level file has changed yet (`git diff --stat app/src/app/src/main/assets/levels` is empty; the levels are updated in section 7).

## 2. Board, placement and flow simulation

- [x] 2.1 Add `Cell.Portal(pair, openSide, partner)` and put both portals of every pair on the board in `Board.from(level)`. Verify with `BoardTest` (portals on a new and a restarted board).
- [x] 2.2 Make `Placement.check` reject a portal tile for hover and drop, whatever `allowReplace` says (reuse the fixed-pipe rejection reason if #135 is merged). Verify with `PlacementTest`.
- [x] 2.3 Add `Channel.PORTAL` and move the next-tile decision into one helper used by `FlowStep.after` and `RouteTracer.walk`: entering a portal through its open side enters the pair unless its channel is used; any other side leaks. Verify with `FlowStepTest` and `RouteTracerTest` cases (enter a to b, enter b to a, closed side, used pair).
- [x] 2.4 In `FlowSimulation`, run the head through a pair in one cell's time without the reservoir factor, leave it from the partner through the partner's open side, award no points, add no `PathStep` and leave the speed ramp count unchanged. Verify with `FlowSimulationTest` cases: timings at 400, 600 and 1,000 ms at 1 cell per second; leak at the exit portal; win through a pair; long tick across a pair; speed ramp after a pair; score unchanged by a pair.
- [x] 2.5 Count a pair as 1 in `GapDistance` (head inside a pair: `1 - progress`). Verify with `GapDistanceTest` cases for the two gap-warning scenarios with portals.
- [x] 2.6 Exclude portals from `AttemptSummary.freeCells` (Wall to Wall). Verify with an `AchievementRulesTest` case for a full board with portals.
- [x] 2.7 Add portal boards to `FlowStepRouteAgreementTest` and run `./gradlew :core:test`; confirm the flow wins on every board the route check calls complete.

## 3. Solver

- [x] 3.1 Teach `LevelSolver` to treat a portal's facing cell as linked to the partner's facing cell, include the two portal cells in the returned path, and let `canAdjoin` accept a portal facing a mandatory channel. Verify with `LevelSolverTest` cases: a level that is only solvable through a pair, a pair that does not cross the wall (unsolvable), a pair combined with a mandatory reservoir, and a level without portals giving the same path as before.
- [x] 3.2 Update `HeadlessPlayTest` and `ScriptedPlay` to skip portal cells when they turn the solver's path into sections, connecting each neighbour of a portal towards the portal's open side. Verify by running `./gradlew :core:test` with a portal test level in the headless suite.
- [x] 3.3 Update the Solvability section of `docs/level-format.md` for portals and check it against the implementation.

## 4. Rendering and accessibility

- [x] 4.1 Add the portal tokens (`portal.1` to `portal.3`, with symbols circle, triangle and square) to `docs/design-system.md` §2.5, their contrast against `surface.deep` to §2.8, and the portal drawing to §6.2, §6.4 and §7 (ring swirl, reduced motion). Verify each colour reaches 3:1 against `surface.deep` and record the ratio.
- [x] 4.2 Add the tokens to the Compose theme and draw portal cells on the playing field: empty-cell tile, straight stub from the open side to the centre, ring in the pair colour with the pair symbol, swirl on game time that freezes on pause and with reduced motion. Verify with a unit test for the token values and a screenshot check in the pipe lab or the gallery.
- [x] 4.3 Draw the liquid through a pair: progress 0–0.5 fills the entry stub towards the ring, 0.5–1 the exit stub outwards; both stay full afterwards. Verify with `app` unit tests for the progress split (`./gradlew :app:testDebugUnitTest`).
- [x] 4.4 Add the portal accessibility description (pair number, symbol, open side, partner cell, passed or not) as translatable strings. Verify with an `app` unit test for the description text.

## 5. Level designer

- [x] 5.1 Extend `LevelDraft` with portal pairs (second portal optional), the selected portal, and the unfinished-pair error; exclude portals from the calculated star thresholds; remove a pair with a portal outside a shrunk grid. Verify with `LevelDraftTest` cases for every new designer scenario (place, move, open side, remove, unfinished pair, at most three, shrink, thresholds).
- [x] 5.2 Make the Start, Finish, Block and mandatory tools leave portals alone. Verify with `LevelDraftTest` cases.
- [x] 5.3 Add the Portal tool, open side control and Remove pair action to the designer screen, highlight portal errors on the grid, and keep portals through export, test play and saved state. Verify with `LevelDesignerViewModel` unit tests and by running `./gradlew :app:testDebugUnitTest`.
- [x] 5.4 Update the "Creating levels with the designer" section of `docs/level-format.md` with the Portal tool.

## 6. First-time tip

- [x] 6.1 Add `TipId.PORTAL`, show it before the countdown the first time a level with portals starts, and order it after the mandatory section tip and before the cross and reservoir tips. Verify with unit tests for the tip queue (first level with portals, shown once, order with the intro tour and the cross tip).
- [x] 6.2 In the tip overlay, highlight both portals of pair 1 in the pair colour and draw a two-headed link between them; other pairs are not highlighted. The text explains that liquid entering either portal comes out of the other (both ways), that same-colour portals belong together, that a pair can be used once, and that portals are part of the level. Verify with an `app` unit test for the highlighted cells on a one-pair and a two-pair level.
- [x] 6.3 Add the tip title, explanation and the portal accessibility strings to `res/values/strings.xml` with translator comments, so Play Console translates them into every supported language (Dutch, German, French, Spanish, Italian, Portuguese (Brazil)). Verify the existing string resource tests and lint pass (`./gradlew :app:testDebugUnitTest :app:lintDebug`).

## 7. Portals in levels 49 to 96

- [ ] 7.1 Add a `BundledLevelsTest` check: levels 1 to 48 have no portals, level 49 has exactly one pair that the solver's shortest route passes, fewer than half of levels 50 to 96 have portals, and no level has more than three pairs.
- [ ] 7.2 Update level 49 with the level designer: one portal pair that the shortest route must use, on an otherwise gentle board. Export it over `assets/levels/level_049.json`, keeping its id and name.
- [ ] 7.3 Pick about eleven more levels spread over 50 to 96, with plain levels between them, and add portal pairs with the designer: one pair up to level 72, up to two pairs from level 73, up to three pairs only in the last few levels. Make at least one of them solvable only through a pair. List the chosen levels in the PR description.
- [ ] 7.4 Retune the star thresholds of an updated level only where three stars would otherwise become unreachable.
- [ ] 7.5 Run `./gradlew :core:test` and confirm `LevelSolvabilityTest`, `HeadlessPlayTest`, `BundledLevelsTest`, `AchievementReachabilityTest` and `ThreeStarReachabilityTest` pass, and that `git diff --stat app/src/app/src/main/assets/levels` only lists the updated levels (levels 1 to 48 and every level without portals byte-identical).

## 8. Instrumented tests and integration

- [ ] 8.1 Add instrumented tests: a level with a portal pair shows both portals with their pair symbol and accessibility description; the liquid passes the pair and the level is won; the portal tip is shown once on level 49, with both portals of the pair highlighted and linked; the designer places, moves and removes a pair. Run only the affected classes with `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...` on an emulator.
- [ ] 8.2 Upgrade check: install the previous build, finish some levels, install this build, and confirm the level history, stars (including those of updated levels), achievements and settings are unchanged; covered by the existing database migration tests plus a manual check.
- [ ] 8.3 Run `./gradlew :core:test :app:testDebugUnitTest` once more on the finished branch and confirm everything passes.
