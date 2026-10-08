# Proposal

## Why

After 96 levels every section can be passed in both directions, so the player never has to think about which way the route runs. A one-way valve lets the liquid through in one direction only. That adds a new kind of planning without new controls, and it fits the existing model well: a valve is a straight section that the level puts on the board, with a direction. This change is one of the seven mechanics split out of [#102](https://github.com/hexmasternl/leak-out/issues/102), each with its own chapter of 24 levels. Tracked in [#136](https://github.com/hexmasternl/leak-out/issues/136).

## What Changes

- **Level format:** a new optional `valves` list. Each entry has an `x`, a `y` and a `direction` (`LEFT`, `RIGHT`, `UP` or `DOWN`): the way the liquid must flow. `LEFT` and `RIGHT` valves are horizontal straights, `UP` and `DOWN` valves vertical ones. `LevelParser`, `LevelValidator`, `LevelWriter` and `docs/level-format.md` handle it. A level without `valves` behaves exactly as today.
- **Board:** valves are on the board from the start of every attempt, like mandatory sections. They are not dealt from the stack and the player can never replace or remove them. They stay on the board after a win and never count as unused sections.
- **Flow:** liquid that enters a valve through its inlet leaves through its outlet and scores 10 points, like a straight. Liquid that reaches a valve from any other side leaks, and the level is lost.
- **Solver:** `LevelSolver` routes through a valve only in its direction. When the only route needs a valve the wrong way, the reason names that valve.
- **Rendering:** a valve is drawn as a straight with a brass direction arrow and collar, per `docs/design-system.md`. The liquid always fills it from the inlet. Screen readers hear "One-way valve" and its direction.
- **Level designer:** a Valve tool places, turns, moves and removes valves, and the live validation and solvability check understand them.
- **Onboarding:** a one-time valve tip the first time a level with valves starts, in every supported language.
- **Levels:** a new chapter of 24 bundled levels (one level library page) introduces valves. Every level passes the solver check. Levels 1 to 96, their scores and saved progress are unchanged.
- Related siblings in #102: fixed pipes ([#135](https://github.com/hexmasternl/leak-out/issues/135)) lay the "placed by the level" groundwork that valves and portals ([#140](https://github.com/hexmasternl/leak-out/issues/140)) may reuse. The order of the chapters is still an open question in #102 (see design.md).

## Capabilities

### New Capabilities
- `game-play-one-way-valves`: the one-way valve mechanic: how a level lists valves, where they may lie, how the liquid passes or leaks at them, that the player cannot change them, how they are described, and the chapter that introduces them.

### Modified Capabilities
- `game-level-model`: `valves` becomes an optional field of the level document.
- `level-serialization`: `LevelWriter` writes valves.
- `level-solvability`: the solver passes valves only in their direction and names a valve that blocks the only route.
- `game-play-flow`: the route check and the flow agree on valve direction.
- `game-play-leak`: liquid reaching a valve against its direction leaks.
- `game-play-points`: a valve scores as a straight section.
- `game-play-placement`: a valve tile never accepts a section.
- `game-play-final-score`: valves never count as unused sections.
- `pipe-liquid-rendering`: a valve tile shows its direction and fills from its inlet.
- `level-designer`: Valve tool, valves in live validation, export, test play and saved state.
- `game-play-onboarding`: a one-way valve tip, its place in the tip order and its "Got it" button.

## Impact

- `app/src/core`: level model, `LevelDto`, `LevelParser`, `LevelValidator`, `LevelWriter`, `LevelSolver`, `LevelDraft`, `Board`/`Cell`, `FlowStep`, `FlowSimulation`, `RouteTracer`, `FinalScore`, and their unit tests.
- `app/src/app`: playing field and tile rendering, `CellDescription`, placement, level designer, `TipId` and the tip scheduler and overlay; new strings in `res/values/strings.xml` (translated by Play Console like every other string).
- `app/src/app/src/main/assets/levels`: 24 new `level_NNN.json` files; possibly a valve overlay asset under `assets/pipes/`.
- `docs/level-format.md`, `docs/design-system.md`.
- Saved tip state gains one new key; existing keys and saved progress are untouched. No server change: the server accepts scores for any level id.
