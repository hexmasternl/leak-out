# Design

## Context

- `PlayMenuDialog` (`app/ui/title/PlayMenuDialog.kt`) is a Compose `Dialog` with `dismissOnBackPress = true`, `dismissOnClickOutside = true`, the platform's default width and window dim. Its content is a `Column` with a `SurfaceCard` background, a 3 dp `InkDark` border, `RoundedCornerShape(16.dp)`, 24 dp padding and max width 360 dp, holding the uppercased `Header1` title "PLAY" and two `CartoonButton`s: Primary (Hydro Blue) "Continue" and Secondary (yellow) "Select level".
- `WelcomeScreen` shows it while `showPlayMenu` is true; a choice reads `resolveNextLevel` once, closes the dialog and navigates; Back or a tap outside calls `onDismiss`.
- The Deep Flow dialog pieces already exist in the game package: `ResultDialogPanel` (level 3 shadow, `radius.lg`, `SurfaceBase`, `LineHighlight` top edge), `ResultDialogMetrics` (padding, gutter, button spacing), `ResultDialogEntry` (fade, 0.92 scale, `motion.pop` spring, reduced-motion branch), `rememberReducedMotion()`, `PanelScale` test semantics, `LeakOutColors.Scrim`, and `FlowButton` with Primary/Secondary variants and a Medium size. `ResultDialogWindow` draws its own scrim with the platform dim off, but it does not dismiss on a tap outside, and it adds the Regular/Compact layout switch, which the play menu does not need.
- The game is landscape only, down to 640 × 360 dp.

## Goals / Non-Goals

**Goals:**
- Make the play menu match design system §4.3 (level 3), §4.4 (lip), §5.1 (buttons), §5.6 (dialogs) and §7.2/§7.4 (motion), using the components the other dialogs already use.
- Keep its behaviour, tags and accessibility (pane title, Back) as they are.

**Non-Goals:**
- Restyling the welcome screen itself (its Play, Settings and Exit `CartoonButton`s, the background and the title). That is a separate change; this one covers only the dialog from issue #57.
- The Daylight theme: the app ships only the Deep theme, and the tokens will follow a future theme switch without extra work.
- A Compact layout: the panel is about 210 dp high and fits the 328 dp available on a 640 × 360 dp screen with room to spare, also at 130 % font scale (the labels use display styles, which do not scale).

## Decisions

### 1. Move the level 3 panel and entry motion to `ui/components`
Move `ResultDialogPanel` (renamed `FloatingPanel`), the shared metrics (corner radius, shadow, padding, gutter, button spacing, 360 dp max width), `ResultDialogEntry` (renamed `DialogEntry`) and `PanelScale` from `ui/game/ResultDialogLayout.kt` to `ui/components/FloatingPanel.kt`. The result and pause dialogs call the moved code; their behaviour and tests are unchanged.

*Alternative:* call the `internal` game-package functions from `ui/title`. It compiles (same module), but it makes the title screen depend on result-dialog internals that carry result-dialog names. Rejected.

*Alternative:* copy the panel into `PlayMenuDialog`. Rejected: two copies of one design-system recipe will drift apart.

### 2. Full-window dialog that draws its own scrim and dismisses on a scrim tap
`Dialog` with `DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)` and the window dim off (`DialogWindowProvider` → `setDimAmount(0f)`), as the result dialogs do. The content is a `fillMaxSize` box holding a `play_menu_scrim` box in `LeakOutColors.Scrim` whose tap calls `onDismiss`, with the panel centred over it with the 16 dp gutter. The panel consumes its own taps, so a tap between the buttons does not close it.

Keeping a `Dialog` keeps the focus trap, Back routing and TalkBack pane behaviour. With `usePlatformDefaultWidth = false` the window fills the screen and the platform has no "outside" left, so the scrim tap replaces `dismissOnClickOutside`.

### 3. Buttons: brass Primary "Continue", glass Secondary "Select Level", Medium
§5.1 allows one Primary per screen. Continue is the main action, the one most players want, so it is brass; Select Level is the glass Secondary. Medium size (48 dp face) matches the pause dialog, the other menu-style dialog, so both dialogs look like one family. The label "Select level" becomes "Select Level" because §5.1 labels are Title Case; the test tag stays `play_menu_select_level`.

### 4. Entry motion as the result dialogs
Scrim alpha 0 → 1 over 160 ms (`LinearOutSlowInEasing`), panel scale 0.92 → 1 with the `motion.pop` spring, and panel alpha with the scrim. With reduced motion (`rememberReducedMotion()`) there is no scale and the panel fades in over 160 ms. The chosen `DialogEntry` is passed into an internal `PlayMenuDialogContent`, so a test can drive it with the clock paused, as `PauseDialogContent` does.

## Risks / Trade-offs

- [Moving shared code touches the pause and result dialogs] → it is only a move and rename; their existing JVM and instrumented tests are the regression check (task 4).
- [A scrim tap must not reach the welcome screen] → the scrim lives in the dialog window, so the welcome screen never receives the tap; an instrumented test checks that a tap on the scrim closes the dialog without navigating.
