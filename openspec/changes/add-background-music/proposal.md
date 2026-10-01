## Why

The game is silent. Ten background tracks are now in `assets/audio/`, with their credits in `credits.json`. Playing the same opening track on every launch quickly gets repetitive, so the playlist should continue where the last session left off. The settings page (change `app-settings-page`) already stores `musicEnabled` and `musicVolume` but shows them disabled with "Coming soon". This change makes the music switch and slider work.

## What Changes

- **Bundled tracks**: the ten MP3s (already stripped to audio frames only) go into `app/src/main/assets/music/` under stable, filesystem-safe names, next to a playlist manifest generated from `assets/audio/credits.json`. The manifest keeps title, artist, album, license and source for every track, so a later credits screen can read the same file.
- **Fixed playlist order**: tracks play in manifest order (the order of `credits.json`). When a track ends the next one starts, and after the last track it wraps to the first.
- **Rotation across launches**: the app stores the id of the last track that *started*. On the next cold start it begins with the track after that one, not with track 1. If no id is stored, or the stored id is no longer in the playlist, playback starts at the first track.
- **Lifecycle**: music plays on every screen (welcome, library, game) as one continuous stream. It pauses when the app goes to the background and resumes the same track at the same position when it comes back. It respects audio focus: it pauses when another app takes focus and ducks for short interruptions.
- **Settings**: the Music switch and volume slider on the Settings screen become interactive and take effect immediately. Sound effects stay disabled with the "Coming soon" note, because effects are not part of this change.

## Capabilities

### New Capabilities
- `background-music`: the bundled playlist and its order, continuous playback with wrap-around, rotation of the starting track across launches, lifecycle and audio-focus behaviour, and reacting to the music settings.

### Modified Capabilities
- `settings-screen`: the requirement "Audio panel is complete but disabled" changes so that the Music switch and slider are enabled while Sound effects stays disabled. This capability is introduced by change `app-settings-page`, which must be archived before this change.

## Impact

- `app/`: a new `audio/` package (playlist manifest parser, pure `MusicRotation` logic, `BackgroundMusicPlayer` wrapping `MediaPlayer` and audio focus, a lifecycle-aware controller), `MainActivity` wiring, the settings UI (enable the music controls), and new assets under `app/src/main/assets/music/`.
- Persistence: one new DataStore Preferences key, `music_last_started_track`, in the existing `settings` file from `app-settings-page`. No Room changes.
- Dependencies: none new. `MediaPlayer` and `AudioManager` are platform APIs, and DataStore comes with `app-settings-page`.
- APK size grows by about 61 MB of audio. MP3 is not recompressed by AAPT.
- Licensing: `credits.json` records the license wherever the source tags had one. One track (Jahzzar, "Breaking Bad") is CC BY-NC-SA 3.0, and six tracks have no license recorded. These must be confirmed before a store release. Implementation does not depend on it.
- No changes to `core`.
