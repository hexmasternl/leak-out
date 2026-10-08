# Proposal

## Why

Every level today has one start, one finish and a single liquid head, so the puzzle is always one route. A splitter divides the liquid over two outputs, so the player has to build two routes at the same time, to two finishes, while both branches flow. It is the biggest of the new mechanics from #102 and gives the game a new kind of puzzle after the first 96 levels.

This change is part of #102, which splits the new mechanics into one change each, each with its own chapter of 24 levels (one level library page). The splitter is placed by the level, like the fixed pipes of #135 and the mandatory sections, so it reuses that "placed by the level" groundwork; valves (#136) and portals (#140) may reuse it too. #102 has not decided the chapter order yet; this change suggests the splitter chapter comes last, because it changes the most.

Tracked in [#137](https://github.com/hexmasternl/leak-out/issues/137).

## What Changes

- A new **splitter** section: a T-shaped section with three open sides and one closed side, in four orientations (closed side left, right, top or bottom). It is never dealt from the stack: a level places at most one splitter on the board, and the player cannot remove or replace it.
- Liquid that enters a splitter through one open side leaves through **both** other open sides at the same moment, as two liquid heads. Liquid that reaches the closed side leaks.
- A level can have a **second finish**. A level with a splitter has exactly two finishes, and a level with two finishes has a splitter. Levels with one finish keep working unchanged.
- The flow moves **two liquid heads** after the splitter, each at its own speed. A branch that reaches a finish stops there while the other keeps flowing. The level is won when both finishes are reached, and lost as soon as either branch leaks. A branch that runs into a channel the other branch has already filled, or is filling, leaks.
- Liquid that reaches a finish without passing the splitter is refused there, like a missed mandatory section, and the level is lost.
- The splitter scores 30 points when the liquid leaves it, and both branches score their own sections.
- The route check (fast forward), the gap warning and the HUD understand two branches: the route is complete only when both branches reach a finish; the gap is the nearest gap of the two heads; the HUD shows the faster head's speed.
- `LevelParser`, `LevelValidator`, `LevelWriter` and `LevelSolver` support the optional `secondFinish` and `splitter` fields. The solver finds a route that splits and reaches both finishes, or names the finish it cannot reach.
- The playing field draws the splitter with new pipe art and animates the liquid splitting at its centre. Its cell has an accessibility description.
- The level designer gets a Second finish tool and a Splitter tool, with live validation and the solver's route for both branches.
- A first-time **splitter tip** explains the mechanic the first time a level with a splitter starts.
- A new chapter of 24 bundled levels introduces the splitter. Every one passes the solver check. Levels 1 to 96, their scores and saved progress are unaffected.

## Capabilities

### New Capabilities
- `game-play-splitter`: the splitter section during play: its shape and orientations, how it divides the liquid into two heads, how the two branches move, finish, collide and leak, how it is shown and described, and that it cannot be removed.

### Modified Capabilities
- `game-level-model`: the optional `secondFinish` and `splitter` fields with their validation.
- `game-play-flow`: the flow can have two liquid heads, each with its own speed; leaving a tile, large ticks and the agreement with the route check cover both.
- `game-play-points`: the splitter's points.
- `game-play-level-complete`: a level with two finishes is won when both are reached.
- `game-play-fast-forward`: the route follows both branches and is complete only when both reach a finish.
- `game-play-gap-warning`: the gap distance is the nearest gap of the two heads.
- `game-play-placement`: a tile with the splitter never accepts a section.
- `level-solvability`: the solver requires a route through the splitter to both finishes.
- `level-serialization`: written levels round-trip with a splitter and a second finish.
- `level-designer`: tools for the second finish and the splitter, and validation of both.
- `game-play-onboarding`: the splitter tip and its place in the tip order.
- `pipe-liquid-rendering`: the liquid splitting inside the splitter.

## Impact

- `core/level`: `PipeSection` (four splitter sections that are not stack sections), `Level`, `LevelDto`, `LevelParser`, `LevelValidator`, `LevelWriter`, `LevelSolver`, `LevelDraft`, `StackSettings` (weights only for stack sections).
- `core/board`, `core/flow`, `core/engine`, `core/route`, `core/score`: the board holds the splitter and the second finish, `FlowState` holds up to two heads, `FlowSimulation`, `FlowStep`, `RouteTracer`, `GapDistance` and `Points` handle the split.
- `app`: splitter SVG assets and liquid drawing, playing field and cell semantics, the HUD speed, the splitter tip, the designer tools, new strings in the default string resources (translated through Play Console like every other string).
- `app/src/app/src/main/assets/levels/`: 24 new level files; existing files are not touched.
- `docs/level-format.md` and `docs/design-system.md` document the splitter and the second finish.
- Sibling changes from #102 may modify the same requirements (points per section, leaving a tile, the solver). Whichever lands second merges its deltas onto the first.
