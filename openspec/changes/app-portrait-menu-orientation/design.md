## Context

`MainActivity` is the only activity in release builds. It shows every screen through a `Screen` sealed interface in Compose and is locked to `sensorLandscape` in the manifest (`landscape-orientation` spec). Play infers a required `android.hardware.screen.landscape` feature from that lock and now blocks production review because of it (#179).

The session-start ad (change `app-admob-session-start-ads`, implemented but not archived yet) is a Google Mobile Ads interstitial. It is shown in the SDK's own `AdActivity` by `SessionStartAds.beforeMenuStart`, before `Screen.Game` or `Screen.DailyPipe` is set.

`ScreenSaver` keeps only the designer across activity recreation; every other screen restarts on the welcome screen. An orientation switch that recreates the activity would therefore throw the player out of a level.

## Goals / Non-Goals

**Goals:**

- Splash, welcome screen and the session-start ad follow the device orientation.
- A level is always played in landscape (both sides, sensor-driven).
- Play no longer reports a required screen-orientation hardware feature.
- Switching orientation never resets the current screen or a level in progress.

**Non-Goals:**

- Portrait layouts for Select level, Settings, Achievements, the level designer or the playing field.
- Changing the ad itself (format, frequency, consent).

## Decisions

1. **Orientation is set at runtime from the current screen.** A pure function `orientationFor(screen)` returns "follow device" (`SCREEN_ORIENTATION_FULL_USER`, which respects the user's rotation lock) for the welcome screen and "landscape" (`SCREEN_ORIENTATION_SENSOR_LANDSCAPE`) for every other screen. `MainActivity` applies it to `requestedOrientation` whenever the screen changes. The splash is shown while the screen is Welcome, so it follows the device too. Alternative: `SCREEN_ORIENTATION_SENSOR`, rejected because it ignores the user's rotation lock.
2. **The ad keeps the orientation the player is in.** `beforeMenuStart` shows the ad while the menu screen is still current, so the welcome screen's "follow device" applies. The screen, and with it the landscape lock, changes only in the ad's completion callback. When the ad is started from Select level, that screen is landscape-locked, so the ad shows in landscape there. The SDK declares `AdActivity` without an orientation lock, so it follows the device.
3. **`MainActivity` handles orientation changes itself.** Add `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"` so a rotation recomposes instead of recreating the activity. This keeps the current screen, the running game and the splash state across the switch to landscape. Alternative: save every screen in `ScreenSaver`, rejected because a level and the running game state are not cheap to save and restore.
4. **Portrait welcome layout.** `WelcomeLayout` picks its arrangement from the available size (height greater than width means portrait). Portrait: background full-screen, title at the top, buttons in a single column of equal width, centred horizontally and anchored to the bottom with the design system's screen padding plus the navigation bar inset. Landscape stays unchanged.
5. **Features declared optional.** The manifest adds `<uses-feature android:name="android.hardware.screen.landscape" android:required="false" />` and the same for `android.hardware.screen.portrait`. With no manifest lock left nothing implies a required feature any more; the explicit entries guard against a lock being added back later.

## Risks / Trade-offs

- Handling `configChanges` means resources read once at creation are not refreshed automatically. The app is pure Compose, which recomposes on a configuration change; instrumented tests rotate the device on every affected screen.
- A player holding the phone in portrait has to turn it when a level opens. This is the requested behaviour; the lock applies as soon as the level appears.
- On devices without a landscape screen (some PC or XR setups) the game is letterboxed. Before this change they could not install the app at all.

## Open Questions

- Should Select level, Settings and Achievements also follow the device later? This change keeps them landscape-locked.
- Should the welcome screen dialogs (play menu, exit confirmation) get their own portrait layout, or is their current centred layout enough? This change only checks that they fit in portrait.
