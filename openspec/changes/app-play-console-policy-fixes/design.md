# Design

## Context

See proposal.md for the two Play Console reports. The relevant current state:

- `androidx.fragment:fragment` is not a direct dependency. `./gradlew :app:dependencyInsight --configuration releaseRuntimeClasspath --dependency androidx.fragment:fragment` shows it comes only from `play-services-basement` 18.9.0 (wants 1.1.0) and `play-services-base` 18.5.0 (wants 1.0.0), both via `play-services-games-v2` 22.1.0. The latest `play-services-basement` (18.12.0) still declares 1.1.0, and 22.1.0 is the latest `play-services-games-v2`.
- The version catalog pins every version exactly, including other AndroidX libraries (`lifecycle` 2.9.4, `activityCompose` 1.13.0).
- `PauseDialog` is the only release code that sets a cutout mode. `ResultDialogLayout` and `PlayMenuDialog` keep the dialog default and are not reported. `MainActivity` uses `enableEdgeToEdge`, which sets the activity window's cutout mode itself (`ALWAYS` on API 30+, `SHORT_EDGES` on API 28–29) and is not reported either.
- `minSdk` is 26. Cutout modes exist from API 28, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` from API 30. On Android 15+ with targetSdk 35 or higher, `SHORT_EDGES` is treated as `ALWAYS`, so nothing changes on new devices.

## Goals / Non-Goals

**Goals:**
- Both Play Console warnings disappear for the next uploaded release.
- Regression guards so neither returns silently, for example after a Play services bump or when a new full-screen dialog copies the old code.

**Non-Goals:**
- Upgrading Play Games Services or any dependency beyond what #88 needs.
- Changing how the result dialogs or the play menu handle the cutout.
- Using fragments in the app.

## Decisions

### 1. Pin fragment 1.8.9 as a direct dependency

Add `fragment = "1.8.9"` and `androidx-fragment = { group = "androidx.fragment", name = "fragment", version.ref = "fragment" }` to `libs.versions.toml`, and `implementation(libs.androidx.fragment)` to `app/build.gradle.kts`, with a comment that it only lifts the version Play services pulls in (issue #88).

- **Why 1.8.9 and not 1.9.1, the newest stable?** 1.9.1 depends on `lifecycle` 2.10.0 and would silently override the catalog's pinned `lifecycle` 2.9.4 through conflict resolution, against the "pinned exactly" rule and widening this change. 1.8.9 is the newest release whose dependencies (lifecycle 2.6.1, activity 1.8.1, core 1.2.0) are all lower than what the app already pins, so nothing else moves. Fragment 1.9.x can come with the next lifecycle bump.
- **Alternative: a dependency constraint** (`constraints { implementation(...) }`) instead of a direct dependency. It also works and does not suggest the app uses fragments, but the project has no constraint blocks and declaring the library through the catalog matches every other dependency. The direct dependency is chosen for consistency.
- **Alternative: upgrade Play services.** Rejected: no published version fixes it.

### 2. Cutout mode per SDK level in `PauseDialog`

Replace the `SDK_INT >= P` block with:

- API 30+: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`.
- API 28–29: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`, with `@Suppress("DEPRECATION")` and a comment pointing at #89.
- Below 28: nothing, as now.

The choice goes into a small pure function, for example `fullScreenDialogCutoutMode(sdkInt: Int): Int?`, so each SDK level can be unit tested on the JVM; the `SideEffect` only applies it.

- **Why keep `SHORT_EDGES` on API 28–29?** `ALWAYS` does not exist there, and without a cutout mode a landscape window is letterboxed away from the cutout, which would break the game-play-pause requirement that the backdrop covers the cutout. This mirrors the API 28 path of AndroidX `enableEdgeToEdge`, which is in the same bundle and is not reported, so the Play check appears to accept a use gated below API 30.
- **Alternative: drop the cutout mode on API 28–29.** The fallback if the Play Console still reports the warning after upload (see Risks). It costs a strip beside the cutout on a small number of old devices.

### 3. Tests

- **JVM unit test** (`app/src/test`): `FragmentVersionTest` asserts that a fragment API added after 1.1.0 is on the classpath, such as `FragmentManager.OnBackStackChangedListener#onBackStackChangeProgressed` (added in 1.7.0). It fails if the pin is removed and fragment falls back to 1.1.0. The unit test classpath resolves this library the same way the app does.
- **JVM unit test**: `fullScreenDialogCutoutMode` returns `ALWAYS` for 30, 35 and 37, `SHORT_EDGES` for 28 and 29, and `null` for 26 and 27.
- **JVM source scan** in the style of `HardcodedTextTest`: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` appears in `src/main/kotlin` only inside the cutout helper, so a new full-screen dialog cannot copy the deprecated mode.
- **Instrumented**: `PauseDialogTest` checks that the pause dialog window's `attributes.layoutInDisplayCutoutMode` is `ALWAYS` on the emulator (API 30+). The existing `backdropCoversTheWholeWindow` keeps covering the visible behaviour. Following the project's emulator scope, only `PauseDialogTest` runs on the device.

## Risks / Trade-offs

- [The newer fragment library changes behaviour in Play Games' own UI] → Play services only uses fragments for its internal dialogs (such as resolution dialogs), and fragment 1.x stays API compatible. Check sign-in and an achievement unlock on an emulator with Play Games, or on the internal track build.
- [The Play Console still reports the API 28–29 `SHORT_EDGES` branch] → Only visible after an upload. If so, apply the fallback from decision 2 in a follow-up and note the letterboxing trade-off in the game-play-pause spec.
- [Play services later declares a fragment version above 1.8.9] → Gradle picks the higher one and the tests still pass; the pin can then be removed.

## Migration Plan

Ships with the normal release flow: merge into `development`, then `main`, which uploads to the internal track. Confirm in the Play Console that both warnings are gone for the new version code, then close #88 and #89. Rollback is reverting the commit; there is no data migration.
