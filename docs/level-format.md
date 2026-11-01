# Level file format

Every level is one JSON file in `src/app/src/main/assets/levels/`, named `level_NNN.json` (`level_001.json`, `level_002.json`, ...). The files are parsed by `LevelParser` and checked by `LevelValidator` in the `core` module (`nl.hexmaster.leakout.core.level`). A unit test parses and validates every bundled file, so a broken level fails the build's tests. A second, separate check confirms that every bundled level is actually [solvable](#solvability).

## Full example

```jsonc
{
  "id": 3,                          // required, positive, unique across all levels
  "name": "First Crossing",         // required, not blank; English only, never translated (players do not see it)
  "grid": { "columns": 7, "rows": 9 },                // required, columns 4 to 25, rows 4 to 15
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
    "visibleSize": 5,               // optional, 1 to 10: sections always visible on the stack
    "weights": { "H": 7, "V": 7, "LT": 7, "LB": 7, "RT": 7, "RB": 7, "X": 3, "RES": 3 }
  },
  "rules": {                        // optional
    "allowReplace": true,           // optional: may an unfilled pipe be replaced
    "replacePenaltyMs": 1500        // optional, 0 or more: length of a replacement, no placing meanwhile
  },
  "stars": { "one": 100, "two": 200, "three": 350 }, // required, positive, strictly increasing
  "mandatory": [                    // optional, at most 6; [] or absent for none
    { "x": 3, "y": 2, "section": "X" },
    { "x": 5, "y": 5, "section": "RES" }
  ],
  "portals": [                      // optional, at most 3 pairs; [] or absent for none
    { "a": { "x": 1, "y": 6, "openSide": "TOP" }, "b": { "x": 5, "y": 1, "openSide": "BOTTOM" } }
  ]
}
```

Comments are shown for explanation only. Level files are plain JSON and must not contain comments. Unknown keys are rejected, so a misspelled key is an error, not a silently ignored setting.

The game shows a level by its number, never by its `name`. The name is only for the level files and the level designer, so it is not translated when the app is shown in other languages (see [Translations](deployment.md#translations)).

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

Defaults live in `LevelDefaults`. Each optional field can be overridden on its own, and every other field keeps its default. `"stack": { "weights": { "X": 3 } }` enables the cross and leaves every other weight and the visible size unchanged. Changing a default changes every level that does not override it.

| Field | Default |
|-------|---------|
| `flow.reservoirSpeedFactor` | 0.3 |
| `flow.speedRampPerCell` | 0.0 |
| `stack.visibleSize` | 5 |
| `stack.weights.H`, `V`, `LT`, `LB`, `RT`, `RB` | 7 |
| `stack.weights.X`, `RES` | 0 |
| `rules.allowReplace` | true |
| `rules.replacePenaltyMs` | 1500 |
| `mandatory` | none (see [Mandatory sections](#mandatory-sections)) |
| `portals` | none (see [Portal pairs](#portal-pairs)) |

## Coordinates and open sides

- `x` is the column and `y` the row, both zero-based, with the origin at the top-left and `y` growing downward.
- `openSide` is one of `LEFT`, `RIGHT`, `TOP`, `BOTTOM`: the side of the cell with the opening. The neighbouring cell on that side is the endpoint's *facing cell*, which a pipe must occupy to connect it.

## Stack size

`stack.visibleSize` is the number of stack sections the player is guaranteed to see. The stack itself holds ten sections more (`PipeStack.EXTRA_SECTIONS`): 15 with the default visible size of 5, at most 20. The stack rail sizes its slots so that the bottom `visibleSize` sections always fit; the extra sections continue upwards as far as the screen has room and are clipped at the top of the rail. Design and balance a level against `visibleSize`: how many of the extra sections a player sees depends on their screen.

## Stack weights

Each pipe section has a weight. A section's weight divided by the sum of all weights is its *target share*: the part of the sections dealt in an attempt that the stack steers it towards (see [How the stack deals](#how-the-stack-deals)). A weight of 0 means the section never appears.

With the default standard weights (7 each, 42 in total), a cross at weight 3 makes up 3/45 ≈ 6.7 % of the stack and a cross at weight 6 makes up 6/48 = 12.5 %.

| Id | Section | Rule |
|----|---------|------|
| `H`, `V` | Straight | weight at least 1 |
| `LT`, `LB`, `RT`, `RB` | Corner | weight at least 1 |
| `X` | Cross | 0 or more; off by default |
| `RES` | Reservoir | 0 or more; off by default |

## How the stack deals

Sections are dealt one at a time, at random, but steered so that every section stays close to its target share throughout the attempt. Two rules do this; both live in `Dealer` and use the constants in `LevelDefaults`.

**Deficit steering.** After `n` sections have been dealt, a section's *deficit* is how often it should have been dealt by now (target share × `n`) minus how often it was. Each section is drawn with weight `share × e^(k × deficit)`, with steering strength `k` = `STEERING_STRENGTH` (1.0). A section that is behind becomes more likely the longer it stays behind; a section that is ahead, for example one just dealt twice, becomes less likely. Deficits always add up to zero, so nothing drifts over a long level, and the very first section follows the weights exactly. The initial stack is dealt the same way, one section after the other.

**Drought limit.** A straight or corner section that has not been dealt for `⌈DROUGHT_FACTOR / share⌉` draws (`DROUGHT_FACTOR` 2.5) is dealt next without drawing. That is 15 draws with six equal standard sections, 17 next to a cross at weight 3 and 18 next to a cross at weight 6. If several sections reach their limit at once, the one missing longest is dealt, and on a tie the first in the order `H`, `V`, `LT`, `LB`, `RT`, `RB`. The cross and the reservoir are never forced: a designer who makes them rare means it. With steering in place the limit fires about once every two levels; it is a safety net that guarantees no standard section is ever missing for long.

The dealing state (draws so far, count and drought per section) belongs to the attempt: it carries over between placements and across pause, and starts fresh when the level is started or restarted.

The level has no random seed. Every attempt deals a new stack, so a retry still deals a different, possibly easier sequence; steering only makes each sequence fairer, not the same. Tests pass a seeded random source to get a repeatable sequence.

## Start and finish rules

A level is rejected unless, for both the start and the finish:

1. The cell lies inside the grid and is not blocked or a portal.
2. The start and the finish are different cells.
3. The facing cell lies inside the grid. An open side that points at the grid edge can never be connected.
4. The facing cell is not blocked and is not a portal.
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

## Mandatory sections

`mandatory` lists sections that are on the board when an attempt starts or restarts, without liquid. They are not drawn from the stack, and the stack does not depend on them. The player cannot remove or replace them, whatever `rules.allowReplace` says, and they stay on the board when the unused sections are removed after a win. The playing field draws them on a red-tinted tile.

A level is rejected unless:

1. It lists at most 6 mandatory sections (`LevelValidator.MAX_MANDATORY_SECTIONS`).
2. Each `section` is `X` (cross) or `RES` (reservoir). Straights and corners cannot be mandatory.
3. Each cell lies inside the grid, is not blocked, and is not the start, the finish or a portal.
4. No two mandatory sections share a cell.

A mandatory section ignores its stack weight, so a level can have a mandatory cross while `stack.weights.X` is 0.

For the liquid a mandatory section is an ordinary section of its kind: it is entered through an open side, scores its points and slows the liquid down inside a reservoir. A mandatory section is **used** once the liquid has run through every channel it has, in either direction: both the horizontal and the vertical channel of a cross, the single left-to-right channel of a reservoir. The level is only won when the liquid enters the finish after every mandatory section is used. If the liquid reaches the finish with a mandatory section still unused, the finish refuses it and the level is lost. In the same way the route only counts as complete, and fast forward only becomes available, once it runs through every mandatory channel.

## Portal pairs

`portals` links two cells of the board: liquid that enters one portal of a pair comes out of the other. Each pair has two portals, `a` and `b`, and each portal has the shape of the start and the finish: a cell (`x`, `y`) and the `openSide` that the liquid goes in and out through.

```json
"portals": [
  { "a": { "x": 2, "y": 1, "openSide": "LEFT" }, "b": { "x": 6, "y": 5, "openSide": "RIGHT" } },
  { "a": { "x": 1, "y": 7, "openSide": "TOP" },  "b": { "x": 4, "y": 0, "openSide": "BOTTOM" } }
]
```

Pairs are numbered in the order they are listed: the first pair is pair 1. The number picks the pair's colour and symbol on the board (see the design system), so there is no colour field and two pairs can never look alike.

A level is rejected unless:

1. It lists at most 3 pairs (`LevelValidator.MAX_PORTAL_PAIRS`).
2. Each portal lies inside the grid and is not on a blocked cell, the start, the finish, a mandatory section or another portal. The two portals of a pair are therefore different cells.
3. Each portal's facing cell, the neighbour on its open side, lies inside the grid and is not blocked.
4. A portal's facing cell is not the start, the finish or a portal, and the start's and finish's facing cells are not a portal.

Errors name the portal, for example `portals[0].b`. Two portals, or a portal and the start or finish, may share a facing cell, and a portal's facing cell may hold a mandatory section.

How the liquid passes a pair:

- Liquid that leaves a tile into a portal through the portal's open side enters the pair, and comes out of the other portal through that portal's open side, into the cell it faces. Pairs work both ways: the liquid can enter either portal.
- Liquid that reaches a portal through any other side leaks at the tile it left.
- Passing a pair takes the time of one cell at the current speed, without the reservoir factor. It gives no points, and a pair does not count as a section for `flow.speedRampPerCell`.
- A pair can be passed once per attempt. A portal has one open side and the tiles in front of a passed pair are filled, so a second pass cannot happen in play; the rule protects the route check against loops.
- If the cell the exit portal faces does not take the liquid, it leaks at the exit portal.

Portals are part of the level, like mandatory sections: they are on the board from the start, never come from the stack, and cannot be replaced or removed. They are not free cells, so they do not count for the designer's star thresholds or for the Wall to Wall achievement.

## Solvability

`LevelValidator` only checks a level's shape and values; it does not check that the level can be won. That is a separate concern, handled by `LevelSolver`: it searches the grid for a route of adjacent cells from the start's facing cell to the finish's facing cell, using only cells that lie inside the grid, are not blocked, and are not the start, the finish or a portal. Cross (`X`) and reservoir (`RES`) sections are never required for such a route, so their weights do not affect the outcome.

A portal pair links the two cells its portals face: a route at one of them may continue at the other, through the pair, so a pair can carry a route across a wall of blocked cells. The route never uses a cell twice, so it passes each pair at most once, and using a pair is optional. The returned path lists both portals, in flow order, between the two cells facing them; those are the only steps of the path between cells that are not neighbours. A portal may also face a mandatory section and feed its channel. An unsolvable level never names a portal as the reason: the reason is still the endpoint or mandatory section that could not be connected.

With mandatory sections, the route must run through every mandatory channel, entering each section through an open side, and must not pass any cell twice except a mandatory cross (once per channel). The solver first checks that both entry neighbours of every mandatory channel can carry a pipe and names the section if they cannot. It then tries every order and direction of the mandatory channels, nearest first, and joins them with shortest paths; on a hopeless level it gives up after a bounded amount of work (`LevelSolver.MAX_SEARCH_WORK`, about a second). Because those shortest paths are chosen greedily, one can occasionally block a later one, so the solver may reject a crowded level that a player could still win. It never accepts a level that cannot be won: the route it finds is a real, self-avoiding route. If a level is wrongly rejected, move a mandatory section or a blocked cell; the failure names the section the solver could not reach.

`LevelSolvabilityTest` runs the solver over every `level_NNN.json` file in the levels directory (the `leakout.levelsDir` system property, wired to the bundled assets folder) and fails per level, naming the file and why no route was found. So an unwinnable level fails `./gradlew :core:test` just like a parse or validation error, and pointing the property at another directory checks a different set of levels with the same suite.

## Stars

`stars.one`, `two` and `three` are the minimum points for one, two and three stars on a won level. A won level that scores below `stars.one` earns 0 stars and still counts as won. Stars reward longer, more elaborate routes, not merely reaching the finish. So the validator does not check that a threshold can be reached: tune thresholds by play-testing.

## Errors

- **Parse errors** (shape and types) stop at the first problem and name the field by its path, for example `flow.cellsPerSecond`, `stack.weights.Z`, `blocked[1].y`, `mandatory[0].section` or `portals[0].b`.
- **Validation errors** (meaning) are all reported together, each naming its field, so a file can be fixed in one pass.

## Creating levels with the designer

Debug builds have a level designer; release builds do not (it sits behind the `levelDesigner` feature flag, which is `BuildConfig.LEVEL_DESIGNER`: on in debug, off in release). Open it with the small pencil button in the top-right corner of the welcome screen.

1. Start from a blank level (7 by 5, start on the left, finish on the right, id one past the bundled levels) or with **Start from…** from a copy of a bundled level.
2. Lay out the grid. Set columns (4 to 25) and rows (4 to 15) with the steppers; shrinking drops blocked cells outside the grid, moves the start or finish inside it and removes a portal pair with a portal outside it. Pick a tool and tap the grid: **Start** and **Finish** move an endpoint (keeping its open side), **Block** toggles a cell, and a drag paints every cell it passes; **Mandatory cross** and **Mandatory reservoir** place, change or remove a mandatory section. Set each endpoint's open side in the panel. None of these tools touches a portal.
3. Add portal pairs with the **Portal** tool. Tap a free cell to place a portal: the first tap starts a pair, the next completes it, and a new portal opens to the first of right, left, bottom and top that faces a cell inside the grid. Tap a portal to select it (framed in brass) and tap it again to deselect it; with a portal selected, tap a free cell to move it there. The panel then shows the selected portal's open side and **Remove pair**, which removes both portals; the pairs after it move up and take its number, colour and symbol. A drag only acts on the cell it starts on. At most three pairs; a pair with only one portal is reported as an error (`portals[i].b`) and keeps the design from being played or exported. Portals are not free cells, so each pair lowers the calculated star thresholds.
4. Fill in the other fields: id, name, flow, stack weights and visible size, rules and stars. Text that is not a number is marked and does not change the level.
5. Watch the status. It runs `LevelValidator` and then `LevelSolver` on every change, lists every error by field and frames the cells they are about. A solvable level shows the solver's route faintly on the grid.
6. **Test play** plays the level on the normal game screen. Nothing is recorded in the level history, and leaving returns to the designer with the design intact.
7. **Export** writes the level with `LevelWriter`, copies it to the clipboard and opens the share sheet with the suggested file name `level_NNN.json`. The JSON leaves out every optional field that holds its default, so it reads like a hand-written file.

Save the exported JSON as `src/app/src/main/assets/levels/level_NNN.json`, with `NNN` matching the level's id, and run `./gradlew :core:test` from `src/`: the bundled level tests parse, validate, solve and round-trip it, and check that its id is not used twice.

Test play and Export are only enabled for a valid, solvable level. The design survives rotation and the app being recreated, but it is not stored permanently: export it before you leave. Portal pairs, an unfinished pair and the selected portal survive with it.
