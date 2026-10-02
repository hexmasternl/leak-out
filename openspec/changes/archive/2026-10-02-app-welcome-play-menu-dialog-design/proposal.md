# Proposal

Tracking issue: [#57 The game start dialog is not in the correct style](https://github.com/hexmasternl/leak-out/issues/57).

## Why

Tapping **Play** on the welcome screen opens the play menu ("Continue" / "Select level"). That dialog still uses the old "slim" cartoon look: a white `SurfaceCard` column with a 3 dp `InkDark` outline and 16 dp corners, an uppercased `Header1` title "PLAY", blue and yellow `CartoonButton`s, and the platform's grey window dim. The design system (`docs/design-system.md`, *Deep Flow*) prescribes one dialog look for the whole game (§4.3 level 3, §5.1 buttons, §5.6 dialogs, §7.2/§7.4 motion), and the pause, win and leak dialogs already follow it. The play menu is the first dialog every player sees, and it is the only one left that does not follow it.

## What Changes

- **Panel per §5.6**: level 3 floating panel (shadow `0 24 64` black 60 %, no outline), `radius.lg` (24 dp), `surface.base`, a 1 dp `line.highlight` top edge, 24 dp padding, max width 360 dp, centred with a 16 dp gutter to the window edge. It reuses the panel the result and pause dialogs already draw.
- **Scrim**: the dialog draws the see-through `surface.scrim` (`#07111C` at 72 %) itself over the whole window, platform dim off. A tap on the scrim outside the panel closes the dialog, as a tap outside does today.
- **Title**: "Play" in Title Case, `display.lg`, `text.primary` (no longer uppercased).
- **Buttons per §5.1**: brass **Primary** "Continue" (the one main action) and glass **Secondary** "Select Level", both `FlowButton` Medium (48 dp face + 4 dp lip), full panel width, 12 dp apart. The label becomes Title Case, "Select Level", as §5.1 requires for button labels.
- **Entry motion per §7.2**: the scrim fades in over `motion.quick` (160 ms) while the panel scales 0.92 → 1 with `motion.pop`; with reduced motion (§7.4) the panel fades in over 160 ms without the scale. Closing is immediate.
- `docs/design-system.md` §5.6 gets a **Play menu** entry next to Pause, Win and Leak.
- No behaviour change: Continue, Select Level, Back and tap outside work as today, including the locked-page fallback.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `play-menu-dialog`: the dialog's look follows the design system (panel, scrim, title, button variants, entry motion); the second button's label becomes "Select Level".

## Impact

- `app/ui/title/PlayMenuDialog.kt`: full-window dialog with its own scrim, the shared level 3 panel, `FlowButton`s and the entry motion; test tags unchanged (`play_menu_dialog`, `play_menu_continue`, `play_menu_select_level`) plus a new `play_menu_scrim`.
- `app/ui/game/ResultDialogLayout.kt` → `app/ui/components`: the level 3 panel (`ResultDialogPanel`), its metrics and `ResultDialogEntry` move to a shared component so a title-screen dialog does not depend on the game package (see design).
- `app/res/values/strings.xml`: `play_menu_select_level` becomes "Select Level".
- `app/androidTest/.../PlayMenuDialogTest.kt`: the title assertion changes from "PLAY" to "Play"; new tests for the look and the motion.
- `docs/design-system.md` §5.6.
- No `core` change. The welcome screen's own buttons are out of scope (see design, non-goals).
