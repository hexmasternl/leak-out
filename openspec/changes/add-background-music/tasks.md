## 0. Prerequisite

- [ ] 0.1 Confirm change `app-settings-page` is merged into `development` and archived (`openspec/specs/settings-screen` and `openspec/specs/settings-persistence` exist, and `SettingsRepository` with its shared DataStore instance is in `app/`), then merge `development` into this branch; otherwise stop

## 1. Music assets

- [x] 1.1 Rename the design-source tracks in `assets/audio/` to `bg_music_0001.mp3` … `bg_music_0010.mp3` in playback order, and `credits.json` to `audio_credits.json` with each `file` updated
- [x] 1.2 Copy the ten tracks and `audio_credits.json` unchanged from `assets/audio/` (repository root) to `app/src/main/assets/music/`
- [x] 1.3 Check that the APK keeps the MP3s uncompressed (`assets.openFd("music/<file>")` works); add `androidResources { noCompress += "mp3" }` only if it does not

## 2. Playlist and rotation logic (pure Kotlin, `app/.../audio/`)

- [x] 2.1 Add `Track` (id = file stem, file, title, artist) and `Playlist` (ordered, non-empty, unique ids)
- [x] 2.2 Add `AudioCreditsParser` for `audio_credits.json` using kotlinx.serialization; a missing, invalid or empty file, or one listing the same file twice, yields no playlist instead of throwing
- [x] 2.3 Add `MusicRotation` with `first(lastStartedId: String?): Track` (the track after the stored id, wrapping; the first track when the id is null or unknown) and `after(track: Track): Track` (wrapping)

## 3. Rotation persistence

- [ ] 3.1 Define `MusicRotationStore` (`suspend fun lastStartedTrackId(): String?`, `suspend fun setLastStartedTrackId(id: String)`)
- [ ] 3.2 Implement `DataStoreMusicRotationStore` with key `music_last_started_track` on the existing `settings` DataStore instance (never a second instance for the same file)
- [ ] 3.3 Add `InMemoryMusicRotationStore` for tests

## 4. Playback engine and controller

- [ ] 4.1 Define the `MusicEngine` seam: `play(track)` (open and prepare asynchronously, then start), `pause()`, `resume()`, `setVolume(Float)`, `release()`, with completion and error callbacks
- [ ] 4.2 Implement `MediaPlayerMusicEngine`: `assets.openFd("music/<file>")`, `AudioAttributes(USAGE_GAME, CONTENT_TYPE_MUSIC)`, `prepareAsync`, and release of the old player before a new track
- [ ] 4.3 Define the `AudioFocus` seam (request, abandon, and focus-change callbacks for gain, loss, transient loss and can-duck) and implement it with `AudioManager` and `AudioFocusRequest`
- [ ] 4.4 Implement `BackgroundMusic` (controller): pick the first track from `MusicRotation` and the store; record the id each time a new track starts (not on resume); advance and wrap on completion; on an error, skip without recording and go silent after every track has failed in one session
- [ ] 4.5 In `BackgroundMusic`, collect `SettingsRepository.settings`: `musicEnabled` off pauses and on resumes (or starts the rotation's track if none has started); `musicVolume` is applied immediately; ducking plays at 20 % of the set volume
- [ ] 4.6 Expose `onForeground()` and `onBackground()` on `BackgroundMusic`: foreground requests focus and starts or resumes; background pauses and abandons focus

## 5. App wiring

- [ ] 5.1 Add `MusicViewModel` owning `BackgroundMusic`, created with the shared DataStore, the settings repository and the asset manager; `onCleared` releases the engine
- [ ] 5.2 In `MainActivity`, obtain `MusicViewModel` and add a lifecycle observer: `onStart` → `onForeground()`, `onStop` → `onBackground()`
- [ ] 5.3 Confirm the debug Pipe Lab activity does not create or drive `BackgroundMusic`

## 6. Settings screen

- [ ] 6.1 Split `AUDIO_AVAILABLE` into `MUSIC_AVAILABLE = true` and `SOUND_EFFECTS_AVAILABLE = false`, and enable the Music switch and slider with normal semantics
- [ ] 6.2 Move the "Coming soon" note from the Audio panel title to the Sound effects row
- [ ] 6.3 Add `onMusicEnabledChange` and `onMusicVolumeChange` to `SettingsViewModel`: the switch writes immediately; the slider sends live values to the music while dragging and persists `musicVolume` when the drag ends
- [ ] 6.4 Add the music credits (the parsed playlist, or none) to the Settings UI state, and render a "Music" section in the About panel below the existing lines: one row per track in playlist order, title in `body.md` and artist in `text.muted`; leave the section out when no credits are available

## 7. Tests

- [x] 7.1 JVM test of the bundled copy against its source: `app/src/main/assets/music/audio_credits.json` has the same content as the root `assets/audio/audio_credits.json`, it lists `bg_music_0001.mp3` … `bg_music_0010.mp3` in order, and every bundled track exists with the same size as its source
- [x] 7.2 JVM test `AudioCreditsParser`: a valid file (ids are file stems), and empty, invalid JSON and duplicate-file inputs yielding no playlist
- [x] 7.3 JVM test `MusicRotation`: null id → first, id of track 4 → track 5, last → first, unknown id → first, `after` wraps
- [ ] 7.4 JVM test `BackgroundMusic` with a fake engine, fake focus, `InMemorySettingsRepository` and `InMemoryMusicRotationStore`, covering every `background-music` scenario: first launch, the rotation across simulated launches, later tracks recorded, completion and wrap, resume without advancing, music disabled at launch, toggling off and on, enabling in a silent session, volume changes, focus loss, transient loss and ducking, a failing track skipped, and every track failing
- [ ] 7.5 JVM test `SettingsViewModel`: the music switch and slider write `musicEnabled` and `musicVolume`; sound effects writes do not happen; the UI state carries the music credits
- [ ] 7.6 Instrumented test `DataStoreMusicRotationStore`: absent on a fresh file, the value survives a new store instance on the same file, and it coexists with `DataStoreSettingsRepository` on the shared instance
- [ ] 7.7 Instrumented test `MediaPlayerMusicEngine`: plays a bundled track (prepared and playing within a timeout), pauses, resumes and reports completion (by seeking near the end)
- [ ] 7.8 Instrumented test for `MainActivity`: the stored last-started id advances by one per cold launch, and stays unchanged when the activity goes through stop/start and through recreation
- [ ] 7.9 Update the settings Compose UI test: the Music controls are enabled and change the stored values, the Sound effects controls are disabled and unchanged on tap, "Coming soon" shows on the Sound effects row, the About panel lists the ten music credits in order, and the Music section is left out without credits
- [ ] 7.10 Run `:app:testDebugUnitTest` and the affected instrumented tests, and record the results

## 8. Docs

- [ ] 8.1 Update `docs/project-plan.md`: §2.10 adds the `music_last_started_track` key, §1.11 notes background music on all screens and the music credits on the About panel, and Milestone 5 ticks off background music
