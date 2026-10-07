# Proposal

## Why

The Play Console flags two problems in the current release (version code 1008), tracked as GitHub issues #88 and #89. Neither blocks the internal track today, but Google asks for both to be fixed before publishing changes, and #89 concerns the Android 15 edge-to-edge rules that apply because the app targets SDK 37.

- **#88: outdated SDK `androidx.fragment:fragment` 1.1.0.** The app does not use fragments itself. Play Games Services v2 (`play-services-games-v2` 22.1.0) pulls in `play-services-basement` and `play-services-base`, which declare `androidx.fragment:fragment` 1.1.0 and 1.0.0; Gradle resolves 1.1.0. Even the newest `play-services-basement` (18.12.0) still declares 1.1.0, so upgrading Play services alone does not fix it. Google asks for 1.2.1 or newer.
- **#89: deprecated edge-to-edge API.** `PauseDialog` sets `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` on its dialog window (reported at `PauseDialogKt.PauseDialog$lambda$0$0$0`) so the opaque pause backdrop also covers the display cutout. That mode is deprecated as of Android 15, where it already behaves as `ALWAYS`; the fix is to ask for the non-deprecated mode directly.

## What Changes

- Pin `androidx.fragment:fragment` in the version catalog and add it as a direct `app` dependency, so Gradle resolves a current fragment library instead of the 1.1.0 that Play services asks for.
- Have the pause dialog window request `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` on Android 11 (API 30) and newer. `SHORT_EDGES` is kept only on Android 9–10 (API 28–29), where `ALWAYS` does not exist, behind an explicit SDK gate, the same way AndroidX `enableEdgeToEdge` does it.
- Guard both fixes with tests: a unit test that fails if fragment falls back to an outdated version, a source scan that fails if release code uses `SHORT_EDGES` outside the API 28–29 gate, and an instrumented check of the pause dialog's cutout mode.
- No visible behaviour changes: the pause backdrop keeps covering the cutout and the system bar areas.

## Capabilities

### New Capabilities
- `play-store-compliance`: release builds avoid the SDK versions and platform APIs that the Play Console reports as outdated or deprecated.

### Modified Capabilities
<!-- None: game-play-pause already requires the backdrop to cover the cutout, and that requirement is unchanged. -->

## Impact

- `src/gradle/libs.versions.toml`: new `fragment` version and `androidx-fragment` library.
- `src/app/build.gradle.kts`: `implementation(libs.androidx.fragment)`.
- `src/app/src/main/kotlin/nl/hexmaster/leakout/ui/game/PauseDialog.kt`: cutout mode chosen per SDK level.
- Tests in `src/app/src/test` (JVM) and `src/app/src/androidTest/.../ui/game/PauseDialogTest.kt`.
- No data, settings or level format changes. Play Games sign-in and achievements must keep working with the newer fragment library.
- Closes #88 and #89 once a release built from this change is uploaded and the Play Console no longer lists the warnings.
