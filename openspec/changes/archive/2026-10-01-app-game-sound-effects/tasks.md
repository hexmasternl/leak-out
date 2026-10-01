## 0. Prerequisite

- [x] 0.1 Confirm change `app-settings-page` is merged into `development` (`SettingsRepository` with `soundEnabled` and `soundVolume` is in `app/`), and check whether `add-background-music` has already split `AUDIO_AVAILABLE` into `MUSIC_AVAILABLE` and `SOUND_EFFECTS_AVAILABLE`; note the result for task 6.1

## 1. Sound assets

- [x] 1.1 Copy the seven MP3s unchanged from `assets/sfx/` (repository root) to `app/src/main/assets/sfx/`
- [x] 1.2 Check that the APK keeps the MP3s uncompressed (`assets.openFd("sfx/<file>")` works); add `noCompress` only if it does not
- [x] 1.3 JVM unit test: the app's `assets/sfx/` holds the same file names with the same bytes as the root `assets/sfx/`

## 2. Catalogue and controller (pure Kotlin, `app/.../audio/`)

- [x] 2.1 Add the `GameSound` enum (`SectionPlaced`, `SectionDestroyed`, `PlacementFailed`, `LevelComplete`, `LevelFailed`) with its group names
- [x] 2.2 Add `SoundCatalog.from(fileNames)`: strip `.mp3`, strip a trailing `_NNN` (exactly three digits), map the group to a `GameSound`, ignore unknown groups and non-MP3 files
- [x] 2.3 Define the `SoundEffectPlayer` seam (`play(file, volume)`, `release()`) and a recording fake for tests
- [x] 2.4 Implement `SoundEffects`: holds the catalogue, an injectable `Random` and the current enabled flag and volume; `play(sound)` is silent when sound is off, the volume is 0 or the sound has no files, and otherwise plays a random variant at the volume; `preview(volume)` plays a `SectionPlaced` variant at the given volume when sound is on
- [x] 2.5 JVM unit tests: grouping of the bundled names (three placement variants, one of each other sound, `section_placement_failed` kept separate), unknown files ignored, missing sound silent, seeded random covers all three variants, single variant always chosen, sound off and volume 0 silent, volume passed through, settings changes apply to the next play, preview respects the enabled flag

## 3. SoundPool player and lifetime

- [x] 3.1 Implement `SoundPoolSoundEffectPlayer`: `SoundPool.Builder().setMaxStreams(4)` with `AudioAttributes(USAGE_GAME, CONTENT_TYPE_SONIFICATION)`, load every `sfx/` asset at creation through `assets.openFd`, skip (and log in debug) a play whose load has not completed, release the pool in `release()`
- [x] 3.2 Add `SoundEffectsViewModel`: builds the catalogue from `assets.list("sfx")`, owns `SoundEffects` and the player, collects `SettingsRepository.settings` into `SoundEffects` in `viewModelScope`, releases the player in `onCleared`
- [x] 3.3 Wire it in `MainActivity` with the shared settings repository (no second DataStore instance), and pass `SoundEffects` to `GameScreen`
- [x] 3.4 Instrumented test: `SoundPoolSoundEffectPlayer` loads all bundled effects and plays each without error, and `release()` is safe to call twice

## 4. Placement sounds in the game

- [x] 4.1 Add `GameSession.place(at): PlaceResult` (`Placed`, `Replaced`, `Rejected`): read whether the tile holds a `Cell.Pipe` before sending `GameCommand.Place`, and classify by that and by the command's result
- [x] 4.2 Give `GameScreen` an optional `soundEffects: SoundEffects? = null` parameter; route `FieldDropTarget.tryPlace` and the field's `onTap` through `session.place` and play `SectionPlaced`, `SectionDestroyed` or `PlacementFailed`
- [x] 4.3 Keep drops outside the level's tiles (`field.cellAt` is null) and cancelled drags silent
- [x] 4.4 JVM unit tests for `GameSession.place`: empty tile is `Placed`, replacing an unfilled section is `Replaced`, blocked/start/finish, filled section, replace not allowed and replace lock are `Rejected`, and the state is unchanged when rejected

## 5. End-of-level sounds

- [x] 5.1 In `GameScreen`, play `LevelComplete` when `showWinDialog` turns true and `LevelFailed` when `showLostDialog` turns true, once per attempt each, tracked with a `remember(session)` marker of played attempts
- [x] 5.2 Gate both on the lifecycle being at least `STARTED`, so a dialog that appears while the app is in the background plays its sound once on return

## 6. Settings

- [x] 6.1 Set `SOUND_EFFECTS_AVAILABLE = true` for the Sound effects switch and slider; if the flag is not yet split (task 0.1), split `AUDIO_AVAILABLE` into `SOUND_EFFECTS_AVAILABLE = true` and `MUSIC_AVAILABLE = false` and move the "Coming soon" note from the panel title to the Music row
- [x] 6.2 Add `onSoundEnabledChange` and `onSoundVolumeChange` to `SettingsViewModel`; the slider shows the live value while dragging and writes on release
- [x] 6.3 On slider release, while sound is on, call an `onSoundPreview(volume)` callback that `MainActivity` connects to `SoundEffects.preview`
- [x] 6.4 JVM unit tests for the new `SettingsViewModel` setters

## 7. Instrumented tests

- [x] 7.1 Game screen with a recording `SoundEffects`: drop on an empty tile plays one placed sound; tap on an empty tile plays one placed sound; replacing a section plays one destroy sound and no placed sound; drop on a blocked tile and tap on the start tile play one failed sound; letting go over the stack and a drag cancelled by pause play nothing
- [x] 7.2 Game screen: winning plays the level complete sound once when the dialog appears and not again during the tally; restart and win again plays it once more; a leak plays the level failed sound only once the lost dialog appears; sound off plays nothing
- [x] 7.3 Game screen: going to the background during the finish flash or the leak effect plays the end sound once after returning to the foreground, and not while stopped
- [x] 7.4 Settings screen: the Sound effects switch and slider are enabled and write `soundEnabled` and `soundVolume`; releasing the slider calls the preview only while sound is on; the Music controls match the music flag, with "Coming soon" on the Music row only when music is not available
- [x] 7.5 Run `:app:testDebugUnitTest` and the affected instrumented tests on the emulator, and make sure they pass (unit tests run and passing; instrumented tests compiled but not run on an emulator, at the user's request)

## 8. Docs

- [x] 8.1 Update `docs/project-plan.md`: the sound effects, their events and the `assets/sfx/` folder, and the Settings description (Sound effects no longer "Coming soon")
