# Design

## Context

See proposal.md for the motivation. Today the whole game assumes one liquid head and one finish:

- `Level` has one `finish: Endpoint`; `Board` has one `Cell.Finish`.
- `FlowState.Flowing` is a single head (position, entry, channel, progress, used channels). `FlowSimulation.advance` walks it from tile border to tile border and `leaveTile` ends the attempt on the finish or a leak.
- `PipeSection.exitFor(entry)` returns one exit side. `FlowStep`, `RouteTracer`, `GapDistance` and the solver all build on that single exit.
- `GameState.flowSpeed` uses `sectionsLeft`, the total number of passes, which would double-count with two heads.
- `StackSettings` requires a weight for every `PipeSection` entry, and `PipeSection` doubles as the id list for stack weights.
- Mandatory sections (`Level.mandatory`, `Cell.Pipe.mandatory`, `MandatoryProgress`, `LossReason.MANDATORY_MISSED`) already give us "placed by the level, never removable, must be used before the finish counts". #135 (fixed pipes) generalises "placed by the level" further.

## Goals / Non-Goals

**Goals:**
- One splitter per level, two finishes, two liquid heads, with win, loss and collision rules that the flow, the route check, the gap warning and the solver all share.
- Levels without `splitter` and `secondFinish` behave and serialise exactly as before.

**Non-Goals:**
- More than two finishes, more than one splitter, or branches that merge again (out of scope in #137).
- Splitters dealt from the stack (see Open Questions).
- Changing the level library or the page unlock rule: the chapter is just the next 24 level files.

## Decisions

### 1. The splitter is placed by the level, not dealt from the stack

The level file gets an optional `splitter: { x, y, closedSide }`. The board starts with it like a mandatory section: it cannot be removed or replaced, it survives "remove unused", and the liquid must pass it before a finish counts.

*Why:* a splitter only makes sense with two finishes laid out around it, so the designer must control where it is. Dealing it from the stack would allow several splitters on one route (out of scope) and needs four new weights that are 0 in 96 levels.
*Alternative:* four stack section ids with weights. Kept as an open question; the rules below do not depend on how the splitter got on the board.

### 2. Four `PipeSection` entries that are not stack sections

Add `SPL_L`, `SPL_R`, `SPL_T`, `SPL_B` (named by the closed side) with three open sides each, and a `stackable` flag. `StackSettings`, the dealer, `LevelDto` weights and `LevelWriter` only use stackable sections, so `stack.weights.SPL_L` is still an unknown key. Rendering, points and `Cell.Pipe` reuse the existing section plumbing.
*Alternative:* a separate `Cell.Splitter`. Rejected: every `when (cell)` in board, placement, route, rendering and semantics would need a new branch, while a `PipeSection` gets most of it for free.

### 3. Exits become a list

Add `PipeSection.exitsFor(entry): List<Side>` (empty when the side is closed; two sides for a splitter; one otherwise) and keep `exitFor` for single-exit sections. A splitter has one channel (`Channel.SINGLE`): once the liquid has passed it, any later arrival leaks, as with a used reservoir.

### 4. Second finish: an optional `secondFinish` next to `finish`

`finish` stays required; `secondFinish` is an optional endpoint with the same shape and the same placement rules (inside the grid, not blocked, not the start or the other finish, facing cell inside and free, not facing the start directly). The validator requires `splitter` and `secondFinish` together. `Level` gets `finishes: List<Endpoint>` derived from the two.
*Why:* existing files and `LevelWriter` output stay byte-identical, and the "at most two finishes" scope is enforced by the shape of the format.
*Alternative:* a `finishes` list replacing `finish`: cleaner for more finishes later, but it would rewrite all 96 level files or need two accepted shapes. Listed as an open question.

### 5. Flow state holds up to two heads

`FlowState.Flowing` becomes a list of `Head(position, entry, channel, progress, sectionsLeft)` plus the shared `used` channels and the set of reached finishes. Before the splitter there is one head; when it leaves the splitter it is replaced by two heads, one per exit, in a fixed order (exit sides in the order `LEFT`, `TOP`, `RIGHT`, `BOTTOM`). A head that enters a finish is removed and the finish is added to `reached`.

`advance` keeps the border-to-border walk: per step it computes, for each head, the flow time to its next border at its own speed, advances every head by the smallest of those, and lets the heads that reached their border leave their tile in the fixed order. So "large ticks skip no tile" holds for two heads, and ties are deterministic.

### 6. Speed per head

Each head's speed is `cellsPerSecond × (1 + speedRampPerCell × n)`, where `n` is the number of sections that head's own path has left (the shared part before the splitter plus its own branch), times the reservoir factor inside a reservoir. Before the split this equals today's rule, so single-finish levels are unchanged. The HUD shows the higher speed of the two heads without the reservoir factor.
*Alternative:* one shared `n` over both branches: both heads would speed up twice as fast after the split. Rejected as surprising.

### 7. Collisions: a filled or filling channel leaks

A head that tries to enter a channel that is already in `used`, or that the other head is in right now, leaks at the tile it leaves, through the side it leaves. This is the existing "used cross channel" rule extended to "claimed channel". Entering the other, unused channel of a cross the other branch has passed is allowed and scores the cross's second pass. When both heads would enter the same channel in the same step, the first head in the fixed order enters and the second leaks.

### 8. Win and loss

- A head entering a finish through its open side stops there; the other head keeps flowing.
- The level is won when the second finish is reached and every mandatory section is used. Mandatory sections are checked only at that moment, on the union of both branches' channels.
- The level is lost as soon as either head leaks. The outcome's leak position and side are that head's; the other head stops where it is.
- Reaching a finish (either one) before the splitter has been passed is refused like a missed mandatory section: `LossReason.MANDATORY_MISSED` at that finish. The splitter counts as a mandatory section for `MandatoryProgress`, so the existing lost message applies.
- A head entering a finish that is already reached is a leak (it cannot happen geometrically with one open side per finish, but the rule keeps the code total).

### 9. Route check, gap and solver share the branch walk

`RouteTracer.walk` becomes a walk that forks at the splitter: it returns `Complete` only when both branches end in different finishes, no channel is used twice across the branches, and every mandatory section (including the splitter) is used. `GapDistance` takes the minimum over the live heads of their walk ahead; a head that will reach its finish has no gap. `FlowStepRouteAgreementTest` is extended with two-finish boards so the flow and the route check keep agreeing.

`LevelSolver` with a splitter: find a path from the start's facing cell to one open side of the splitter (entering through an open side, not the closed one), then two vertex-disjoint paths from the other two open sides to the two finishes' facing cells, avoiding cells the first path used. It tries each of the three entry sides and both finish assignments, nearest first, within `MAX_SEARCH_WORK`, and combines with the existing mandatory waypoint search by treating the splitter as the last waypoint of the shared part. When it fails it names the finish it could not reach (or the splitter, when the splitter itself cannot be reached). Like today it may reject a crowded level a player could win, but never accepts an unwinnable one.

### 10. Rendering

Four new pipe SVGs (`spl_l.svg`, ...) drawn per the design system's pipe recipe (steel body, glass overlay), marking the three bore ends. The liquid fill of a splitter: progress 0 to 0.5 fills from the entry side to the centre; 0.5 to 1 fills both outgoing arms at the same rate. The splitter tile uses the mandatory tile tint (`leak.tint` with a `leak.500` edge) because it is placed by the level and cannot be replaced. The second finish uses the same checkered finish and glow as the first. Cell semantics: "Splitter, closed on the <side>, liquid has passed" / "not passed yet".

### 11. Points

The splitter scores 30 points when the liquid leaves it (once, when it splits). Each branch then scores its own sections by the normal rules.

### 12. Chapter of 24 levels

24 new `level_NNN.json` files with the next free ids at implementation time. The chapter teaches in steps: the first levels have short, symmetric branches and slow flow; later ones add blocked cells, a reservoir on one branch, mandatory sections and a cross both branches use. Existing level files are not touched; `BundledLevelsTest`, `LevelSolvabilityTest` and the round-trip test cover the new files.

## Risks / Trade-offs

- [Two heads touch every flow consumer: HUD, gap warning, heartbeat, fast forward, liquid drawing, sound] → Keep a single `heads` list with "one head" as the common case and test every consumer with a one-head and a two-head state.
- [Sibling #102 changes modify the same requirements (points table, leaving a tile, solver)] → Each lands on `development` in turn; the later change merges its deltas on top before archive.
- [The solver's greedy path search may reject winnable splitter levels] → It never accepts an unwinnable one; the failure names the finish, and the designer shows it live so the author can move a blocked cell.
- [Fast forward runs both heads 10x; a late collision is hard to see] → The route check already refuses a route where the branches collide, so fast forward is only offered on a clean route.
- [`PipeSection.entries` is used as "all stack sections" in places] → Introduce `PipeSection.stackSections` and replace those uses in one task, guarded by the existing stack and parser tests.

## Migration Plan

No data migration. Saved progress and scores are keyed by level id; new ids do not touch existing ones. Rollback is reverting the change; level files 1 to 96 are untouched.

## Open Questions

Each has a recommended default that the specs and tasks already follow, so the change is implementable as written.

1. **Dealt from the stack or placed by the level?** Recommended: placed by the level, one per level (decision 1). Changing this later adds stack ids and weights; the flow rules stay.
2. **Second finish in the level format: `finishes` list or optional second finish?** Recommended: optional `secondFinish` next to `finish` (decision 4), so levels 1 to 96 stay unchanged.
3. **A branch reaching a pipe the other branch already filled?** Recommended: a leak, as for a used cross channel; the unused channel of a cross is still fine (decision 7).
4. **Do fast forward, the gap warning and the HUD change for two heads?** Recommended: fast forward only when both branches are complete and do not collide; the gap warning and heartbeat follow the nearest gap; the HUD shows the faster head's speed (decisions 6 and 9).
5. **Chapter position.** Recommended: the last of the #102 chapters, because it changes the most; the level ids follow from that order. Unlocking uses the existing page star rule unless #102 decides otherwise.
6. **Splitter points.** Recommended: 30 points, between a reservoir (25) and a second cross pass (50).
7. **Speed ramp with two heads.** Recommended: per head, counting the sections on that head's own path (decision 6).
8. **Should the second finish look different from the first?** Recommended: no, both use the checkered finish; the splitter tip highlights both.
9. **Should the splitter's tile use the mandatory red tint or its own tint?** Recommended: the mandatory tint, since it is placed by the level and cannot be replaced; revisit if #135 introduces a separate "fixed" tint.
