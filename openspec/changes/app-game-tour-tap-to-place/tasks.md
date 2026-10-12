# Tasks

## 1. Step model and texts

- [ ] 1.1 Raise `INTRO_STEPS` to 6 and map step 4 to the new tap to place texts in `tipContent` (`ui/game/onboarding/TipOverlay.kt`); zoom and pan becomes step 5.
- [ ] 1.2 Add `tip_intro_tap_title`, `tip_intro_tap_text` and `tip_intro_tap_target` to `res/values/strings.xml` with the texts from design.md.
- [ ] 1.3 Unit test: the six steps come in the order start, finish, stack, placing, tap to place, zoom and pan, the counter reads "5 / 6" on the new step, and the last step still offers "Let's go".

## 2. Tap hint in the overlay

- [ ] 2.1 Replace the overlay's nullable `DragHint` with a sealed hint type (`DragHint`, `TapHint(cell)`) and draw the tap hint: a ring that scales up and fades on the cell, looping on `motion.pulse`; a static ring with reduced motion.
- [ ] 2.2 In `GameScreen`, spotlight only the hint cell on step 4, reveal it as the placing step does, and pass a `TapHint` for it; keep the `DragHint` on step 3.
- [ ] 2.3 Add a `Tap` entry to `TipMockupStep` and a preview for it.
- [ ] 2.4 Compose UI test: on the tap to place step the hint cell is highlighted, a tap hint is shown and no drag hint is; with reduced motion the ring is static.

## 3. Accessibility and existing tests

- [ ] 3.1 Check that the screen reader announcement reads "Step 1 of 6" and names "an empty cell" on the new step; update the accessibility test.
- [ ] 3.2 Update instrumented tests that walk or count the tour's steps (`MainActivityTest` and any tour test) to the six-step tour, and run the affected ones on an emulator.

## 4. Documentation

- [ ] 4.1 Add the new step to the tip overlay table in `docs/design-system.md` section 5.9 (target, texts, step counters "n / 6", the tap hint animation and its reduced-motion form).

## 5. Verify

- [ ] 5.1 Run `./gradlew :app:testDebugUnitTest` and the affected instrumented tests; all pass.
