## Context

This change builds on three open changes and must merge after them:
- `app-game-play-stack`: `GameScreen`, `StackPanel`, `SectionDragState` and `PipeSection.openSides`.
- `app-game-play-hud`: `GameSession` with `elapsedMs` (the game clock, stopped while paused), `paused`, `restart()`, the frame loop that calls `tick(dt)`, and `SectionDragState.cancel()` plus an `enabled` flag on `StackPanel`.
- `app-game-play-playing-field`: `Board` and `Cell` in `core.board` (`Cell.Pipe(section, liquidEntered)`), `Placement.check` and `GameSession.place` / `canPlace`, and `PlayingField` in the `Row` next to the stack.

`app-game-play-points` is also open. It does not touch the same code paths, except that its popups run on `elapsedMs`. The two changes can merge in either order.

There is no flow simulation yet. The plan (section 2.5) describes a single liquid head that moves by `progress += dt / 1000 * cellsPerSecond * speedFactor * rampFactor`, and a `Waiting(remainingMs)` state for the countdown. Channels are not in `core` yet: the plan lists `Channel` and `channelFor` for the flow.

Because each section has fixed channels, the path from the start is deterministic: every entry side leads to exactly one exit side. So "is there a complete route" is a single walk from the start, not a search.

## Goals / Non-Goals

**Goals:**
- A pure route tracer in `core` that uses the same channel rule the flow will use, unit tested on the JVM.
- One place in `GameSession` that decides whether the button shows, whether fast forward is on, and what the flow's time scale is.
- A clear contract for the flow change: multiply the flow's time step by `flowTimeScale`.
- A fast forward button in the bottom right corner that matches the cartoon style.

**Non-Goals:**
- The flow simulation. Tests check `flowTimeScale` and the button, not a moving liquid.
- A time bonus for fast forward or for starting the flow early.
- Other speeds than 10x and normal speed.
- Showing the complete route on the field (for example a glow along it).
- Showing the 10x speed in the HUD. The HUD keeps showing the base flow speed.

## Decisions

**1. `PipeSection.exitFor(entry: Side): Side?` in `core.level`.**
```kotlin
fun PipeSection.exitFor(entry: Side): Side? = when {
    entry !in openSides -> null
    this == X -> entry.opposite()
    else -> (openSides - entry).single()
}
```
`RES` is `LEFT + RIGHT` in `openSides`, so a reservoir from the top returns `null` without a special case. The cross's channel is identified by its axis (`entry` horizontal or vertical). The flow change reuses this function, so the route check and the real flow cannot disagree. *Other option:* add the plan's `Channel` type now. It was not chosen because the flow change owns the fill state per channel and can introduce `Channel` together with it. `exitFor` then becomes `channelFor(entry).exitFor(entry)` without changing callers.

**2. `RouteTracer.trace(board, level): Route` in `core.route`.**
```kotlin
sealed interface Route {
    data class Complete(val cells: List<Position>) : Route
    data class Open(val endsAt: Position, val reason: Reason) : Route  // EMPTY, BLOCKED, NO_OPENING, WRONG_SIDE, OFF_GRID, LOOP
}
```
The walk goes like this:
- Start at `level.start.position`, going out of `level.start.openSide`.
- On each step, `next = current.neighbour(exit)` and `entry = exit.opposite()`. Check the next cell in this order: outside the grid → `OFF_GRID`; `Finish` → `Complete` if `entry == finish.openSide`, else `WRONG_SIDE`; `Empty` → `EMPTY`; `Blocked` or `Start` → `BLOCKED`; `Pipe` → `section.exitFor(entry)`, where `null` → `NO_OPENING`.
- A `visited: Set<Pair<Position, Axis>>` stops the walk with `LOOP` when a channel is used twice. The axis is horizontal or vertical, so a cross can be passed once on each axis.

The walk is at most `2 × columns × rows` steps (384 on 16 × 12), so it runs on every placement without any caching. `liquidEntered` is ignored. `Open.reason` is only for tests and debugging.

**3. `GameSession` owns the fast forward state.**
- `routeComplete: Boolean` is recomputed at the end of every accepted `place(...)`: `RouteTracer.trace(board, level) is Route.Complete`. `restart()` sets it to `false`.
- `fastForward: Boolean`. `startFastForward()` sets it only when `routeComplete && !paused && !fastForward`. `stopFastForward()` clears it only when `!paused`. `toggleFastForward()`, which the button calls, does one or the other. Every call is safe while paused and does nothing then.
- `flowTimeScale: Int get() = if (fastForward) FAST_FORWARD_FACTOR else 1`, with `FAST_FORWARD_FACTOR = 10`.
- `showFastForward: Boolean get() = routeComplete || fastForward`. While on, the button stays even if something odd happens to the route, which it cannot, because placing is locked. After turning it off, the route is still complete, so the button shows as the fast forward button again.
- `place(...)` and `canPlace(...)` return rejected while `fastForward` is true. This is checked in `GameSession`, next to the pause check, and not in `Placement.check`, because it is session state, not a board rule. Once fast forward is off again, placing works as before, so a player who slows down can keep building; breaking the route hides the button as usual.
- `restart()` sets `fastForward = false`.

The contract for the flow change: the flow runs on `flowDtMs = dtMs * session.flowTimeScale` for both the countdown (`Waiting.remainingMs -= flowDtMs`) and the head (`progress += flowDtMs / 1000 * …`). Everything else keeps `dtMs`: `elapsedMs`, the replace lock and the points popups. So the popups keep their normal 1 second and fast forward does not make them flash. At 10x, a speed of 2 cells per second becomes 20 cells per second, and one 16 ms frame moves the head about a third of a cell. The flow step must therefore loop while `progress >= 1.0`, which the plan's overshoot carry already needs.

When `GameEngine` arrives, `fastForward` moves into `GameState` with a `FastForward` command, and `Tick` scales the flow's delta itself.

**4. Turning fast forward on cancels a drag and disables the stack.**
`GameScreen` calls `session.toggleFastForward()` and then, when fast forward is now on, `dragState.cancel()`. `StackPanel`'s `enabled` becomes `!session.paused && !session.fastForward`. A drag cannot really be going on when the button is tapped, because the button is a separate touch target. But a second finger can tap the button during a drag, and cancelling covers that case. The field's pan and zoom keep their own `enabled` flag, which only looks at the section drag and pause, so viewing still works.

**5. `FastForwardButton` is an overlay aligned to the bottom right.**
`GameScreen`'s root becomes a `Box`. The existing `Column { GameHud; Row { StackPanel; PlayingField } }` fills it, and `FastForwardButton` sits on top with `Modifier.align(Alignment.BottomEnd).padding(16.dp)`. That puts it at the bottom right of the screen, which is also the bottom right of the field, because the field reaches both edges.
- `AnimatedVisibility(visible = session.showFastForward, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut())`, about 200 ms.
- The button is a 56 dp circular `CartoonButton`-style surface with a 3 px ink border. Off, it is the brass fast forward button with the `ic_fast_forward.xml` vector (two triangles) and a gentle pulse, using `rememberInfiniteTransition` scale 1.0 → 1.06, so the player notices it. On, it is the slow button: filled liquid blue, the `ic_slow.xml` vector (one triangle, normal play), and no pulse.
- Semantics: off, `contentDescription = "Fast forward"`; on, `contentDescription = "Normal speed"` and `stateDescription = "Fast forward on"`. Test tag `fast_forward` in both states.
- `onClick = { session.toggleFastForward(); if (session.fastForward) dragState.cancel() }`. While paused, the pause dialog covers the screen, and `toggleFastForward` ignores the call anyway.

The button covers the field's bottom right corner. It sits above the field's pointer input, so touches on it do not pan the field. The player can pan a covered tile into view. *Other option:* put the button in the HUD. It was not chosen because the user asked for the bottom right, and the HUD's right side is taken by the score and the cog wheel.

**6. No change to the placement highlight code.**
`canPlace` returns false while fast forward is on, and the drag cannot start, so the highlight from the playing-field change never shows. Nothing is added to `PlayingField`.

## Risks / Trade-offs

- [This change edits `GameSession`, `GameScreen` and `StackPanel` from unmerged changes] → Merge `app-game-play-stack`, `app-game-play-hud` and `app-game-play-playing-field` first. The first task checks this.
- [The button can be seen and tapped before the flow exists, but does nothing visible] → Accepted for this change. Tests check `flowTimeScale`. The flow change adds a test that plays a complete route at 10x and checks that the win comes 10 times sooner in flow time.
- [The route tracer and the flow could disagree on the rules] → Both use `PipeSection.exitFor`. The flow change adds a test that every `Route.Complete` board is won by the flow.
- [At 10x the head can cross more than one cell per frame and skip events] → The contract in decision 3 requires the flow step to loop over whole cells within one step. The flow change tests a large delta.
- [Locking placement surprises a player who wants to keep building for points] → Only sections on the route score, and the route is already complete, so placing more sections off the route gains nothing. Replacing a section on the route could only break it.
- [The button covers the bottom right tile of the field] → Only while a complete route exists, when nothing is placed anymore after fast forward. Before the tap, the player can pan the tile out from under the button.

## Migration Plan

Nothing is stored or migrated. To roll back, revert the change: the button is gone, and `flowTimeScale` does not exist for the flow change to read.

## Open Questions

- Should the complete route glow briefly when it is found, so the player sees why the button appeared? Left to the polish milestone.
- Should fast forward give a small time bonus, like the plan's "start flow now"? Not for now, because points come only from sections the liquid passes.
- Should the HUD show "×10" next to the flow speed while fast forward is on? Not for now. The active button already shows it.
