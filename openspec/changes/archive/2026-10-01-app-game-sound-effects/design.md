## Context

The repository root `assets/sfx/` holds seven MP3 effects: three placement variants (`section_placement_001..003.mp3`), one destroy variant (`section_destroy_001.mp3`), `section_placement_failed.mp3`, `level_complete.mp3` and `level_failed.mp3`, about 450 KB in total. As with the music and artwork, the root `assets/` folder is the design source and the app ships its own copy under `app/src/main/assets/`.

The plan (`docs/project-plan.md` §2.1) picks `SoundPool` for effects. `SettingsRepository` (change `app-settings-page`) already stores `soundEnabled` (default on) and `soundVolume` (default 0.8), and the Audio panel shows them disabled behind `AUDIO_AVAILABLE`. Change `add-background-music` is in progress: its playlist logic lives in the `audio/` package, and it splits `AUDIO_AVAILABLE` into `MUSIC_AVAILABLE` and `SOUND_EFFECTS_AVAILABLE`.

In the game, every placement goes through `GameSession.send(GameCommand.Place(cell))`, from `FieldDropTarget.tryPlace` (drop) and from the field's `onTap` (tap). The engine uses one placement rule (`Placement.check`), which returns `Place`, `Replace` or `Rejected(reason)`, and the session exposes it as `canPlace(at)`. The lost and win dialogs show when `GameSession.showLostDialog` and `showWinDialog` turn true. `GameScreen` skips the end effects on `ON_STOP`, so a dialog can become visible while the app is in the background.

## Goals / Non-Goals

**Goals:**
- Audible feedback for placing, replacing and refused placements, and for the end of a level.
- Random choice between variants, with new variants added by dropping in a file.
- Low latency for the placement sounds, which the player triggers many times a minute.
- Working Sound effects switch and volume slider.
- Event and variant logic testable on the JVM without `SoundPool`.

**Non-Goals:**
- Sounds for other events: the liquid flowing, the finish flash, points popups, the tally, fast forward, pause, buttons or menus.
- Sounds for the unused sections taken off the board during the win tally. They overlap the level complete sound.
- Haptics. The vibration setting stays stored only.
- Audio focus for effects, and ducking the music for effects.
- Avoiding repeats between consecutive random picks.

## Decisions

**Assets: `app/src/main/assets/sfx/`, copied unchanged.** The source names are already asset-safe, so they keep their names. MP3 is on AAPT's no-compress list, so `assets.openFd("sfx/<file>")` works for `SoundPool.load(AssetFileDescriptor, …)`. A JVM unit test compares the app folder with the root folder (same names, same bytes), like the music drift test, so the copies cannot drift without build logic outside `src/`. *Alternative:* `res/raw` (rejected: resource names cannot carry the grouping without a hand-kept table, and levels, pipes and music already ship as assets).

**Grouping by file name, not by a manifest.** `SoundCatalog.from(fileNames)` strips `.mp3`, then strips a trailing `_` plus three digits if present, and maps the resulting group to a `GameSound` enum: `SectionPlaced`, `SectionDestroyed`, `PlacementFailed`, `LevelComplete`, `LevelFailed`. Files are listed with `assets.list("sfx")`. The suffix must be exactly `_NNN`, so `section_placement_failed` stays its own group. Unknown groups are ignored, and a sound without files has no variants, which means silence. *Alternative:* a JSON manifest like `audio_credits.json` (rejected: effects have no credits to carry, and a manifest is one more file to keep in step).

**Pure controller, thin player.** `SoundEffects` (in `audio/`) owns every decision: it holds the catalogue, a `Random` (injectable, `Random.Default` in the app), and the latest `soundEnabled`/`soundVolume`. `play(sound)` returns early when sound is off, the volume is 0 or the sound has no variants, picks `variants.random(random)` and calls `SoundEffectPlayer.play(file, volume)`. `SoundEffectPlayer` is the seam: `SoundPoolSoundEffectPlayer` loads every file once at creation and plays by sound id, and a recording fake serves the JVM and Compose tests. Settings are collected from `SettingsRepository.settings` into the controller's fields, so a change applies to the next `play` without any restart.

**`SoundPool` setup.** `SoundPool.Builder().setMaxStreams(4)` with `AudioAttributes(USAGE_GAME, CONTENT_TYPE_SONIFICATION)`. Every file is loaded at startup (seven small files, loaded asynchronously). A `play` for a file whose load has not completed is skipped, which only matters in the first moments after a cold start, long before a level can end. Four streams let fast placements overlap without cutting each other off. Volume is passed as `(v, v)` per play, priority 1, no loop, rate 1.0.

**Lifetime: a ViewModel on `MainActivity`.** A `SoundEffectsViewModel` owns `SoundEffects` and the `SoundPool` player, collects the settings in `viewModelScope`, and releases the pool in `onCleared`. This matches how `add-background-music` keeps its player, and a configuration change does not reload the sounds. `MainActivity` passes `soundEffects` to `GameScreen` and the preview callback to `SettingsScreen`. `GameScreen` takes a nullable `SoundEffects?` (default `null`, silent), so existing tests and previews are unchanged. The debug Pipe Lab activity stays silent.

**Classifying a placement in the app, not in `core`.** `GameSession` gains `fun place(at: Position): PlaceResult` with `Placed`, `Replaced` and `Rejected`. Before sending the command it reads whether `state.board[at]` is a `Cell.Pipe`. If the engine accepts the command, a pipe on that tile means a replacement and an empty tile means a plain placement. `send` returning false means rejected. This needs no new engine API, keeps `core` unchanged, and gives the hover highlight, the drop and the sound one source of truth. Both `FieldDropTarget.tryPlace` and the field's `onTap` call `session.place` and map the result to a sound. *Alternative:* expose the `PlacementCheck` from the engine (rejected: the board read is enough, and a core change for an app concern is not needed).

**What "an illegal location" is.** The failure sound plays only when the drop or tap maps to a tile of the level (`field.cellAt` returns a cell) and the placement is refused. Letting go over the stack, the HUD or outside the level returns `null` from `cellAt`, which is a cancel and stays silent, as is a drag cancelled by pause or fast forward. While the game is paused or stopped the field takes no input, so the `LOCKED` reasons for pause and fast forward cannot reach the sound in practice. The replace lock, which is also `LOCKED`, does play the failure sound, because the player actively tried and was refused.

**Replace plays destroy only.** One placement plays one sound. A replacement plays `SectionDestroyed` in place of `SectionPlaced`, so the two do not stack into noise.

**End-of-level sounds keyed per attempt and gated on the foreground.** In `GameScreen`, one effect per dialog: `LaunchedEffect(attempt, showWinDialog, started)` where `started` is the lifecycle being at least `STARTED` (`LocalLifecycleOwner.current.lifecycle.currentStateAsState()`). It plays when the dialog shows, the app is started and the sound has not yet played for this attempt. The "played" marker is a `remember(session)` set of attempt numbers per dialog, so the tally recomposing the dialog does not replay it, a restart (new attempt) plays again, and a dialog that appeared during `ON_STOP` plays once on return. The level failed sound waits for `showLostDialog`, so it plays after the one second leak effect, as the user asked, and not when the leak starts.

**Settings UI.** `SOUND_EFFECTS_AVAILABLE = true` gates the Sound effects switch and slider. If `add-background-music` has already split the flag, this change only flips that one. Otherwise this change does the split, with `MUSIC_AVAILABLE = false`, and moves the "Coming soon" note from the panel title to the Music row. `SettingsViewModel` gains `onSoundEnabledChange` and `onSoundVolumeChange`. The slider shows the live value during the drag and writes on release (`onValueChangeFinished`), the same as the music slider. On release, if sound is on, the screen calls `onSoundPreview(volume)`, which plays one `SectionPlaced` variant at that volume through `SoundEffects.preview`, so the player hears the level they picked. The preview uses the new volume even before the DataStore write has come back through the flow.

## Risks / Trade-offs

- [`SoundPool` loads asynchronously, so a sound triggered before its load finishes is lost] → Load at app start. The first sound a player can trigger is a placement after choosing a level, well after the seven small loads finish. The skipped play is logged in debug builds.
- [MP3 decoding in `SoundPool` takes memory, since samples are decoded to PCM] → Seven short effects; `level_complete.mp3` (about 140 KB MP3) is the largest. Acceptable. Re-encoding to Ogg is a later option if needed.
- [Rapid placements could stack many streams] → `maxStreams = 4`; the oldest stream is stopped when a fifth starts.
- [The settings delta and `add-background-music` both modify "Audio panel is complete but disabled"] → The delta here describes the music controls conditionally, so it stays true whichever change lands first. The change archived second reconciles the requirement text at archive.
- [The level complete sound and a possible future win jingle in the music] → Not an issue today. Music keeps playing under effects.

## Migration Plan

No data migration: `soundEnabled` and `soundVolume` already exist with defaults. Rollback is reverting the change.

## Open Questions

- Licensing of the seven effects is not recorded. Confirm before a Play Store release; implementation does not depend on it.
