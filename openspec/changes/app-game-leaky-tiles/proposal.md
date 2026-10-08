# Proposal

## Why

After 96 levels the board only knows free and blocked cells, so the best route is almost always the one that fits the time. A leaky tile is a third kind of cell: a pipe can go on it, but liquid that passes it drains away and costs points. That makes the player weigh a short route over a leaky tile against a longer, safer detour. This change is one of the seven mechanics split out of [#102](https://github.com/hexmasternl/leak-out/issues/102), and it brings its own chapter of 24 levels (one level library page). Tracked in [#138](https://github.com/hexmasternl/leak-out/issues/138).

## What Changes

- A level can list **leaky tiles** in a new optional `leaky` array of cells. A leaky tile lies inside the grid, is not blocked, not the start or finish and not a mandatory section, and is listed only once.
- Any section can be placed on a leaky tile, by drag or tap, under the normal placement and replace rules.
- Each time the liquid exits a section on a leaky tile, that pass scores **no section points** and costs a fixed **leak penalty of 20 points** instead. A cross on a leaky tile passed twice costs the penalty twice. The points popup shows "−20" in the leak colour.
- The running score of an attempt may go below 0. The final total of a won attempt stays at 0 or above, as it does today.
- The flow speed and the route rules do not change on a leaky tile, so the flow and the route check keep agreeing.
- Leaky tiles are drawn as cracked, damp tiles with a drip badge, under any placed section. While the liquid runs through a pipe on a leaky tile, drops fall from it; afterwards the tile keeps a wet stain. Reduced motion shows the stain without drops. Screen readers name the cell as leaky.
- `LevelSolver` treats leaky tiles as free cells and also reports whether a route exists that avoids every leaky tile. The level designer shows that as a note, not as an error.
- The level designer gets a Leaky tile tool, writes the `leaky` field on export, and leaves leaky tiles out of the free cells it uses for the star thresholds.
- A one-time **leaky tile tip** explains the mechanic the first time a level with leaky tiles starts.
- A new chapter of **24 bundled levels** introduces leaky tiles. Each passes the solver check. Levels 1 to 96, their scores and saved progress do not change.

Related changes from #102: fixed pipes (#135) lay the "placed by the level" groundwork that valves (#136) and portals (#140) may reuse. Leaky tiles do not depend on it: a leaky tile is a cell property, not a pre-placed section. The order of the chapters is still open in #102.

## Capabilities

### New Capabilities
- `game-play-leaky-tiles`: leaky tiles during play: where they can be, that any section can go on them, the score effect of passing one, how they and the liquid passing them look, and how they are described to screen readers.

### Modified Capabilities
- `game-level-model`: the level format gains the optional `leaky` field with its validation.
- `level-serialization`: `LevelWriter` writes the `leaky` field when the level has leaky tiles.
- `level-solvability`: leaky tiles count as usable cells, and the solver reports whether a leak-free route exists.
- `game-play-flow`: leaving a tile awards the leak penalty instead of the section points on a leaky tile.
- `game-play-points`: a pass over a leaky tile scores no section points and costs the penalty; the running score may go below 0.
- `game-play-points-popup`: a penalty is shown as a negative popup in the leak colour.
- `game-play-final-score`: earned points may be negative; the total stays at 0 or above.
- `game-play-playing-field`: the field draws leaky tiles.
- `game-play-onboarding`: a new leaky tile tip and its place in the tip order.
- `level-designer`: a Leaky tile tool, leaky tiles in copy, export and test play, the leak-free note and the star thresholds without leaky cells.

## Impact

- `core/level`: `Level`, `LevelDto`, `LevelParser`, `LevelValidator`, `LevelWriter`, `LevelSolver`, `LevelDraft`, `StarBenchmarks`.
- `core/score` and `core/engine`: `Points`, `ScoreSheet`, `FlowSimulation`, `Award` (negative points).
- `app`: playing field drawing and cell semantics, points popup, HUD score bar with a negative score, the onboarding tip and its seen flag in settings, the level designer, string resources.
- `app/src/app/src/main/assets/levels`: 24 new `level_NNN.json` files; the existing files are untouched.
- `docs/level-format.md` and `docs/design-system.md` document the field, the tile and the drip effect.
- Existing level files, the level history and the online scores are unaffected: the field is optional and defaults to none, and a final total is never negative.
