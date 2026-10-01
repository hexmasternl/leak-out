## Why

The game is silent. Ten background tracks are now in `assets/audio/` as `bg_music_0001.mp3` through `bg_music_0010.mp3`, with their credits in `audio_credits.json`. Playing the same opening track on every launch quickly gets repetitive, so the playlist should continue where the last session left off. The settings page (change `app-settings-page`) already stores `musicEnabled` and `musicVolume` but shows them disabled with "Coming soon". This change makes the music switch and slider work, and credits the musicians on the About panel.

## What Changes

- **Bundled tracks**: the ten MP3s (already stripped to audio frames only) and `audio_credits.json` are copied unchanged into `app/src/main/assets/music/`. `audio_credits.json` is both the playlist manifest and the credits: `title`, `artist` and `file` for every track. A track's id is its file stem (`bg_music_0001`).
- **Fixed playlist order**: tracks play in the order of `audio_credits.json`, which is the order of the file numbers. When a track ends the next one starts, and after the last track it wraps to the first.
- **Rotation across launches**: the app stores the id of the last track that *started*. On the next cold start it begins with the track after that one, not with track 1. If no id is stored, or the stored id is no longer in the playlist, playback starts at the first track.
- **Lifecycle**: music plays on every screen (welcome, library, settings, game) as one continuous stream. It pauses when the app goes to the background and resumes the same track at the same position when it comes back. It respects audio focus: it pauses when another app takes focus and ducks for short interruptions.
- **Settings**: the Music switch and volume slider on the Settings screen become interactive and take effect immediately. Sound effects stay disabled with the "Coming soon" note, because effects are not part of this change.
- **Music credits on the About panel**: a "Music" section lists every track as title and artist, in playlist order, read from the bundled `audio_credits.json`.

## Capabilities

### New Capabilities
- `background-music`: the bundled playlist and its order, continuous playback with wrap-around, rotation of the starting track across launches, lifecycle and audio-focus behaviour, and reacting to the music settings.

### Modified Capabilities
- `settings-screen`: the requirement "Audio panel is complete but disabled" changes so that the Music switch and slider are enabled while Sound effects stays disabled. A new requirement adds the music credits to the About panel. This capability is introduced by change `app-settings-page`, which must be merged and archived before this change is implemented.

## Impact

- `app/`: a new `audio/` package (credits/playlist parser, pure `MusicRotation` logic, `MediaPlayerMusicEngine` wrapping `MediaPlayer`, audio focus, the `BackgroundMusic` controller), `MainActivity` wiring, the settings UI (enable the music controls, the About music credits), and new assets under `app/src/main/assets/music/`.
- Persistence: one new DataStore Preferences key, `music_last_started_track`, in the existing `settings` file from `app-settings-page`. No Room changes.
- Dependencies: none new. `MediaPlayer` and `AudioManager` are platform APIs, and DataStore comes with `app-settings-page`.
- APK size grows by about 61 MB of audio. MP3 is not recompressed by AAPT. The repository holds the audio twice: the design source in `assets/audio/` and the app copy.
- Licensing: one track (Jahzzar, "Breaking Bad", `bg_music_0009`) was tagged CC BY-NC-SA 3.0, and six tracks have no license recorded. These must be confirmed before a store release. Implementation does not depend on it.
- No changes to `core`.
