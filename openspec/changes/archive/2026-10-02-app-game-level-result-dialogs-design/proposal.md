# Proposal

Tracking issue: [#40 Win and loss dialogs design](https://github.com/hexmasternl/leak-out/issues/40).

## Why

The level complete and level lost dialogs still use the old "slim" look: a white `SurfaceCard` with a 3 dp ink outline, uppercased `Header1` titles and the blue/yellow/red `CartoonButton`s. The design system (`docs/design-system.md`, *Deep Flow*, §5.6) prescribes dark level 3 floating panels with brass, glass and success buttons, but its own win dialog describes a score row of **Pipes, Bonus, Score** that the game never had: the game scores **points** earned during play, minus a **penalty** for unused sections, giving the **score**. The design system also only describes one tall, centred panel, which does not fit the game's 360 dp high landscape screens, so the app had to improvise its own layout.

## What Changes

- **Design system first** (`docs/design-system.md` §5.6, §7.2, §7.4 and the `docs/design-system.html` mock-up):
  - The win dialog's score row becomes **Points · Penalty · Score** (Penalty shown as "−12" in `leak.500`, Score in `brass.300`), replacing Pipes · Bonus · Score.
  - Both result dialogs are specified completely: eyebrow "LEVEL 7" (`success.500` on a win, `leak.500` on a leak), `display.xl` headline ("Level Complete!" / "Leaked!"), a `body.md` line, stars (win) or the splash (leak), the score row and the buttons. Win: Success "Next Level", glass "Restart", tertiary "Exit". Leak: Primary (brass) "Restart", tertiary "Exit". The names follow the game ("Restart", "Exit") instead of "Replay", "Try Again" and "Levels".
  - Two panel layouts: **Regular** (one centred column, max 360 dp wide, large buttons) when it fits the available height, and **Compact** (two columns: the information on the left, the buttons stacked on the right, max 600 dp wide, medium buttons, smaller headline and stars, message line hidden) when it does not. In Compact, if the information column still does not fit (large font scale), it scrolls and the buttons stay visible.
  - Entry motion (scale 0.92 → 1 with `motion.pop`, scrim fade 160 ms) and its reduced-motion variant; the leak splash animation and its static reduced-motion sprite.
  - The design is proposed on issue #40 with Compose preview mock-ups and approved there before the app work starts.
- **App dialogs comply with the design system**: `LevelCompleteDialog` and `LevelLostDialog` are rebuilt as level 3 `surface.base` panels over the 72 % `surface.scrim`, use the `FlowButton` component (from `app-game-pause-dialog-enhancements`, extended with Success and Tertiary variants), pick the Regular or Compact layout from the space they have, and show the level number in the eyebrow.
- **Row labels**: "Pipe points", "Unused pipes", "Total" become "Points", "Penalty", "Score", also for the screen reader ("Points 120", "Penalty minus 12", "Score 108").
- No behaviour change: the tally, star reveal, button enabling, Next Level / Restart / Exit, Back and background handling work as today. The scrim stays see-through on purpose, because the tally removes unused sections from the field behind the win dialog and the leak dialog relies on the player seeing where it leaked.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `game-play-level-complete`: the dialog's rows are Points, Penalty and Score; the dialog shows the level number, follows the design system and switches to a two-column layout when the stacked one does not fit the screen height; accessibility reads the new row labels.
- `game-play-level-lost`: the dialog shows the level number, follows the design system and switches to a two-column layout when the stacked one does not fit the screen height.

## Impact

- `docs/design-system.md` §5.1 (Success and Tertiary in use), §5.6 (win and leak dialogs, Regular and Compact layouts), §7.2/§7.4 (dialog entry, splash, reduced motion); `docs/design-system.html` dialog mock-ups.
- `app/ui/game/LevelCompleteDialog.kt`, `app/ui/game/LevelLostDialog.kt`: new panels and layouts; test tags unchanged plus `win_layout_*` / `lost_layout_*` semantics for the chosen layout and a `lost_splash` tag.
- New `app/ui/game/ResultDialogLayout.kt`: the pure layout choice and shared panel scaffolding for both dialogs.
- `app/ui/components/FlowButton.kt`: Success and Tertiary variants (the component itself comes from `app-game-pause-dialog-enhancements`; if that change has not landed yet, this change adds it as designed there).
- `app/res/values/strings.xml`: row labels and descriptions, eyebrow "Level %1$d", Title Case button labels.
- `GameScreen.kt`: passes the level number to both dialogs.
- No `core` change. Existing instrumented tests that drive the dialogs by tag keep working; tests that assert the old row texts are updated.
