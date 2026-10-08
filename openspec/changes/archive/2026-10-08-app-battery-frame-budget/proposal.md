# Proposal

## Why

Players report heavy battery use while playing ([#91](https://github.com/hexmasternl/leak-out/issues/91)). Part of it is avoidable. The game clock sends a tick on every display frame for as long as the game screen is open and not paused. That includes the time after a level ends, when the result dialog can stay open indefinitely with nothing moving, and every tick redraws the whole playing field and HUD. On 90/120 Hz phones the game also simulates and redraws at the panel's full rate, although its slow liquid and 1.6–2 s pulses look the same at 60 fps. Each frame also allocates more than it needs to.

## What Changes

- **The game clock goes idle once an attempt is over.** After a win or a loss the clock keeps running only until every end effect has finished: the leak effect and missed-section pulse, the finish flash, the win tally, and the points popups. After that, no more ticks are sent and the game screen stops redrawing until something changes: a restart, the next level, or input.
- **The finish glow settles after a loss.** Today it pulses for as long as the clock runs. After a loss it becomes steady at 60 % (as under reduced motion), so freezing the clock never leaves it frozen mid-pulse.
- **Game ticks are capped at about 60 per second.** On panels faster than 60 Hz the clock skips frames, so the game state, and therefore the field and HUD, change at most about 60 times a second. Time is never lost: the skipped frames' time goes into the next tick. Input-driven motion (dragging, panning, zooming, Compose animations) is not affected.
- **Less allocation per frame (easy cases only):**
  - A tick only rebuilds the awards list when an award has actually expired.
  - The text layout of each points popup is measured once per text instead of on every frame.
  - The liquid fills of tiles the liquid has already left are computed only when the used channels change, not on every frame.
- No visible change to the game during play. The end screens look the same once their effects have finished.

## Capabilities

### New Capabilities
- `game-play-frame-budget`: how often the game screen advances the game clock and redraws: idle once an attempt's end effects are over, at most about 60 ticks a second while running, and no time lost through the cap.

### Modified Capabilities
- `game-play-playing-field`: *Endpoint glows*. After a loss the finish ring's glow stops pulsing and stays steady at 60 %.

## Impact

- `app`: `ui/game/GameScreen.kt` (`GameClock` gets the cap and the idle condition), `ui/game/GameSession.kt` (a pure "needs the clock" rule from the end-effect timings), `ui/game/Endpoints.kt` (finish glow after a loss), `ui/game/PlayingField.kt` (popup text layout cache, memoised fills for settled tiles).
- `core`: `engine/GameEngine.kt` keeps the same awards list when none has expired. The `game-engine` behaviour is unchanged: a tick after an attempt ends still advances the clock. The app just stops sending ticks once nothing is left to animate.
- No new dependencies, permissions or settings. Out of scope (possible follow-up after profiling): caching the static board in a separate layer, and an energy-saver setting.
