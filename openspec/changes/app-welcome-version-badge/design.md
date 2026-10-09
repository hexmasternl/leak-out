## Context

`MainActivity` already reads the installed version once into `AppInfo` (version name and code) for the Settings About panel. The welcome screen (`ui/title/WelcomeScreen.kt`) is the first screen after the splash and is shown again whenever the player comes back from Settings, the game or the level designer.

## Goals / Non-Goals

**Goals:**
- Show the version on the welcome screen for 5 s after app start, then fade it out over 300 ms.
- Show it once per process, not on every return to the welcome screen.

**Non-Goals:**
- Changing the Settings About panel.
- A permanent version label or a tap-to-show gesture.

## Decisions

- **Format "v<versionName> (<versionCode>)"**: matches the About panel's "Version <name> (<code>)" in a compact form that fits a corner. Not localized beyond the digits, like the About panel.
- **Once per process**: a `versionLabelShown` flag lives in `MainActivity` (or its host view model) and is set as soon as the label starts showing, so leaving before the fade completes also counts. Surviving configuration changes is enough; a process restart shows it again, which matches "when the app starts".
- **Timing in Compose**: a `LaunchedEffect` waits 5000 ms then animates alpha with `tween(300)` (`tween(LeakOutMotion.quick)` with reduced motion); the label is removed from composition once alpha reaches 0. Tests drive it with `mainClock`.
- **Placement**: a `Box` overlay aligned `BottomEnd` with safe-area insets and the standard screen margin, label style from the design system's small/caption type in a muted text colour, so it does not compete with the title or buttons. It has no click modifier, so touches fall through.
- **Stateless screen**: `WelcomeScreen` takes `versionLabel: String?` and `onVersionLabelShown: () -> Unit`; `null` means do not show. This keeps the screen previewable and testable.

## Risks / Trade-offs

- [Label overlaps the level designer button or buttons on small screens] → it sits bottom-right while the designer button is top-right and the menu is centred; the pseudo-locale layout test covers small widths.
- [Timing flakiness in UI tests] → use the Compose test clock with auto-advance off rather than real time.

## Open Questions

- Should the label show only on a cold start (current decision) or every time the welcome screen appears?
- Is "v1.4.0 (57)" the wanted format, or only the version name ("v1.4.0")?
- Should the 5 s countdown start when the welcome screen appears (current decision) or at process start, which includes the splash?
