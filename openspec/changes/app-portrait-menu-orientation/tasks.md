## 1. Manifest

- [x] 1.1 Remove `android:screenOrientation` from `MainActivity` in `app/src/main/AndroidManifest.xml` and add `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"`, with a comment explaining why.
- [x] 1.2 Declare `android.hardware.screen.landscape` and `android.hardware.screen.portrait` with `android:required="false"`.
- [x] 1.3 Add a check (unit test on the merged release manifest, or a Gradle verification task run by `testReleaseUnitTest`/CI) that fails when a release activity sets `screenOrientation` or a screen feature is required.

## 2. Orientation policy

- [x] 2.1 Add a pure `orientationFor(screen)` mapping in `app` (Welcome to follow device with `SCREEN_ORIENTATION_FULL_USER`; every other screen to `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`).
- [x] 2.2 Unit test the mapping for every `Screen`.
- [x] 2.3 Apply it in `MainActivity` whenever the screen changes, so the splash and welcome screen follow the device and every other screen is landscape-locked.
- [x] 2.4 Make sure the screen only switches to `Game`/`DailyPipe` after the session-start ad has closed or been skipped, so the ad shows in the current orientation.

## 3. Portrait welcome screen

- [ ] 3.1 Give `WelcomeLayout` a portrait arrangement: title at the top, buttons in one column centred horizontally at the bottom, above the navigation bar inset.
- [ ] 3.2 Check the background, version badge, play menu dialog, exit confirmation and update dialogs in portrait.
- [ ] 3.3 Check the splash wordmark in portrait and that rotating during the splash neither replays nor restarts it.

## 4. Tests

- [ ] 4.1 Instrumented test: welcome screen in portrait shows the buttons stacked, centred and at the bottom, also at 130 % font scale on 360×640 dp.
- [ ] 4.2 Instrumented test: rotating on the welcome screen keeps the screen and any open dialog; the splash is not replayed.
- [ ] 4.3 Instrumented test: starting a level from the welcome screen in portrait (fake ad provider) ends in landscape with the level intact; returning to the welcome screen unlocks orientation again.
- [ ] 4.4 Run `:core:test`, `:app:testDebugUnitTest` and the affected instrumented tests.

## 5. Release

- [ ] 5.1 Update the `landscape-orientation` spec purpose when archiving, and bump `versionName`.
- [ ] 5.2 After the release reaches the Play Console, confirm production review is no longer blocked by the hardware feature check.
