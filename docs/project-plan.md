# Leak Out – Project Plan

A 2D pipe-building puzzle game for Android. The player connects a start pipe to a finish pipe on a grid using pipe sections drawn from a stack, before the liquid that starts flowing after a short delay leaks out.

This document is the working plan for building the game from an empty repository to a releasable first version. It is split into three parts:

1. **Game design specification** – the rules, precisely enough to implement and test.
2. **Technical design** – stack, architecture, data model, and the flow simulation.
3. **Roadmap** – milestones with concrete tasks, in build order.

---

## 1. Game design specification

### 1.1 Core loop

1. A level starts. The board shows a grid, a **start** section with one open end and a **finish** section with one open end.
2. The player sees a **stack** of upcoming pipe sections. Only the bottom section (the next section) can be placed.
3. The player drags the next section from the bottom of the stack onto a grid cell. The stack shifts down and a new random section enters at the top.
4. After a configured **initial delay**, liquid starts flowing out of the start section at the configured **flow speed**.
5. The liquid follows the pipes. If it reaches the finish section the level is **won**. If it reaches the end of a pipe with nothing valid to flow into, it **leaks** and the level is **lost**.
6. On a win the next level unlocks. On a loss the player can retry the same level.

### 1.2 The board

- Rectangular grid of `columns × rows` cells. The app runs in landscape only (see 1.11), so the board layout must fit a landscape screen; revisit the default 7 × 9 when the game screen is built. Configurable per level.
- Each cell is one of:
  - **Empty** – a pipe can be placed here.
  - **Pipe** – holds one pipe section and its fill state.
  - **Start** – fixed, cannot be replaced. Has exactly one open port (the direction the liquid exits).
  - **Finish** – fixed, cannot be replaced. Has exactly one open port (the direction liquid must enter from).
  - **Blocked** (optional, later levels) – a wall; nothing can be placed here.
- Start and finish are placed by the level definition. They may sit anywhere on the grid, not only on the edge, as long as their open port points at a cell inside the grid.
- On screen, cells are always between 40 dp and 120 dp. A level that fits at that size is shown completely and centred. A larger level (the bundled levels go up to 16 × 12) starts at 40 dp around the start cell; the player pans with one finger and pinch-zooms with two, but can't zoom out further than the size at which the whole level fits. The board never rotates.

### 1.3 Pipe sections

A pipe section is defined by its **ports** (which of the four sides are open) and its **channels** (which ports are connected to each other). All sections are two-way: liquid enters through any port of a channel and exits through the other port of that channel.

| Id | Name | Ports | Channels | Notes |
|----|------|-------|----------|-------|
| `H` | Left – Right | L, R | L↔R | Straight, horizontal |
| `V` | Top – Bottom | T, B | T↔B | Straight, vertical |
| `LT` | Left – Top | L, T | L↔T | Corner |
| `LB` | Left – Bottom | L, B | L↔B | Corner |
| `RT` | Right – Top | R, T | R↔T | Corner |
| `RB` | Right – Bottom | R, B | R↔B | Corner |
| `X` | Cross | L, R, T, B | L↔R **and** T↔B | Two independent channels that do not mix |
| `RES` | Reservoir | L, R | L↔R | Horizontal only, slow flow, see 1.5 |

Rules that follow from this:

- A section has no rotation state. The six straight/corner sections already cover every orientation.
- Liquid entering a section through a side that is **not** an open port is a leak (the previous section's exit hits a wall). Example: liquid exits a cell to the right into a cell holding `V`. `V` has no left port, so the liquid leaks.
- Liquid exiting a section into an **Empty** cell, a **Blocked** cell, or off the grid is a leak.
- Liquid exiting into the **Finish** cell is a win only if it enters through the finish's open port. Hitting the finish from any other side is a leak.

### 1.4 The cross section

- Holds two channels: horizontal (L↔R) and vertical (T↔B).
- When liquid enters from the left or right it fills only the horizontal channel and exits on the opposite side. The vertical channel stays available.
- If the liquid later comes back through the top or bottom, it fills the vertical channel and passes through. Both channels of one cross section can therefore be used in a single level.
- A channel that has already been filled cannot be entered again (that would mean the path loops back on itself, which is a leak at the previous section's exit).
- Score bonus when both channels of the same cross are filled (optional, section 1.9).

### 1.5 The reservoir

- A horizontal section with two open ports, left and right. Liquid entering from the left fills the tank and exits on the right, or the other way around. It has no top or bottom ports, so liquid arriving from above or below leaks.
- Its flow speed is multiplied by the level's `reservoirSpeedFactor` (for example `0.3`), so it takes roughly three times longer to pass. This is the "buy time" mechanic.
- Because it is horizontal only, placing it well means planning a left-right run into the route. That is a deliberate constraint: the delay has to be earned.

### 1.6 The stack

- A visible queue of `visibleSize` sections (default 5), ordered from top to bottom. The **bottom** section is the next section and is the only one that can be placed.
- After every accepted placement the queue shifts down by one and a new section is drawn at random from the level's **pool** and inserted at the top.
- The pool is a weighted set. Straight and corner sections (`H`, `V`, `LT`, `LB`, `RT`, `RB`) always have a non-zero weight. `X` and `RES` are optional and may have weight zero.
- Random draws use an injectable random source so tests can be deterministic. Levels do not store a seed, so normal attempts get a fresh stack order.

### 1.7 Placing sections

- Dragging the next section onto an **Empty** cell places it there. Tap-to-place is not supported.
- While dragging, the cell under the dragged section is highlighted when the drop would be accepted there. Cells that refuse the section are not highlighted. The highlight and the drop use the same rule (`Placement.check` in `core.board`), so they always agree.
- Dropping on a cell that holds an **unfilled** pipe **replaces** it, if the level allows replacing (`allowReplace`, default `true`). Replacing costs a small time penalty (`replacePenaltyMs`, default 500 ms) during which no new placement is accepted, to stop the player from spamming replacements. This is the classic Pipe Mania rule and keeps the stack from becoming a dead end.
- Cells that are Start, Finish, Blocked, or contain a pipe that is filled or currently being filled cannot be changed.
- There is no discard action; the only way to get rid of an unwanted section is to place it somewhere.

### 1.8 Timing and flow speed

All timing lives in the level definition:

| Field | Meaning | Typical range |
|-------|---------|---------------|
| `initialDelayMs` | Time before liquid leaves the start section | 8 000 – 20 000 |
| `cellsPerSecond` | Base speed: how many cell lengths the liquid front advances per second | 0.4 – 2.0 |
| `reservoirSpeedFactor` | Multiplier applied while inside a reservoir | 0.2 – 0.5 |
| `speedRampPerCell` | Optional: speed increase per filled cell, makes long levels harder | 0 – 0.05 |

A **countdown** for the initial delay is shown on screen. Optionally the player can tap a "start flow" button to skip the wait for a time bonus.

**Fast forward**: after every placement the game checks whether the placed sections form a complete route from the start to the finish, following the liquid's rules (`core.route.RouteTracer`, using `PipeSection.exitFor`). Once they do, a fast forward button appears. Tapping it runs the countdown and the flow at 10x (`GameSession.flowTimeScale`) and locks placing, so the route cannot be broken. The button then becomes a slow button: tapping it restores the level's normal speed and unlocks placing again. Pause and resume keep the current speed; restart turns fast forward off. Points and the result are the same at either speed.

### 1.9 Win, lose, and score

- **Win**: liquid enters the finish section through its open port, after the points for the last section are awarded. A win stops the game the same way a leak does (no flow, countdown, score, placing, dragging, pause or fast forward; pan and zoom still work). The finish flashes a success-green ring for 1 second (a fill animation comes with the liquid drawing), then the level complete dialog appears over the dimmed screen: "Level complete!", "You kept the water flowing.", three stars, the score rows and **Next level**, **Restart** and **Exit** to the main menu.
  - **Final score** (`core.score.FinalScore`): the earned points shown in the HUD, minus **3 points for every placed section the liquid never entered** (a cross used through one channel counts as used), never below 0. The stars come from the level's thresholds applied to that total.
  - **Tally**, on the game clock (`ui.game.WinTally`): the earned points show at once; after 0.5 s the penalty counts up in steps of 3 while the unused sections disappear from the field one by one, in row-major order (120 ms a step, at most 2 s in all); the total counts up from 0 in 0.8 s; then the earned stars light up in brass one by one, 300 ms apart, each popping to 1.35× and back.
  - The buttons show from the start and are enabled when the tally is done; Next level stays disabled on the last level. Tapping outside does nothing, Back does nothing during the flash and the tally and works as Exit after it, and going to the background skips to the finished dialog instead of the pause dialog.
- **Lose**: any leak (`core.flow.FlowStep`): the liquid leaves a tile into an empty, blocked or start tile, over the grid edge, into a section without a matching opening, into the finish from the wrong side, or into a cross channel it already used. The start is checked the same way when the flow starts. A leak stops the game at once: no flow, countdown, score, placing, dragging (a drag in progress goes back to its slot), pause or fast forward; pan and zoom still work. Liquid spurts from the leaking side for 1 second, then the level lost dialog appears over the dimmed screen with "Leaked!", "The pipe is leaking. Level lost.", the score, **Restart** and **Exit** to the main menu. Tapping outside does nothing, Back works as Exit (and does nothing during the spurt), and going to the background shows the level lost dialog on return instead of the pause dialog.
- **Score**: points are awarded for a placed section at the moment the liquid **exits** it (`core.score.Points` and `ScoreSheet`):
  - Straight (`H`, `V`): +10.
  - Corner (`LT`, `LB`, `RT`, `RB`): +15.
  - Reservoir (`RES`): +25.
  - Cross (`X`): +10 for the first pass and +50 for the second pass on the same tile, whichever channel comes first.
  - The start and finish tiles give nothing, and neither does a section the liquid never reaches.
  - The last section before a leak still scores: the liquid did run through it.
  - Restart sets the score back to 0 and forgets the cross passes.
  - Time bonus (not built yet): remaining initial-delay seconds × 5 if the player starts the flow early.
- Stars per level (1–3) based on score thresholds in the level definition. Stars are optional for the first release.

### 1.10 Level progression

- Levels are numbered and unlocked in order. Level `n+1` unlocks when level `n` is won.
- A level select screen shows locked/unlocked/completed status and stars.
- Levels are stored as JSON files bundled in the app (`assets/levels/level_001.json`, …). Progress is stored on device.

Difficulty grows through the level definitions, not through code: bigger grids, further apart start/finish, shorter initial delay, faster flow, blocked cells, fewer reservoirs.

### 1.11 Screens

All screens are landscape only: `MainActivity` uses `android:screenOrientation="sensorLandscape"`, so the app never shows portrait and follows the sensor between both landscape sides.

1. **Title** – play, level select, settings (sound on/off).
2. **Level select** – grid of level tiles.
3. **Game** – a HUD bar across the top (level, flow speed, three stars earned so far, score, cog wheel pause button), with the stack and the board below it. Once the route is complete, a round fast forward / slow button sits in the bottom right corner, 16 dp from the edges. Back and going to the background also pause the game.
4. **Pause dialog** – opened from the cog wheel: Resume, Restart, a separator and Exit to the main menu (welcome screen). Tapping outside does nothing; Back works as Resume.
5. **Level complete / failed overlay** – level complete (the level complete dialog, see 1.9): "Level complete!", the stars, pipe points, unused pipes and total, and Next level, Restart and Exit to the main menu. Level failed (the level lost dialog, see 1.9): "Leaked!", the score, Restart and Exit to the main menu.
6. **Settings** – opened from the welcome screen's Settings button; Back returns to the welcome screen. Two columns: Audio (sound effects and music switches with volume sliders, disabled with "Coming soon" until audio lands) and Feel (vibration) on the left, Progress (finished levels and stars, and Reset progress with a confirm dialog) and About (name, version, maker, privacy line) on the right.

---

## 2. Technical design

### 2.1 Stack

| Concern | Choice | Why |
|---------|--------|-----|
| Language | Kotlin 2.x | Native Android, modern, coroutine support |
| UI | Jetpack Compose (Material 3) | Menus and overlays with little code; Canvas for the board |
| Board rendering | Compose `Canvas` with a frame-clock driven loop | Simple grid game; no need for a game engine. Can swap to a `SurfaceView` later if profiling shows the need |
| Game loop | `withFrameNanos` inside a `LaunchedEffect` | Frame-synced delta time, pauses with the composable |
| Serialization | `kotlinx.serialization` (JSON) | Level files |
| Persistence | Room (SQLite) for level progress; Jetpack DataStore (Preferences) for settings | Progress is per-level rows updated conditionally; settings are a few flags |
| Audio | `SoundPool` for effects, `MediaPlayer` for music | Low-latency, built in |
| DI | None initially; manual construction in a small `AppContainer` | Project is small; add Hilt only if it grows |
| Tests | JUnit 5 + kotlin.test for `core`; Compose UI tests for screens | Core logic must be testable without a device |
| Build | Gradle Kotlin DSL, version catalog (`libs.versions.toml`), AGP 8.x | Standard |
| Min SDK | 26 (Android 8.0) | Covers nearly all active devices; keeps APIs modern |
| Target SDK | Latest stable | Play Store requirement |

No third-party game engine (libGDX, Unity, Godot) is used. The rule set is small and a hand-written simulation is easier to test and reason about.

### 2.2 Module layout

**The app is written in the `src/` directory of the repository.** `src/` is the Gradle root: the settings file, the Gradle wrapper, the version catalog and both modules live there. Run all Gradle commands from `src/`. Paths such as `core/…` or `app/…` elsewhere in the docs and specs are relative to `src/`.

```
leak-out/
├── docs/                           this plan, design notes, level design sheets
├── openspec/                       specs and change proposals
└── src/                            the app – Gradle root
    ├── settings.gradle.kts, build.gradle.kts, gradlew, gradlew.bat
    ├── gradle/libs.versions.toml
    ├── core/                       pure Kotlin (JVM) module – no Android dependencies
    │   └── src/main/kotlin/nl/hexmaster/leakout/core/
    │       ├── model/              Direction, PipeSection, Cell, Board, Level
    │       ├── stack/              PipeStack, WeightedPool
    │       ├── flow/               FlowStep, Channel, ChannelUse
    │       ├── engine/             GameEngine (ties board + stack + flow), GameState, GameCommand, FlowSimulation, FlowState
    │       └── level/              LevelDefinition (serializable), LevelParser, LevelValidator
    └── app/                        Android application module
        └── src/main/kotlin/nl/hexmaster/leakout/
            ├── ui/theme/
            ├── ui/title/
            ├── ui/levelselect/
            ├── ui/game/            GameScreen, BoardCanvas, StackPanel, overlays
            ├── render/             PipeDrawer (paths per section), LiquidDrawer, palette
            ├── audio/
            ├── history/            LevelHistoryRepository; data/: Room database, entity and DAO
            ├── data/               LevelRepository (assets), SettingsRepository (DataStore)
            └── LeakOutApp.kt, MainActivity.kt
```

The **core** module is the heart of the project. It can be run and tested on the JVM in milliseconds. The **app** module only renders `GameState` and forwards input as `GameCommand`s.

### 2.3 Coordinate conventions

- Column `x` grows to the right, row `y` grows downward, origin top-left.
- `Direction` = `LEFT, RIGHT, TOP, BOTTOM` with `opposite()` and `delta()` (`LEFT = (-1, 0)`, `TOP = (0, -1)`, …).
- "Port" always means a side of a cell, and the port through which liquid *enters* a cell is the opposite of the direction it *exited* the previous cell.

### 2.4 Domain model (core)

```kotlin
enum class Direction { LEFT, RIGHT, TOP, BOTTOM; fun opposite(): Direction; val dx: Int; val dy: Int }

data class Channel(val a: Direction, val b: Direction) {
    fun exitFor(entry: Direction): Direction? = when (entry) { a -> b; b -> a; else -> null }
}

enum class PipeSection(val channels: List<Channel>, val speedFactor: Double = 1.0) {
    H  (listOf(Channel(LEFT, RIGHT))),
    V  (listOf(Channel(TOP, BOTTOM))),
    LT (listOf(Channel(LEFT, TOP))),
    LB (listOf(Channel(LEFT, BOTTOM))),
    RT (listOf(Channel(RIGHT, TOP))),
    RB (listOf(Channel(RIGHT, BOTTOM))),
    X  (listOf(Channel(LEFT, RIGHT), Channel(TOP, BOTTOM))),
    RES(listOf(Channel(LEFT, RIGHT)), speedFactor = 0.3);
    fun channelFor(entry: Direction): Channel?
}
```

Modelling every section as a list of channels means the reservoir, cross and straights share one code path. The reservoir is a horizontal straight (`H`) with a different `speedFactor`; nothing else in the engine treats it specially. The speed factor in the enum is a default; the level file overrides it.

```kotlin
sealed interface Cell {
    data object Empty : Cell
    data object Blocked : Cell
    data class Start(val exit: Direction) : Cell
    data class Finish(val entry: Direction) : Cell
    data class Pipe(val section: PipeSection, val filled: Set<Channel> = emptySet()) : Cell
}

class Board(val columns: Int, val rows: Int, cells: Map<Position, Cell>) {
    operator fun get(pos: Position): Cell
    fun with(pos: Position, cell: Cell): Board       // immutable copy
    fun neighbour(pos: Position, dir: Direction): Position?  // null when off-grid
}
```

The board is immutable; every command returns a new `GameState`. That keeps the engine deterministic, trivially testable, and safe to hand to the Compose renderer.

### 2.5 Flow simulation

The liquid is a single **head** that moves through the network one cell at a time (`core.engine.FlowState`, `FlowSimulation`). Sides are `core.level.Side`; a channel is `core.flow.Channel` (`HORIZONTAL` or `VERTICAL` for a cross, `SINGLE` for every other section), and `used` holds the channels the liquid has run through, as `ChannelUse(position, channel)`.

```kotlin
sealed interface FlowState {
    val used: Set<ChannelUse>
    data class Waiting(val remainingMs: Long) : FlowState
    data class Flowing(
        val position: Position,
        val entry: Side,             // side the liquid entered this cell from
        val channel: Channel,
        val progress: Double,        // 0.0 at the entry side, 1.0 at the exit side
        override val used: Set<ChannelUse>,
    ) : FlowState
    data class Ended(override val used: Set<ChannelUse>) : FlowState   // won or lost, see GameState.outcome
}
```

`FlowSimulation.advance(state, flowDtMs): GameState` walks from cell border to cell border:

1. **Waiting**: subtract the flow time. When the countdown reaches zero, switch to `Flowing` at the start cell with `entry = start.openSide.opposite()` and a `SINGLE` channel, so the start visibly fills too, and use the leftover time.
2. **Flowing**: `speed = cellsPerSecond × (1 + speedRampPerCell × sectionsLeft)`, times `reservoirSpeedFactor` inside a reservoir. `sectionsLeft` is the number of exits from placed sections so far. If the remaining time does not reach the border, add `time × speed` to `progress` and stop. A progress within 1e-9 of the border counts as reaching it.
3. Otherwise use up the time to the border and leave the cell, in this order:
   - Add the channel to `used`.
   - Award the section's points (`ScoreSheet.award`, none for the start) and add an `Award(position, points, clockMs)` for the popup.
   - `FlowStep.after(board, position, exit, used)` decides: `Enter` → continue `Flowing` in the next cell at progress 0 and mark it `liquidEntered` on the board at once (so it can no longer be replaced), then loop with the remaining time; `Finish` → `Outcome.Won`; `Leak` → `Outcome.Lost(at, side)`. After a win or a loss the flow is `Ended` and the remaining time is dropped.

The flow runs on `flowDt = dt × flowTimeScale` (1, or 10 with fast forward) for both the countdown and the head; the game clock, the replace lock and the points popups keep `dt`. Because the loop walks every border inside one tick, a long tick (or 10x speed) never skips a cell, and 1 ms ticks give the same outcome and score as one long tick.

The flow and the complete-route check (`RouteTracer`) share `PipeSection.exitFor` and `FlowStep`, so every board the route check calls complete is won by the flow.

### 2.6 Game engine and commands

All game rules live in one engine in `core.engine`. It holds one immutable `GameState` per attempt and changes it only through commands.

```kotlin
data class GameState(
    val attempt: Int,                // 1, 2, …: screen state that must reset is keyed on it
    val level: Level,
    val board: Board,
    val stack: PipeStack,
    val flow: FlowState,
    val sheet: ScoreSheet,
    val awards: List<Award>,         // Award(position, points, atMs), dropped after Award.VISIBLE_MS
    val replaceLockUntilMs: Long,
    val clockMs: Long,
    val paused: Boolean,
    val fastForward: Boolean,
    val routeComplete: Boolean,
    val outcome: Outcome,            // Playing, Won(atMs), Lost(at, side, atMs)
    val finalScore: FinalScore?,     // set by the flow on the win: earned, unused sections, total
    val sectionsRemoved: Int,        // unused sections taken off the board by the tally so far
) {
    // Derived, so they cannot drift: score, stars, finalStars (from the final total), sectionsLeft,
    // flowSpeed (the HUD speed, without the reservoir factor), flowTimeScale and isStopped (paused,
    // won or lost).
}

sealed interface GameCommand {
    data class Start(val level: Level) : GameCommand
    data class Place(val at: Position) : GameCommand
    data object Pause : GameCommand
    data object Resume : GameCommand
    data object Restart : GameCommand
    data object FastForward : GameCommand
    data object NormalSpeed : GameCommand
    data class Tick(val dtMs: Long) : GameCommand
    data class RemoveUnused(val upTo: Int) : GameCommand
}

class GameEngine(private val random: Random) {
    fun start(level: Level, previous: GameState? = null): GameState
    fun handle(state: GameState, command: GameCommand): GameState
    fun canPlace(state: GameState, at: Position): Boolean
}
```

`handle` is a reducer: a refused command returns the same instance, so "accepted" is `new !== old`. The only impure input is the `Random` that draws the stack. The rules per command:

- **Start / Restart**: `start(level, previous)` builds a fresh state (the level's board, a new stack, the countdown at the start delay, score 0, clock 0, playing) with `attempt` one higher. It is the only constructor path, so a restart cannot forget a field. Accepted at any time.
- **Pause**: only while playing and not paused. **Resume**: only while paused. While paused a tick changes nothing.
- **FastForward**: only on a complete route, while playing and not paused. **NormalSpeed** turns it off again (the slow button) and unlocks placing.
- **Place**: the stack's next section goes on the tile when `canPlace` says so. `canPlace` and `Place` share one check: not stopped, not fast forward, and `Placement.check` (empty tile: place; unentered section with replacing allowed: replace; the replace lock must have passed). On success the stack moves down and draws a new section, a replacement starts the replace lock, and `RouteTracer` sets `routeComplete`.
- **Tick(dt)**: refused when `dt <= 0` or paused. Moves the clock, drops awards older than `Award.VISIBLE_MS`, and while playing runs `FlowSimulation.advance(state, dt × flowTimeScale)`. After a win or a loss only the clock moves, for the end effects.
- **RemoveUnused(upTo)**: only after a win, and only when `upTo` is ahead of `sectionsRemoved`. Empties the next unused sections of `finalScore` in row-major order, up to `upTo` (at most all of them). The tally timeline lives in the app: `GameSession.send` follows every accepted command after a win with `RemoveUnused(winFrame.sectionsRemoved)`, so the field empties in step with the penalty row.

On the Android side, `GameSession` holds the current `GameState` in a Compose `mutableStateOf`, sends commands with `send(command): Boolean` and feeds `Tick` from the frame clock. It keeps no game data of its own. The views read the state through `derivedStateOf`, so a clock tick recomposes only what changed, and the stack and the field are keyed on `attempt`, so a new attempt starts them fresh. A drop sends `Place` and succeeds exactly when the engine accepts it.

### 2.7 Level definition

JSON, one file per level, parsed with `kotlinx.serialization`:

```json
{
  "id": 3,
  "name": "First Crossing",
  "grid": { "columns": 7, "rows": 9 },
  "start":  { "col": 0, "row": 1, "direction": "RIGHT" },
  "finish": { "col": 6, "row": 7, "direction": "LEFT" },
  "blocked": [ { "col": 3, "row": 4 } ],
  "flow": {
    "initialDelayMs": 12000,
    "cellsPerSecond": 0.6,
    "reservoirSpeedFactor": 0.3,
    "speedRampPerCell": 0.0
  },
  "stack": {
    "visibleSize": 5,
    "weights": { "H": 3, "V": 3, "LT": 2, "LB": 2, "RT": 2, "RB": 2, "X": 1, "RES": 1 }
  },
  "rules": { "allowReplace": true, "replacePenaltyMs": 500 },
  "stars": { "two": 200, "three": 350 }
}
```

`LevelValidator` runs at load time (and in a unit test over every bundled level) and rejects:

- start/finish outside the grid, or their open port pointing off-grid or at a blocked cell,
- start and finish on the same cell,
- zero weight for any of the six mandatory sections,
- `visibleSize < 1`, non-positive speeds or delays.

A `docs/levels.md` sheet will track the intended difficulty curve (grid size, delay, speed, which optional sections) per level so tuning is done in one place.

### 2.8 Rendering (app)

- The `PlayingField` composable fills the space right of the stack and below the HUD. A pure `FieldViewport` holds the cell size and offset: it starts at `min(width / columns, height / rows)` clamped to 40–120 dp, centres every axis on which the grid fits, clamps panning to the grid edges, zooms around the pinch centre, and maps screen points to cells. Only the visible cells are drawn, clipped to the field.
- **Pipes** come from SVG assets, one per section, in `app/src/main/assets/pipes/` (format in [pipe-assets.md](pipe-assets.md)). `PipeArtParser` reads the small SVG subset they use, `PipeArtLibrary` turns them into Compose `Path`s once per process, and `PipeTile` scales them to any cell size. The art can be edited in an SVG editor without touching code.
- **Liquid** is drawn in code on top of the art, along the channel centre lines marked in each SVG, from a `ChannelFill(progress, entry)` per channel. It starts at the entry side, so every channel flows both ways:
  - Straight and corner: the first `progress` part of the channel, measured from the entry side.
  - Cross: each channel has its own fill; the vertical liquid is hidden under the horizontal pipe (the SVG's `bridge`).
  - Reservoir: the inlet stub fills during `progress` 0–0.1, the tank bottom-up during 0.1–0.9, the outlet stub after that.
  - `LiquidTiming` turns time into progress: `cellsPerSecond × factor` sections per second, where the factor is `reservoirSpeedFactor` for `RES` and 1 otherwise.
- Filled channels are drawn fully in liquid colour.
- Debug builds have a **Pipe Lab** launcher entry that previews every section and plays the liquid with direction, flow speed and reservoir speed controls.
- The **stack** is a vertical column of mini-boards using the same `PipeTile` at a smaller cell size; the top item is highlighted and slightly larger.
- A **points popup** ("+10", "+15", "+25", "+50") appears on a section's tile each time it scores: Fredoka bold at 40 % of a tile, an ink outline under a white fill, drawn last in the field canvas. It fades in over 150 ms (growing from 80 % to full size), holds for 500 ms and fades out over 350 ms, rising a quarter tile. Its age is taken from the game clock, so it freezes while paused and follows the tile during pan and zoom.
- A **leak** shows a short particle burst at the exit port; a **win** flashes the finish cell.
- Draw calls read only from an immutable `GameState`; no allocations in the draw loop beyond what Compose needs. The pipe art is prepared once per section in view-box units and scaled per tile; only the liquid segment is rebuilt each frame, into a reused `Path`.

### 2.9 Testing strategy

**core (JUnit on the JVM)**

- `PipeSectionTest`: every section maps each entry port to the correct exit port and returns `null` for closed sides.
- `FlowSimulationTest`: scripted boards for straight run, corner chain, cross used once, cross used twice, reservoir slows by factor, leak into empty, leak into wrong side, leak off grid, finish hit from wrong side, win.
- `GameEngineTest`: placement on empty, replace allowed/denied, replace lock, cannot replace filled/flowing cell, stack refills with pooled sections only, seeded random is reproducible, score accumulates.
- `LevelValidatorTest` plus a parameterised test that loads every bundled level file and validates it.
- A **headless solver** test helper that, given a level and a seed, plays a known-good sequence of placements and asserts a win. Used to prove every shipped level is winnable with its stack seed range.

**app**

- Compose UI tests: tapping a cell issues `Place`, pause stops ticks, overlays appear on win/lose.
- Screenshot tests of `PipeDrawer` for all eight sections at three progress values, to catch rendering regressions.

Target: core module ≥ 90 % line coverage; it is small and entirely deterministic.

### 2.10 Persistence

**Level progress: Room (SQLite).** `LevelHistoryRepository` keeps one row per finished level in the `level_history` table of `LeakOutDatabase`:

- `level_id` (primary key), `highscore`, `stars` (0-3, the stars earned with that highscore)
- a row exists only for finished levels, so "finished" means "has a row"; a leak is never recorded
- a win replaces the row only when it scores strictly higher, updating highscore and stars together
- unlocking is derived from these rows by the level library rules; nothing like `highestUnlockedLevel` is stored

Room was chosen over DataStore because progress is per-level rows that are listed and updated conditionally. Every schema version is exported to `app/schemas` and a version bump needs a written migration.

**Settings: DataStore (Preferences).** `SettingsRepository` keeps the settings in the DataStore file `settings`. Each key has a default used while nothing is stored, and volumes are clamped to 0.0-1.0 on write and read:

| Key | Type | Default |
|-----|------|---------|
| `sound_enabled` | Boolean | on |
| `sound_volume` | Float | 0.8 |
| `music_enabled` | Boolean | on |
| `music_volume` | Float | 0.6 |
| `vibration_enabled` | Boolean | on |

Reset progress clears `level_history` only; the settings are kept.

No accounts, no network in the first release.

---

## 3. Roadmap

Each milestone ends in something runnable. Estimates are in focused working days for one developer.

### Milestone 0 – Project skeleton (1 day)

- [ ] Create the Android project with Gradle Kotlin DSL and a version catalog.
- [ ] Add the `core` JVM module and the `app` module; `app` depends on `core`.
- [ ] Set up Compose, kotlinx.serialization, DataStore, JUnit 5.
- [ ] Add `.editorconfig`, ktlint or detekt, and a GitHub Actions workflow that runs `./gradlew test` on every push.
- [ ] Write `README.md` with build instructions.

### Milestone 1 – Core rules, headless (3 days)

- [ ] `Direction`, `Channel`, `PipeSection`, `Position`, `Cell`, `Board`.
- [ ] `WeightedPool` and `PipeStack` with seeded `Random`.
- [ ] `LevelDefinition`, parser, validator.
- [ ] `FlowSimulation` with all transitions from 2.5.
- [ ] `GameEngine` with all commands from 2.6.
- [ ] Unit tests from 2.9. Everything passes on the JVM before any UI exists.

**Exit criterion**: a JUnit test plays a full level headlessly from `newGame` to `Won`.

### Milestone 2 – Board on screen (3 days)

- [ ] `GameViewModel` holding `StateFlow<GameState>` and a frame-clock tick loop.
- [ ] `BoardCanvas`: grid, start, finish, blocked cells.
- [ ] `PipeDrawer`: all eight sections drawn procedurally.
- [ ] Tap-to-place with hit testing; replacement rule visible (a cell flashes red if locked).
- [ ] `StackPanel` showing the visible queue.
- [ ] Hard-coded level 1 loaded from assets.

**Exit criterion**: you can build a pipe on a phone; nothing flows yet.

### Milestone 3 – Liquid (3 days)

- [ ] Countdown display for the initial delay, "start now" button.
- [ ] `LiquidDrawer` with per-section progress clipping (straight, corner, cross, reservoir).
- [ ] Win and leak overlays with restart.
- [ ] Speed and reservoir factor read from the level file; verify feel with two throwaway levels (slow and fast).

**Exit criterion**: level 1 is playable start to finish, both win and lose paths.

### Milestone 4 – Levels and progression (3 days)

- [ ] Title screen and level select.
- [ ] `LevelRepository` reading all `assets/levels/*.json`.
- [ ] `ProgressRepository` with unlock logic and best scores.
- [ ] Score display and star thresholds.
- [ ] Author the first 15 levels following `docs/levels.md`; introduce cross at level ~4 and reservoir at level ~6; add blocked cells from level ~8.
- [ ] Headless winnability test over all bundled levels.

**Exit criterion**: a complete, if plain, game loop from title to last level.

### Milestone 5 – Polish (4 days)

- [ ] Sound effects: place, replace, flow tick, reservoir gurgle, leak, win. Background music optional.
- [ ] Animations: section drop-in on place, liquid particles on leak, finish flash on win, stack slide.
- [ ] Haptic tick on placement.
- [ ] Pause overlay; handle `onStop` by pausing the tick loop.
- [ ] Settings (sound/music toggles).
- [ ] Accessibility pass: colour-blind safe liquid colour, content descriptions on stack items, large tap targets.
- [ ] Tablet layout: stack on the side in landscape.

### Milestone 6 – Release (2 days)

- [ ] App icon, splash, Play Store listing assets. (Splash done in change `app-splash-screen`.)
- [ ] Release signing, ProGuard/R8 config, `versionCode` scheme.
- [ ] Manual test matrix: small phone, large phone, tablet, Android 8 and latest.
- [ ] Internal test track on Google Play.

### Later ideas (not planned, kept for reference)

- Timed "endless" mode where the flow keeps going through a scrolling board.
- Daily challenge with a fixed seed.
- Sections that appear pre-placed on the board as obstacles or helpers.
- One-way valve section, bomb section that clears a cell.
- Leaderboards through Play Games Services.

---

## 4. Suggested first steps

1. Do milestone 0 in one sitting, commit, and confirm CI is green.
2. Start milestone 1 by writing `PipeSectionTest` **first** for all eight sections; it forces the port/channel model to be right before anything else depends on it.
3. Implement `FlowSimulation` against the scripted-board tests before touching Compose. When it passes, the rest of the project is rendering and plumbing.

---

## 5. Open decisions

Decisions assumed in this plan that you may want to change. All are data or small code changes.

| # | Question | Assumed answer |
|---|----------|----------------|
| 1 | Reservoir port layout | Decided: horizontal only (L↔R), a slow straight with a tank |
| 2 | Can an unfilled placed pipe be replaced? | Yes, with a 500 ms lock, configurable per level |
| 3 | Can the player start the flow early? | Yes, for a time bonus |
| 4 | Grid size | 7 × 9 default, per-level override |
| 5 | Can start/finish sit in the middle of the grid? | Yes, level designer's choice |
| 6 | Package name | `nl.hexmaster.leakout` |
| 7 | Is the liquid always a single stream? | Yes; the cross never splits the flow, it only lets the path cross itself |

