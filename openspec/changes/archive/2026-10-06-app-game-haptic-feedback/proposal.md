# Proposal

## Why

The Settings screen has a Vibration switch, but nothing in the game vibrates. The switch only stores the preference. Haptic feedback makes the important moments easier to feel, especially the gap warning and the leak, which the player may otherwise miss while they look at another part of the field. Tracked in [#85](https://github.com/hexmasternl/leak-out/issues/85).

## What Changes

- **Section replaced:** a short, soft vibration (about 30 ms, low strength) when the player drops a section on a placed one and the old section is destroyed, at the same moment as the section destroyed sound.
- **Gap warning:** a brief vibration (about 50 ms, medium strength) every time the blinking score bar of the gap warning (#80, `game-play-gap-warning`) switches to the word "Warning", so once per second while the gap is 3 sections away or less. It stops when the bar stops blinking, and it does not vibrate while the game is paused, because the bar does not blink then either.
- **Level lost:** a vibration of one second (full strength) at the moment the liquid leaks and the attempt is lost, whatever the loss reason. It does not wait for the level lost dialog.
- All haptics follow the stored `vibrationEnabled` setting. Turning it off stops a running vibration at once. A device without a vibrator stays silent and the game works as usual. Haptics stop when the app leaves the foreground.
- The Vibration switch's requirement loses the note "this change makes nothing vibrate".
- The app declares the `android.permission.VIBRATE` permission. It is a normal permission, granted at install, so there is no prompt.

## Capabilities

### New Capabilities
- `game-haptics`: when and how the game vibrates (section replaced, gap warning, level lost), and how vibration follows the setting, the device and the app lifecycle.

### Modified Capabilities
- `settings-screen`: the *Vibration switch* requirement no longer says that nothing vibrates. The stored setting now turns the game's haptics on and off.

## Impact

- `app`: a new `haptics` package (a `HapticPlayer` seam, a `Vibrator` implementation, a recording fake, the `Haptics` decision class and a `HapticsViewModel` kept in step with the settings), `MainActivity.kt` (creating the view model and passing `haptics` on), `GameScreen.kt` and `TestPlayScreen.kt` (the triggers), `GameSounds.kt` (`placeWithSound` also triggers the replacement haptic), `GameHud.kt` (reports each time the "Warning" text shows), and `AndroidManifest.xml` (the `VIBRATE` permission).
- `core`: no changes. The triggers come from game state that already exists (`PlaceResult.Replaced`, `Outcome.Lost`, the gap distance).
- Depends on `app-game-play-gap-warning` (#80), whose code is on `development` already. If that change is not archived yet when this one is, the haptics spec still refers to `game-play-gap-warning` by name.
- `docs/project-plan.md`: the Milestone 5 item "Haptic tick on placement" is not part of this change (#85 does not ask for it) and stays open.
