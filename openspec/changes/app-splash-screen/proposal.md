## Why

Android 12+ always shows a system splash screen at cold and warm starts. Out of the box it shows the launcher icon on the theme's window background. For Leak Out that looks generic and does not match the Deep Flow design system. The first thing a player sees should be the game's own wordmark: glowing liquid on a dark workshop background. It should also hand off to the welcome screen without a flash of a different colour.

## What Changes

- Style the system splash screen (via `androidx.core:core-splashscreen`, so it looks the same on API 26–30 and 31+). Its background becomes `surface.deep` (`#0C1A2A`) and it shows no visible icon, so it reads as a plain dark screen.
- Add an in-app splash screen (Compose) that shows right after the system splash, on the same `surface.deep` background:
  - The app name as a two-line wordmark, **LEAK** on the first line and **OUT** on the second, centred horizontally and vertically.
  - Set in Wendy One (`display.hero` scale or larger), filled with the liquid gradient (`liquid.glow → liquid.300 → liquid.500 → liquid.700`, top to bottom), with a soft `liquid.500` outer glow. This matches the logo in `docs/design-system.html`.
- The splash screen stays up for a short minimum time, then cross-fades into the welcome screen. It respects reduced motion (no fade, static glow).
- The splash always uses the Deep palette, even when the Daylight theme is active.
- The status and navigation bars match `surface.deep` while the splash is visible (edge-to-edge, no light bars).

## Capabilities

### New Capabilities
- `splash-screen`: What the app shows from launch until the welcome screen. Covers the system splash's background and icon, the in-app wordmark screen (layout, typography, gradient and glow), how long it shows, the transition into the welcome screen, and reduced-motion behaviour.

### Modified Capabilities
<!-- None: there are no archived specs in openspec/specs/. The welcome-screen capability (change app-welcome-screen) keeps its requirements; it just becomes the destination after the splash. -->

## Impact

- **Depends on** `app-welcome-screen`, which scaffolds the `app` module, `MainActivity` and `LeakOutTheme`. This change builds on that scaffolding.
- `gradle/libs.versions.toml` and `app/build.gradle.kts`: add `androidx.core:core-splashscreen`.
- `app/src/main/res/values/themes.xml`: add a `Theme.LeakOut.Splash` (parent `Theme.SplashScreen`) with `surface.deep` background, a transparent icon and `postSplashScreenTheme`. `AndroidManifest.xml`: `MainActivity` uses the splash theme.
- `app/src/main/res/values/colors.xml`: expose `surface_deep` for XML themes.
- `MainActivity`: call `installSplashScreen()` before `super.onCreate`, and show the in-app splash before the welcome screen.
- New code: `ui/splash/SplashScreen.kt` (the wordmark screen) and `ui/splash/LiquidWordmark.kt` (reusable gradient + glow text, which the welcome title can also use later).
- Assets: Wendy One (`res/font/wendy_one_regular.ttf`, OFL) if not already bundled.
- Docs: in `docs/project-plan.md`, the "App icon, splash…" checklist item is partly covered.
