# Proposal

## Why

The liquid leaks the moment it reaches an open end, and today nothing tells the player that this is about to happen. While they are busy at another part of the field, the first sign of trouble is the leak itself. A warning that builds up as the liquid closes in on the gap gives the player a fair chance to react. Tracked in [#80](https://github.com/hexmasternl/leak-out/issues/80).

## What Changes

- The game works out how far the liquid head is from the **gap**, which is the point where it would leak if nothing is placed. The distance is in sections and includes the part of the head's tile still to run. There is no gap while the route leads into the finish, before the liquid starts, and after the attempt is won or lost.
- **Score bar colour:** with the gap 5 sections away or less, the score bar's fill slowly turns from its normal brass to red (`leak.500`), reaching full red at 3 sections. Above 5 sections the bar looks as it does now.
- **Score bar blink:** with the gap 3 sections away or less, the score bar switches every 500 ms between the score and the word "Warning". Once the player builds the gap more than 3 sections away, the blinking stops. Once it is more than 5 sections away, the bar is back to normal.
- **Heartbeat:** the new `heartbeat.mp3` (from the repository's `assets/sfx/`) loops while the gap is 5 sections away or less. It starts quietly and gets louder as the liquid comes closer. It follows the sound effect settings and stops when the warning ends, the game pauses, or the attempt ends.
- The score bar's screen reader description names the warning while the bar blinks.

## Capabilities

### New Capabilities
- `game-play-gap-warning`: the distance from the liquid head to the gap, and the warning shown and heard while that distance is small (score bar colour and blinking, heartbeat).

### Modified Capabilities
- `game-sound-effects`: the bundled sounds gain the `heartbeat` group, played as a looping warning sound instead of a one-shot effect.

## Impact

- `core`: a pure function that measures the gap distance from a `GameState`, built on the same walk as `RouteTracer`, with unit tests.
- `app`: `ScoreBar.kt` and `GameHud.kt` (warning colour, alternating text, content description), `GameScreen.kt` (feeding the distance to the HUD and the heartbeat), the audio package (a looping heartbeat player with live volume), `app/src/main/assets/sfx/heartbeat.mp3`, and a new `Warning` string in `strings.xml`.
- Assets: `assets/sfx/heartbeat.mp3` is added to the repository.
- `app-game-play-points-drop-sound` (#82) also changes the `Bundled sound effects` requirement. Whichever change is archived second must keep both groups.
