## Context

Six open changes build the game screen piece by piece. Each one added state to `GameSession`, a Compose state holder in `app/.../ui/game/`, and left the rest to a future engine or flow change:

- `app-game-play-stack`: `PipeStack` and `WeightedPool` in `core.stack`, `StackPanel`, `SectionDragState` and the `SectionDropTarget` contract (`tryPlace`, later `canPlace`).
- `app-game-play-hud`: `GameSession` with `paused`, `pauseMenuOpen`, `elapsedMs`, `score`, `flowSpeed`, `tick`, `pause`, `resume`, `restart`, the `withFrameNanos` frame loop, `GameHud` and `PauseDialog`.
- `app-game-play-playing-field`: immutable `Board` and `Cell` in `core.board` (`Cell.Pipe(section, liquidEntered)`), `Placement.check(board, rules, at, nowMs, lockUntilMs)`, `PlayingField` and `FieldViewport`.
- `app-game-play-points`: `Points` and the immutable `ScoreSheet` in `core.score`, `GameSession.liquidExited(at)`, popups with `PopupTiming.TOTAL_MS`.
- `app-game-play-fast-forward`: `PipeSection.exitFor(entry)`, `RouteTracer` in `core.route`, `routeComplete`, `fastForward` and `flowTimeScale` in `GameSession`.
- `app-game-play-level-lost`: `FlowStep.after(board, from, exit, usedChannels)` and `ChannelUse` in `core.flow`, `Outcome` (`Playing`, `Lost(at, side, atMs)`) and `liquidLeaked` in `GameSession`, `LevelLostDialog`.

Each of those designs says that `GameEngine` will take over this state later, with the plan's pure `handle(state, command)` (section 2.6), and that the flow change will call `liquidExited`, `FlowStep.after` and `liquidLeaked`. No flow change exists. This change is both: the engine, and the flow that runs inside it. `app-game-play-level-complete` (still empty) will show the win dialog from the outcome this engine reports.

## Goals / Non-Goals

**Goals:**
- One immutable `GameState` in `core` that holds everything about the attempt, and one `GameEngine` that changes it only through commands.
- The flow simulation (countdown, head, speed, ramp, reservoir, fast forward, points, win, leak) inside the engine's tick, built from the rules the open changes already define.
- `GameSession` reduced to a holder of the current state, so the views render from `GameState` alone.
- A full reset of the game and of every view on restart and on starting a level.
- A headless JUnit test that plays a level from start to win, and one to a leak.

**Non-Goals:**
- The level complete dialog, stars on a win and recording history. The engine only reports `Outcome.Won`.
- The liquid drawing inside the pipes (`LiquidDrawer`). The playing field gets the head position and filled channels from the state; how they look is a separate change.
- A "start flow now" button and the time bonus for starting early (plan sections 1.8 and 1.9).
- Keeping an attempt over process death, saving games, or undo.
- A navigation library or an AndroidX `ViewModel`.

## Decisions

**1. The engine is a pure reducer in `core.engine`.**
```kotlin
class GameEngine(private val random: Random) {
    fun start(level: Level, previous: GameState? = null): GameState
    fun handle(state: GameState, command: GameCommand): GameState
    fun canPlace(state: GameState, at: Position): Boolean
}

sealed interface GameCommand {
    data class Start(val level: Level) : GameCommand
    data class Place(val at: Position) : GameCommand
    data object Pause : GameCommand
    data object Resume : GameCommand
    data object Restart : GameCommand
    data object FastForward : GameCommand
    data class Tick(val dtMs: Long) : GameCommand
}
```
`handle` returns the same instance when a command is refused, so "accepted" is `new !== old`. The only impure input is the random source for the stack, which the tests pass in with a fixed seed. *Other option:* a mutable engine object with methods. It was not chosen because an immutable state is what `Board`, `PipeStack` and `ScoreSheet` already are, it makes "restart resets everything" a single assignment, and every rule can be tested as input and output.

**2. `GameState` holds the attempt, and derived values are computed.**
```kotlin
data class GameState(
    val attempt: Int,
    val level: Level,
    val board: Board,
    val stack: PipeStack,
    val flow: FlowState,
    val sheet: ScoreSheet,
    val awards: List<Award>,          // Award(position, points, atMs), recent only
    val replaceLockUntilMs: Long,
    val clockMs: Long,
    val paused: Boolean,
    val fastForward: Boolean,
    val routeComplete: Boolean,
    val outcome: Outcome,
)
```
Derived, not stored: `score = sheet.total`, `stars = level.stars.starsFor(score)`, `flowSpeed` (base speed with the ramp, for the HUD), `flowTimeScale` (10 or 1), `isStopped = paused || outcome !is Playing`. `Outcome` moves from `app` to `core.engine`: `Playing`, `Won(atMs)`, `Lost(at, side, atMs)`. *Other option:* keep score and stars as fields. Not chosen for the same reason as the HUD design: a derived value cannot drift.

**3. `FlowState` follows plan section 2.5, on the `Side` model.**
```kotlin
sealed interface FlowState {
    data class Waiting(val remainingMs: Long) : FlowState
    data class Flowing(
        val position: Position, val entry: Side, val channel: Channel,
        val progress: Double, val sectionsLeft: Int, val used: Set<ChannelUse>,
    ) : FlowState
    data object Ended : FlowState
}
```
`Channel` is the one from `core.flow` (horizontal or vertical for a cross, the single channel otherwise). `used` is the `usedChannels` set that `FlowStep` already takes, so the cross rule is not written twice. `sectionsLeft` drives the ramp. The start tile gets a synthetic channel from the side opposite its open side to the open side, as in the plan, so the liquid visibly crosses it. When the head enters a pipe, the board is updated with `markLiquidEntered`, which makes the placement rule refuse it at once (spec: "entered as soon as liquid reaches it").

**4. `FlowSimulation.advance` walks tile borders inside one tick.**
`FlowSimulation.advance(state, flowDtMs): GameState` is a loop:
1. `Waiting`: subtract from the countdown. At 0, switch to `Flowing` at the start and keep the remainder.
2. `Flowing`: `speed = level.flow.cellsPerSecond * (1 + speedRampPerCell * sectionsLeft) * (reservoirSpeedFactor if RES)`. The time to the border is `(1 - progress) / speed`. If the remaining time is shorter, add to `progress` and stop.
3. Otherwise use up that time and leave the tile: add the channel to `used`, call `sheet.award(position, section)` (skipped for the start), append an `Award` at `clockMs`, then `FlowStep.after(board, position, exit, used)`: `Enter` → new `Flowing` with progress 0 and loop; `Finish` → `Outcome.Won`, flow `Ended`; `Leak` → `Outcome.Lost`, flow `Ended`. Remaining time after an end is dropped.

Awards use the tick's `clockMs` (after it is advanced), so all awards from one long tick start their popup together; fast forward keeps its real time for the popups. The order of steps is the contract the points and level-lost designs wrote for the flow change, so their "last section still scores" rule holds. *Other option:* one step per tick, capped at one tile. Not chosen: at 10x speed and a slow frame the head would lag behind real time, and the fast-forward design already asks the flow to loop over whole cells.

**5. Tick order.**
`Tick(dt)`: refused if `dt < 0` or `paused`. Otherwise `clockMs += dt`, drop awards older than `Award.VISIBLE_MS` (1,000 ms, which `PopupTiming.TOTAL_MS` in `app` now refers to), and, only while `outcome is Playing`, `FlowSimulation.advance(state, dt * flowTimeScale)`. After a win or a loss the clock keeps running for the leak effect and a future win effect.

**6. Placement reuses `Placement.check` and adds the session rules around it.**
`canPlace(state, at)` = `!isStopped && !fastForward && Placement.check(board, level.rules, at, clockMs, replaceLockUntilMs)` is `Place` or `Replace`. `Place(at)` runs the same check and on success puts `stack.next` on the tile, `stack.afterPlacement(pool, random)`, sets `replaceLockUntilMs = clockMs + replacePenaltyMs` after a `Replace`, and sets `routeComplete = RouteTracer.trace(board, level) is Route.Complete`. `canPlace` and `handle(Place)` share one private function, so the highlight and the drop cannot disagree.

**7. Start and restart build a new state from scratch.**
`start(level, previous)` is the only constructor path: `Board.from(level)`, `PipeStack.create(level.stack, random)`, `Waiting(level.flow.initialDelayMs)`, `ScoreSheet()`, empty awards, clock 0, `attempt = (previous?.attempt ?: 0) + 1`. `Restart` is `start(state.level, state)` and `Start(level)` is `start(level, state)`. There is no field-by-field reset to forget. *Other option:* a `reset()` that clears fields. Not chosen: each new field would need a line in it, which is how restarts get bugs.

**8. `GameSession` becomes a thin holder.**
`GameSession(level, engine)` keeps `var state: GameState by mutableStateOf(engine.start(level))` and offers `send(command): Boolean` (true when the state changed) and `canPlace(at)`. Everything else it had (`score`, `board`, `paused`, `pauseMenuOpen`, `outcome`, `routeComplete`, `fastForward`, `flowTimeScale`, `liquidExited`, `liquidLeaked`) is removed; the views read those values from `session.state`. `showPauseDialog = state.paused && state.outcome is Playing`, `showLostDialog = outcome is Lost && clockMs - atMs >= LeakTiming.EFFECT_MS`. The level-lost `skipLeakEffect()` becomes `send(Tick(remainingEffectMs))`, which after a loss only moves the clock. The frame loop stays as in the HUD design: `LaunchedEffect(state.paused)` with `withFrameNanos`, first delta 0 after a restart of the loop, calling `send(Tick(dt))`. *Other option:* an AndroidX `ViewModel` with a `StateFlow`. Not chosen yet, for the reason in the HUD design: without a back stack, an activity-scoped ViewModel outlives the level. A `mutableStateOf` holder gives Compose the same per-frame recomposition.

**9. Views reset by keying their screen state on the attempt.**
In `GameScreen`, the stack column and the playing field sit inside `key(state.attempt) { … }`. Everything they `remember` (the `FieldViewport` zoom and offset, the stack slide animation, the leak and popup animation state) is created again for a new attempt, so the field returns to its starting fit and the stack does not animate from the old sections. `LaunchedEffect(state.attempt) { dragState.cancel() }` ends a drag. The dialogs close by themselves because they are derived from `paused` and `outcome`. The HUD needs nothing: it only shows `score`, `stars` and `flowSpeed`, which are fresh in the new state. *Other option:* an explicit "reset" event that every view listens to. Not chosen: a key is one line and cannot be forgotten by a new view placed inside it.

**10. Views read, commands write.**
- `StackPanel(stack = state.stack, enabled = !state.isStopped && !state.fastForward)`.
- `PlayingField(board, flow, awards, outcome, clockMs, …)`; the drop target is `canPlace = session::canPlace`, `tryPlace = { session.send(Place(it)) }`.
- `GameHud(level.id, flowSpeed, stars, score, onPause = { send(Pause) })`.
- `FastForwardButton(visible = routeComplete && !fastForward && outcome is Playing, onClick = { send(FastForward) })`.
- `PauseDialog` sends `Resume` and `Restart`; `LevelLostDialog` sends `Restart`; Exit stays the `onExit` callback.
- `BackHandler` and `LifecycleEventEffect(ON_STOP)` send `Pause` (or skip the leak effect when lost), as before.

## Risks / Trade-offs

- [This change rewrites `GameSession` and touches `GameScreen` from six unmerged changes] → All six must be in `development` first; the first task checks this. Their tests that call `liquidExited` or `liquidLeaked` are moved to engine tests that reach the same state with ticks.
- [A new `GameState` every frame allocates and recomposes the whole screen] → The state is a few small immutable objects and the board shares its unchanged map. Views read only the fields they need, and `PlayingField` reads the clock-driven values inside its draw lambda, as the points and level-lost designs already do. A unit test checks that a tick on a paused state returns the same instance.
- [The flow and `RouteTracer` could disagree] → Both use `PipeSection.exitFor` and `FlowStep`. A test runs every scripted complete-route board through the engine to a win.
- [Floating point progress lands just short of 1 and a border is missed] → The loop compares the remaining time with the time to the border, not the progress with 1, and treats a difference below 1e-9 as reaching the border. A test ticks 1 ms at a time through a route and checks the same outcome and score as one long tick.
- [The random source makes `handle` not strictly pure] → The engine owns the `Random`; tests use a seeded one and the "same commands give the same state" test uses two engines with the same seed.
- [`key(attempt)` also resets animations that could have continued] → That is the requested behaviour: a new attempt starts clean.

## Migration Plan

Nothing is stored or migrated. To roll back, revert the change: `GameSession` goes back to holding its own fields and the liquid stops moving again.

## Open Questions

- Should the HUD show the countdown before the flow starts? The state has it (`FlowState.Waiting.remainingMs`); showing it is a small follow-up in the HUD.
- Should the "start flow now" command from the plan come back? It fits as one more command that sets the countdown to 0, but it is not requested.
- Should the flow speed in the HUD also slow down inside a reservoir? The spec says no, following the HUD design's suggestion, so the indicator stays steady.
