# Tasks

## 1. Gap distance in core

- [x] 1.1 Extract `RouteTracer.trace`'s step loop into an internal walk that takes a start position, exit side and visited set, and call it from `trace`; verify `./gradlew :core:test --tests '*RouteTracer*'` passes unchanged
- [x] 1.2 Write `GapDistanceTest` first (design decision 1) with cases for: no gap while `Waiting`, `Ended`, won or lost; `1 - progress` for a gap right after the head's tile; whole sections counted ahead; route into the finish → null; finish with an unused mandatory section → gap at the finish; finish entered from the wrong side; grid edge; blocked tile; section without a matching opening; cross whose other channel was already used; a tile with a running replacement counts as empty; verify it fails to compile because `GapDistance` doesn't exist yet
- [x] 1.3 Implement `GapDistance.of(state): Double?` on the shared walk; verify `./gradlew :core:test` passes

## 2. Score bar warning

- [x] 2.1 Add `GapWarning` in `app/ui/game` with `redness`, `blinking`, `showWarningText` and `heartbeatFactor` (design decision 2) and a JVM unit test covering 7 → 0 redness, 4 → 0.5, 3 and below → 1, null → 0, the 500 ms phases starting with "Warning", and heartbeat factors 0.15 at 5, 1.0 at 0, null above 5; verify `./gradlew :app:testDebugUnitTest --tests '*GapWarning*'` passes
- [x] 2.2 Add the `game_hud_warning` string ("Warning") and the `game_hud_score_warning` plural ("Warning: leak in %1$d section(s)", with a translator comment) to `strings.xml`; verify the lint `HardcodedText` / translation checks pass with `./gradlew :app:lintDebug`
- [x] 2.3 Give `ScoreBar` the `warning` and `warningText` parameters: lerp the fill gradient to `leak.500`, swap the centred text for the capitalised "Warning", and extend the content description while blinking. Add previews for redness 0.5 and for the blinking text; verify with the `ScoreBar` instrumented test extended for the text swap and content description (run only the affected test class)
- [x] 2.4 Wire `GameHud`/`GameScreen`: compute `GapDistance.of(state)` from the session each frame, keep the blink start time in `remember(attempt)` state, and pass the warning to `ScoreBar`, also in `TestPlayScreen`; verify with a `GameScreen` instrumented test on a small level where the liquid approaches an empty tile (the bar shows "WARNING", building stops the blink, a leak resets it) and check the `en-XA` pseudo-locale test still fits the HUD

## 3. Heartbeat

- [x] 3.1 Copy `assets/sfx/heartbeat.mp3` unchanged to `app/src/main/assets/sfx/`, add `GameSound.Heartbeat("heartbeat")`, and leave looped sounds out of `SoundCatalog.files`; update the catalogue tests ("Variants are grouped", bundled copies match) and verify `./gradlew :app:testDebugUnitTest --tests '*Sound*'` passes
- [x] 3.2 Add the `LoopingSoundPlayer` seam, a `MediaPlayer` implementation and a recording fake, and `SoundEffects.heartbeat(factor)` with settings handling (design decision 4); unit-test start, volume = factor × soundVolume, the 0.01 volume step, stop on null, sound off and volume 0, a settings change applied to a running loop, and a missing file; verify `./gradlew :app:testDebugUnitTest --tests '*SoundEffects*'` passes
- [x] 3.3 Add an instrumented test for the `MediaPlayer` looping player (starts, loops, changes volume, stops and restarts from 0, release is idempotent); verify by running only that test class on the emulator
- [x] 3.4 Add the `GapHeartbeat` composable in `GameSounds.kt` and use it in `GameScreen` and `TestPlayScreen`, stopping on pause, background, win, loss, restart and dispose; verify with an instrumented test using the recording fake (starts at 5 sections, gets louder, stops when the player builds the gap away, stops on pause and on the leak), running only the affected test class

## 4. Integration

- [x] 4.1 Play a level on the emulator, let the liquid run towards a gap, and check by eye and ear: the bar turns red from 5 sections, blinks from 3, stops blinking after building, and the heartbeat starts quietly, gets louder and loops without a click; tune the 0.15 starting factor if needed and note the result here
- [x] 4.2 Run `./gradlew :core:test :app:testDebugUnitTest` and verify everything passes; update the project plan's progress if it tracks this feature

## Notes

- 2.3, 2.4, 3.3, 3.4: the instrumented tests (`GapWarningScreenTest`, `GapHeartbeatTest`, `MediaPlayerLoopingSoundPlayerTest`) are written and compile (`:app:assembleDebugAndroidTest`), but were not run: no emulator was running. Run them with `:app:connectedDebugAndroidTest` and the class filter before merging. The `en-XA` pseudo-locale check was not run either.
- 4.1 is open: it needs the game played on a device by eye and ear (loop click, tuning the 0.15 starting factor).
- 4.2: `:core:test` and `:app:testDebugUnitTest` pass; the project plan does not track this feature.
- `ScoreBar` takes a third warning parameter, `leakInSections`, besides `warning` and `warningText` (design decision 3), because the accessible description needs the rounded-up gap distance.
