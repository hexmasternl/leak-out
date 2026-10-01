## Context

Today `GameEngine.place` handles a replacement in one step. It writes the new section to the board, moves the stack, and sets `replaceLockUntilMs = clockMs + rules.replacePenaltyMs` (default 500 ms). `Placement.check` then refuses everything with `LOCKED` until the lock has passed. In the app, `GameSession.place` reports `PlaceResult.Replaced`, and `placementSound` maps that to `GameSound.SectionDestroyed`. Nothing marks the end of the lock, and the field draws the new section at once.

This change turns the replacement into a timed sequence: destroy, wait, land. The sequence has to stay deterministic in `core` (the engine is pure and tick-length independent: 1 ms ticks and one long tick give the same outcome), and the app only renders it and plays its sounds.

## Goals / Non-Goals

**Goals:**
- Model the replacement sequence in `core` as game state, so its timing, its interaction with the flow, pause and restart, and its effect on the route are unit tested without Android.
- Keep "how long" configurable per level through the existing `rules.replacePenaltyMs`, with a default of 3000.
- Block all placement input during the sequence, give it a clear animation, and play a sound at the start and at the landing.

**Non-Goals:**
- A global setting (in the settings screen) for the penalty. Configuration stays per level.
- Changing what can be replaced (`allowReplace`, filled sections).
- Penalizing the score. The cost is time only.
- New audio or image assets.

## Decisions

### 1. The sequence is core state: `GameState.replacement: Replacement?`

```kotlin
data class Replacement(
    val at: Position,
    val removed: PipeSection,   // the destroyed section, for the break animation
    val incoming: PipeSection,  // the section taken from the stack, lands at landsAtMs
    val startedAtMs: Long,
    val landsAtMs: Long,
)
```

`place` on a `PlacementCheck.Replace` does the following when `replacePenaltyMs > 0`:
- sets the cell to `Cell.Empty`,
- moves the stack,
- stores `replacement`,
- sets `replaceLockUntilMs = landsAtMs`,
- recomputes `routeComplete`, which is now incomplete if the tile was on the route.

With a penalty of 0 it keeps today's instant swap and sets no `Replacement`.

`replaceLockUntilMs` stays, and it stays the single input to `Placement.check`. That keeps the shared `canPlace`/`place` check unchanged. The lock and the sequence end at the same moment by construction.

*Alternative considered:* keep the instant swap in core and fake the delay in the UI. Rejected. The liquid could flow through a section the player can't see yet, the hover highlight and the drop could disagree, and none of it would be testable in `core`.

*Alternative considered:* leave the old section on the board until landing. Rejected. It looks broken but would still carry liquid, which contradicts what the player sees.

### 2. Landing happens in `tick`, split at the landing time

`tick(state, dt)` with a pending replacement whose `landsAtMs <= clockMs + dt` runs in three steps:
1. Advance the clock and the flow up to `landsAtMs`. That part is `landsAtMs - clockMs` of game time, times `flowTimeScale` of flow time.
2. If the outcome is still `Playing` and the tile is still empty, write `incoming` to the board, clear `replacement`, and recompute `routeComplete`.
3. Advance the rest of `dt`.

This keeps the existing guarantee that tick length doesn't change the outcome. A long tick can't let the liquid slip into a tile that should already hold its section, and it can't let the liquid skip the leak it should have hit. If the liquid enters the empty tile before `landsAtMs`, `FlowStep` reports a leak there and the attempt is lost. That needs no special case, because the cell really is `Cell.Empty`.

### 3. End of the attempt clears the sequence

When the outcome becomes `Won` or `Lost`, `replacement` is set to null in the same step, so nothing lands after the end. `start` (and so `Restart`) builds the state with `replacement = null`. Pause needs no extra code: `tick` already returns early while paused, and the clock doesn't move.

### 4. Fast forward and the sequence

`FastForward` is refused while `replacement != null`. The route is usually incomplete then anyway, but the replaced tile can lie off the route, so the check is explicit. `place` is already refused during fast forward, so a sequence can't start while fast forward is on. The sequence runs on clock time, not flow time, so it is never sped up.

### 5. App: sounds come from state transitions

- At the drop, `PlaceResult.Replaced` keeps mapping to `SectionDestroyed` in `placementSound`. With a penalty of 0, `placeWithSound` plays `SectionDestroyed` and then `SectionPlaced`.
- At the landing, `GameSession` already funnels every command through `send`. After each tick it compares the state before and after: `before.replacement != null`, `after.replacement == null` and `after.outcome is Playing` means a landing. `GameSession` exposes that as an event (a counter or a callback), which `GameScreen` turns into `soundEffects.play(GameSound.SectionPlaced)`. The landing is a pure function `landed(before, after)` in `GameSounds.kt`, so it can be unit tested.
- `placementSound(Rejected, state)` returns null while `state.replacement != null`, so taps during the sequence are silent.

### 6. App: input lock

`GameScreen` derives `val replacing = state.replacement != null` and passes it on as follows:
- `StackPanel(enabled = … && !replacing)`, so the next section can't be picked up,
- the `PlayingField` drag highlight and drop: no tile accepts, because `canPlace` is already false through the lock,
- `PlayingField` taps: the tap goes through `placeWithSound`, which is rejected silently (decision 5),
- the `FastForwardButton` is shown disabled.

The pause button and the system back button are unchanged.

### 7. App: animation driven by the game clock

The animation progress is `(clockMs - startedAtMs) / (landsAtMs - startedAtMs)`, which freezes on pause for free.
- **Break phase**, the first `min(600 ms, 40 %)` of the sequence: the `removed` section is drawn with the existing pipe-drawing code and then split into 4–6 fragments. They drift outward, rotate and fade, with an ease-out curve. The fragments are procedural and seeded by the tile position, so they look the same every frame and in screenshot tests.
- **Busy phase**, until landing: a thin ring on the tile that fills clockwise, in the design-system accent colour, plus a faint tile tint. It tells the player the wait is temporary.
- **Landing**: when the section is written to the board, the normal cell drawing takes over with a short scale-in pop (about 150 ms, game clock), keyed on the tile and the landing time.

Colours and durations come from `docs/design-system.md`. The break duration constant lives in the app, because it is presentation. The total length comes from the level.

### 8. Default 3000 ms in `LevelDefaults`

`LevelDefaults.REPLACE_PENALTY_MS = 3000`. No bundled level sets `replacePenaltyMs` explicitly, so every level picks up the new default. Levels can still set any value of 0 or more. The docs (`level-format.md`, `project-plan.md`) are updated with it.

## Risks / Trade-offs

- [3 s with the liquid flowing may feel harsh on fast levels and could make the liquid leak at the tile] → This is intended: it is the penalty. Levels can lower `replacePenaltyMs`, and the busy ring makes the cost visible. It can be tuned after playtesting.
- [Splitting the tick adds a branch to the hottest code path] → It only runs when a replacement lands, which happens at most once per sequence. The tests run the same scenario with 1 ms ticks and one long tick and compare the results.
- [The landing sound is derived by diffing states and could double-fire after recomposition] → The diff runs inside `GameSession.send`, which runs once per command, not in composition. A unit test checks that one sequence gives exactly one landing event.
- [Existing tests assume an instant swap or a 500 ms default] → Update `GameEngineTest`, `PlacementTest`, `LevelParserTest`, `GameSessionPlaceTest`, `HeadlessPlayTest` and `PlayingFieldTest` along with the change. Tests that don't test replacing keep explicit penalties where they rely on them.

## Migration Plan

This is a gameplay-only change with no persisted data. Level history and scores are unaffected. To roll it back, revert the change, which restores the instant swap and the 500 ms default.

## Open Questions

- Should the stack visibly show the "in flight" section during the sequence, for example a ghost on the tile? It is left out for now. The busy ring is enough feedback. It can be revisited after playtesting.
