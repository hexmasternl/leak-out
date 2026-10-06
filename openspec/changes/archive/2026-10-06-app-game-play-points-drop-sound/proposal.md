# Proposal

## Why

When the liquid has passed a tile, the points for that tile pop up on it, but silently. A short sound with each popup makes the score feel earned, and lets the player follow the liquid by ear while they look elsewhere on the field. The repository now has a `drop.mp3` effect for this. Tracked in [#82](https://github.com/hexmasternl/leak-out/issues/82).

## What Changes

- The new `drop.mp3` (from the repository's `assets/sfx/`) is bundled as the **points awarded** sound.
- The drop plays once when a points popup starts, so each time the liquid leaves a section and points are awarded. A cross that scores twice drops twice.
- Popups that start in the same frame share one drop. Drops are at most one per 100 ms, so fast forward does not turn them into a buzz.
- The drop follows the sound effect settings like every other effect. It does not play while paused (no popups start then), and it plays in the level designer's test play as well.

## Capabilities

### New Capabilities

None.

### Modified Capabilities
- `game-sound-effects`: the bundled sounds gain the `drop` group, mapped to a new points awarded sound, with a requirement for when it plays.

## Impact

- `app`: `GameSound` (new `PointsAwarded("drop")`), `GameSounds.kt` (a composable that plays the drop for new awards), `GameScreen.kt` and `TestPlayScreen.kt` (use it), `app/src/main/assets/sfx/drop.mp3`, and the sound catalogue tests.
- Assets: `assets/sfx/drop.mp3` is added to the repository.
- No `core` changes: `GameState.awards` already holds each award with its game time.
- `app-game-play-gap-warning` (#80) also changes the `Bundled sound effects` requirement. Whichever change is archived second must keep both groups.
