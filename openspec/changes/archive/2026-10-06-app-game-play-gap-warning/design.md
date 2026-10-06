# Design

## Context

Issue [#80](https://github.com/hexmasternl/leak-out/issues/80) asks for a warning while the liquid closes in on an open end: the score bar turns from yellow to red between 5 and 3 sections, and blinks between the score and "warning" at 3 sections or closer. A comment on the issue adds a heartbeat (`assets/sfx/heartbeat.mp3`) that starts quietly at 5 sections and gets louder as the liquid comes closer.

What exists:
- `GameState.flow` is `FlowState.Flowing(position, entry, channel, progress, used)` while the liquid runs. `GameSession.state` is Compose state that the engine updates every frame.
- `RouteTracer.trace(board, level)` walks the route from the start with the flow's own rules (`exitFor`, `channelFor`, `MandatoryProgress`) and reports where it stops being connected (`Route.Open.endsAt`, with a reason).
- `ScoreBar.kt` draws a `brass.500 → brass.300` fill on a `surface.abyss` track and the score as centred text. `GameHud` passes `score`, `stars`, `attempt` and `reducedMotion` to it.
- One-shot effects go through `SoundEffects` → `SoundPoolSoundEffectPlayer`. `SoundCatalog` groups files by stem. Music uses `MediaPlayerMusicEngine`.
- `heartbeat.mp3` is about 8 s at 256 kbps. Decoded to 16-bit stereo PCM that is about 1.4 MB, which is above `SoundPool`'s roughly 1 MB limit per sample, and it has to loop with a volume that changes while it plays.

## Goals / Non-Goals

**Goals:**
- One pure, unit-tested measure of the gap distance in `core`, used by both the HUD and the heartbeat.
- A warning that ramps smoothly and stops at once when the player fixes the gap.

**Non-Goals:**
- Showing the gap on the playing field (highlighting the open tile). That can come later on top of the same distance.
- Warnings before the flow starts, or for gaps elsewhere on the board that the liquid will not reach.
- A setting to turn the warning off. It follows the sound settings for the heartbeat and is always shown on the bar.

## Decisions

### 1. Gap distance in `core`, walking ahead from the head
Add `GapDistance.of(state: GameState): Double?` in `core/engine` (or next to `RouteTracer` in `core/route`). It returns null unless `state.outcome` is `Playing` and `state.flow` is `Flowing`. Otherwise it starts with `1 - progress` for the head's tile, takes the head channel's exit side and walks on with the `RouteTracer` rules. The visited set starts as `flow.used` plus the head's channel, so a cross whose other channel was already used is a gap, the same as in the flow. Each placed section the walk enters adds 1. The walk ends at the first tile that would leak (return the distance), or at a finish that the liquid would win in (return null). A finish with unused mandatory sections is a gap, by the same `MandatoryProgress` check the flow uses.

Reuse: pull the step loop of `RouteTracer.trace` into an internal function that takes a starting position, exit side and visited set, and have `trace` call it from the start tile. Both then share one set of leak rules, and the existing `RouteTracer` tests guard the refactor.

*Alternative:* store the distance in `GameState` and update it in the engine. Rejected: it follows from `board` and `flow`, and `GameState` computes derived values instead of storing them so they cannot drift. Measuring once per frame is cheap, because the walk is linear in the route length.

*Alternative:* count whole tiles only. Rejected: the colour would jump once per tile instead of changing smoothly, as the issue asks ("slowly changes").

### 2. Warning levels are pure functions of the distance
In `app/ui/game`, a small `GapWarning` holds the thresholds (`COLOUR_FROM = 5.0`, `BLINK_AT = 3.0`) and the pure mappings:
- `redness(distance: Double?): Float`: 0 above 5 or null, 1 at 3 or less, linear between.
- `blinking(distance: Double?): Boolean`: distance ≤ 3.
- `showWarningText(distance, nowMs, sinceMs): Boolean`: whether the 500 ms phase shows "Warning". The first phase shows "Warning". `sinceMs` is the game time at which blinking started, kept in `remember(attempt)` state and cleared when blinking stops.
- `heartbeatFactor(distance: Double?): Float?`: null above 5 or null, else `0.15 + 0.85 * (5 - d) / 5`.

These are JVM-unit-tested without Compose. The distance only shrinks while the liquid flows and only grows on a placement, so there is no flicker around a threshold and no hysteresis is needed.

### 3. Score bar takes a warning, not a distance
`ScoreBar` gets two new parameters, `warning: Float` (0–1 redness) and `warningText: Boolean`. The fill brush lerps both gradient stops towards `leak.500` by `warning`, so at 1 the fill is solid red. When `warningText` is true, the centred text is the localised "Warning" in capitals (`uppercase(locale)`, as `app-localization` requires). It is measured in the same style and shadow as the score, so it fits the fixed 200 dp bar. The `en-XA` pseudo-locale check covers the 40 % longer text. The content description gains the plural warning string while blinking. `GameHud` computes these from the distance and `state.clockMs`, so blinking follows the game clock and stops while paused.

### 4. Heartbeat on a looping `MediaPlayer`, not `SoundPool`
Add a `LoopingSoundPlayer` seam (`start(file, volume)`, `setVolume(volume)`, `stop()`, `release()`) with a `MediaPlayer` implementation (`isLooping = true`, `setVolume(v, v)`) and a recording fake for tests, next to the existing `SoundEffectPlayer`. `SoundEffects` gains `heartbeat(factor: Float?)`: with sound off, volume 0, no `heartbeat` file or a null factor it stops the loop. Otherwise it starts the loop if needed and sets the volume to `factor * soundVolume`. `apply(settings)` applies a changed volume or "sound off" to a running loop at once.

`GameSound` gains `Heartbeat("heartbeat")`, so `SoundCatalog` groups the file, but `SoundEffects.play(Heartbeat)` is not used. The heartbeat file is not preloaded into `SoundPool`: `SoundCatalog.files` (used for preloading) leaves out looped sounds.

A `GapHeartbeat(session, soundEffects)` composable, next to `ReplacementLandingSound` in `GameSounds.kt`, observes `snapshotFlow { factor(state) }` while the lifecycle is at least STARTED, with the factor null when the game is paused or the attempt is over. It stops the loop on dispose and when the lifecycle drops below STARTED. Volume updates are only sent when the rounded volume changes by 0.01 or more, to avoid a `MediaPlayer` call on every frame.

*Alternative:* re-trigger a short one-shot beat from `SoundPool` faster as the liquid approaches. Rejected: the bundled file is a long recording, not a single beat, and the issue asks for loudness rather than tempo.

### 5. Assets
Copy `assets/sfx/heartbeat.mp3` unchanged to `app/src/main/assets/sfx/`. The existing "bundled copies match the source" test then covers it.

## Risks / Trade-offs

- [The loop may click at the wrap point] → Listen on the emulator. If it clicks, trim the source file in the repository's `assets/sfx/` (both copies stay identical) instead of adding crossfade code.
- [`MediaPlayer` start latency (~100 ms) when the warning begins] → Acceptable for a sound that starts quietly. Prepare the player once per game screen and keep it prepared while stopped (`pause` + `seekTo(0)` instead of `stop`).
- [Audio focus or music ducking] → The heartbeat is an effect. Like other effects it SHALL NOT pause or duck the music, so no audio focus request is made for it.
- [Spec conflict with `app-game-play-points-drop-sound` (#82)] → Both modify `game-sound-effects` › *Bundled sound effects*. Whichever is archived second must merge its group into the text the first one archived.

## Open Questions

- None blocking. The 0.15 starting factor is a first guess to tune by ear on a device.
