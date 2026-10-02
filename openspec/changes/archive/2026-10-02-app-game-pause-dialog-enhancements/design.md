# Design

## Context

- `PauseDialog` is a Compose `Dialog` (`dismissOnBackPress = true`, `dismissOnClickOutside = false`) holding a white `SurfaceCard` column with a 3 dp `InkDark` border, an uppercased `Header1` title and three `CartoonButton`s (Primary blue, Secondary yellow, Danger red) with a `SteelLight` divider. The platform dims the game window behind it, so the board stays readable.
- `GameScreen` shows it while `GameSession.showPauseDialog` is true (`state.paused && outcome is Playing`); the engine is frozen meanwhile. The game runs in landscape, down to 360 dp high.
- `CartoonButton` implements the old slim recipe (border, 6 dp shadow, 4 dp travel). The Deep Flow button recipe (design system §4.4, §5.1) does not exist in code yet.
- `LeakOutColors` has `SurfaceAbyss`, `SurfaceDeep`, `SurfaceBase`, `SurfaceOverlay`, `LineSubtle`, `LineStrong`, `Brass*`, `TextPrimary`; it lacks `Leak500`, `Leak700`, `LineHighlight`, `TextOnBrass` and `TextOnStatus`.

## Goals / Non-Goals

**Goals:**
- Hide the frozen game completely while paused.
- Make the pause dialog match design system §4.3 (level 3), §4.4 (lip), §5.1 (buttons), §5.6 (dialogs) and §7 (motion, reduced motion).
- Introduce the Deep Flow button once, reusable by later dialog work.

**Non-Goals:**
- Restyling the level lost, level complete and reset progress dialogs, or migrating other `CartoonButton` users. They can adopt `FlowButton` in their own change.
- The Daylight theme: the app only ships the Deep theme. The backdrop uses the `surface.abyss` / `surface.deep` tokens, so it follows a future theme switch for free.
- Any change to what the buttons do.

## Decisions

### 1. Full-screen dialog window that paints its own opaque backdrop
`Dialog` with `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)` and the window's dim turned off (`DialogWindowProvider` → `window.setDimAmount(0f)`, layout `MATCH_PARENT`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`). Its content is a `fillMaxSize` box tagged `pause_backdrop` with `Brush.linearGradient(listOf(SurfaceAbyss, SurfaceDeep), start = Offset.Zero, end = Offset.Infinite)` (top left → bottom right), consuming taps so a tap on it does nothing, with the panel centred on it.

Keeping a `Dialog` keeps today's focus, Back and screen-reader behaviour (the game window is not reachable by TalkBack while the dialog is open) and every existing test that finds the dialog by tag.

*Alternative:* render the pause overlay inside `GameScreen` instead of a dialog window. Rejected: we would re-implement Back routing, focus trapping and accessibility pane semantics that the dialog gives us.

*Alternative:* a fully opaque `surface.scrim`. Rejected: the issue asks for the diagonal gradient, and §2.1 defines the scrim as 72 % (see-through) for other dialogs.

### 2. Backdrop fade-in, no fade-out
The backdrop alpha animates 0 → 1 over `motion.quick` (160 ms) when the dialog opens, with the panel's `motion.pop` entry on top. The game was visible a moment earlier, so the short fade gives no information away. On Resume/Restart/Exit the dialog closes immediately, because the game must continue at once. With reduced motion (system animator scale 0) the backdrop appears at full alpha and the panel fades in over 160 ms ease-out without scaling (§7.4).

### 3. `FlowButton` as a new component, not new `CartoonButton` variants
`ui/components/FlowButton.kt` with `FlowButtonVariant { Primary, Secondary, Destructive }` and `FlowButtonSize { Large(56), Medium(48), Small(40) }`, implementing §5.1 and §4.4: Primary = vertical gradient `brass.300 → brass.500`, label `text.onBrass`, lip `brass.700`, brass glow (`0 0 24` at 45 %); Secondary (glass) = `surface.overlay` fill, 1 dp `line.strong` border, label `text.primary`, lip `surface.abyss`; Destructive = gradient `leak.500 → leak.700`, label `text.onStatus`, lip `leak.700`. Label `display.sm` (Wendy One 18 sp, Title Case), radius `radius.md`, 24 dp horizontal padding, 4 dp lip that becomes 2 dp while pressed with the face 2 dp lower, 90 ms linear. The footprint (face + lip) stays constant, as in `CartoonButton`, so pressing never moves the layout. The press numbers live in a `FlowButtonMetrics` object so they are JVM-testable, mirroring `CartoonButtonMetrics`.

*Alternative:* add Glass/Brass/Leak variants to `CartoonButton`. Rejected: its border, shine and 6 dp shadow recipe is the v1 look the design system replaced (§12); mixing both recipes in one component would make the old look hard to retire.

### 4. Resume is the brass primary
§5.6 says "glass 'Resume' as primary emphasis", while §5.1 reserves the primary for the one main action per screen. Resume is that action, so it gets the Primary (brass) variant and Restart the glass one; Exit stays destructive. The design system text is updated to say so, together with "Exit" instead of "Quit to Levels" (Exit returns to the main menu per the `game-play-pause` spec) and the separator between Restart and Exit.

### 5. Layout fits 360 dp high
24 dp padding + 31 dp title + 12 + 3 × 52 dp buttons (48 face + 4 lip) + 2 × 12 + separator 1 + 12 + 24 ≈ 284 dp. Medium buttons are used for that reason; the panel is width-capped at 360 dp and fills less on narrow screens (16 dp gutter).

## Risks / Trade-offs

- [The dialog window does not reach behind the status/navigation bar area or a display cutout, leaving a strip of game visible] → Draw edge to edge with `decorFitsSystemWindows = false` and the short-edges cutout mode; an instrumented test checks the backdrop's bounds equal the root window's size.
- [Platform dim (`FLAG_DIM_BEHIND`) darkens the gradient so it no longer matches the tokens] → Dim amount set to 0; the backdrop is opaque by itself.
- [Compose `Brush.linearGradient` with `Offset.Infinite` follows the box's aspect ratio, so on a wide screen the "diagonal" is shallow] → Accepted: it runs corner to corner, which is what "diagonal" means here.
- [Two button components coexist for a while] → Accepted and stated as a non-goal; later dialog changes migrate to `FlowButton`.
