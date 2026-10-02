# Tasks

Tracking issue: [#35 Random tile stack population needs to be less random](https://github.com/hexmasternl/leak-out/issues/35).

## 1. Level model

- [x] 1.1 In `core/.../level/LevelDefaults.kt` set the `H`, `V`, `LT`, `LB`, `RT`, `RB` weights to 7 and add `STEERING_STRENGTH` 1.0 and `DROUGHT_FACTOR` 2.5, each with KDoc
- [x] 1.2 Tests in `LevelParserTest` and `LevelModelTest` for the new defaults and the `X` 3 override; run `./gradlew :core:test`

## 2. Dealer

- [x] 2.1 Add immutable `core/.../stack/Dealer.kt`: shares of the allowed sections, draws so far, dealt count and drought per section, `Dealer.initial(settings)` and `deal(random): Pair<PipeSection, Dealer>` with deficit steering (design §2, `StrictMath.exp`, `random.nextDouble()`) and the drought limit for the standard sections (design §3)
- [x] 2.2 `DealerTest`: first draw distribution within 1 percentage point over 100,000 fresh dealers, disabled section never drawn in 10,000 draws, state updates per draw, a section behind is more likely than a section ahead, drought limit forces the longest drought with tie by `PipeSection` order, `X` and `RES` never forced, same seed same sequence
- [x] 2.3 Statistical tests over 10,000 seeded runs of 40 draws with default weights: no standard section missing for more than 15 draws, mean per-section standard deviation at most 0.8, back-to-back repeats at most 12 %; with `X` 6 the cross share is within 1 percentage point of 12.5 %; run `./gradlew :core:test`

## 3. Stack and engine

- [ ] 3.1 Give `PipeStack` a `dealer`; `create(settings, random)` draws the initial sections through it; `afterPlacement(random)` deals from and returns the updated dealer; delete `WeightedPool` and move its remaining assertions to `DealerTest`
- [ ] 3.2 In `GameEngine.place` use `state.stack.afterPlacement(random)` instead of a new `WeightedPool` per placement
- [ ] 3.3 `PipeStackTest` and `GameEngineTest`: the dealer carries over between placements and across pause/resume, restart resets it, two engines with the same seed deal the same stacks after the same placements; existing stack-order tests still pass; run `./gradlew :core:test`

## 4. Levels and docs

- [ ] 4.1 Re-tune the bundled levels in `app/src/main/assets/levels/`: `"X": 1` → `"X": 3`, `"X": 2` → `"X": 6`; verify every level still parses, validates and is solvable with `./gradlew :core:test` (`LevelSolvabilityTest` and the bundled-level tests)
- [ ] 4.2 Update `docs/level-format.md`: example and defaults table (weights 7), "Stack weights" explains that a weight is a target share, and a new "How the stack deals" section on deficit steering, the drought limit (standard sections only) and why a retry still deals a different sequence

## 5. Verify

- [ ] 5.1 Run `./gradlew :core:test` and `./gradlew :app:testDebugUnitTest`; run only the instrumented tests that drive the stack or start a level (`:app:connectedDebugAndroidTest` filtered to the stack and game screen tests) and confirm they pass
- [ ] 5.2 Play two bundled levels with a cross on the emulator and post a short note on #35 on how the stack feels
