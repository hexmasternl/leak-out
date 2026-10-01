# Level file format

Every level is one JSON file in `src/app/src/main/assets/levels/`, named `level_NNN.json` (`level_001.json`, `level_002.json`, ...). The files are parsed by `LevelParser` and checked by `LevelValidator` in the `core` module (`nl.hexmaster.leakout.core.level`). A unit test parses and validates every bundled file, so a broken level fails the build's tests. A second, separate check confirms that every bundled level is actually [solvable](#solvability).

## Full example

```jsonc
{
  "id": 3,                          // required, positive, unique across all levels
  "name": "First Crossing",         // required, not blank; shown to the player
  "grid": { "columns": 7, "rows": 9 },                // required, each 4 to 50
  "start":  { "x": 0, "y": 1, "openSide": "RIGHT" },  // required; liquid leaves through openSide
  "finish": { "x": 6, "y": 7, "openSide": "LEFT" },   // required; liquid must enter through openSide
  "blocked": [ { "x": 3, "y": 4 } ],                  // required; [] for none
  "flow": {
    "initialDelayMs": 12000,        // required, 0 or more: time before the liquid starts
    "cellsPerSecond": 0.6,          // required, above 0 and at most 10: base speed
    "reservoirSpeedFactor": 0.3,    // optional, above 0 and at most 1: speed multiplier inside a reservoir
    "speedRampPerCell": 0.0         // optional, 0 or more: speed increase per filled cell
  },
  "stack": {                        // optional
    "visibleSize": 5,               // optional, 1 to 10: sections shown on the stack
    "weights": { "H": 3, "V": 3, "LT": 2, "LB": 2, "RT": 2, "RB": 2, "X": 1, "RES": 1 }
  },
  "rules": {                        // optional
    "allowReplace": true,           // optional: may an unfilled pipe be replaced
    "replacePenaltyMs": 3000        // optional, 0 or more: length of a replacement, no placing meanwhile
  },
  "stars": { "one": 100, "two": 200, "three": 350 }  // required, positive, strictly increasing
}
```

Comments are shown for explanation only. Level files are plain JSON and must not contain comments. Unknown keys are rejected, so a misspelled key is an error, not a silently ignored setting.

## Minimal example

A level only needs its required fields. Everything else takes the defaults below.

```json
{
  "id": 1,
  "name": "First Leak",
  "grid": { "columns": 7, "rows": 5 },
  "start":  { "x": 0, "y": 2, "openSide": "RIGHT" },
  "finish": { "x": 6, "y": 2, "openSide": "LEFT" },
  "blocked": [],
  "flow": { "initialDelayMs": 20000, "cellsPerSecond": 0.4 },
  "stars": { "one": 50, "two": 80, "three": 120 }
}
```

## Defaults

Defaults live in `LevelDefaults`. Each optional field can be overridden on its own, and every other field keeps its default. `"stack": { "weights": { "X": 1 } }` enables the cross and leaves every other weight and the visible size unchanged. Changing a default changes every level that does not override it.

| Field | Default |
|-------|---------|
| `flow.reservoirSpeedFactor` | 0.3 |
| `flow.speedRampPerCell` | 0.0 |
| `stack.visibleSize` | 5 |
| `stack.weights.H`, `V` | 3 |
| `stack.weights.LT`, `LB`, `RT`, `RB` | 2 |
| `stack.weights.X`, `RES` | 0 |
| `rules.allowReplace` | true |
| `rules.replacePenaltyMs` | 3000 |

## Coordinates and open sides

- `x` is the column and `y` the row, both zero-based, with the origin at the top-left and `y` growing downward.
- `openSide` is one of `LEFT`, `RIGHT`, `TOP`, `BOTTOM`: the side of the cell with the opening. The neighbouring cell on that side is the endpoint's *facing cell*, which a pipe must occupy to connect it.

## Stack weights

Each pipe section has a weight. Its chance to be drawn is its weight divided by the sum of all weights. A weight of 0 means the section never appears.

| Id | Section | Rule |
|----|---------|------|
| `H`, `V` | Straight | weight at least 1 |
| `LT`, `LB`, `RT`, `RB` | Corner | weight at least 1 |
| `X` | Cross | 0 or more; off by default |
| `RES` | Reservoir | 0 or more; off by default |

The level has no random seed. Every attempt draws a new stack, so a retry can deal a different, possibly easier sequence.

## Start and finish rules

A level is rejected unless, for both the start and the finish:

1. The cell lies inside the grid and is not blocked.
2. The start and the finish are different cells.
3. The facing cell lies inside the grid. An open side that points at the grid edge can never be connected.
4. The facing cell is not blocked.
5. The start's facing cell is not the finish, and the finish's facing cell is not the start.

```
  valid                  rule 3                 rule 5
  ┌───┬───┬───┬───┐      ┌───┬───┬───┬───┐      ┌───┬───┬───┬───┐
  │ S→│ · │ · │ · │     ←│ S │ · │ · │ · │      │ S→│←F │ · │ · │
  ├───┼───┼───┼───┤      ├───┼───┼───┼───┤      ├───┼───┼───┼───┤
  │ · │ · │ · │←F │      │ · │ · │ · │←F │      │ · │ · │ · │ · │
  └───┴───┴───┴───┘      └───┴───┴───┴───┘      └───┴───┴───┴───┘
```

The start and the finish may share a facing cell: one pipe section then connects them.

## Solvability

`LevelValidator` only checks a level's shape and values; it does not check that the level can be won. That is a separate concern, handled by `LevelSolver`: it searches the grid for a route of adjacent cells from the start's facing cell to the finish's facing cell, using only cells that lie inside the grid, are not blocked, and are not the start or finish cell itself. Cross (`X`) and reservoir (`RES`) sections are never required for such a route, so their weights do not affect the outcome.

`LevelSolvabilityTest` runs the solver over every `level_NNN.json` file in the levels directory (the `leakout.levelsDir` system property, wired to the bundled assets folder) and fails per level, naming the file and why no route was found. So an unwinnable level fails `./gradlew :core:test` just like a parse or validation error, and pointing the property at another directory checks a different set of levels with the same suite.

## Stars

`stars.one`, `two` and `three` are the minimum points for one, two and three stars on a won level. A won level that scores below `stars.one` earns 0 stars and still counts as won. Stars reward longer, more elaborate routes, not merely reaching the finish. So the validator does not check that a threshold can be reached: tune thresholds by play-testing.

## Errors

- **Parse errors** (shape and types) stop at the first problem and name the field by its path, for example `flow.cellsPerSecond`, `stack.weights.Z` or `blocked[1].y`.
- **Validation errors** (meaning) are all reported together, each naming its field, so a file can be fixed in one pass.
