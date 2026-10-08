# Design

## Context

See proposal.md for the motivation and specs/ for the behaviour. This is one of the mechanics from #102; each gets its own change and its own chapter of 24 levels. The relevant parts of the current code (paths relative to `app/src/`):

- `Level` (core/level) holds concrete values. `LevelDto` is the JSON shape, `LevelParser` turns parse failures into field-path errors, `LevelValidator` collects every meaning error and `LevelWriter` writes only non-default fields. `LevelDraft` is the designer's editable level.
- `Board.from(level)` builds the starting board. `Cell.Pipe(section, liquidEntered, mandatory)` is a section on the board.
- `FlowStep.after(board, from, exit, usedChannels)` is the one place that decides enter, finish or leak. `FlowSimulation.leaveTile` adds the channel just left to `used` before it calls `FlowStep`, so `used` holds exactly the channels the liquid has left.
- `RouteTracer.walk` repeats those rules for the route check and the gap distance, with its own `visited` set of `ChannelUse`s.
- `LevelSolver` searches self-avoiding routes of free cells; only a mandatory cross may appear twice.
- `CellDescription` builds the screen-reader text of a cell. `TipId` and `TipScheduler` decide which one-time tips are due; the mandatory section tip already highlights cells on the board at level start.
- Every player-facing string lives in `res/values/strings.xml`. Play Console translates them into the supported languages (`SupportedLanguages`), so "every supported language" means: the strings are resources, follow the translator rules, and fit 40 % longer text.

## Goals / Non-Goals

**Goals:**
- Freezing cells as level data, with the freeze rule in `core` so every rule is JVM-unit-testable.
- One freeze rule shared by the flow, the route check and the solver, so they cannot disagree.
- A clear icy look before, while and after the liquid freezes, with a screen-reader description.
- A chapter of 24 levels that teaches the mechanic, all passing the solver check.

**Non-Goals:**
- Ice that melts during an attempt (out of scope in #141).
- Slowing the liquid on a freezing cell, or a frozen cell blocking its neighbours (see Open Questions).
- A new loss reason or lost-dialog message for a frozen section (see Open Questions).
- A freeze sound effect or haptic. They can follow later without a spec change to the rules.
- Changing the order of the chapters or how pages unlock; that is decided in #102.

## Decisions

### Level format: a top-level `freezing` array of cells
`"freezing": [ { "x": 3, "y": 4 } ]`, optional, default `[]`, the same shape as `blocked`. It goes into `Level.freezing: List<Position>` with `Level.isFreezing(position)`. The validator checks inside the grid, not blocked, not the start or finish, no duplicates, and no mandatory `X` on a freezing cell; every error names `freezing[i]`. A mandatory `RES` may lie on a freezing cell, because it only has one channel. There is no cap on the number of freezing cells: unlike mandatory sections they do not make the solver search harder.
*Alternative:* a flag on each entry of a generic `tiles` list shared with the other #102 mechanics. Rejected for now: no other mechanic has landed, and a plain array matches `blocked` and `mandatory`. If #102 later settles on a shared shape, the parser can accept both.

### Freezing is a property of the cell, not of the section
`Board` gets `freezing: Set<Position>`, filled by `Board.from(level)`, and takes part in `equals`/`hashCode`. `Cell` is unchanged, so placement, replacement, points, the reservoir speed and the final score work as they do today. A freezing cell can be empty, so the mark cannot live on `Cell.Pipe`.
*Alternative:* a `Cell.Freezing` type or a flag on `Cell.Pipe`. Rejected: the first breaks every `is Cell.Pipe` branch, the second cannot mark an empty cell.

### One freeze rule: `FreezeRule.isFrozen(board, position, used)`
A small pure helper in `core/flow`: a position is frozen when it is in `board.freezing` and `used` holds any `ChannelUse` at that position. Because `used` only gets a channel when the liquid leaves it, a section the liquid is still inside is not frozen, as the spec asks. No new field in `FlowState` is needed, so restart, pause and fast forward need no change.
- `FlowStep.after`: in the `Cell.Pipe` branch, a frozen `next` gives `Leak(from, exit)`, checked before the channel checks.
- `RouteTracer.walk`: the same check on its `visited` set, returning `Route.Open(next, Reason.FROZEN)` (a new reason value, used for tests only). `FlowStepRouteAgreementTest` gets boards with freezing cells.
- The app: the set of frozen cells for drawing and for `CellDescription` is `used` filtered by `board.freezing`.

### Loss is a plain leak
Liquid that reaches a frozen section leaks at the tile it left, through the side it left, exactly like a used cross channel. `Outcome.Lost` keeps `LossReason.LEAK`, so the leak effect, the lost dialog ("The pipe is leaking. Level lost.") and the achievements need no change. Whether this deserves its own reason is an open question.

### Solver: freezing cells are free cells, used at most once
The solver's routes are already self-avoiding except for a mandatory cross, and the validator forbids a mandatory cross on a freezing cell, so the existing search never passes a freezing cell twice. The change makes this explicit: `LevelSolver` treats a freezing cell as free, adds a pre-check that reports a mandatory cross on a freezing cell as unsolvable by name (for callers that skip validation), and a test asserts that no returned route repeats a freezing cell. The solver still never needs a cross from the stack.

### Rendering: frost tile, ice liquid, 400 ms freeze
New design-system tokens, desaturated so the aqua liquid stays the brightest thing on screen:

| Token | Value | Use |
| --- | --- | --- |
| `ice.100` | `#EAFBFF` | Ice highlight |
| `ice.300` | `#BDEBFA` | Ice body, frost tile edge |
| `ice.500` | `#86C9E0` | Ice shadow edge |
| `ice.tint` | `#BDEBFA` at 14 % | Frost tile wash |

- `PlayingField.drawCell` draws a freezing cell's tile with `ice.tint`, a 1 dp `ice.300` edge and small frost crystals in the four corners, then any section's pipe art on top. A mandatory reservoir on a freezing cell keeps the frost tile and gets the red mandatory edge, so both marks stay visible.
- The pipe tile renderer gets a per-channel `frozen` progress (0 to 1). Ice is drawn on the same path and width as the liquid, with the ice gradient, no head and no glow. The freeze spreads from the exit side to the entry side.
- The freeze starts at the game time of the award for that cell (the moment the liquid left it) and lasts 400 ms of game clock, so it stops while paused. Reduced motion draws ice at once. The frosted glass overlay is the normal glass overlay with a white frost layer at 20 %.
- `docs/design-system.md` gets the tokens, a "Freezing cell" tile state, the ice look and the tip text.

### Accessibility
`CellDescription` gets three resource strings: `cell_freezing_empty` ("Freezing cell, empty"), `cell_placed_freezing` ("Placed %1$s on a freezing cell") and `cell_placed_frozen` ("Placed %1$s, frozen"), each with a translator comment for its placeholder. A mandatory section on a freezing cell keeps its mandatory text with ", on a freezing cell" or ", frozen" added through two more strings.

### Tip: `TipId.Freezing("freezing")`
Due at level start when the level has freezing cells and the tip is unseen, ordered after the mandatory section tip and before the cross tip. It reuses the mandatory tip's multi-cell highlight and pan-to-target. Strings: `tip_freezing_title` "Freezing Cells", `tip_freezing_text` "The liquid freezes in a section on an icy cell once it has passed. Your pipe can cross that cell only once, so a cross there can use only one channel.", `tip_freezing_target` "freezing cells". The key `freezing` is new, so it is unseen for every existing player.

### Chapter of 24 levels
The levels are 24 new `level_NNN.json` files filling one level library page. Their ids are the next free page when this change is implemented: 97 to 120 if it is the first #102 chapter to land, otherwise the page after the last chapter that landed. A rough curve:
- Levels 1 to 4 of the chapter: one or two freezing cells on an obvious route, crosses off. They teach the look.
- Levels 5 to 12: crosses on the stack and freezing cells where a self-crossing route is tempting, so the player must cross elsewhere.
- Levels 13 to 20: freezing cells combined with blocked cells and mandatory reservoirs; a mandatory cross next to freezing corridors.
- Levels 21 to 24: larger grids and faster flow, mixing every earlier rule.

Star thresholds start from `StarBenchmarks.forFreeCells` and are tuned by test play; a level's three-star score must not need a cross's second pass on a freezing cell. `BundledLevelsTest` and `LevelSolvabilityTest` pick the new files up without changes. The page unlocks with the existing page star rule.

### Existing levels and progress
Levels 1 to 96 are not edited. A test asserts that each of them parses with no freezing cells and round-trips through `LevelWriter` unchanged. Level history, achievements and settings are keyed by level id or tip key, so new ids and a new tip key leave stored data as it is.

## Risks / Trade-offs

- [The mechanic only bites on crosses, so a whole chapter may feel thin] → The level curve leans on crosses on the stack and self-crossing routes, and this is the first open question for the maintainer.
- [Players do not see why the level was lost] → The frost tile is visible before placing, the leak effect shows exactly where, and the tip explains it once. A dedicated message stays an option (open question).
- [Ice colours compete with the aqua liquid] → Ice tokens are pale and desaturated, with no glow, so flowing liquid stays the most saturated element.
- [Chapter ids collide with a sibling #102 change] → Ids are chosen when implementing, from the next free page, and `BundledLevelsTest` rejects duplicate ids.
- [Designer export drops `freezing`] → `LevelWriter` round-trip tests include freezing cells.

## Migration Plan

No migration is needed. The field is optional, `Board.freezing` defaults to empty, no stored data changes, and the new tip key is simply unseen.

## Open Questions

1. **Is "crossed only once" enough for a whole chapter?** Only a cross can be passed twice, so the rule only matters where a cross is useful. Alternatives from #141: the liquid slows down on a freezing cell, or a frozen cell also blocks placing on its neighbours. *Recommended default:* ship "crossed only once" as specified, and make it bite by giving the chapter crosses on the stack and layouts where the tempting self-crossing lies on a freezing cell. Revisit after test play; slowing down would overlap with the reservoir.
2. **Leak or its own loss reason?** Liquid reaching a frozen section could count as a leak or get its own loss reason and message ("The pipe is frozen. Level lost."). *Recommended default:* a plain leak, as #141 proposes. A `LossReason.FROZEN` and its message can be added later without changing the rule.
3. **Chapter position.** #141 suggests after the leaky tile chapter (#102 leaves the order open). *Recommended default:* after the leaky tile chapter; the level ids are the next free page when the change is implemented.
4. **How the chapter unlocks.** #102 asks whether chapters unlock with the existing page star rule. *Recommended default:* the existing page star rule, no new unlock logic.
5. **Interaction with sibling mechanics.** Fixed pipes (#135) and portals (#140) may put level-placed sections or teleports on the board. *Recommended default:* the validator rejects a fixed cross on a freezing cell like a mandatory one, and a portal cell cannot be a freezing cell; whichever change lands second adds that rule and its test.
6. **Freeze sound or haptic.** *Recommended default:* none in this change; the visual freeze is enough, and a crackle can be added with the sound effects later.
