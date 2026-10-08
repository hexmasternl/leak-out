# Proposal

## Why

After the first 96 levels the board has nothing new to offer: the liquid only ever moves to a neighbouring cell, so a wall of blocked cells always forces the same detour. Portal pairs let the liquid jump from one place on the board to another, which opens up routes across walls and gives the game a fresh puzzle shape for a new chapter.

This change is one of the seven mechanics that #102 splits into their own issues; each mechanic gets its own chapter of 24 levels (one level library page). Portals are pre-placed by the level and cannot be replaced, the same "placed by the level" idea that fixed pipes (#135) set up and that one-way valves (#136) also reuse. Where #135 lands first, this change builds on its groundwork instead of adding a second mechanism. Chapter order is still an open question in #102; the issue suggests portals come after the leaky tile and freezing cell chapters.

Tracked in [#140](https://github.com/hexmasternl/leak-out/issues/140).

## What Changes

- New optional `portals` list in the level format: at most three pairs, each with two portals that have a cell and an open side, like the start and the finish.
- `LevelParser`, `LevelValidator`, `LevelWriter` and `LevelSolver` support portals. The validator applies the start and finish facing-cell rules to every portal and rejects a portal on a blocked, start, finish, mandatory or other portal cell. The solver routes through portal pairs.
- The flow simulation in `core` moves the liquid through a pair: it enters one portal through its open side and leaves the other through that portal's open side, in either direction, in the time of one cell. The route check (fast forward) and the gap warning follow the same rule.
- Portals score no points, do not count for the speed ramp, never accept a section, and are not free cells for star thresholds or the Wall to Wall achievement.
- Portals are drawn on the board with a pair colour and a pair symbol, with the liquid draining into one portal and coming out of the other, and have an accessibility description.
- The level designer gets a Portal tool to place, move, re-orient and remove portal pairs, and its live validation and solvability check understand them.
- A first-time portal tip, in every supported language.
- A new chapter of 24 bundled levels (one library page) that introduces portals; every one passes the solver check.
- `docs/level-format.md` and `docs/design-system.md` describe portals. Levels 1 to 96, their scores and saved progress do not change.

## Capabilities

### New Capabilities
- `game-play-portals`: portal pairs as level data, their validation, how the liquid passes them, how they are drawn and described, and the portal chapter of bundled levels.

### Modified Capabilities
- `game-level-model`: the level document gains the optional `portals` field; start, finish, blocked cells and mandatory sections must not share a cell with a portal, and the start's and finish's facing cells must not be a portal.
- `level-serialization`: `LevelWriter` writes the `portals` list when a level has portals.
- `level-solvability`: the solver routes through portal pairs.
- `game-play-flow`: leaving a tile into a portal moves the liquid to the other portal of the pair; a portal does not count for the speed ramp; the flow and the route check agree on portals.
- `game-play-points`: portals give no points, like the start and the finish.
- `game-play-placement`: a portal tile never accepts a section.
- `game-play-gap-warning`: the walk ahead passes portal pairs, counting a pair as one section.
- `pipe-liquid-rendering`: the progress of a portal pair is split over its two cells.
- `level-designer`: Portal tool; the other tools leave portals alone; portal errors are highlighted; portals are not free cells for the star thresholds.
- `game-play-onboarding`: a portal tip and its place in the tip order.
- `game-achievements`: Wall to Wall does not count portal cells as free cells.

## Impact

- `core`: `level/Level.kt`, `LevelDto.kt`, `LevelParser.kt`, `LevelValidator.kt`, `LevelWriter.kt`, `LevelSolver.kt`, `LevelDraft.kt`; `board/Board.kt`, `board/Placement.kt`; `flow/FlowStep.kt`; `engine/FlowSimulation.kt`, `GapDistance.kt`; `route/RouteTracer.kt`; `achievements/AttemptSummary.kt`; tests in every one of those packages, plus `HeadlessPlayTest` and `ScriptedPlay`, which turn the solver's path into sections.
- `app`: playing field rendering, tile accessibility text, onboarding tips (`TipId`, tip overlay), level designer screen and view model, `res/values/strings.xml`, instrumented tests.
- Content: 24 new `assets/levels/level_NNN.json` files. Level files 1 to 96 stay byte-identical.
- Docs: `docs/level-format.md` (portals section with an example and its validation rules), `docs/design-system.md` (portal tokens and drawing).
- No new dependencies. The server keys scores by level id and needs no change.
