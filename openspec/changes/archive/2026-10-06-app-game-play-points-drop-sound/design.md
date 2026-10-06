# Design

## Context

Issue [#82](https://github.com/hexmasternl/leak-out/issues/82): when the liquid has passed a tile, its points pop up on the tile. Play `assets/sfx/drop.mp3` when that popup animation starts.

What exists:
- The engine appends `Award(position, points, atMs)` to `GameState.awards` in `FlowSimulation` each time the liquid leaves a section, and drops awards older than `Award.VISIBLE_MS`. `PlayingField` draws one popup per award, timed from `atMs` on the game clock. So a popup starts exactly when its award appears in the state.
- `GameSounds.kt` holds the game's sound hooks: `placeWithSound`, `ReplacementLandingSound` (a `snapshotFlow` on session state) and `OncePerAttemptSound`.
- `SoundEffects.play(GameSound)` applies the settings and picks a variant. `SoundCatalog` groups the files in `app/src/main/assets/sfx/` by stem and ignores unknown groups, so `drop.mp3` is ignored today.
- `drop.mp3` is about 1 s, so it fits in `SoundPool` like the other one-shot effects.

## Goals / Non-Goals

**Goals:**
- One drop per popup, in sync with its start, without changing `core`.

**Non-Goals:**
- Changing the sound by the number of points (pitch or variants). Variants can be added later as `drop_001.mp3`, `drop_002.mp3`, and the existing random-variant rule picks between them with no code change.
- Sounds for the final score tally in the level complete dialog.

## Decisions

### 1. Detect new awards in the app, by identity of the newest award
A `PointsAwardedSound(session, soundEffects)` composable in `GameSounds.kt` collects `snapshotFlow { session.state.attempt to session.state.awards.lastOrNull() }`. It plays `GameSound.PointsAwarded` when the newest award changes to a non-null one in the same attempt. Comparing the newest `Award` (a data class with position, points and `atMs`) is enough. Awards are only appended, and two awards on the same tile at the same game time can only happen in one frame, which plays one drop anyway. A change of `attempt` (restart) resets the comparison without playing. The award that is dropped when it ages does not change the newest one while a newer one exists. When the list empties, nothing plays.

*Alternative:* emit a sound event from the engine. Rejected: the engine is pure and already exposes awards with their times, and every other game sound is decided in the app the same way.

*Alternative:* trigger from the popup drawing code in `PlayingField`. Rejected: drawing runs per frame inside `Canvas`, and popups for tiles that are off-screen are not drawn while their timing continues. The sound should not depend on the viewport.

### 2. Throttle in real time
The composable keeps the uptime of the last drop and skips a drop within 100 ms of it (`DROP_MIN_INTERVAL_MS = 100`), using a clock passed in for tests. Real time, not game time, because the point is what the player hears: in fast forward the game clock runs 10× and would let drops pile up. The throttle is a small pure helper (`DropThrottle`) with a JVM unit test.

### 3. Catalogue and asset
Copy `assets/sfx/drop.mp3` unchanged to `app/src/main/assets/sfx/` and add `GameSound.PointsAwarded("drop")`. `SoundPoolSoundEffectPlayer` preloads it with the others. The existing bundled-copy and grouping tests are updated for the new group.

### 4. Where it is used
`GameScreen` and `TestPlayScreen` both call `PointsAwardedSound` next to `ReplacementLandingSound`, so the designer's test play sounds the same as the game.

## Risks / Trade-offs

- [Many drops in quick succession sound noisy at normal speed on fast levels] → The 100 ms throttle caps it. Adjust the constant after listening on a device.
- [Drops overlap the level complete sound at the finish] → The level complete sound plays when the dialog appears, which is after the last award, so they follow each other rather than overlap.
- [Spec conflict with `app-game-play-gap-warning` (#80)] → Both modify `game-sound-effects` › *Bundled sound effects*. Whichever is archived second must merge its group into the text the first one archived.
