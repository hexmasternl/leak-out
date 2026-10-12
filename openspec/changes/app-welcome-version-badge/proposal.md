## Why

The installed version is only shown deep in Settings > About, so testers and the maintainer have to dig to see which build they are running. Showing it briefly on the welcome screen when the app starts makes the build visible at a glance without cluttering the screen during normal use.

Tracked in [#153](https://github.com/hexmasternl/leak-out/issues/153).

## What Changes

- When the app starts, the welcome screen shows the installed version in its lower-right corner, inside the safe area, as "v<versionName> (<versionCode>)".
- The version stays fully visible for 5 seconds, then fades out over 300 ms and is gone for the rest of the session.
- The label is decorative: it does not take touches and does not move or cover the Play, Settings and Exit buttons.
- With reduced motion on, the label disappears after 5 seconds without fading (per design system 7.4 it fades over 160 ms instead).
- The design system gets a short entry for the version label and its fade.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `welcome-screen`: adds the startup version label requirement.

## Impact

- `app/ui/title/WelcomeScreen.kt`: new `versionLabel` parameter and the fading label composable.
- `app/MainActivity.kt`: passes the existing `AppInfo` to the welcome screen and remembers, for the process lifetime, that the label has been shown.
- `docs/design-system.md`: version label typography, position and motion.
- Tests: Compose UI tests for the label's text, position and timing using the test clock.
