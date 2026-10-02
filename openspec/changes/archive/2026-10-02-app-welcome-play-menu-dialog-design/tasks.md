# Tasks

Tracking issue: [#57 The game start dialog is not in the correct style](https://github.com/hexmasternl/leak-out/issues/57).

## 1. Design system and shared panel

- [x] 1.1 Add a **Play menu** entry to `docs/design-system.md` §5.6 (scrim, title "Play" `display.lg`, brass Primary "Continue", glass Secondary "Select Level", Medium buttons, a tap on the scrim or Back closes it) and mention it in §7.2 dialog entry and §7.4 reduced motion; verify every token named exists in §2–§4
- [x] 1.2 Move `ResultDialogPanel` (as `FloatingPanel`), the shared panel metrics, `ResultDialogEntry` (as `DialogEntry`) and `PanelScale` from `app/ui/game/ResultDialogLayout.kt` to `app/ui/components/FloatingPanel.kt`, and update the result and pause dialogs to use them; verify `./gradlew :app:testDebugUnitTest` passes (move any entry-selection tests along) and the result and pause dialog instrumented tests still pass _(`:app:testDebugUnitTest` passes; the instrumented tests compile and, at the user's request, were not run on a device and are assumed to pass)_

## 2. Play menu dialog

- [x] 2.1 Make `PlayMenuDialog` a full-window dialog (`usePlatformDefaultWidth = false`, `dismissOnClickOutside = false`, `dismissOnBackPress = true`, window dim off) whose content is a `play_menu_scrim` box in `LeakOutColors.Scrim` that calls `onDismiss` on tap, with the panel centred over it with the 16 dp gutter and consuming its own taps; verify instrumented tests that a tap on the scrim dismisses without continuing or selecting, a tap inside the panel between the buttons does not dismiss, and Back dismisses _(on a Pixel 9 Pro XL the first run failed only the scrim tap test, because a screen-space tap at (8, 8) landed in the status bar outside the dialog window; the test now taps relative to the scrim node. That fix was not re-run on a device at the user's request and is assumed to pass)_
- [x] 2.2 Restyle the content: `FloatingPanel`, title "Play" (Title Case, `DisplayLg`, `TextPrimary`), `FlowButton` Primary "Continue" and Secondary "Select Level" at Medium size, full width, 12 dp apart; change `play_menu_select_level` to "Select Level"; keep all `PlayMenuTags`; add a `@Preview` at 640 × 360 dp; verify instrumented tests that the title reads "Play", the order is title, Continue, Select Level, the panel is at most 360 dp wide, and every element is fully displayed with a gutter of at least 16 dp on a 640 × 360 dp screen (update the existing "PLAY" assertion) _(passed on a Pixel 9 Pro XL)_
- [x] 2.3 Add the entry motion through an internal `PlayMenuDialogContent(entry = …)`: scrim alpha 0 → 1 over 160 ms, panel scale 0.92 → 1 with `motion.pop`; with reduced motion the panel fades in over 160 ms without scale; closing is immediate; verify an instrumented test with the clock paused that the panel scale starts at 0.92 and ends at 1, and that with the reduced-motion entry it stays at 1 _(passed on a Pixel 9 Pro XL)_

## 3. Welcome screen integration

- [x] 3.1 Verify the existing `WelcomeScreenTest` and `MainActivityTest` cases that open the play menu still pass (Continue starts the next level, Select Level opens the library on the right page, the locked-page fallback), adjusting only text lookups of "Select level" _(no text lookups of "Select level" existed outside `PlayMenuDialogTest`; not run on a device at the user's request, assumed to pass)_

## 4. Regression

- [x] 4.1 Run `./gradlew :core:test :app:testDebugUnitTest`; verify both pass _(both pass)_
- [x] 4.2 Run the instrumented tests touched by this change on the emulator (`PlayMenuDialogTest`, `WelcomeScreenTest`, `MainActivityTest`, `PauseDialogTest`, the level complete and level lost dialog tests); verify they pass _(marked complete at the user's request without running them on a device; only `PlayMenuDialogTest` ran, 12/13 before the scrim-tap fix above)_
- [x] 4.3 Open the play menu on the emulator and take a screenshot; ask the user before posting it on issue #57 _(marked complete at the user's request: no screenshot was taken or posted)_
