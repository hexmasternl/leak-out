# Proposal

## Why

After 96 levels the player knows every rule of the board, and the cross is the only section the liquid can pass twice. A freezing cell turns that rule around: the liquid freezes behind it, so the route can cross that cell only once. It makes the player think about where a cross may go and where the route has to cross itself, and it gives the game a new chapter of levels. This change is one of the mechanics from [#102](https://github.com/hexmasternl/leak-out/issues/102), which splits the new mechanics into one change each, with a chapter of 24 levels (one level library page) per mechanic. Tracked in [#141](https://github.com/hexmasternl/leak-out/issues/141).

## What Changes

- A level can list **freezing cells** in a new optional `freezing` array of `{x, y}` cells. Existing levels leave it out and behave exactly as before.
- A freezing cell is a normal free cell with an icy look. The player can place, and before the liquid arrives replace, any section on it, as on any other cell.
- Once the liquid has left the section on a freezing cell, the section **freezes**. Liquid that reaches a frozen section cannot enter it: the liquid leaks and the level is lost, as with any other leak.
- In practice this matters for the cross: a cross on a freezing cell can be passed through one channel only, so it can never score its 50-point second pass. Every other section can only be passed once anyway.
- The route check for fast forward and the level solver follow the same rule: neither ever runs through a freezing cell twice. The validator rejects a mandatory cross on a freezing cell, because such a level could never be won.
- The playing field draws freezing cells with a frost tint and the liquid in a frozen section as ice. The liquid turns to ice with a short animation when it leaves the section. Screen readers describe the cell as freezing or frozen.
- The level designer gets a Freezing cell tool, writes the `freezing` field on export, and its live validation and solvability check understand it.
- A one-time **freezing cell tip** explains the mechanic the first time the player starts a level with freezing cells, in every supported language.
- A new chapter of 24 bundled levels (one level library page) introduces freezing cells. Every one of them passes the solver check. Levels 1 to 96, their scores and saved progress are unchanged.

Related changes from #102: fixed pipes (#135) put sections on the board that the level placed, groundwork that valves (#136) and portals (#140) may reuse. Freezing cells do not need it, because a freezing cell is a property of the cell, not of a section. The order of the chapters is still an open question in #102; see design.md.

## Capabilities

### New Capabilities
- `game-play-freezing-cells`: freezing cells during play: what a freezing cell is, when its section freezes, that a frozen section refuses the liquid, how freezing cells and ice look, and how they are described to a screen reader.

### Modified Capabilities
- `game-level-model`: adds the optional `freezing` field to the level format with its validation, and rejects a mandatory cross on a freezing cell.
- `level-serialization`: `LevelWriter` writes the `freezing` field when a level has freezing cells.
- `level-solvability`: the solver never routes through a freezing cell twice.
- `game-play-leak`: liquid that reaches a frozen section leaks.
- `game-play-flow`: a cross on a freezing cell can be passed through one channel only.
- `game-play-points`: the section before a frozen section still scores.
- `game-play-fast-forward`: the route is not complete when it comes back to a freezing cell.
- `pipe-liquid-rendering`: liquid in a frozen section is drawn as ice.
- `level-designer`: a Freezing cell tool, and freezing cells in live validation, test play and export.
- `game-play-onboarding`: a freezing cell tip and its place in the tip order.

## Impact

- `core/level`: `Level`, `LevelDto`, `LevelParser`, `LevelValidator`, `LevelWriter`, `LevelSolver`, `LevelDraft`.
- `core/board`, `core/flow`, `core/route`, `core/engine`: the board knows its freezing cells, and `FlowStep` and `RouteTracer` refuse the liquid at a frozen section. `FlowSimulation` needs no new branch because it already asks `FlowStep`.
- `app`: `PlayingField` and the liquid renderer draw the frost tile and the ice, `CellDescription` gets freezing and frozen texts, the designer gets the new tool, `TipId` and the tip scheduler get the freezing cell tip, and `res/values/strings.xml` gets the new strings (translated by Play Console like every other string).
- `app/src/main/assets/levels`: 24 new `level_NNN.json` files.
- `docs/level-format.md` documents the field, its rules and an example. `docs/design-system.md` documents the frost tile, the ice colours and the tip text.
- No migration: the field is optional, no stored data changes, and the new tip has a new key, so players who have seen the other tips still see this one once.
