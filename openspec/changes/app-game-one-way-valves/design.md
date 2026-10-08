# Design

## Context

See proposal.md for the motivation. Today every section is two-way: `PipeSection.exitFor(entry)` in `core/level/PipeSection.kt` returns the other open side whatever side the liquid came in through, and both the flow (`FlowStep`, `FlowSimulation`) and the route check (`RouteTracer`) rely on it. `PipeSection` is also the list of stack ids: `StackSettings` requires a weight for every entry, so a new enum value would change the `stack.weights` format and every level's defaults.

Mandatory sections already show how a level puts a section on the board: `Level.mandatory`, `Board.from(level)` adds them as `Cell.Pipe(mandatory = true)`, placement refuses those tiles, `FinalScore` skips them, the designer has tools for them and the playing field tints them. The fixed pipes change ([#135](https://github.com/hexmasternl/leak-out/issues/135)) generalises this into "placed by the level" cells. Valves need the same treatment plus a direction.

## Goals / Non-Goals

**Goals:**
- Valves are a level-placed, directed straight. One rule in `core` decides how liquid passes them, and the flow, the route check and the solver all use it.
- Levels without `valves` parse, write, play, score and solve exactly as before.

**Non-Goals:**
- Valves on the stack, on corners or on crosses (out of scope in #136).
- Requiring the route to pass a valve (see Open Questions).
- A new loss reason or lost-dialog message for valves.

## Decisions

### 1. Valves are a separate list, not new `PipeSection` ids

`Level` gets `valves: List<Valve>` with `Valve(position, direction)`. `direction` is a new enum `FlowDirection { LEFT, RIGHT, UP, DOWN }` with its outlet `Side` (`UP` → `TOP`, `DOWN` → `BOTTOM`) and its inlet (the opposite side). The JSON uses the issue's names, so the file reads as "the liquid flows RIGHT".

*Alternative:* four new section ids (`VL`, `VR`, `VU`, `VD`). Rejected: every level would need weights for them, they would show up in the stack weight controls, and the stack could deal them, which is out of scope.

### 2. On the board a valve is a straight pipe with a direction

`Board.from(level)` puts each valve on the board as `Cell.Pipe(section = H or V, valve = direction)`. A pipe with a `valve` is placed by the level: placement refuses it, `FinalScore` never counts it as unused, and the unused-section removal after a win leaves it. If #135 has landed, use its "placed by the level" flag for this instead of a second flag, so placement, scoring and win clean-up need no valve-specific branches.

Keeping the section `H` or `V` means scoring (`ScoreSheet.award`), rendering of the steel body and the liquid path all work as for a straight without changes.

### 3. One direction rule, shared by flow and route check

Add `Cell.Pipe.exitFor(entry: Side): Side?` (or an equivalent function in `core/flow`) that returns `null` for a valve when `entry` is not its inlet, and otherwise defers to `PipeSection.exitFor`. `FlowStep.after` and `RouteTracer` call it instead of `section.exitFor`. A `null` exit is already a leak in `FlowStep` ("the next section is not connected"), so the wrong-way case needs no new code path: the leak is reported at the tile the liquid left, through the side it left by, with `LossReason.LEAK`, and the lost dialog shows the normal leak message.

`FlowSimulation.leaveTile` takes the exit from `section.exitFor(entry)`; it can keep doing so because a valve is only ever entered through its inlet.

### 4. Solver: valves are one-way edges

The solver's breadth-first search works on cells. A valve cell is not an open cell (like a mandatory cell); instead, when the search stands on the valve's inlet neighbour, it may step through the valve to its outlet neighbour, and the valve cell is added to the path. Each valve can be used once. The start's facing cell may be a valve whose inlet faces the start; the finish's facing cell may be a valve whose outlet faces the finish. Mandatory waypoints are joined by the same search, so they honour valves for free.

For the reason of an unsolvable level: when the search fails and the level has valves, run it once more with valves treated as two-way. If that finds a route, the reason names the first valve on it that the route passes against its direction, for example "No route from the start's facing cell (1, 2) can pass the valve at (4, 2) (valves[0]) in its direction RIGHT". Otherwise the existing reasons apply. This costs one extra search on unsolvable levels only, within `MAX_SEARCH_WORK`.

### 5. Validation rules

`LevelValidator` checks, reporting all problems together:
1. At most `MAX_VALVES` = 8 valves.
2. Each valve lies inside the grid, not on a blocked cell, the start, the finish or a mandatory section, and not on the same cell as another valve.
3. Each valve's inlet and outlet neighbour can carry the route: inside the grid and not blocked, or the start when the start faces the valve (inlet only), or the finish when the finish faces the valve (outlet only). A valve that can never be passed would just be a wall, which blocked cells already are.

An unknown `direction` is a parse error that names `valves[i].direction`.

### 6. Rendering

The valve uses the `H` or `V` steel asset, plus a valve overlay drawn between the glass and the liquid head: a brass collar band across the middle of the pipe and a brass chevron pointing to the outlet, reusing the start valve's chevron geometry (design system 6.4) so "brass arrow" already means "liquid goes this way". The tile is not tinted, so it is not confused with a mandatory section. The chevron stays visible over the liquid. The liquid fill always uses the inlet as its entry side. `docs/design-system.md` 6.2 and 6.4 get a "One-way valve" row.

### 7. Accessibility

`CellDescription` gets two strings: "One-way valve, flows %1$s" and "One-way valve, flows %1$s, filled", with the direction as a translated word (left, right, up, down).

### 8. Onboarding

New `TipId.Valve("valve")`. The tip is shown before the countdown the first time a level with at least one valve starts, highlighting every valve on the board, like the mandatory section tip. Order: intro tour, mandatory section tip, valve tip, cross tip, reservoir tip. All tip text is in `res/values/strings.xml`; Play Console translates it into the supported languages like every other string.

### 9. Level designer

A Valve tool with a direction control (`LEFT`, `RIGHT`, `UP`, `DOWN`, default `RIGHT`). Tapping a free cell places a valve with the selected direction; tapping a valve with the same direction removes it; tapping a valve with another direction turns it to the selected one. A drag that starts on a valve and ends on a free cell moves the valve there. The Start, Finish, Block and mandatory tools do nothing on a valve cell. `LevelDraft` keeps valves, so they survive process death, are exported and are on the board in a test play.

### 10. The valve chapter

24 new bundled levels form one level library page and introduce valves gently: the first few have one valve that the obvious route crosses in its direction, then valves that force a detour, then valves combined with blocked cells, mandatory sections and crosses. Every file passes `LevelSolvabilityTest`; they are made with the designer. The level ids are the next 24 free ids after the chapters that ship before it (see Open Questions). The page unlocks with the existing page star rule; no library code changes.

## Risks / Trade-offs

- [Sibling changes touch the same files (`Level`, `LevelDto`, `Board`, `LevelValidator`, `LevelSolver`, designer, `TipId`)] → Implement after or rebase on whichever sibling lands first, and reuse its "placed by the level" flag (#135) rather than adding a parallel one.
- [Solver greediness: one-way edges make the greedy shortest paths between mandatory waypoints more likely to block each other] → The solver stays sound (it never accepts an unwinnable level). If a chapter level is wrongly rejected, move a valve or blocked cell; the reason names the valve.
- [Players may read the brass chevron as decoration] → The valve tip explains it, and the arrow points the same way as the start valve's chevron.
- [Level ids depend on chapter order] → Ids are chosen when the chapter is built; the files are self-contained so renumbering is a rename plus the `id` field.

## Migration Plan

No migration. `valves` defaults to an empty list, `LevelWriter` leaves it out when empty, so levels 1 to 96 round-trip unchanged and their scores, stars and history keys stay the same. The tip store ignores unknown keys, so an older app reading the new `valve` key is unaffected. Rollback is removing the chapter files and the feature code.

## Open Questions

1. **Should valves be dealt from the stack instead (four new section ids, one per direction)?** Recommended default: no. Valves are placed by the level only, as in this design; stack valves can be a later change.
2. **Does liquid arriving from the wrong side count as a leak, or a dead end with its own loss reason and message?** Recommended default: a normal leak (`LossReason.LEAK`), shown and worded like any other leak.
3. **Where does the valve chapter go?** Recommended default (issue #136): the second new chapter, right after fixed pipes (#135), so levels 121 to 144 if fixed pipes take 97 to 120. If valves ship first, they take the next free page (97 to 120). Chapter order is still open in #102.
4. **Must the route pass every valve?** Recommended default: no. A valve is optional; levels make it matter by putting it in the only gap of a wall. A "mandatory valve" can be added later through the mandatory list.
5. **How many valves may a level have?** Recommended default: 8 (`LevelValidator.MAX_VALVES`).
6. **How does the valve chapter unlock?** Recommended default: the existing page star rule (50 of 72 stars on the previous page), unless #102 decides otherwise.
7. **Valve look:** brass chevron and collar on an untinted straight (recommended), or a tinted tile like mandatory sections?
