## Why

Playing a level is silent: placing a section, replacing one, a drop that is refused and the end of a level give no audible feedback. Seven sound effects are now in `assets/sfx/` at the repository root. The settings page (change `app-settings-page`) already stores `soundEnabled` and `soundVolume`, but shows the Sound effects controls disabled with "Coming soon". This change plays the effects in the game and makes those controls work.

## What Changes

- **Bundled effects**: the seven MP3s in `assets/sfx/` are copied unchanged into `app/src/main/assets/sfx/`:
  - `section_placement_001.mp3`, `section_placement_002.mp3`, `section_placement_003.mp3`
  - `section_destroy_001.mp3`
  - `section_placement_failed.mp3`
  - `level_complete.mp3`
  - `level_failed.mp3`
- **Sound events** in the game screen:
  - **Section placed**: a section is placed on an empty tile. One of the placement variants is picked at random each time.
  - **Section destroyed**: a section is placed on a tile that held a placed section, so the old section is destroyed. One of the destroy variants is picked at random. A replacement plays the destroy sound instead of a placement sound, not both.
  - **Placement failed**: the player drops or taps a section on a tile of the field that refuses it (a blocked, start or finish tile, a section the liquid has entered, a level without replacing, or while the replace lock runs). Letting go outside the field is a cancel and stays silent.
  - **Level complete**: plays once when the level complete dialog appears.
  - **Level failed**: plays once when the level lost dialog appears.
- Events are grouped by file name prefix (`section_placement_NNN`, `section_destroy_NNN`), so adding a variant later is only adding a file.
- **Settings**: effects play only when `soundEnabled` is on, at `soundVolume`. The Sound effects switch and volume slider on the Settings screen become interactive and take effect immediately. Releasing the volume slider plays a placement sound at the new volume as a preview. The "Coming soon" note leaves the Sound effects row.

## Capabilities

### New Capabilities
- `game-sound-effects`: the bundled effects and their grouping, which game event plays which effect, random choice between variants, playing at most once per dialog appearance, and following the sound settings.

### Modified Capabilities
- `settings-screen`: the requirement "Audio panel is complete but disabled" changes so that the Sound effects switch and slider are enabled and change `soundEnabled` and `soundVolume`. This capability is introduced by change `app-settings-page`, which must be archived before this change is archived. Change `add-background-music` modifies the same requirement for the Music controls; the delta here describes the panel with both changes in place, and whichever change lands second reconciles the requirement text.

## Impact

- `app/`: a new sound effects part of the `audio/` package (the effect catalogue, a pure `SoundEffects` controller with random variant choice, a `SoundPool` backed player), `GameScreen` wiring for the five events, `MainActivity` (create and release the player, pass it to the game screen), and the settings UI (enable the Sound effects controls, `SettingsViewModel` setters).
- Assets: about 450 KB of MP3s under `app/src/main/assets/sfx/`. The root `assets/sfx/` stays the design source.
- Persistence: none new. `soundEnabled` and `soundVolume` already exist in `SettingsRepository`.
- Dependencies: none new. `SoundPool` is a platform API.
- No changes to `core`: the app already knows, before sending a place command, whether the target tile holds a placed section and whether the engine accepts the placement.
