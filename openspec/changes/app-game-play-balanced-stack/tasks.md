# Tasks

Tracking issue: [#35 Random tile stack population needs to be less random](https://github.com/hexmasternl/leak-out/issues/35).

## 1. Level model

- [ ] 1.1 In `core/.../level/LevelDefaults.kt` set the `H`, `V`, `LT`, `LB`, `RT`, `RB` weights to 10 and add `STACK_ADAPTIVE` (`X` false, `RES` false), `LIKELINESS_MIN` 1, `LIKELINESS_MAX` 20, `LIKELINESS_DROP` 3 and `LIKELINESS_GAIN` 1, each with KDoc
- [ ] 1.2 Add `adaptive: Map<PipeSection, Boolean>` to `StackSettings` (standard sections always true, `X`/`RES` from the level) with an `isAdaptive(section)` helper; add `AdaptiveDto` (`X`, `RES` only) to `StackDto` in `LevelDto.kt` so any other key is an unknown-key parse error
- [ ] 1.3 In `LevelValidator.checkStack` reject weights above `LIKELINESS_MAX`, naming `stack.weights.<id>`
- [ ] 1.4 Tests in `LevelParserTest`, `LevelModelTest` and `LevelValidatorTest` for every scenario of the `game-level-model` delta (new defaults, `X` 6 override, adaptive `RES`, `adaptive.H` rejected, weight 21 rejected); run `./gradlew :core:test`

## 2. Likeliness algorithm

- [ ] 2.1 Add immutable `core/.../stack/Likeliness.kt`: current values for the allowed sections, the adaptive set, `draw(random): Pair<PipeSection, Likeliness>` with the drop/gain/clamp rule from design §1 (gain also after a fixed section is drawn), and `Likeliness.initial(settings)`
- [ ] 2.2 `LikelinessTest`: drop and gain, clamp at 1 and 20, fixed section unchanged and still lets the others gain, adaptive special section, disabled section never drawn in 10,000 draws, first draw distribution within 1 percentage point over 100,000 fresh states, all values within 1..20 over 100,000 draws, same seed same sequence
- [ ] 2.3 Spread test: 10,000 seeded runs of 40 draws with default weights, mean per-section standard deviation at most 1.8 and at least 99.9 % of runs drawing every standard section; run `./gradlew :core:test`

## 3. Stack and engine

- [ ] 3.1 Give `PipeStack` a `likeliness`; `create(settings, random)` draws the initial sections through it; `afterPlacement(random)` draws from and returns the updated likeliness; delete `WeightedPool` and move its remaining assertions to `LikelinessTest`
- [ ] 3.2 In `GameEngine.place` use `state.stack.afterPlacement(random)` instead of a new `WeightedPool` per placement
- [ ] 3.3 `PipeStackTest` and `GameEngineTest`: likeliness carries over between placements and across pause/resume, restart resets it to the level's weights, two engines with the same seed deal the same stacks and likeliness after the same placements; existing stack-order tests still pass; run `./gradlew :core:test`

## 4. Levels and docs

- [ ] 4.1 Re-tune the bundled levels in `app/src/main/assets/levels/`: `"X": 1` → `"X": 6`, `"X": 2` → `"X": 12`; verify every level still parses, validates and is solvable with `./gradlew :core:test` (`LevelSolvabilityTest` and the bundled-level tests)
- [ ] 4.2 Update `docs/level-format.md`: example and defaults table (weights 10, `stack.adaptive`), "Stack weights" renamed/extended to explain initial likeliness, the drop 3 / gain 1 / min 1 / max 20 rule, fixed vs adaptive special sections and the warning that an adaptive rare section climbs to 20
- [ ] 4.3 If `app-game-level-designer` has landed: add `X`/`RES` adaptive toggles to the designer, have `LevelWriter` write `stack.adaptive` only when it differs from the defaults, and extend its round-trip test; otherwise leave a note on that change's tracking issue that it must add them

## 5. Verify

- [ ] 5.1 Run `./gradlew :core:test` and `./gradlew :app:testDebugUnitTest`; run only the instrumented tests that drive the stack or start a level (`:app:connectedDebugAndroidTest` filtered to the stack and game screen tests) and confirm they pass
- [ ] 5.2 Play two bundled levels with a cross on the emulator and post a short note on #35 on how the stack feels
