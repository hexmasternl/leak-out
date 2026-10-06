# Tasks

## 1. Bundle the drop sound

- [x] 1.1 Copy `assets/sfx/drop.mp3` unchanged to `app/src/main/assets/sfx/` and add `GameSound.PointsAwarded("drop")`; update the catalogue tests ("Variants are grouped": points awarded has one variant; bundled copies match) and verify `./gradlew :app:testDebugUnitTest --tests '*Sound*'` passes

## 2. Play it with each popup

- [ ] 2.1 Write a JVM unit test for `DropThrottle` first (first drop plays, a drop 99 ms later is skipped, one 100 ms later plays, the injected clock is used), then implement it (design decision 2); verify `./gradlew :app:testDebugUnitTest --tests '*DropThrottle*'` passes
- [ ] 2.2 Add `PointsAwardedSound(session, soundEffects)` to `GameSounds.kt` (design decision 1) and call it from `GameScreen` and `TestPlayScreen`; verify with an instrumented test using `RecordingSoundEffectPlayer`: one drop per award, one drop for two awards in one frame, a cross scoring twice drops twice, no drop on restart or while paused, and no drop with sound off; run only that test class on the emulator

## 3. Integration

- [ ] 3.1 Play a level on the emulator at normal speed and in fast forward and check by ear that the drop starts with each popup and does not buzz in fast forward; adjust `DROP_MIN_INTERVAL_MS` if needed and note the result here
- [ ] 3.2 Run `./gradlew :core:test :app:testDebugUnitTest` and verify everything passes
