# Proposal

Tracking issue: [#39 Pause dialog enhancements](https://github.com/hexmasternl/leak-out/issues/39).

## Why

The pause dialog still uses the old "slim" cartoon look (white card, 3 dp ink outline, uppercase title, blue/yellow cartoon buttons) while the design system (`docs/design-system.md`, *Deep Flow*) prescribes a dark floating panel with glass, brass and leak buttons. It also sits over a 72 % scrim through which the frozen board stays readable, so a player can pause to study the field and the stack without the countdown or the flow running: a free advantage.

## What Changes

- **Opaque backdrop**: while the pause dialog is open the whole screen (HUD, board, stack and the system bar areas) is covered by an opaque diagonal gradient from `surface.abyss` (top left) to `surface.deep` (bottom right). Nothing of the game can be seen through it, also not when the dialog is shown after coming back from the background. The backdrop fades in over `motion.quick` (160 ms).
- **Panel per design system §5.6**: level 3 floating panel (no ink outline, shadow `0 24 64` black 60 %), `radius.lg` (24 dp), `surface.base`, 24 dp padding, max width 360 dp, centred. Entry: scale 0.92 → 1 with `motion.pop`; with reduced motion a 160 ms ease-out fade without scaling.
- **Title**: "Paused" in Title Case, `display.lg`, `text.primary` (no longer uppercased).
- **Buttons per design system §5.1**: brass **Primary** "Resume", glass **Secondary** "Restart", a `line.subtle` separator, **Destructive** "Exit". Medium height (48 dp face + 4 dp lip) so the panel fits a 360 dp high landscape screen. Lip and 2 dp press travel over 90 ms per §4.4.
- A new design-system button component (`FlowButton`) with the Primary, Secondary (glass) and Destructive variants, plus the missing colour tokens it needs. Other screens keep `CartoonButton` for now.
- `docs/design-system.md` §5.6 is aligned with the game: the pause entry names the opaque gradient backdrop, the separator, "Exit" (which returns to the main menu, not to the levels) and Resume as the brass primary.
- No behaviour change: Resume, Restart, Exit, Back, tap outside and the background pause work as today.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `game-play-pause`: the game screen is no longer visible (dimmed) behind the pause dialog but fully hidden by an opaque gradient backdrop; the dialog's look follows the design system.

## Impact

- `app/ui/game/PauseDialog.kt`: full-screen dialog window with the gradient backdrop, new panel and buttons; test tags unchanged (`pause_dialog`, `pause_resume`, `pause_restart`, `pause_divider`, `pause_exit`) plus a new `pause_backdrop`.
- `app/ui/components`: new `FlowButton` (design system §5.1) next to `CartoonButton`.
- `app/ui/theme/Color.kt`: `Leak500`, `Leak700`, `LineHighlight`, `TextOnBrass`, `TextOnStatus` tokens if still missing (the in-flight `app-game-play-mandatory-sections` change may add `Leak500` first).
- `app/res/values/strings.xml`: unchanged ("Paused", "Resume", "Restart", "Exit").
- `docs/design-system.md` §5.6.
- No `core` change. Existing instrumented tests that drive the pause dialog by tag keep working.
