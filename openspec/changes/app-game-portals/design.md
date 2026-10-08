# Design

## Context

See proposal.md for the motivation and specs/ for the behaviour. The parts of the current code that shape the approach (paths relative to `app/src/`):

- `core/level/Level.kt` holds concrete values; `LevelDto` is the JSON shape, `LevelParser` turns parse failures into field-path errors, `LevelValidator` collects every meaning error, `LevelWriter` writes only non-default fields. `Endpoint` already models "a cell with an open side and a facing cell".
- `core/board/Board.kt` builds the starting board from the level (`Cell.Start`, `Cell.Finish`, `Cell.Blocked`, mandatory `Cell.Pipe`). `Placement.check` is the single placement rule for hover and drop.
- `core/flow/FlowStep.after(board, from, exit, used)` decides what happens when the liquid leaves a tile. `FlowSimulation.leaveTile` uses it; `RouteTracer.walk` (route check and gap distance) has its own copy of the same rules. `FlowState.Flowing` holds a position, entry side, `Channel` and progress, and `used` is a set of `ChannelUse(position, channel)`.
- `core/level/LevelSolver.kt` is a breadth-first search over free cells with mandatory channels as waypoints. Its path is a list of cells in flow order; `HeadlessPlayTest` and `ScriptedPlay` turn consecutive path cells into straights and corners, so they assume consecutive cells are neighbours.
- `Level.freeCells` and `AttemptSummary.freeCells` both define "free cell" (star thresholds, Wall to Wall).
- Tips are keyed by `TipId` and stored as seen in DataStore. Strings live only in `res/values/strings.xml`; the other languages come from Play Console's automatic translation on release.
- Fixed pipes (#135) and one-way valves (#136) are being specified at the same time and also put things on the board that the level owns.

## Goals / Non-Goals

**Goals:**
- Portals as level data with all rules in `core`, unit-testable on the JVM.
- One portal rule shared by the flow, the route check, the gap distance and the solver, so they cannot disagree.
- No change for levels without portals: old files, ids, scores and saved progress stay the same.

**Non-Goals:**
- One-way portals, portals that move during play, portals as stack sections.
- A new chapter unlock rule (the page star rule stays).
- Server changes: scores are keyed by level id.

## Decisions

### Level format: a top-level `portals` array of pairs
```json
"portals": [
  { "a": { "x": 2, "y": 1, "openSide": "LEFT" }, "b": { "x": 6, "y": 5, "openSide": "RIGHT" } }
]
```
Optional, default `[]`, at most 3 pairs. Each portal reuses the endpoint shape (`x`, `y`, `openSide`), so parsing and the facing-cell checks can share code with the start and finish. The pair's number (and so its colour and symbol) is its index + 1; there is no explicit colour field, which keeps the file small and makes duplicate colours impossible.
*Alternative:* a flat list of portals with a `pair` id or colour name. Rejected: needs extra validation (exactly two per id, no gaps) for no benefit.

### Model: `PortalPair(a: Endpoint, b: Endpoint)` and `Cell.Portal`
`Level.portals: List<PortalPair>`, with `portalAt(position)` and `partnerOf(position)`. On the board a new `Cell.Portal(pair: Int, openSide: Side, partner: Position)`; it is not a `Cell.Pipe`, so it never scores, never counts as an unused section and is skipped by every `is Cell.Pipe` branch. `Placement.check` rejects it like a mandatory section. If fixed pipes (#135) are already merged, reuse its "owned by the level" rejection reason rather than adding a new one.
*Alternative:* a new `PipeSection.PORTAL`. Rejected: it would appear in stack weights, the dealer and the points table, and a portal has a single open side, not a channel through the cell.

### Flow: a pair is one "tile" for the head
When `FlowStep.after` meets a `Cell.Portal` entered through its open side and the pair's channel is unused, it returns `Enter(next = portal, entry = openSide)`. The head is then in the pair with `Channel.PORTAL` and runs progress 0 → 1 at the normal speed (no reservoir factor), so passing a pair takes one cell's time. When it leaves, `leaveTile` uses the partner as the tile it leaves from and the partner's open side as the exit, so the next tile, the leak position and the leak side are all at the exit portal. No points and no `PathStep` are added, and the speed ramp's section count does not grow. The pair counts as used through a `ChannelUse` on both portal cells.
Because each portal has one open side and the cells in front of them are filled pipes, a used pair can never actually be reached again; the used check is kept for loop safety and symmetry with the cross.

### One walk rule for flow, route check and gap distance
`RouteTracer.walk` gets the same portal branch, and the shared next-tile decision moves into one helper used by both `FlowStep` and `RouteTracer`, so `FlowStepRouteAgreementTest` keeps them equal. The gap distance counts a pair as 1 (head inside a pair: `1 - progress`).

### Solver: portal pairs as teleport edges
In `shortestPaths`, a cell that is the facing cell of a portal also links to the facing cell of the partner portal, if that cell is open. Portals themselves are not open cells. The returned path includes the two portal cells, in flow order, between the two facing cells, so consumers can see the jump. Facing cells already on the route block a second use of the same pair, so a route passes a pair at most once. `HeadlessPlayTest` and `ScriptedPlay` skip portal cells when choosing sections and treat the portal's open side as the side the neighbour connects to. The unsolvable reasons stay the same; a portal is never named as the reason.
*Alternative:* treat each pair as a waypoint like a mandatory channel. Rejected: portals are optional to use, and the BFS edge keeps the search cost unchanged.

### Validation
Applied to every portal (field `portals[i].a` / `.b`): inside the grid; not on a blocked cell, the start, the finish, a mandatory section or another portal; facing cell inside the grid, not blocked, not the start, finish or a portal. The start's and finish's facing cells must not be a portal. A portal's facing cell may hold a mandatory section; the solver's `canAdjoin` learns that a portal facing a mandatory channel can feed it.

### Free cells
`Level.freeCells` and `AttemptSummary.freeCells` exclude portal cells. Levels 1 to 96 have no portals, so their thresholds and Wall to Wall do not change.

### Rendering
Per the design system's endpoint recipe: the plain empty-cell tile, the straight-pipe stub clipped from the open side to the centre (as the start and finish), and a portal ring of radius 0.24 c in the pair colour with the pair symbol in `text.primary` (Deep) on a `surface.deep` disc. Three new tokens are added to `docs/design-system.md` §2.5 and §6.4, each with a symbol: pair 1 `portal.1` pink `#FF6FD8` with a circle, pair 2 `portal.2` lime `#B8F25C` with a triangle, pair 3 `portal.3` orange `#FF8A3D` with a square. Contrast against `surface.deep` must be at least 3:1 and is recorded in §2.8. The ring slowly swirls (`motion.drift`) and freezes on pause and with reduced motion. Liquid: progress 0–0.5 fills the entry stub towards the ring, 0.5–1 fills the exit stub from the ring outwards, with the normal liquid look and head (6.5).
Accessibility: "Portal pair 1, circle, opens right, leads to column 7, row 4, not passed yet" (columns and rows 1-based as in the existing cell descriptions).

### Onboarding
A new `TipId.PORTAL`, shown before the countdown on the first level with portals, highlighting every portal, after the mandatory section tip and before the stack tips (board tips first, then stack tips). Its strings go into `res/values/strings.xml` and are translated by Play Console like every other string; translators get a `<!-- -->` comment for each.

### Level designer
`LevelDraft` gets `portals: List<DraftPortalPair>` where `b` can be missing, plus the selected portal. The draft turns into a `Level` only when every pair is complete; an unfinished pair is a draft error (`portals[i].b`) listed like a validation error. A Portal tool, an open side control and a Remove pair action follow the existing tool and endpoint patterns. The draft is saved through the existing saved-state path.

### Chapter of 24 levels
24 new `level_NNN.json` files with the next free ids, one full library page. If portals are the first mechanic chapter to ship, that is levels 97 to 120; otherwise the ids follow the chapters already bundled. The chapter ramps up: one pair that the shortest route must use, then pairs that are optional shortcuts, then two pairs, then three pairs combined with blocked walls and mandatory sections. At least one level is solvable only through a pair. Every level is designed and exported with the level designer, then checked by `LevelSolvabilityTest`, `HeadlessPlayTest`, `BundledLevelsTest` and the achievement reachability tests.

## Risks / Trade-offs

- [The solver's path is no longer a chain of neighbours] → the path includes the portal cells explicitly, and `HeadlessPlayTest` and `ScriptedPlay` are updated and run over the new chapter.
- [Flow and route check drift apart] → one shared next-tile helper and `FlowStepRouteAgreementTest` cases with portals.
- [Colour-blind players cannot tell pairs apart] → every pair has its own symbol and the accessibility text names the pair and its partner cell.
- [Chapter ids clash with sibling chapters (#102)] → ids are taken at implementation time from the last bundled level; the chapter is one self-contained page.
- [Sibling changes also extend `Cell`, `Placement` and the tip order] → whichever lands second rebases onto the first; the specs only add portal cases to shared rules.

## Migration Plan

No data migration. Old level files are untouched and a level without `portals` parses to the same `Level` as before (`BundledLevelsTest` and the round-trip test prove it). Rollback is reverting the change; saved results for the new level ids would then simply have no level to show.

## Open Questions

- **Do pairs work both ways or one way?** Recommended default: both ways (as in the issue and these specs). One-way portals are out of scope.
- **Can each pair be used only once per attempt?** Recommended default: yes, as one channel. With one open side per portal a second use cannot happen in play anyway, so the rule only guards loops.
- **Chapter position.** Recommended default: after the leaky tile and freezing cell chapters, as the issue suggests; the final order is decided in #102. The level ids follow from that order.
- **How do chapters unlock?** Recommended default: the existing page star rule (50 of 72 stars), as #102 suggests unless it decides otherwise.
- **Pair colours and symbols.** Recommended default: pink circle, lime triangle, orange square as above; the maintainer may pick other values in the design system, as long as each pair differs in both colour and symbol.
- **Does a portal count for the speed ramp?** Recommended default: no, it is not a pipe section.
- **Reuse of fixed-pipe groundwork (#135).** Recommended default: if #135 is merged first, reuse its "owned by the level" placement rejection and designer patterns; otherwise implement them here and let #135 reuse them.
- **May a portal's facing cell be shared with the start, the finish or another portal's facing cell?** Recommended default: yes, as the start and finish may share one; the validator does not forbid it.
