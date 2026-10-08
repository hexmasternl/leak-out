# Proposal

## Why

The liquid only ever moves to a neighbouring cell, so a wall of blocked cells always forces the same detour. Portal pairs let the liquid jump from one place on the board to another, which opens up routes across walls and gives the second half of the game a fresh puzzle shape.

This change is one of the mechanics that #102 splits into their own issues. Unlike the chapter-per-mechanic plan of #102, portals do not get a chapter of their own: the maintainer decided that portals appear from level 49 onwards, and that some (not all) of the existing levels 49 to 96 are updated to hold portal pairs. Portals are placed by the level designer, never come from the stack and cannot be replaced or removed, the same "placed by the level" idea that fixed pipes (#135) set up and that one-way valves (#136) also reuse. Where #135 lands first, this change builds on its groundwork instead of adding a second mechanism.

Tracked in [#140](https://github.com/hexmasternl/leak-out/issues/140).

## What Changes

- New optional `portals` list in the level format: at most three pairs, each with two portals that have a cell and an open side, like the start and the finish.
- `LevelParser`, `LevelValidator`, `LevelWriter` and `LevelSolver` support portals. The validator rejects a level with more than three pairs, applies the start and finish facing-cell rules to every portal, and rejects a portal on a blocked, start, finish, mandatory or other portal cell. The solver routes through portal pairs.
- The flow simulation in `core` moves the liquid through a pair: it enters one portal through its open side and leaves the other through that portal's open side. Pairs work both ways, and each pair can be used once per attempt. Passing a pair takes the time of one cell. The route check (fast forward) and the gap warning follow the same rule.
- Portals never appear on the stack, never accept a section and cannot be replaced or removed by the player. They score no points, do not count for the speed ramp, and are not free cells for star thresholds or the Wall to Wall achievement.
- Portals are drawn on the board in a pair colour, the identifier that tells the pairs apart, backed by a pair symbol so colour is not the only cue. The liquid drains into one portal and comes out of the other. Portals have an accessibility description.
- The level designer gets a Portal tool to place, move, re-orient and remove portal pairs, and its live validation and solvability check understand them.
- A first-time portal tip, shown the first time the player sees a portal pair: it points out both portals of the pair and explains that the pair works both ways. In every supported language.
- Level 49 becomes the first level with a portal pair. From there on, portal pairs are added occasionally to the bundled levels 49 to 96: a minority of them, growing from one pair to at most three. Every updated level passes the solver check. Levels 1 to 48 do not change, and no level id changes.
- `docs/level-format.md` and `docs/design-system.md` describe portals. Saved progress (level history, stars, achievements, settings) is kept.

## Capabilities

### New Capabilities
- `game-play-portals`: portal pairs as level data, their validation, how the liquid passes them, how they are drawn and described, and the bundled levels from 49 onwards that use them.

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
- `game-play-onboarding`: a portal tip that points out both portals of a pair and explains that it works both ways, and its place in the tip order.
- `game-achievements`: Wall to Wall does not count portal cells as free cells.

## Impact

- `core`: `level/Level.kt`, `LevelDto.kt`, `LevelParser.kt`, `LevelValidator.kt`, `LevelWriter.kt`, `LevelSolver.kt`, `LevelDraft.kt`; `board/Board.kt`, `board/Placement.kt`; `flow/FlowStep.kt`; `engine/FlowSimulation.kt`, `GapDistance.kt`; `route/RouteTracer.kt`; `achievements/AttemptSummary.kt`; tests in every one of those packages, plus `HeadlessPlayTest` and `ScriptedPlay`, which turn the solver's path into sections.
- `app`: playing field rendering, tile accessibility text, onboarding tips (`TipId`, tip overlay), level designer screen and view model, `res/values/strings.xml`, instrumented tests.
- Content: `assets/levels/level_049.json` and a selection of `level_050.json` to `level_096.json` gain portal pairs and, where the best route changes, retuned star thresholds. Level files 1 to 48 stay byte-identical; no level is added or renumbered.
- Docs: `docs/level-format.md` (portals section with an example and its validation rules), `docs/design-system.md` (portal tokens and drawing).
- No new dependencies. The server keys scores by level id and needs no code change, but online top scores of an updated level were set on its old layout (see design.md, Open Questions).
