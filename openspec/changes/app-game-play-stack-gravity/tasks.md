# Tasks

Tracking issue: [#37 Stack animation](https://github.com/hexmasternl/leak-out/issues/37).

## 1. Docs

- [x] 1.1 Update `docs/design-system.md` §5.4 "Stack rail" to match the game (left-hand column in landscape, next piece at the bottom with the brass frame, preview pieces above, `visibleSize` slots guaranteed, extra pieces clipped at the top edge, no scrolling) and describe the gravity drop; add `motion.fall` (quadratic ease-in fall 120–260 ms, one ≤ 4 dp bounce over 120 ms, 24 ms stagger per piece from the bottom up) to §7.1/§7.2 and the stack rule to §7.4 reduced motion (160 ms ease-out slide, new piece fades in); update `docs/design-system.html` if it mirrors these sections; verify every token named exists in the document
- [x] 1.2 Update `docs/level-format.md`: `stack.visibleSize` is the guaranteed visible part of the stack, the stack holds `visibleSize + 10` sections; verify the defaults table and example still agree with `LevelDefaults`

## 2. Core

- [x] 2.1 In `core/stack/PipeStack.kt` add `EXTRA_SECTIONS = 10`, make `create` fill `visibleSize + EXTRA_SECTIONS` sections, add `placements: Int = 0` (incremented by `afterPlacement`) and `idAt(index)` = `placements + size - 1 - index`; verify `PipeStackTest`: a new stack holds `visibleSize + 10` for sizes 1, 5 and 10, size is kept after a placement, `idAt` of the new next section equals the old `idAt` of the second-from-bottom, the new top id is unique, and the same-seed test still passes
- [x] 2.2 Check `GameEngine`, `LevelSolver` and the headless level tester only use the next section and are unaffected; verify with `./gradlew :core:test`

## 3. Layout and motion model (app, JVM)

- [ ] 3.1 Change `StackLayout.metrics` callers to pass `visibleSize` (from the level) instead of `stack.size`; add `slotTops(metrics, stackSize, railHeight)` (bottom-anchored positions) and `fullyVisibleSlots(...)`; verify `StackLayoutTest`: on 360 dp minus the HUD with `visibleSize` 10 and a stack of 20 the bottom 10 slots are fully visible and slot sizes equal today's metrics, a taller rail fully shows more slots at the same size, a slot crossing the top edge is not in `fullyVisibleSlots`, and slots more than one pitch above the edge are reported as not composed
- [x] 3.2 Add a pure `StackGravity` model in `app/ui/game`: `fallDuration(d)`, `offsetAt(d, t)` (quadratic fall, one bounce ≤ min(0.12 d, 4 dp), rest), `startDelay(indexFromBottom)` (24 ms, capped at 8), `retarget(currentOffset, pitch)` and the reduced-motion variant (160 ms ease-out, no bounce, no stagger); verify JVM tests: offset starts at d and ends at 0, velocity increases during the fall, the bounce peak is ≤ 4 dp, one slot rests within 400 ms, the bottom section starts first, retargeting never yields an offset above the section's current position, and the reduced-motion curve is monotonic over 160 ms

## 4. Stack panel (app, Compose)

- [ ] 4.1 Rebuild `StackPanel` as a clipped, bottom-anchored `Box` using `slotTops`; one `Animatable` offset per section keyed by `PipeStack.idAt`; offsets applied via `graphicsLayer` translation; the falling next section interpolates size and frame from preview to next; the new section starts one pitch above the top slot; compose only slots within one pitch above the rail; remove the unused `downOffset`; verify an instrumented test that the stack panel shows the next section at the bottom and that no slot node is drawn outside the panel bounds
- [ ] 4.2 Drive the gravity animation on accepted placements (and retarget on a placement mid-fall) and the reduced-motion branch via `rememberReducedMotion()`; verify instrumented tests with the compose clock paused: after a placement the second-from-bottom section is above its new slot at 0 ms, lower at 100 ms and at rest by 400 ms; two quick placements never move a section upwards; with reduced motion the section is at rest after 160 ms with no bounce
- [ ] 4.3 Keep the next section draggable during its fall (the drag snaps its offset to 0, slot shows empty); keep disabled states (fast forward, replacement, stopped game) as today; verify an instrumented test that starts a drag 50 ms after a placement and drops it on a valid cell, and that rejected drops still leave the stack unchanged
- [ ] 4.4 Give only fully visible slots semantics; clipped slots use `clearAndSetSemantics {}`; verify an instrumented test on a 640 × 360 dp screen with `visibleSize` 10 that exactly the fully visible slots have content descriptions and the next one says it is next
- [ ] 4.5 Add `@Preview`s of the rail with `visibleSize` 5 and 10 on a 360 dp tall screen, and post a short screen recording of the drop on issue #37

## 5. Regression

- [ ] 5.1 Update tests that assume the stack size equals `visibleSize` (`GamePlayHudTest`, `GameEngineScreenTest`, `FieldTestSupport`) and run `./gradlew :core:test :app:testDebugUnitTest`; verify both pass
- [ ] 5.2 Run the affected instrumented tests on the emulator (`GameScreenTest`, `GameEngineScreenTest`, `GamePlayHudTest`, `PlayingFieldTest`, `FastForwardTest` and the new stack tests); verify they pass
