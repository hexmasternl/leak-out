# Tasks

## 1. Lift the fragment library (#88)

- [x] 1.1 Add `fragment = "1.8.9"` and the `androidx-fragment` library to `gradle/libs.versions.toml`, with a comment that it only overrides the 1.1.0 that Play services pulls in (issue #88), and add `implementation(libs.androidx.fragment)` to `app/build.gradle.kts`; verify `./gradlew :app:dependencyInsight --configuration releaseRuntimeClasspath --dependency androidx.fragment:fragment` resolves 1.8.9 and that no other pinned AndroidX version (lifecycle, activity, core) changes in `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` compared to before
- [x] 1.2 Add `FragmentVersionTest` in `app/src/test` asserting that a fragment API added after 1.1.0 (`FragmentManager.OnBackStackChangedListener#onBackStackChangeProgressed`) is on the classpath; verify it passes with `./gradlew :app:testDebugUnitTest --tests "*FragmentVersionTest"` and fails when the dependency from 1.1 is temporarily removed

## 2. Replace the deprecated cutout mode (#89)

- [x] 2.1 Add a pure `fullScreenDialogCutoutMode(sdkInt: Int): Int?` next to `PauseDialog` (`ALWAYS` on API 30+, `SHORT_EDGES` with `@Suppress("DEPRECATION")` on API 28–29, `null` below) and use it in `PauseDialog`'s `SideEffect`; add a JVM unit test covering SDK 26, 27, 28, 29, 30, 35 and 37 and verify it passes with `./gradlew :app:testDebugUnitTest`
- [ ] 2.2 Add a source-scan unit test in the style of `HardcodedTextTest` asserting `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` appears in `src/main/kotlin` only inside `fullScreenDialogCutoutMode`; verify it passes, and fails on a deliberately added second use
- [ ] 2.3 Extend `PauseDialogTest` with a check that the pause dialog window's `layoutInDisplayCutoutMode` is `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` on the emulator (API 30+); verify with `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=nl.hexmaster.leakout.ui.game.PauseDialogTest` that the new and existing pause tests, including `backdropCoversTheWholeWindow`, pass

## 3. Integration

- [ ] 3.1 Run `./gradlew :core:test :app:testDebugUnitTest` and `./gradlew :app:assembleRelease` and verify both succeed
- [ ] 3.2 On an emulator with Play Games, sign in and unlock an achievement on a build with the new fragment library and verify both still work, or record that this check is deferred to the internal track build
- [ ] 3.3 After the release from `main` is uploaded, verify in the Play Console that the outdated-SDK and edge-to-edge warnings are gone for the new version code, then close #88 and #89; if the `SHORT_EDGES` warning remains, open a follow-up to drop the API 28–29 branch (design.md, decision 2)
