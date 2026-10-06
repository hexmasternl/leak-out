# Tasks

## 1. Haptics decision and player

- [x] 1.1 Write `HapticsTest` first (design decision 1) with the recording fake: no vibration before `apply` with vibration off, each `HapticEffect` passes its duration and amplitude, nothing while `vibrationEnabled` is off, turning the setting off cancels a running vibration, turning it back on applies to the next effect, `stop()` cancels, and a null player is silent; verify it fails to compile because `Haptics` doesn't exist yet
- [x] 1.2 Add `HapticEffect`, `HapticPlayer`, `RecordingHapticPlayer` and `Haptics` in `app/.../haptics`; verify `./gradlew :app:testDebugUnitTest --tests '*Haptics*'` passes
- [x] 1.3 Add `HapticsViewModel` (collects the settings into `haptics.apply`, stops on clear) with a JVM test using a fake `SettingsRepository`; verify `./gradlew :app:testDebugUnitTest --tests '*HapticsViewModel*'` passes
- [x] 1.4 Add `VibratorHapticPlayer` (design decision 2: `VibratorManager` on API 31+, no-op without a vibrator, amplitude only with amplitude control, `SecurityException` ignored) and the `android.permission.VIBRATE` permission in `AndroidManifest.xml`; verify with an instrumented `VibratorHapticPlayerTest` that `vibrate` and `cancel` do not throw on the emulator, running only that test class

## 2. Triggers in the game

- [ ] 2.1 Give `placeWithSound` an optional `haptics` and play `SectionReplaced` on `PlaceResult.Replaced` only; pass it on from the tap handler and `FieldDropTarget`; extend the JVM tests of `GameSounds.kt` (or add `GameHapticsTest`) for: replace vibrates once, empty-tile placement and refused placement do not, a zero-penalty replacement vibrates once; verify `./gradlew :app:testDebugUnitTest --tests '*GameSounds*' --tests '*GameHaptics*'` passes
- [ ] 2.2 Add `onWarningShown` to `GameHud` (design decision 3), called on every false → true change of `warningText`; verify with a Compose test on a small level where the liquid approaches an empty tile that the callback fires once per "WARNING" phase, stops after building the gap away, and does not fire while paused, running only the affected test class
- [ ] 2.3 Add `GameHaptics.kt` with the pure `lostOnTheWay(before, after)` helper and the `LostHaptic` composable, plus the lifecycle and dispose `stop()`; JVM-test the helper (lost once per attempt, restart and lose again, won does not count, a new attempt resets); verify `./gradlew :app:testDebugUnitTest --tests '*GameHaptics*'` passes
- [ ] 2.4 Give `GameScreen` and `TestPlayScreen` an optional `haptics: Haptics?`, wire the three triggers, and create `HapticsViewModel` with `VibratorHapticPlayer` in `MainActivity`; verify with a `GameScreen` instrumented test using the recording fake (replace → one soft vibration, the gap warning vibrates with the text, the leak → one 1000 ms vibration, leaving the screen cancels, vibration off → nothing), running only the affected test class

## 3. Specs and docs

- [ ] 3.1 Update `docs/project-plan.md` where it describes the Settings screen or Milestone 5, so it says the Vibration switch now drives the game's haptics (the "Haptic tick on placement" item stays open); verify `openspec validate app-game-haptic-feedback --strict` passes

## 4. Integration

- [ ] 4.1 Play a level on a real device with vibration on and check by feel: a soft buzz on a replacement, a short buzz with each "WARNING", a one-second buzz on the leak, nothing with the Vibration switch off; tune the amplitudes in `HapticEffect` if needed and note the result here
- [ ] 4.2 Run `./gradlew :core:test :app:testDebugUnitTest` and verify everything passes
