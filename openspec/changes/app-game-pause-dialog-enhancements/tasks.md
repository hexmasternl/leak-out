# Tasks

Tracking issue: [#39 Pause dialog enhancements](https://github.com/hexmasternl/leak-out/issues/39).

## 1. Design system and tokens

- [ ] 1.1 Update `docs/design-system.md` §5.6 "Pause": opaque diagonal `surface.abyss → surface.deep` backdrop (instead of the scrim) with its 160 ms fade-in, title "Paused" `display.lg`, brass primary "Resume", glass "Restart", `line.subtle` separator, destructive "Exit" (returns to the main menu), medium buttons; add the backdrop to §7.4 reduced motion; verify every token named exists in §2–§4 and `docs/design-system.html` is updated to match if it mirrors §5.6
- [ ] 1.2 Add the missing tokens to `LeakOutColors` with the design-system values: `Leak500` `#FF5A5F` (unless `app-game-play-mandatory-sections` already added it), `Leak700`, `LineHighlight` (`#FFFFFF` at 22 %), `TextOnBrass` `#2A1A03`, `TextOnStatus` `#FFFFFF`; verify with a JVM unit test asserting each token's ARGB value, run with `./gradlew :app:testDebugUnitTest`

## 2. FlowButton component

- [ ] 2.1 Add `FlowButtonMetrics` (lip 4 dp resting / 2 dp pressed, 2 dp press travel, sizes 56/48/40 dp, 48 dp minimum touch target, `radius.md`) and `FlowButtonVariant` (Primary, Secondary glass, Destructive: fill brush colours, label, lip, glow) in `app/ui/components/FlowButton.kt`; verify JVM unit tests for face offset and visible lip pressed/unpressed, constant footprint, minimum touch target per size, and each variant's colours against the design system §5.1 table
- [ ] 2.2 Build the `FlowButton` composable (gradient or glass fill, 1 dp `line.strong` border for glass, lip, brass glow for Primary, `display.sm` label in Title Case, 90 ms linear press, disabled look per §5.1, `Role.Button`); add `@Preview`s of the three variants at rest and pressed; verify an instrumented test that a click calls `onClick`, a disabled button ignores clicks, the node has the Button role and its label, and its height is at least 48 dp

## 3. Pause dialog

- [ ] 3.1 Make `PauseDialog` a full-screen, edge-to-edge dialog window without platform dim (design decision 1) whose content is a `pause_backdrop` box with the diagonal `SurfaceAbyss → SurfaceDeep` gradient that consumes taps; keep `dismissOnBackPress = true` and `dismissOnClickOutside = false`; verify instrumented tests that the backdrop's bounds equal the root's size, that a tap on the backdrop leaves the dialog open and the game paused, and that Back resumes
- [ ] 3.2 Verify the backdrop is opaque with an instrumented screenshot test: pause on a level, capture the root, and assert the top-left pixel is `#07111C`, the bottom-right pixel `#0C1A2A`, and that every pixel outside the panel lies on the gradient between those two colours (so nothing of the board, stack or HUD shows through)
- [ ] 3.3 Restyle the panel per §5.6 (level 3 shadow, `radius.lg`, `surface.base`, 24 dp padding, max 360 dp wide, no border), the Title Case `display.lg` `text.primary` title, `FlowButton`s Medium (Resume Primary, Restart Secondary, Exit Destructive) with 12 dp spacing and the 1 dp `line.subtle` separator; keep all `PauseTags`; add a `@Preview` at 640 × 360 dp; verify instrumented tests that the title text is "Paused", the order is title, Resume, Restart, divider, Exit, and every element is fully displayed on a 640 × 360 dp screen
- [ ] 3.4 Add the entry motion: backdrop alpha 0 → 1 over `motion.quick`, panel scale 0.92 → 1 with `motion.pop`; with animations off (animator duration scale 0) the backdrop is shown at once and the panel fades in over 160 ms without scale; the dialog closes immediately on Resume/Restart/Exit; verify an instrumented test with the clock paused that the panel scale starts at 0.92 and ends at 1, and the reduced-motion branch with a JVM test of the motion spec selection

## 4. Regression

- [ ] 4.1 Run `./gradlew :core:test :app:testDebugUnitTest` and fix failures; verify both pass
- [ ] 4.2 Run the instrumented tests that drive the pause dialog (`MainActivityTest`, `FastForwardTest` and the new pause dialog tests) plus `FlowButton`'s test on the emulator; verify they pass
- [ ] 4.3 Pause during a level on the emulator, take a screenshot of the dialog and post it on issue #39; verify the board, stack and HUD cannot be seen and the comment links the screenshot
