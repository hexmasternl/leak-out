# Tasks

## 1. Reproduce

- [x] 1.1 Use an S21-like AVD (the existing Pixel_10: 1080×2424, 420 dpi, punch-hole cutout; the S21 is 1080×2400, 421 dpi) and S21-sized test windows (914×411 dp); verify the app launches on it with the intro tour shown
- [x] 1.2 Add an instrumented test in `GameTipsTest` that, with no tips seen yet, taps "Skip tour" on the start step with a real down/up right after the overlay appears; verify it fails on the current code (or record in design.md why it cannot be reproduced on the emulator)
- [x] 1.3 Add an instrumented test that taps "Next" while the finish step's field reveal pan and spotlight glide are running; run it on the current code and record the result in design.md

## 2. Stable callout placement

- [x] 2.1 Remove the layout-phase state write of `pointer` in `CalloutLayer` and derive it in the same pass; verify with a test that the card's bounds do not change between frames once a step has settled
- [x] 2.2 Place the card from each step's final target rects and do not move it while a pointer is down on it; verify with an instrumented test that holds "Skip tour" during the spotlight glide and checks that the button's bounds do not change
- [x] 2.3 Read the glow and entry animations in the draw phase so `TipOverlay` no longer recomposes every frame; verify with a recomposition-count assertion in `TipOverlayTest`

## 3. Safe area

- [x] 3.1 Feed the display cutout, safe drawing and system gesture insets into `CalloutPlacement`; verify with new JVM tests covering a side cutout, large gesture insets and a 640×360 dp window
- [x] 3.2 Add an instrumented test on the S21 profile asserting the "Next" and "Skip tour" bounds lie outside the cutout and gesture insets; verify it passes

## 4. Rapid taps

- [x] 4.1 Add tests that two quick taps on "Next" from the start step show the stack step, and that a tap right after "Skip tour" neither reopens the tour nor reaches the game; verify both pass

## 5. Verify

- [ ] 5.1 Run `./gradlew :core:test :app:testDebugUnitTest` and the affected instrumented tests (`TipOverlayTest`, `GameTipsTest`), and verify they all pass, including the tests from 1.2 and 1.3 that failed before
- [ ] 5.2 Install a build on a real Samsung S21 (or through the internal testing track), clear the app data, and confirm "Next", "Skip tour" and "Let's go" respond on the first tap; note the result in the GitHub issue
