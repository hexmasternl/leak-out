# Proposal

## Why

After 96 levels every puzzle is built from the same pieces: an empty grid, blocked cells and the odd mandatory cross or reservoir. A **fixed pipe** is a section the level bolts onto the board before the attempt starts. The player can't move or replace it, but the route does not have to use it. It can be a free piece of route or a corner that points the wrong way, so the player has to plan around it. It fits the current model best, which makes it the natural first new chapter.

This change is part of [#102](https://github.com/hexmasternl/leak-out/issues/102), which splits the new mechanics into one change each, with one chapter of 24 levels per mechanic. Fixed pipes lay the "placed by the level" groundwork (a section on the board that the stack did not deal and the player can't touch) that one-way valves ([#136](https://github.com/hexmasternl/leak-out/issues/136)) and portals ([#140](https://github.com/hexmasternl/leak-out/issues/140)) may reuse. The order of the chapters is still an open question in #102. Tracked in [#135](https://github.com/hexmasternl/leak-out/issues/135).

## What Changes

- A level can list **fixed pipes** in a new optional `fixed` array: cells with a section id. Any of the eight sections (`H`, `V`, the four corners, `X`, `RES`) can be fixed.
- A fixed pipe is on the board, without liquid, when an attempt starts or restarts. The stack does not deal it and does not depend on it.
- The player can't replace a fixed pipe, whatever `rules.allowReplace` says. A drop on it is refused and the stack does not change. It stays on the board when the unused sections are removed after a win, and it never counts as an unused section.
- For the liquid, the route check, the gap warning and the points, a fixed pipe behaves like a placed section of the same kind. The level does not require the liquid to pass it.
- A fixed pipe is drawn with its normal pipe art on a darker steel tile with four bolts, so it looks different from a placed section and from a mandatory section. Its accessibility description says that it is fixed.
- `LevelParser`, `LevelValidator`, `LevelWriter` and `LevelSolver` understand fixed pipes. The validator rejects a fixed pipe outside the grid, on a blocked cell, the start, the finish, a mandatory section's cell or another fixed pipe's cell. The solver can run the route through a fixed pipe, but only along its channels.
- The level designer gets a Fixed pipe tool to place, change, move and remove fixed pipes. Its live validation, solvability check, export and test play include them.
- A one-time **fixed pipe tip** explains the mechanic the first time the player starts a level with fixed pipes.
- A new chapter of 24 bundled levels (one level library page, levels 97 to 120 by default) introduces fixed pipes. Every one of them passes the solver check.
- Levels 1 to 96, their scores and saved progress do not change.

## Capabilities

### New Capabilities
- `game-play-fixed-pipes`: fixed pipes during play: that they are on the board from the start, can't be replaced, behave like placed sections for the liquid, are not required, how they look and how they are described, and the chapter of levels that introduces them.

### Modified Capabilities
- `game-level-model`: adds the optional `fixed` field to the level format, with its validation.
- `level-serialization`: `LevelWriter` writes the `fixed` field.
- `level-solvability`: the solver may run the route through fixed pipes along their channels and never through a fixed pipe's closed side.
- `game-play-placement`: a tile with a fixed pipe never accepts a section.
- `game-play-points`: a fixed pipe scores like a placed section when the liquid leaves it.
- `game-play-final-score`: a fixed pipe is never an unused section and is never removed after a win.
- `game-play-onboarding`: adds the fixed pipe tip and its place in the tip order.
- `level-designer`: adds the Fixed pipe tool, and the other tools, the copy of a bundled level, export, test play and the star calculation take fixed pipes into account.

## Impact

- `core/level`: `Level`, `LevelDto`, `LevelParser`, `LevelValidator`, `LevelWriter`, `LevelSolver`, `LevelDraft`.
- `core/board`, `core/score`: the board starts with the fixed pipes, placement refuses them, the final score skips them.
- `app`: `PlayingField` and `CellDescription` (rendering and accessibility), `TipId`, the tip scheduler and overlay, the designer screen, grid and view model, new English strings in `values/strings.xml` (translated by Play's automatic translation like every other string).
- `app/src/main/assets/levels`: 24 new files, `level_097.json` to `level_120.json`.
- `docs/level-format.md` and `docs/design-system.md`.
- No migration: the field is optional, existing level files parse unchanged and the level history, tips and achievements keep their stored keys.
- No server change is expected: scores for the new level ids go to the score server like any other level.
