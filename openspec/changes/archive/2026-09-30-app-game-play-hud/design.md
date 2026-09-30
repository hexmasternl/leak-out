## Context

The open `app-game-play-stack` change adds a stand-in `GameScreen(level, random)`: `StackPanel` on the left, `PlaceholderBoard` in the rest of the width, a `PipeStack` in local state and a `SectionDragState` for drag and drop. `MainActivity` wires **Play** to that screen with level 1. There is no `GameEngine`, flow simulation, countdown or scoring yet. The plan (section 2.6) gives the future `GameState` a `score` and a `paused` flag, and the engine `Pause`, `Resume`, `Restart` and `Tick` commands. It picks `withFrameNanos` in a `LaunchedEffect` for the game loop (section 2.7).

This change builds on the stack change and must merge after it. It adds the HUD and the pause menu without waiting for the engine, so it needs a small state holder that the engine can take over later.

Every `Level` carries `StarBenchmarks` (`stars.one`, `two`, `three`) with `starsFor(points): Int` in `core` (from `app-game-level-model`), which returns 0 to 3. The design system (`docs/design-system.md`) draws an active star in `brass.300` with a brass glow and a missed star in a muted grey, and lists "star pop" under `motion.pop`.

The welcome screen already has a dialog style from `app-welcome-play-button` and `app-welcome-exit-button`: a Compose `Dialog` with the design-system card (white surface, 3 px ink border, Fredoka title) and `CartoonButton`s.

## Goals / Non-Goals

**Goals:**
- A `GameHud` composable with the level, flow speed, stars, score and cog wheel, placed as the spec describes.
- Three stars in the HUD that show, from the current score and the level's thresholds, how many stars the player has earned so far: grey until earned, glowing brass once earned.
- A `PauseDialog` with Resume, Restart, a separator and Exit.
- One place that owns "paused", the game clock, the score and restart, so that pause really freezes everything and restart really resets everything.
- Back and going to the background both pause the game.
- Text formatting and layout rules that can be unit tested on the JVM.

**Non-Goals:**
- The flow simulation, the countdown display, scoring rules and the speed ramp. The HUD only shows the values it is given.
- Level-complete and level-failed overlays.
- Sound and music toggles or any other setting in the pause dialog.
- The stars on the win dialog and on the level tiles. `HudStars` is written so it can be reused there, but only the HUD is in scope.
- A navigation library. Two screens do not need one yet.

## Decisions

**1. `GameSession` is the single owner of the running game.**
A plain Kotlin class in `app/.../ui/game/` (state held in Compose `mutableStateOf`), created with `remember(level)` in `GameScreen`:
- `level`, `stack: PipeStack`, `placed: Map<Position, PipeSection>`, `score: Int`, `flowSpeed: Double`, `elapsedMs: Long`, `paused: Boolean`, `pauseMenuOpen: Boolean`.
- `stars: Int` is derived, not stored: `level.stars.starsFor(score)`. See decision 10.
- `pause()` sets `paused` and `pauseMenuOpen`. `resume()` clears both. `restart()` resets the stack (a new `PipeStack.create(level.stack, random)`), clears `placed`, sets `score` to 0, `flowSpeed` to `level.flow.cellsPerSecond`, `elapsedMs` to 0, and clears `paused`.
- `tick(dtMs)` adds to `elapsedMs` only when not paused. `place(...)` is refused while paused.

The stack and placed sections move from `GameScreen`'s local state into `GameSession`, so restart can reset them in one step. When `GameEngine` exists, `GameSession` becomes a thin wrapper that sends `Pause`, `Resume`, `Restart` and `Tick` and reads `GameState`, and the HUD and dialog do not change. *Other option:* an AndroidX `ViewModel`. It was not chosen yet because the game screen has no navigation back stack, and a ViewModel scoped to the activity would outlive the level. It can be introduced with the engine.

**2. The game clock is a frame loop that stops while paused.**
`GameScreen` runs `LaunchedEffect(session.paused)` with a `withFrameNanos` loop that calls `session.tick(dt)` only while `paused` is false. The loop ends when the game pauses and starts again on resume, so no frame time from the pause leaks in. The first frame after resume uses a delta of 0. The countdown and flow will read `elapsedMs` later. Tests call `tick` directly and check that a paused session does not move.

**3. HUD layout is one `Row` with a weighted spacer.**
`GameHud(levelId, flowSpeed, stars, score, onPause)` is a `Row` of fixed height (`HudLayout.HEIGHT` = 48 dp) across the full width: level text, flow speed chip, `Spacer(Modifier.weight(1f))`, `HudStars(earned = stars)`, score text with `TextAlign.End`, cog wheel `IconButton` (48 dp touch target). The spacer keeps the gap in the middle and makes the score grow to the left; the stars sit on the left of the score and move with it. *Other option:* the stars next to the level, on the left. It was not chosen because the stars are a reading of the score, so they belong next to it, and the design system's HUD bar puts the star with the score. `GameScreen` becomes a `Column { GameHud; Row { StackPanel; Board } }`. `StackPanel` gets the height below the HUD from its constraints, so `StackLayout.metrics` gets `360 dp - 48 dp` on the smallest screen. A unit test adds a case for `visibleSize` 10 at 312 dp. *Other option:* the HUD drawn over the board. It was not chosen because the spec says the HUD must not cover the stack or board.

**4. Formatting is a pure helper.**
`HudFormat` holds `level(id)`, `speed(cellsPerSecond, locale)` (one decimal, the locale's decimal sign, for example "0.8" or "0,8") and `score(value, locale)` (the locale's digit grouping). The visible speed text is the number with a small speed icon and the unit "/s". Content descriptions come from string resources with placeholders ("Level %1$d", "Flow speed %1$s cells per second", "Score %1$s", "Pause menu"). Unit tests cover the English and Dutch locales.

**5. The cog wheel is a vector drawable, not an icons dependency.**
An `ic_cog.xml` vector in `res/drawable`, tinted with the theme ink color. *Other option:* `material-icons-extended`. It was not chosen because it is a large dependency for one icon.

**6. The pause dialog is a Compose `Dialog` in the existing card style.**
`PauseDialog(onResume, onRestart, onExit)` uses `Dialog(onDismissRequest = onResume, properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true))`. So Back works as Resume and a tap outside does nothing. The content is the design-system card with the title "Paused", `CartoonButton`s for Resume and Restart, a `HorizontalDivider` and an Exit `CartoonButton` in the error-red style, matching the exit dialog. The card gets `Modifier.semantics { paneTitle = "Pause menu" }` so a screen reader announces it. The dialog's default scrim dims the game screen.

**7. Back and background pause through the same call.**
- `BackHandler(enabled = !session.pauseMenuOpen) { session.pause() }` in `GameScreen`. While the dialog is open, the dialog's own back handling calls Resume.
- `LifecycleEventEffect(Lifecycle.Event.ON_STOP) { session.pause() }`. On return the session is still paused and `pauseMenuOpen` is true, so the dialog is shown. Pausing is idempotent, so a pause from the cog wheel followed by `ON_STOP` changes nothing.

**8. Pausing cancels a drag.**
`SectionDragState` gets `cancel()`, which runs the existing "slide back to slot" path without calling `tryPlace`. `session.pause()` is followed by `dragState.cancel()` in `GameScreen`. `StackPanel` gets an `enabled` flag (`!session.paused`). When it is false, the next slot has no drag gesture. The dialog covers the screen while paused anyway, but the flag also covers the moment between pause and the dialog appearing.

**9. Exit goes back to the welcome screen through a callback.**
`GameScreen(level, onExit, random)`. `MainActivity` holds a small `screen` state (`Welcome` or `Game(level)`), which is the one-line Play wiring from the stack change turned into two branches. Exit sets it back to `Welcome`. The play-menu and level-library changes replace this later with their own navigation, and `onExit` stays the contract.

**10. Stars are derived from the score, never stored.**
`GameSession.stars` is `level.stars.starsFor(score)`, the same function the win path and the level history will use, so the HUD can never disagree with the stars a win records. Restart resets the stars because it resets the score. There is no separate "star earned" event: `HudStars` compares the new count with the old one. Unit tests give a level with thresholds 100, 200 and 350 and check 0, 1, 1, 2 and 3 stars at 0, 100, 199, 200 and 350 points, and 0 again after restart. *Other option:* the engine or the points change sends a "star earned" event. It was not chosen because a derived value cannot drift, and the points change then only has to keep changing `score`.

**11. `HudStars` draws three stars from one vector.**
`HudStars(earned: Int, modifier)` is a `Row` of three 20 dp `Icon`s from `ic_star.xml` (filled star, 24 dp grid, design system section 8), 4 dp apart. Star `i` (0-based) is earned when `i < earned`.
- Not earned: tint `LeakOutColors.TextMuted` (`text.muted`), no glow.
- Earned: tint `LeakOutColors.Brass300` (`brass.300`) with a glow drawn behind the star in `drawBehind`: a radial gradient from `Brass500` at 45 % at the centre to transparent at 18 dp from the centre. This follows the design system's glow level G (the element's colour at 45 %) scaled down to fit a 20 dp star in a 48 dp bar; `Modifier.blur` is not used because it needs API 31. The glow is static, not pulsing, so the HUD stays calm.
- Pop: each star has a scale from `animateFloatAsState`, keyed on whether it is earned. It jumps to 1.3 when the star becomes earned and settles to 1 with `spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)`, the design system's `motion.pop`. When `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` (the same check as the splash screen), the animation spec is `snap()`.
- The pop is cosmetic and not on the game clock. Stars only change when the score changes, which cannot happen while paused, so at most a 320 ms pop finishes under the pause scrim.
- Semantics: the `Row` has `semantics(mergeDescendants = true)` with the content description from `hud_stars_description` ("%1$d of 3 stars"), so a screen reader hears one item. For tests each star also gets a test tag `hud_star_1` to `hud_star_3` and a custom `SemanticsPropertyKey<Boolean>` named `StarEarned`, which TalkBack ignores.
- `LeakOutColors` gets `Brass100`, `Brass300`, `Brass500` and `TextMuted` next to the existing Deep Flow tokens, so no hex value lives in the composable.
*Other option:* three "★" text glyphs. It was not chosen because the glyph depends on the font and cannot carry a glow.

## Risks / Trade-offs

- [`GameSession` duplicates what `GameEngine` will own] → Keep it small and keep the HUD and dialog depending only on its read-only values and the three actions. The engine change then replaces its insides and keeps its surface.
- [This change touches files from the unmerged stack change (`GameScreen`, `MainActivity`)] → Merge `app-game-play-stack` first. The tasks start by checking that the stack change is in `development`.
- [The HUD takes 48 dp from a 360 dp tall screen, so the stack slots get smaller] → `StackLayout` already scales by available height. The new unit test proves that 10 sections still fit at 312 dp.
- [A frame delta taken over the pause makes the flow jump on resume] → The frame loop restarts on resume and its first delta is 0.
- [`ON_STOP` is not called if the process is killed] → Then the level is lost anyway. Keeping a level over process death is out of scope.
- [The flow speed shows too many decimals when the ramp changes it a little every cell] → It is always rounded to one decimal, so small changes do not make the text flicker.
- [The star thresholds are defined for a won level, but the HUD shows them during play] → The HUD shows what the player would earn if they finished now, with the same `starsFor` function. A leak still earns nothing; that is for the lose overlay, which is out of scope.
- [The glow behind a 20 dp star does not fit the 48 dp bar] → The glow radius is 18 dp, so a star with its glow is 36 dp tall and fits. The stars row does not clip its content.
- [The design system's HUD bar (section 5.3) describes one small brass star next to the score, not three] → Three stars is the request and shows progress towards all three thresholds. Task 7.1 updates section 5.3 so the design system and the spec agree.

## Migration Plan

Nothing is stored or migrated. To roll back, revert the change. The game screen goes back to the stand-in without a HUD, and Exit is no longer possible from the game.

## Open Questions

- Should the flow speed include the reservoir slow-down while the liquid is inside a reservoir, or only the base speed with the ramp? This change shows whatever `flowSpeed` it is given. The engine change decides; the suggestion is base speed with the ramp, so the indicator stays steady.
- Should Restart and Exit ask for confirmation? Not for now. Both are one tap away inside a menu the player opened on purpose.
- Should the HUD show the countdown before the flow starts? The plan lists a countdown on the game screen. It fits in the empty middle of the HUD and can be added with the flow change.
- Should an earned star also get the small brass particle burst the win dialog has? Not now: the HUD is small, and the pop plus the glow already mark the moment. It can be added when the win dialog's star animation exists and can be shared.
