# Design

## Context

See proposal.md for the motivation and specs/game-haptics/spec.md for the behaviour.

What exists:
- `Settings.vibrationEnabled` (default on) is stored by `DataStoreSettingsRepository` and shown by the Settings screen's Vibration switch. Nothing reads it yet.
- Sound effects show the pattern to follow: `SoundEffects` decides whether and what plays, `SoundEffectPlayer` is the seam to `SoundPool`, `RecordingSoundEffectPlayer` is the test fake, and `SoundEffectsViewModel` keeps `SoundEffects` in step with `settings.settings` for the activity's lifetime.
- Triggers in `GameSounds.kt`: `placeWithSound` plays `SectionDestroyed` on `PlaceResult.Replaced` (used by taps and by `FieldDropTarget`). `GameHud` derives `warningText` from `GapWarning.showWarningText(distance, clock, blinkSince)` on the game clock. `GameState.outcome` turns `Outcome.Lost` when the liquid leaks or misses a mandatory section. The level failed sound waits for the dialog, but the haptic must not.
- `minSdk` is 26, so `VibrationEffect.createOneShot(ms, amplitude)` is always available. `VibratorManager` exists from API 31. `Vibrator.hasAmplitudeControl()` tells whether the strength can be set.

## Goals / Non-Goals

**Goals:**
- One place (`Haptics`) that decides whether to vibrate, tested on the JVM with a fake player, like `SoundEffects`.
- Triggers that follow game state, so they fire exactly once per event and stay in step with what the player sees.

**Non-Goals:**
- A haptic tick on every placement (design system, project plan Milestone 5). #85 does not ask for it, and it would hide the replacement haptic.
- Vibration patterns, rich haptic primitives (`VibrationEffect.Composition`) or a vibration strength setting.
- A sample vibration when the Vibration switch is turned on.

## Decisions

### 1. `Haptics` + `HapticPlayer` seam, mirroring sound effects
In a new `app/.../haptics` package:
- `enum class HapticEffect(val durationMs: Long, val amplitude: Int)`: `SectionReplaced(30, 60)`, `GapWarning(50, 140)`, `LevelLost(1000, 255)`. The amplitude is on Android's 1–255 scale.
- `interface HapticPlayer { fun vibrate(durationMs: Long, amplitude: Int); fun cancel() }`.
- `class Haptics(player: HapticPlayer?)`: `apply(settings)` stores `vibrationEnabled` and calls `cancel()` when it turns off. `play(effect)` does nothing while disabled or without a player, otherwise it calls `vibrate`. `stop()` calls `cancel()`.
- `RecordingHapticPlayer` (in `main`, like `RecordingSoundEffectPlayer`) for the JVM and Compose tests.
- `HapticsViewModel(settings, player)` collects `settings.settings` into `haptics.apply` and calls `stop()` in `onCleared`.

*Alternative:* put haptics into `SoundEffects` as "feedback". Rejected: the two follow different settings, and the sound class already handles a looping player. A second small class with the same shape is easier to test and to read.

### 2. `VibratorHapticPlayer` on the system vibrator
It gets the vibrator from `VibratorManager.defaultVibrator` on API 31+ and from `Context.VIBRATOR_SERVICE` before that. If `hasVibrator()` is false, `vibrate` and `cancel` do nothing. `vibrate` calls `cancel()` first and then `vibrate(VibrationEffect.createOneShot(durationMs, amplitude))`. Without amplitude control it passes `VibrationEffect.DEFAULT_AMPLITUDE`, so a soft vibration is only short. On API 33+ it passes `VibrationAttributes.USAGE_MEDIA`, and before that `AudioAttributes.USAGE_GAME`, so the system's media/game vibration setting applies too. `SecurityException` is caught and ignored, so a missing permission cannot crash the game. `AndroidManifest.xml` declares `android.permission.VIBRATE`, a normal permission.

A new vibration replaces a running one. That is what the spec asks for when the leak follows a warning buzz, and it keeps short buzzes from piling up.

### 3. Triggers
- **Section replaced:** `placeWithSound(at, soundEffects, haptics)` gets an optional `haptics` parameter and calls `haptics?.play(SectionReplaced)` on `PlaceResult.Replaced`, next to the destroy sound. `FieldDropTarget` and the tap handler pass it on. The landing of the new section (`ReplacementLandingSound`) does not vibrate.
- **Gap warning:** `GameHud` gets `onWarningShown: () -> Unit = {}`. A `LaunchedEffect` there collects `snapshotFlow { warningText }` and calls it on every false → true change. The text, the screen reader and the haptic then share one source and cannot drift. `warningText` follows the game clock, so a paused game makes no calls. `GameScreen` and `TestPlayScreen` pass `{ haptics?.play(GapWarning) }`.
- **Level lost:** a `LostHaptic(session, haptics)` composable in a new `GameHaptics.kt` collects `snapshotFlow { session.state.attempt to (session.state.outcome is Outcome.Lost) }` and plays `LevelLost` once per attempt when the flag turns true. A pure `lostOnTheWay(before, after)` helper (like `popupStarted`) holds the rule and is JVM-tested.
- **Lifecycle:** `GameHaptics` also has a `DisposableEffect` that calls `haptics.stop()` on dispose, and an observer that calls `stop()` when the lifecycle drops below STARTED. The game already pauses in the background, so no new haptic is triggered there.

*Alternative for the gap warning:* compute the "Warning" phases a second time in `GameScreen` from `GapWarning`. Rejected: `blinkSince` lives in `GameHud`, and a second copy could vibrate out of phase with the text.

### 4. Wiring in `MainActivity`
`MainActivity` creates `HapticsViewModel` with `VibratorHapticPlayer(applicationContext)` next to `SoundEffectsViewModel`, and passes `hapticsViewModel.haptics` to `GameScreen` and `TestPlayScreen` as an optional `haptics: Haptics? = null`. Previews and existing tests keep working without it.

## Risks / Trade-offs

- [The emulator has no real vibrator, so the feel cannot be checked there] → JVM and Compose tests check what is asked of the player through the recording fake. One instrumented test checks that `VibratorHapticPlayer` does not throw with and without a vibrator. The strengths are tuned on a real device (task 4.1).
- [Some devices have no amplitude control, so "soft" and "medium" feel the same] → The durations still differ (30 ms, 50 ms, 1 s). That is acceptable.
- [A one-second vibration may feel long when the player taps Restart quickly] → The dialog only appears after the leak effect. Leaving the screen stops the vibration. A restart during the vibration lets it run out, which is at most one second.
- [The system "vibration & haptics" setting is off] → The system may then mute the vibration whatever the app asks. That is the player's choice and the spec only covers the app's own setting.

## Open Questions

- The amplitudes (60, 140, 255) are first guesses, to tune on a device.
