# Design

## Context

`GameClock` (`app/.../ui/game/GameScreen.kt`) is a `LaunchedEffect(session, paused)` that loops on `withInfiniteAnimationFrameNanos` and sends `GameCommand.Tick(dtMs)` on every frame. It keeps the sub-millisecond remainder by advancing `last` by whole milliseconds only. Every accepted tick produces a new `GameState`. That invalidates the `PlayingField` `Canvas`, which reads `nowMs()`/`flow()` and redraws every visible cell, and the HUD.

After a win or a loss `GameEngine.advance` still moves `clockMs` (spec `game-engine`, "The game clock advances with ticks"), so the loop never goes quiet behind a result dialog. Every clock-driven end effect is already a pure function of an age that settles:

| Effect | Settles at | Source |
| --- | --- | --- |
| Leak spurt, missed-section pulse | `lossAgeMs >= LeakTiming.EFFECT_MS` | `LeakEffect.kt` |
| Finish flash | `winAgeMs >= WinTiming.FINISH_EFFECT_MS` | `FinishFlash.kt` |
| Win tally | `winAgeMs >= FINISH_EFFECT_MS + WinTally.totalDurationMs(score, stars)` | `GameSession.skipWinAnimation` |
| Points popups | `state.awards` empty (dropped after `Award.VISIBLE_MS`) | `GameEngine.advance` |
| Finish glow | **never** after a loss (pulses on `nowMs`) | `EndpointGlow.finish` |

## Goals / Non-Goals

**Goals:**
- Zero clock ticks, and therefore zero field/HUD redraws, once an ended attempt is visually still.
- No more than about 60 game-state changes per second on high-refresh displays, with no drift in game time.
- Remove the easy per-frame allocations named in the proposal.

**Non-Goals:**
- Caching the static board in its own layer (fix C in #91). That is a separate change, after profiling.
- Changing the engine's clock semantics. A tick after the end still advances the clock, and the `skipLeakEffect` / `skipWinAnimation` paths keep using it.
- Throttling Compose's own animations (dialogs, sparkles, drag return). They are short and input-driven.
- Requesting a lower display refresh rate from the system. See decision 3.

## Decisions

### 1. A pure `GameSession.needsClock` rule decides when the clock may idle

Add `val needsClock: Boolean` to `GameSession`. It is true while the outcome is playing. After a loss it is true while `lossAgeMs < LeakTiming.EFFECT_MS` or awards are still visible. After a win it is true while `winAgeMs` is before the end of the tally or awards are still visible. `GameClock` takes it through `session.select { needsClock }` and adds it to its effect keys: `LaunchedEffect(session, paused, needsClock)` returns immediately when `!needsClock`, just like when paused. A restart changes the outcome back to playing, which flips `needsClock`, so the loop starts again with a first delta of 0.

*Why in the session:* the timings already live there (`showLostDialog`, `skipWinAnimation`). As a plain property of an immutable state it is unit-testable on the JVM, next to `GameSessionWinTest`.
*Alternative:* stop ticking in the engine (refuse ticks after the end). Rejected: it changes the `game-engine` contract and the skip paths depend on post-end ticks.

### 2. The finish glow is steady after a loss

`EndpointGlow.finish(nowMs, won, reducedMotion)` becomes `finish(nowMs, won, lost, reducedMotion)`: `won → 0f`, `lost || reducedMotion → STEADY`, otherwise the pulse. `PlayingField` already knows `won` and gets a `lost` flag (it has `leakAt`, which is non-null exactly when lost). Without this, idling would freeze the pulse at an arbitrary alpha. With it, the frozen frame is the intended steady state. Covered by `EndpointsTest`.

### 3. Cap ticks in `GameClock` rather than asking the system for a lower refresh rate

The loop still waits for every frame, but only sends a tick once at least `MIN_TICK_MS = 12` ms have built up since the last one. Time accumulates in whole milliseconds as today, so the clock never loses or gains time. The resulting tick rates:
- 60 Hz (16.7 ms frames) → every frame, 60/s.
- 90 Hz (11.1 ms) → mostly every second frame; the kept remainder lets every ninth frame tick alone, so 50/s.
- 120 Hz (8.3 ms) → every second frame, 60/s.
- 144 Hz (6.9 ms) → every second frame, 72/s.

The threshold sits below 16 ms so the Compose test clock (16 ms frames) still ticks on every frame. Without a tick, nothing reads changed state, so Compose skips recomposition and redraw for that frame. Waking for the frame callback is cheap.

*Alternative:* `Surface.setFrameRate` / a Compose preferred-frame-rate modifier, to let VRR panels drop to 60 Hz. It saves panel power too, but its availability on our Compose BOM and its interaction with dialogs and drag (which should stay smooth) are uncertain. It is left as a follow-up once fixes A/B have been measured. The throttle is pure and testable today.

The decision "tick now?" is a small pure helper (`TickPacer`: given the frame time, returns the delta to send or null), tested on the JVM with simulated 60/90/120/144 Hz frame times and with the "no time lost" property.

### 4. Easy allocation cuts

- **Awards (core):** in `GameEngine.advance`, keep `state.awards` as it is when it is empty or its oldest award is still visible. Awards are oldest first, so checking the first one is enough. This leaves the result unchanged and avoids a list copy per tick. `GameEngineTest` asserts the same list instance is kept.
- **Popup text (app):** `drawPopups` measures `PointsFormat.text(points)` every frame. Keep a small `HashMap<String, TextLayoutResult>` remembered next to `textMeasurer` (only a handful of distinct point texts exist). The style is constant for a given density. Key the remember on density.
- **Settled fills (app):** split `FieldLiquid.fills` into the settled part (from `used`) and the head part. Cache the settled fills in a per-field holder keyed on the identity of `(board, used)`. The engine keeps the same `used` instance while the head moves inside a tile. Each frame only the head tile's map is rebuilt. `FieldLiquidTest` covers that settled + head equals the current `fills` result for a few layouts.

## Risks / Trade-offs

- [An end effect we missed keeps animating from the clock and freezes when it idles] → the table above was taken from every `nowMs`/age reader in `PlayingField`/`GameHud`. An instrumented test checks that the lost and won screens reach the idle state with the leak puddle / solid finish ring shown. Any other clock reader would show up as a frozen frame in that test's screenshot check.
- [90 Hz panels tick at 50/s, so the liquid moves slightly less smoothly] → the liquid moves about 1 tile per second, so 50 steps a second is still sub-pixel to a few pixels per step. This is acceptable, and it can be tuned by changing `MIN_TICK_MS`.
- [Instrumented tests that advance the clock in sub-16 ms steps] → existing tests advance by frames or by whole durations. The pacer test documents the threshold.
- [The gap warning heartbeat or other sounds depend on ticks after the end] → they key on the outcome being playing. The existing `GapHeartbeatTest` is rerun.
