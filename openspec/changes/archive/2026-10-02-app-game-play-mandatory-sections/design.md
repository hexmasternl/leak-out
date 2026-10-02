# Design

## Context

See proposal.md for the motivation and specs/ for the behaviour. The relevant parts of the current code:

- `Level` (core/level) holds concrete values; `LevelDto` is the JSON shape, `LevelParser` turns parse failures into field-path errors and `LevelValidator` collects every meaning error.
- `PipeSection.mandatory` already exists and means "straight or corner, weight must be at least 1". It collides with the new meaning of the word.
- `Board.from(level)` builds the starting board; `Cell.Pipe(section, liquidEntered)` is a placed section. `Placement.check` is the single placement rule for both hover and drop.
- `RouteTracer.trace` walks the route and already tracks visited channels as `(position, horizontal)`. `FlowSimulation.leaveTile` decides the next step through `FlowStep.after`, and `FlowState.Flowing.used` already holds every `ChannelUse(position, channel)` the liquid has finished.
- `Outcome.Lost(at, side, atMs)` drives both the leak effect and the lost dialog.
- `LevelSolver` is a BFS over free cells, valid only because any orthogonal path of free cells can be paved with straights and corners.

## Goals / Non-Goals

**Goals:**
- Mandatory sections as level data, enforced entirely in `core` so every rule is JVM-unit-testable.
- One definition of "used" shared by the route check, the win/loss decision and the accessibility text.
- A solver that never accepts an unwinnable level with mandatory sections.

**Non-Goals:**
- Editing mandatory sections in the level designer (a follow-up to `app-game-level-designer`).
- Mandatory straights, corners or a vertical reservoir.
- New level content: at most one bundled level gets mandatory sections, as a showcase and an end-to-end test.

## Decisions

### Level format: a top-level `mandatory` array
`"mandatory": [ { "x": 3, "y": 2, "section": "X" } ]`, optional, default `[]`. It goes into `Level.mandatory: List<MandatorySection>` (position plus section, authored order kept so the validator can report duplicates by index, as with `blocked`). `section` parses as a `PipeSection`, so an unknown id is a parse error; restricting it to `X`/`RES` is a validation error (`mandatory[i].section`), so the message can explain the rule. The validator also caps the list at 6 entries, which bounds the solver's search (see below).
*Alternative:* a `"fixed"` flag on a pipe list, or a field inside `blocked`. Rejected: a separate array reads most clearly and leaves `blocked` unchanged.

### Rename `PipeSection.mandatory` to `alwaysOnStack`
The flag and the validator's error wording are renamed so "mandatory" means only a pre-placed section. This is a pure code rename. Behaviour and specs are unchanged.

### Board: `Cell.Pipe` gets `mandatory: Boolean = false`
`Board.from(level)` adds `Cell.Pipe(section, mandatory = true)` for each entry. Keeping it a `Pipe` means the flow, `FlowStep`, points, reservoir speed, `markLiquidEntered`, rendering of the pipe art and the liquid all work unchanged. `Placement.check` returns `Rejected(FIXED)` for a mandatory pipe before the replace checks, so hover and drop agree as they do today.
*Alternative:* a new `Cell.Mandatory` type. Rejected: every `is Cell.Pipe` branch in core and app would need a second case.

### One "used" rule: `MandatoryProgress`
A small pure helper in `core/engine` (or `core/board`) answers, for a set of `ChannelUse`s, which mandatory sections are unused. A section's required channels are its `Channel`s: two for a cross, one for a reservoir. It is used by:
- `FlowSimulation.leaveTile`: on `FlowStep.Result.Finish`, if any mandatory section is unused in `used`, the outcome is `Outcome.Lost(finish.position, finish.openSide, clockMs, LossReason.MANDATORY_MISSED)` instead of `Won`. Points for the section just left are awarded first, as today.
- `RouteTracer`: the visited channel set is already there. A route reaching the finish whose channels miss a mandatory channel returns `Route.Open(finish, Reason.MANDATORY_MISSED)`, so `routeComplete` (and fast forward) needs no other change.
- The app, for the accessibility description and the missed-section pulse, from `FlowState.used`.

### `Outcome.Lost` gets a `reason`
`enum class LossReason { LEAK, MANDATORY_MISSED }`, defaulting to `LEAK` so existing code and tests keep compiling. The leak effect already draws at `(at, side)`; with the finish position and its open side it spurts out of the finish without new geometry. The dialog picks its message from the reason, and the field adds the red pulse on unused mandatory tiles only for `MANDATORY_MISSED`.
*Alternative:* treat the finish as closed and report an ordinary leak at the last pipe. Rejected: the user chose a clear loss at the finish that says why.

### Final score and removal are untouched
A won attempt has every mandatory section used, so `liquidEntered` is true for all of them and `FinalScore.of` never lists one as unused. `FinalScore.of` still filters out mandatory pipes explicitly, so the "never removed" rule does not hinge on that reasoning.

### Solver: ordered waypoint search with backtracking
Each mandatory channel becomes a waypoint: a reservoir has one (left↔right), a cross two (left↔right, top↔bottom). A waypoint is passed in one of two directions, which fixes the cell before and after it. The solver searches orderings and directions depth-first. For each step it finds a BFS shortest path from the current cell to the waypoint's entry neighbour, over cells that are free and not yet used by the route; mandatory cells are only crossable as waypoints. A mandatory cross is the only cell that may appear twice, once per channel. The first complete chain to the finish's facing cell is returned as the route. With at most 6 sections (at most 12 waypoints), the search is pruned as soon as a segment cannot be connected, so bundled levels solve quickly. Pre-checks report a mandatory section whose entry neighbours are blocked, off-grid or an endpoint by name, before any search.
*Trade-off:* greedy shortest segments can block a later segment, so the solver can reject a level that is actually solvable. It never accepts an unsolvable one, because the route it returns is a real self-avoiding path. The level-format doc states this; the fix for a false rejection is to adjust the level.
*Alternative:* a full Hamiltonian-style DFS over the grid. Rejected: exponential on 50 by 50 grids.

### Rendering: tile tint drawn under the pipe art
In `PlayingField.drawCell`, a mandatory `Cell.Pipe` first draws its tile as `LeakOutColors.LeakTint` with a 1 dp `leak.500` (`#FF5A5F`) edge, then the usual pipe art and glass. A new `LeakOutColors.Leak500` token is added if it is missing. The missed-section pulse is an alpha animation of the same edge on the game clock, alongside the existing leak frame. The design system gains a short "Mandatory section" entry under the tile states.

## Risks / Trade-offs

- [Solver false negatives on crowded levels] → Documented, and the per-level test failure names the mandatory section the solver could not reach, so the author knows what to move.
- [A player does not notice a mandatory section until the loss] → The red tint uses the established leak palette, and the lost message names the cause. The pulse marks exactly which section was missed.
- [Rename of `PipeSection.mandatory` touches tests] → It is mechanical, and the full `:core:test` run catches every use.
- [Level designer export drops `mandatory`] → Noted in the proposal's Impact. Whichever change lands second adds the field to the serializer and its round-trip test.

## Migration Plan

No migration is needed. The field is optional, existing levels parse unchanged and `LossReason` defaults to `LEAK`.
