# Design

## Context

- `LevelCompleteDialog` and `LevelLostDialog` are Compose `Dialog`s holding a white `SurfaceCard` column with a 3 dp `InkDark` border, `CartoonButton`s and old text styles. The win card is already a landscape improvisation (celebration and stars left, score rows right, buttons across the bottom, max 560 dp); the lost card stacks pipe + title, message, score and two buttons side by side.
- The win dialog's rows are driven by `WinFrame` (`WinTally`): earned points at once, the penalty counting up while unused sections are removed from the field behind the dialog, then the total counting up, then the stars. Rows are tagged `win_earned`, `win_penalty`, `win_total`.
- Design system §5.6 describes one tall centred panel per dialog and a Pipes · Bonus · Score row that does not match the game's scoring (`game-play-final-score`: earned points − 3 per unused section = total, never below 0).
- The game is landscape only and runs down to 640 × 360 dp.
- `app-game-pause-dialog-enhancements` (in flight) introduces `FlowButton` (Primary, glass Secondary, Destructive) and the missing colour tokens (`Leak500`, `Leak700`, `LineHighlight`, `TextOnBrass`, `TextOnStatus`).

## Goals / Non-Goals

**Goals:**
- One design for each result dialog in the design system, matching the game's scoring and buttons, with a documented variant for short screens.
- Both dialogs in the app look exactly as designed and are fully visible on 640 × 360 dp at 100 % and 130 % font scale.

**Non-Goals:**
- Any behaviour change (tally timing, star rules, buttons, Back, background handling).
- Telling the player *where* it leaked (the design system's "row 4, column 3" body text): a separate feature.
- An opaque backdrop like the pause dialog: the tally needs the field visible (see decision 5).
- The Daylight theme and portrait layouts (the app is landscape only).

## Decisions

### 1. Score row: Points · Penalty · Score
Three equal columns, each a `label.md` caption in `text.secondary` above a value: Points in `numeric.lg` `text.primary`, Penalty as "−12" in `numeric.lg` `leak.500` ("0" in `text.muted` while there is no penalty), Score in `numeric.lg` `brass.300` with a brass `G` glow once the tally is done. Values use digit grouping. Each column keeps its existing tag (`win_earned`, `win_penalty`, `win_total`) and is one merged screen-reader node. The labels map one-to-one to `FinalScore.earned`, the penalty and `FinalScore.total`, so `WinTally` is unchanged. The leak dialog shows a single centred "Score" column with the attempt's score.

### 2. Content and buttons
- Win: eyebrow "LEVEL 7" (`display.xs`, `success.500`), headline "Level Complete!" (`display.xl`, `text.primary`), message "You kept the water flowing." (`body.md`, `text.secondary`) whose place "New best!" takes once the tally is done (`display.sm`, `brass.300`), three stars (40 dp, earned `brass.300`, missed `line.strong`), score row, buttons Success "Next Level", glass "Restart", tertiary "Exit".
- Leak: eyebrow "LEVEL 7" (`display.xs`, `leak.500`), the splash (72 dp), headline "Leaked!" (`display.xl`, `text.primary`), message "The pipe is leaking. Level lost." (`body.md`, `text.secondary`), score column, buttons Primary "Restart", tertiary "Exit".
- "Restart" and "Exit" are kept because they are what the game does (Exit goes to the main menu); the design system's "Replay", "Try Again" and "Levels" are replaced. Exit is tertiary, not destructive: leaving after a finished attempt loses nothing.

### 3. Two layouts, chosen by measuring
- **Regular**: one centred column, max 360 dp wide, 24 dp padding, large (56 dp) buttons stacked full width with 12 dp spacing, the tertiary Exit as a 48 dp text button.
- **Compact**: max 600 dp wide, 24 dp padding. Left column (weight 1, centred): eyebrow, headline in `display.lg`, "New best!" chip or nothing (the message line is dropped), stars 32 dp (win) or splash 48 dp beside the headline (leak), score row. A 1 dp `line.subtle` vertical divider. Right column, fixed 200 dp, vertically centred: the buttons stacked, medium (48 dp face + 4 dp lip) with 12 dp spacing.
- The dialog measures the Regular content at its maximum width with `SubcomposeLayout` and uses it only if its height fits the window height minus 2 × 16 dp gutters; otherwise Compact. The choice is a pure function `ResultDialogLayout.choose(availableHeightPx, regularHeightPx)`, unit tested. Measuring (instead of a fixed breakpoint) keeps it right under font scaling and on tablets, where Regular is used.
- If Compact's information column is still taller than the available height (e.g. 130 % font on 360 dp), that column scrolls vertically and the buttons column stays fully visible.
- The chosen layout is exposed as test semantics (`win_layout_regular` / `win_layout_compact`, same for `lost_`), so instrumented tests on the 360 dp emulator can assert Compact.
- Estimates: Regular win ≈ 440 dp, Regular leak ≈ 456 dp (so phones in landscape get Compact, tablets get Regular); Compact win ≈ 233 dp, buttons column ≈ 176 dp, within 328 dp.

### 4. Shared panel and buttons
A private `ResultDialogPanel` (level 3 shadow `0 24 64` black 60 %, `radius.lg`, `surface.base`, 1 dp `line.highlight` top edge, no outline) and the layout switch live in `ResultDialogLayout.kt` and are used by both dialogs. Buttons use `FlowButton`; this change adds the **Success** (gradient `success.500 → success.700`, label `text.onStatus`, lip `success.700`, success glow) and **Tertiary** (no fill, `liquid.300` label, no lip, 48 dp touch target) variants per §5.1. If `app-game-pause-dialog-enhancements` has not been implemented yet when this one is, its FlowButton tasks (component, tokens) are done here first, exactly as designed there.

### 5. Keep the see-through scrim
The dialogs stay over `surface.scrim` (`#07111C` at 72 %, platform dim off, drawn by the dialog) because the penalty phase visibly removes unused sections from the field and the leak effect shows where the pipe leaked. Hiding the board would remove that feedback; there is no gameplay advantage to protect after the attempt has ended.

### 6. Motion
Entry: scrim fades in over `motion.quick`, panel scales 0.92 → 1 with `motion.pop`. Win stars keep their existing reveal (driven by `WinFrame`, 80 ms stagger with brass burst). Leak splash: 6 droplets fly out of a centre blob over 600 ms once, then rest. Reduced motion: no scale (160 ms ease-out fade), static splash sprite; the tally itself is gameplay feedback and runs as today.

## Risks / Trade-offs

- [Measuring the Regular layout costs an extra subcomposition every recomposition of the tally] → Measure once per window size and font scale (`remember` keyed on constraints and density); the tally only recomposes values.
- [Layout flips between Regular and Compact mid-tally when a value widens] → The measurement uses the final values (final total, max penalty, "New best!"), not the animated ones, so the choice is stable for the dialog's lifetime.
- [Depends on the unlanded `FlowButton`] → Stated in decision 4; tasks cover building it here if needed.
- [Tests that look up "Pipe points" / "Unused pipes" / "Total" by text break] → They are updated in the same change; tag-based tests are unaffected.
