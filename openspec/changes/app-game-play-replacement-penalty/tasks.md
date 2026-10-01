## 1. Core: level default

- [x] 1.1 Change `LevelDefaults.REPLACE_PENALTY_MS` to 3000 in `core/.../level/LevelDefaults.kt`
- [x] 1.2 Update `LevelParserTest` (minimal level gets 3000) and add a test for a custom `replacePenaltyMs` of 1500; run `:core:test`

## 2. Core: replacement sequence in the engine

- [x] 2.1 Add `Replacement(at, removed, incoming, startedAtMs, landsAtMs)` and `GameState.replacement: Replacement?`, initialised to null in `GameEngine.start`
- [x] 2.2 In `GameEngine.place`, for `PlacementCheck.Replace` with a penalty above 0: empty the cell, move the stack, store the `Replacement`, set `replaceLockUntilMs = landsAtMs`, and recompute `routeComplete`. With a penalty of 0, keep the instant swap
- [x] 2.3 In `GameEngine.tick`, split the tick at `landsAtMs`: advance to the landing time, land `incoming` if the outcome is still playing and the tile is empty, clear `replacement`, recompute `routeComplete`, then advance the rest
- [x] 2.4 Clear `replacement` when the outcome becomes won or lost, and refuse `FastForward` while a replacement runs
- [x] 2.5 `GameEngineTest`: the tile is empty and the stack moved at the drop; the section lands exactly at `landsAtMs` (2999 vs 3000); placement is refused during the sequence; a penalty of 0 swaps at once; pause freezes the sequence; restart clears it
- [x] 2.6 `GameEngineTest`: the liquid entering the tile before the landing leaks there and nothing lands; a loss elsewhere ends the sequence; the route is incomplete during the sequence and completes on the landing; fast forward is refused during the sequence
- [x] 2.7 Tick-independence test: the same replacement scenario with 1 ms ticks and with one long tick ends in an equal board, flow, score and outcome
- [x] 2.8 Update `PlacementTest` and `HeadlessPlayTest` for the new behaviour; run `./gradlew :core:test` and make sure it passes

## 3. App: session and sounds

- [x] 3.1 Add a pure `landed(before, after): Boolean` to `GameSounds.kt`, and have `GameSession` count landings in `send` (exposed as observable state)
- [x] 3.2 Make `placementSound(Rejected, state)` return null while `state.replacement != null`, and with a penalty of 0 have `placeWithSound` play `SectionDestroyed` followed by `SectionPlaced`
- [x] 3.3 In `GameScreen`, play `GameSound.SectionPlaced` once for every landing
- [x] 3.4 JVM tests (`GameSessionPlaceTest` and a new `GameSoundsTest` case): destroy at the drop, exactly one placement sound at the landing, no sound for taps during the sequence, no landing sound after a restart or a loss, both sounds in order with a penalty of 0; run `./gradlew :app:testDebugUnitTest`

## 4. App: input lock

- [x] 4.1 Derive `replacing` in `GameScreen`: disable the stack drag (`StackPanel enabled`) and the fast forward button while a replacement runs, and keep pause available
- [x] 4.2 Check that the drag highlight and the drop on `PlayingField` refuse every tile during the sequence (through `canPlace`)

## 5. App: animation

- [x] 5.1 Draw the break animation on the replacement tile in `PlayingField`: the `removed` section splits into seeded fragments that drift, rotate and fade over the first `min(600 ms, 40 %)` of the sequence, driven by the game clock
- [x] 5.2 Draw the busy ring, which fills with the sequence progress, plus the tile tint, for the rest of the sequence
- [x] 5.3 Add a short scale-in pop on the landed section (about 150 ms of game time), keyed on the tile and the landing time
- [x] 5.4 Keep the replacement tile out of the drop highlight, and use colours from `docs/design-system.md`

## 6. Instrumented tests

- [ ] 6.1 Update `PlayingFieldTest` replace and lock tests to the sequence (the tile is empty during it, the section is there after the penalty, and drops and taps are refused during it)
- [ ] 6.2 Add `GameSoundEffectsTest` cases: `section_destroy` at the replacement, `section_placement` after the penalty, and nothing for a tap during the sequence
- [ ] 6.3 Add a test that the stack can't be dragged and fast forward does nothing during the sequence, while pause still works
- [ ] 6.4 Run the affected instrumented tests with `./gradlew :app:connectedDebugAndroidTest` on the emulator

## 7. Documentation

- [ ] 7.1 Update `docs/level-format.md` (default 3000; `replacePenaltyMs` is the length of the replacement sequence)
- [ ] 7.2 Update `docs/project-plan.md` (the replace rule, the engine state, the decision table row "500 ms lock" becomes "3 s replacement sequence")
