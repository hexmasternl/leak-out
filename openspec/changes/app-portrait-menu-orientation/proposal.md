## Why

Google Play now refuses to send a release for production review because the app implies a required `android.hardware.screen.landscape` feature. `MainActivity` is locked to `sensorLandscape`, and Play turns that lock into a hardware requirement that hides the app from Phone, Tablet, Desktop, Google Play Games on PC and Android XR users. Only the playing field needs landscape. The splash screen and the main menu can just as well follow the device, which also feels more natural when the player picks the phone up in portrait.

Tracked in [#179](https://github.com/hexmasternl/leak-out/issues/179).

## What Changes

- The splash screen and the welcome screen (the main menu, with its play menu, exit confirmation and update dialogs) follow the device orientation: landscape or portrait.
- In portrait, the welcome screen keeps the title at the top and stacks the menu buttons vertically, centred horizontally, at the bottom of the screen.
- The session-start ad is shown in whatever orientation the device is in when the player starts a level from the welcome screen.
- Once a level (bundled level or Daily Pipe) is on screen, the app locks to landscape (both landscape sides, following the sensor) until the player returns to the welcome screen.
- All other screens (Select level, Settings, Achievements, level designer and its test play) stay landscape-locked, as today.
- `MainActivity` no longer declares an orientation lock in the manifest. The orientation is set at runtime per screen, and the activity handles orientation changes itself, so switching orientation does not reset the current screen.
- The manifest declares `android.hardware.screen.landscape` and `android.hardware.screen.portrait` as not required, so Play no longer limits the devices the app can be installed on.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `landscape-orientation`: "Landscape only" becomes a per-screen policy: splash, welcome screen and the session-start ad follow the device; playing and every other screen are landscape-locked.
- `welcome-screen`: adds the portrait layout (buttons stacked and centred at the bottom); the background covers portrait screens too.
- `splash-screen`: the wordmark layout and the configuration-change scenario hold in portrait as well.
- `play-store-compliance`: the screen orientation features are declared as not required.

## Impact

- `app/src/main/AndroidManifest.xml`: drop `screenOrientation` on `MainActivity`, add `configChanges` for orientation, add the two `uses-feature ... required="false"` entries.
- `app/MainActivity.kt`: maps the current `Screen` to a requested orientation.
- `app/ui/title/WelcomeLayout.kt`, `WelcomeScreen.kt`: portrait layout.
- `app/ui/splash/SplashScreen.kt`: checked in portrait (the layout is already centred).
- The orientation policy (screen to orientation) is a small pure function with unit tests; the layouts get instrumented tests in portrait.
- The debug-only Pipe Lab activity keeps its own `sensorLandscape` lock; it is not shipped in release builds.
