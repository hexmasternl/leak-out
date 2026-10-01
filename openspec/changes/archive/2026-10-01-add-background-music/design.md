## Context

`assets/audio/` at the repository root holds ten MP3 tracks, `bg_music_0001.mp3` to `bg_music_0010.mp3`, and `audio_credits.json`. The MP3s have been stripped of ID3v2, ID3v1 and cover art, so each file is a bare MPEG frame stream. `audio_credits.json` lists the tracks in playback order, the order of the file numbers, with `title`, `artist` and `file`. As with the welcome and icon artwork, the root `assets/` folder is the design source. The app ships its own copy under `app/src/main/assets/`, next to `levels/` and `pipes/`.

The plan (`docs/project-plan.md` §2.1) picks `MediaPlayer` for music. Change `app-settings-page` introduces `SettingsRepository` on DataStore Preferences (file `settings`), with `musicEnabled` (default on) and `musicVolume` (default 0.6). It also adds an Audio panel whose controls are disabled behind an `AUDIO_AVAILABLE` flag. This change builds on that one and is implemented after it.

`MainActivity` is the only game activity. It switches screens with a `Screen` sealed interface inside one `setContent`, so one player can span all screens. The debug-only Pipe Lab is a separate activity and stays silent.

## Goals / Non-Goals

**Goals:**
- One continuous background stream across the welcome, library, settings and game screens.
- A fixed playlist order with wrap-around, where each cold start begins with the track after the one that last started.
- Pause in the background and resume in place; well-behaved audio focus.
- A working Music switch and volume slider that take effect immediately.
- The musicians credited on the About panel, from the same file that drives the playlist.
- Playlist logic testable on the JVM without `MediaPlayer`.

**Non-Goals:**
- Sound effects (place, leak, win). Those came with change `app-game-sound-effects`, which already enabled their controls.
- A separate credits screen, or license texts per track. The About panel lists title and artist only.
- Shuffle, skip or track selection by the player.
- Cross-fading between tracks, or music that changes with game state (for example tension near a leak).
- Playback while the app is in the background, or a media notification.

## Decisions

**One file is both playlist and credits: `assets/music/audio_credits.json`.** The source names are already asset-safe (`bg_music_NNNN.mp3`), so the ten tracks and `audio_credits.json` are copied unchanged to `app/src/main/assets/music/`. A track's id is its file stem (`bg_music_0001`), so the JSON needs no separate id field. Playlist order is array order. A JVM unit test fails if the app copy of `audio_credits.json` differs from the root source, if an entry points at a missing asset, or if a bundled track's size differs from its source. That keeps the two copies from drifting without build logic that reaches outside `src/`, which `CLAUDE.md` forbids. *Alternatives:* `res/raw` (rejected: no room for the credits metadata, and assets are already how levels ship), and adding `../assets/audio` as an asset source directory (rejected: it puts a non-`src/` path into the build).

**The id is the rotation key, not the index.** The store holds the id of the last track that started. The next track is `playlist[(indexOf(id) + 1) % size]`. If the id is missing or unknown, playback starts at index 0. Storing the id means reordering or inserting tracks later never points at the wrong song, and a removed track degrades to "start at the top". This is a pure function in `MusicRotation` (`first(lastStartedId): Track` and `after(track): Track`), unit tested with no Android.

**When "started" is recorded.** The id is written each time a new track begins (`MediaPlayer.start()` on a freshly prepared track), including later tracks in the same session. It is not written on resume after a pause. So when the player hears tracks 3 and 4 and quits during track 4, the next launch plays track 5. If music is disabled for the whole session, nothing starts and the stored id does not move.

**Storage: a separate `MusicRotationStore` on the same DataStore.** The last-started id is app state, not a user setting, so it gets its own small interface (`suspend fun lastStartedTrackId(): String?`, `suspend fun setLastStartedTrackId(id: String)`). It stays out of the `Settings` data class, which is the screen's model, and the Reset progress button never touches it. The DataStore implementation uses key `music_last_started_track` in the existing `settings` DataStore instance. Android allows only one DataStore per file per process, so the instance is shared and not created twice. An in-memory implementation serves tests.

**Player seam: `MusicEngine` interface around `MediaPlayer`.** `MediaPlayerMusicEngine` opens a track with `assets.openFd("music/<file>")`. MP3 is in AAPT's no-compress list, so the descriptor works. It prepares asynchronously and uses `AudioAttributes(USAGE_GAME, CONTENT_TYPE_MUSIC)`, reporting completion and errors through callbacks. `BackgroundMusic`, the controller, owns all the decisions: which track, when to start, pause, resume and skip, and the volume. It talks only to `MusicEngine`, `MusicRotationStore`, the settings flow and an `AudioFocus` seam. On the JVM it is tested with fakes, and the real engine gets a short instrumented test.

**Lifetime: owned by a ViewModel on `MainActivity`, driven by the activity lifecycle.** A `MusicViewModel` holds `BackgroundMusic`, so a configuration change (theme, locale, a landscape flip that recreates the activity) neither restarts the track nor advances the rotation. `onCleared` releases the `MediaPlayer`. `MainActivity` registers a lifecycle observer: `onStart` → `resume()` (or the first start), `onStop` → `pause()`. *Alternatives:* an `Application`-scoped singleton with `ProcessLifecycleOwner` (rejected: it needs `lifecycle-process` and would also play over the debug Pipe Lab), and a composable `DisposableEffect` (rejected: it would restart on recreation).

**Reacting to settings.** `BackgroundMusic` collects `SettingsRepository.settings`. `musicEnabled` going off pauses the current track and keeps its position. Going back on resumes it, or starts the rotation's first track if none has started yet. Volume changes call `setVolume(v, v)` straight away with the slider value used linearly. Ducking multiplies it by 0.2.

**Audio focus.** The app requests `AUDIOFOCUS_GAIN` with `AudioFocusRequest` (API 26+, matching minSdk) before starting. On permanent loss it pauses and gives up the focus. On transient loss it pauses and resumes when focus returns. On "can duck" it lowers to 20 % of the set volume. If the request is denied, the track does not start until the next `onStart`.

**Failure handling.** If a track fails to open or prepare, or the player reports an error, the controller logs it and moves to the next track without recording the failed one. After every track has failed in one session the controller stops trying, and the game keeps running silently. An empty or unreadable manifest also means silence, never a crash.

**Settings UI.** Change `app-game-sound-effects` already split the audio flag into `SOUND_EFFECTS_AVAILABLE = true` and `MUSIC_AVAILABLE = false`, with "Coming soon" on the Music row. This change sets `MUSIC_AVAILABLE = true`, so the note is gone from the panel. `SettingsViewModel` gains `onMusicEnabledChange` and `onMusicVolumeChange`. Slider writes are persisted when the drag ends, and the live value goes to the player during the drag so the player hears the change.

**About panel music credits.** The About panel gains a "Music" section below the existing lines. It shows one row per track, in playlist order, with the title in `body.md` and the artist in `text.muted` ("Ebb and Flow · Fabian Measures"). The list comes from the same parsed `audio_credits.json` the player uses, through a small `MusicCredits` source passed to `SettingsViewModel`. It is not hard-coded in strings, so editing the JSON updates both the playlist and the credits. Titles and artists are proper names, so they are not translated. If the file cannot be read, the section is left out instead of showing an empty heading. The panel's column already scrolls, so ten rows need no extra layout.

## Risks / Trade-offs

- [About 61 MB of audio inflates the APK] → Acceptable for a first release. A later change can re-encode to lower-bitrate Ogg/Opus or move music to Play Asset Delivery. The manifest abstraction keeps that change local.
- [Licensing: one CC BY-NC-SA track and six tracks with no recorded license] → Not a blocker for implementation, but tracked as a release blocker in Open Questions. Removing a track only means editing the manifest and deleting the file, because rotation is id-based.
- [`MediaPlayer` gapless playback is imperfect, so there is a short gap between tracks] → Acceptable for background music between songs. `setNextMediaPlayer` is a possible follow-up.
- [Stripping the Kate Kody tracks removed the iTunes gapless (`iTunSMPB`) info] → A few milliseconds of encoder padding may be audible at the track boundaries. That doesn't matter between songs.
- [The DataStore instance is shared with `app-settings-page`] → Creating a second instance for the same file throws at runtime. The tasks require using the instance provided from `MainActivity`, and an instrumented test reads both stores from the same instance.
- [The stored id is written while the process can be killed] → DataStore writes are atomic. A lost write means the same track plays again next launch, which is harmless.

## Migration Plan

No data migration: the new key is absent on existing installs, which means "start at track 1". Rollback is reverting the change. A leftover `music_last_started_track` key is ignored.

## Open Questions

- License confirmation for Kate Kody (`bg_music_0002`, `bg_music_0005`), Tommaso Croce (`0003`), Nicolaas Stulting (`0006`), legacyAlli (`0007`), Koi-discovery (`0008`) and Unheard Music Concepts (`0010`). Also whether CC BY-NC-SA (Jahzzar, `0009`) is compatible with how the game will be distributed. CC BY tracks require attribution, which the About credits provide. This must be settled before a Play Store release, not before implementation.
