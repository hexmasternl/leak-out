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
2. The player sees a **stack** of upcoming pipe sections. Only the top section can be placed.
3. The player taps a grid cell to place the top section there. The stack shifts and a new random section is appended at the bottom.
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

- A visible queue of `visibleSize` sections (default 5). The **top** section is the one that will be placed on the next tap.
- After every placement the queue shifts up by one and a new section is drawn at random from the level's **pool** and appended at the bottom.
- The pool is a weighted set. Straight and corner sections (`H`, `V`, `LT`, `LB`, `RT`, `RB`) always have a non-zero weight. `X` and `RES` are optional and may have weight zero.
- Random draws use a seeded random generator so a level can be replayed deterministically in tests and, if wanted, as a "daily challenge".

### 1.7 Placing sections

- Tapping an **Empty** cell places the top section.
- Tapping a cell that holds an **unfilled** pipe **replaces** it, if the level allows replacing (`allowReplace`, default `true`). Replacing costs a small time penalty (`replacePenaltyMs`, default 500 ms) during which no new placement is accepted, to stop the player from spamming replacements. This is the classic Pipe Mania rule and keeps the stack from becoming a dead end.
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

### 1.9 Win, lose, and score

- **Win**: liquid enters the finish section through its open port. The finish plays a fill animation, then the level-complete screen appears.
- **Lose**: any leak. The leak point spurts liquid, then the level-failed screen appears with a retry button.
- **Score** (first version, keep simple):
  - +10 per filled section.
  - +50 extra when both channels of a cross are filled.
  - +20 per filled reservoir.
  - Time bonus: remaining initial-delay seconds × 5 if the player starts the flow early.
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
3. **Game** – a HUD bar across the top (level, flow speed, three stars earned so far, score, cog wheel pause button), with the stack and the board below it. Back and going to the background also pause the game.
4. **Pause dialog** – opened from the cog wheel: Resume, Restart, a separator and Exit to the main menu (welcome screen). Tapping outside does nothing; Back works as Resume.
5. **Level complete / failed overlay** – score, stars, next / retry / level select.

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
| Persistence | Jetpack DataStore (Preferences) | Level progress and settings |
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
    │       ├── flow/               FlowSimulation, FlowState, FlowEvent
    │       ├── game/               GameEngine (ties board + stack + flow), GameState, GameCommand
    │       └── level/              LevelDefinition (serializable), LevelParser, LevelValidator
    └── app/                        Android application module
        └── src/main/kotlin/nl/hexmaster/leakout/
            ├── ui/theme/
            ├── ui/title/
            ├── ui/levelselect/
            ├── ui/game/            GameScreen, BoardCanvas, StackPanel, overlays
            ├── render/             PipeDrawer (paths per section), LiquidDrawer, palette
            ├── audio/
            ├── data/               LevelRepository (assets), ProgressRepository (DataStore)
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

The liquid is a single **head** that moves through the network one cell at a time.

```kotlin
sealed interface FlowState {
    data class Waiting(val remainingMs: Long) : FlowState
    data class Flowing(
        val position: Position,
        val entry: Direction,        // side the liquid entered this cell from
        val channel: Channel,
        val progress: Double         // 0.0 at entry port, 1.0 at exit port
    ) : FlowState
    data class Won(val at: Position) : FlowState
    data class Leaked(val at: Position, val exit: Direction) : FlowState
}
```

`FlowSimulation.step(state, dtMs): Pair<FlowState, List<FlowEvent>>`

1. **Waiting**: subtract `dt`. When it reaches zero, move to `Flowing` at the start cell, with `entry = start.exit.opposite()` and a synthetic channel so the start pipe visibly fills too.
2. **Flowing**: `progress += dt / 1000 * cellsPerSecond * section.speedFactor * rampFactor`. While `progress < 1.0` nothing else happens.
3. When `progress >= 1.0` the head leaves the cell:
   - Mark the channel as filled in the board (`Pipe.filled += channel`); emit `SectionFilled`.
   - `exit = channel.exitFor(entry)`, `next = board.neighbour(position, exit)`.
   - `next == null` or `Empty` or `Blocked` → **Leaked**.
   - `Finish` → **Won** if `finish.entry == exit.opposite()`, else **Leaked**.
   - `Pipe` → `channel = section.channelFor(exit.opposite())`. If `null` (no port on that side) or the channel is already filled → **Leaked**. Otherwise continue `Flowing` in `next` with `entry = exit.opposite()` and `progress` carrying the overshoot (`progress - 1.0`) so speed stays smooth across cell borders.
4. **Won** / **Leaked** are terminal; the engine stops ticking and the UI shows the overlay.

Because the head is the only mutable thing and every branch is an explicit state transition, the whole simulation is covered by a few dozen unit tests (see 2.9).

### 2.6 Game engine and commands

```kotlin
data class GameState(
    val level: Level,
    val board: Board,
    val stack: PipeStack,
    val flow: FlowState,
    val score: Int,
    val replaceLockUntilMs: Long,
    val elapsedMs: Long,
    val paused: Boolean
)

sealed interface GameCommand {
    data class Place(val at: Position) : GameCommand
    data object StartFlowNow : GameCommand
    data object Pause : GameCommand
    data object Resume : GameCommand
    data object Restart : GameCommand
    data class Tick(val dtMs: Long) : GameCommand
}

class GameEngine(private val random: Random) {
    fun newGame(level: Level): GameState
    fun handle(state: GameState, command: GameCommand): GameState
}
```

`handle` is a pure function. The Android layer holds the current `GameState` in a `StateFlow` inside a `GameViewModel`, feeds `Tick` from the frame clock, and maps taps to `Place`.

Placement validation lives in the engine, not the UI:

- Cell must be `Empty`, or `Pipe` with no filled channel and not the flow head's current cell, and `allowReplace` must be true.
- `replaceLockUntilMs` must have passed.
- On success: put the stack's top section in the cell, pop the stack, draw a new section from the pool, set the replace lock if it was a replacement.

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

- The board composable measures its available size, computes `cellSize = min(width / columns, height / rows)`, and centres the grid.
- **Pipes** are drawn procedurally with `Path` and a thick stroke (pipe body) plus a thinner stroke (highlight) rather than bitmaps. Each section is one path; the cross is two paths; the reservoir is a straight path plus a rounded rectangle tank. Procedural drawing scales to any density and makes the liquid fill trivial.
- **Liquid** is drawn as the same path, in the liquid colour, clipped by `progress`:
  - Straight: a rectangle from the entry side grown to `progress`.
  - Corner: two halves; first the leg from the entry port to the centre (`progress` 0–0.5), then the leg from the centre to the exit (0.5–1).
  - Cross: only the active channel's path is filled.
  - Reservoir: the tank fills bottom-up between `progress` 0.1–0.9, the outlet leg after that.
- Filled channels are drawn fully in liquid colour.
- The **stack** is a vertical column of mini-boards using the same `PipeDrawer` at a smaller cell size; the top item is highlighted and slightly larger.
- A **leak** shows a short particle burst at the exit port; a **win** flashes the finish cell.
- Draw calls read only from an immutable `GameState`; no allocations in the draw loop beyond what Compose needs. Precompute one `Path` per section per cell size and cache it.

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

`ProgressRepository` (DataStore Preferences):

- `highestUnlockedLevel: Int`
- per level: `bestScore`, `stars`
- settings: `soundEnabled`, `musicEnabled`

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
