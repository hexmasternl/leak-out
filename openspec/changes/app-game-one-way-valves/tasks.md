# Tasks

Gradle commands run from `app/src/`. Before starting, check which sibling changes from #102 have landed (especially fixed pipes, #135) and the maintainer's answers to the open questions in design.md; reuse the "placed by the level" groundwork if it exists.

## 1. Level format

- [ ] 1.1 Add `FlowDirection` (`LEFT`, `RIGHT`, `UP`, `DOWN`, with inlet and outlet `Side`), `Valve(position, direction)` and `Level.valves` (default empty) plus `Level.valveAt(position)` in `core/level`; verify with unit tests for the inlet and outlet of every direction
- [ ] 1.2 Add `ValveDto` and the optional `valves` list to `LevelDto`, mapped in `toLevel()`; verify with `LevelParserTest` cases for a level without valves, valid valves in authored order, a missing `direction` (error names `valves[0].direction`) and an unknown direction such as `TOP`
- [ ] 1.3 Add the valve rules to `LevelValidator` (at most `MAX_VALVES` = 8; inside the grid; not blocked, start, finish, mandatory section or another valve; inlet and outlet neighbours can carry the route), all reported together with `valves[i]` fields; verify with `LevelValidatorTest` cases for every rule from the `game-play-one-way-valves` spec
- [ ] 1.4 Write valves in `LevelWriter` (in order, left out when empty) and keep them in `LevelDraft`; verify with `LevelWriterTest` for the `valves` JSON shape and a round trip with every optional field set, and that the bundled round-trip test still passes for levels 1 to 96
- [ ] 1.5 Document `valves` in `docs/level-format.md`: field description, an example, the validation rules, how the liquid and the solver treat valves, and the error field names; verify the example parses by adding it as a parser test fixture
- [ ] 1.6 Run `./gradlew :core:test` and verify it passes

## 2. Flow simulation

- [ ] 2.1 Put valves on the board in `Board.from(level)` as `H`/`V` pipes with their direction, placed by the level (reuse the fixed-pipe flag from #135 if present); verify with a `BoardTest` that a `DOWN` valve is a vertical dry pipe with that direction
- [ ] 2.2 Add one direction-aware exit rule for a board cell and use it in `FlowStep.after` and `RouteTracer`; verify with unit tests that liquid passes a valve in its direction and scores 10, and leaks against it and from the side at the tile it left with `LossReason.LEAK`
- [ ] 2.3 Verify the flow and the route check agree: a `RouteTracer` test that a route through a valve the wrong way is not complete, and a property-style test over boards with valves that every complete route ends in a win
- [ ] 2.4 Exclude valves from `FinalScore` unused sections and keep them when unused sections are removed after a win; verify with `FinalScoreTest` (unused valve costs nothing) and the win clean-up test
- [ ] 2.5 Refuse placement on a valve tile in the engine's placement rule whatever `rules.allowReplace` says; verify with a `GameEngine` test that a place command on a valve leaves the state unchanged
- [ ] 2.6 Run `./gradlew :core:test` and verify it passes

## 3. Solver

- [ ] 3.1 Teach `LevelSolver` one-way edges: a valve is passed only from its inlet neighbour to its outlet neighbour, at most once, also as the start's or finish's facing cell and between mandatory waypoints; verify with `LevelSolverTest` cases for a wall whose only gap holds a valve in the right direction (solvable, route passes the valve inlet to outlet) and a route around a valve
- [ ] 3.2 When no route is found and the level has valves, search again with valves as two-way and, if that succeeds, report the first valve passed the wrong way, naming its position, `valves[i]` and direction; verify with a `LevelSolverTest` case whose only route runs against a valve
- [ ] 3.3 Run `./gradlew :core:test` and verify `LevelSolvabilityTest` still passes for levels 1 to 96

## 4. Rendering and accessibility

- [ ] 4.1 Specify the valve in `docs/design-system.md` (6.2 cell table and 6.4 sections: brass collar and chevron pointing to the outlet on an untinted straight, reusing the start valve chevron geometry); verify the section reads consistently with the start valve entry
- [ ] 4.2 Draw the valve marking on the playing field and in the shared tile drawing, over the liquid, with the fill always entered from the inlet; verify with app unit tests on the fill resolution and a check in the pipe gallery or Pipe Lab at 44 dp and larger
- [ ] 4.3 Add `CellDescription` strings for a dry and a filled valve with a translated direction word to `res/values/strings.xml`; verify with `CellDescriptionTest` and that the lint check for hard-coded text passes
- [ ] 4.4 Run `./gradlew :app:testDebugUnitTest` and verify it passes

## 5. Level designer

- [ ] 5.1 Add the Valve tool with its direction control: place, turn, remove and move by drag, refused on blocked cells, endpoints and mandatory sections, and capped at 8; verify with `LevelDesignerViewModel` unit tests for each scenario of the Valve tool requirement
- [ ] 5.2 Make the Start, Finish, Block and mandatory tools skip valve cells, and draw valves on the designer grid as in the game; verify with view model tests for the Block drag and Finish tap on a valve
- [ ] 5.3 Highlight valve validation errors on the grid and show the solver's valve reason; verify with a view model test for a valve in the last column and one blocking the only route
- [ ] 5.4 Keep valves through export, test play, copy of a bundled level and process death (saved state); verify with view model tests for export round trip and saved-state restore
- [ ] 5.5 Run `./gradlew :app:testDebugUnitTest` and verify it passes

## 6. Onboarding tip

- [ ] 6.1 Add `TipId.Valve("valve")` and schedule it before the countdown on the first level with valves, after the mandatory section tip and before the cross tip, highlighting every valve; verify with `TipScheduler` unit tests for first level with valves, shown once, no valves, and the order with a mandatory section
- [ ] 6.2 Add the tip title, text and highlight target strings to `res/values/strings.xml` (translated by Play Console into every supported language) and show them with "Got it"; verify with a unit test that "Got it" stores the tip as seen and the lint check passes
- [ ] 6.3 Run `./gradlew :app:testDebugUnitTest` and verify it passes

## 7. Valve chapter

- [ ] 7.1 Agree the chapter's level ids with the maintainer (design.md open question 3) and build 24 levels with the designer that introduce valves step by step, each with at least one valve; verify each exported file parses and validates
- [ ] 7.2 Add the 24 `level_NNN.json` files to `app/src/main/assets/levels`; verify `./gradlew :core:test` passes, including `LevelSolvabilityTest` for every new level and the bundled round-trip test
- [ ] 7.3 Verify levels 1 to 96 are unchanged (`git diff` shows no change to their files) and add a unit test that level history, best scores and seen tips stored before this change load unchanged and that the valve chapter page unlocks by the page star rule

## 8. Instrumented tests and final check

- [ ] 8.1 Add instrumented tests: a level with a valve shows it on the board with its accessibility description, the valve tip is shown once, and the liquid passes a valve in its direction to a win and leaks against it; verify with `./gradlew :app:connectedDebugAndroidTest` on an emulator, running only the affected test classes
- [ ] 8.2 Run `./gradlew :core:test :app:testDebugUnitTest` and the affected instrumented tests, and verify all pass
- [ ] 8.3 Run `openspec validate app-game-one-way-valves --strict` and verify it passes
