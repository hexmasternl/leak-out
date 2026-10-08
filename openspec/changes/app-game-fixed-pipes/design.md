# Design

## Context

See proposal.md for the motivation and specs/ for the behaviour. This is the first chapter of [#102](https://github.com/hexmasternl/leak-out/issues/102). The relevant parts of the current code (paths relative to `app/src/`):

- `core/level`: `Level` holds `mandatory: List<MandatorySection>`; `LevelDto` is the JSON shape; `LevelParser` turns parse failures into field-path errors; `LevelValidator` collects every meaning error; `LevelWriter` writes only non-default fields; `LevelDraft` is the designer's editable level.
- `core/board/Board.kt`: `Cell.Pipe(section, liquidEntered, mandatory)`. `Board.from(level)` places the mandatory sections. `Placement.check` rejects a mandatory pipe with `Reason.FIXED`.
- `core/flow/FlowStep.kt` decides every step of the liquid from `Cell.Pipe` alone, so anything that is a `Cell.Pipe` already flows, scores and slows the liquid like a placed section. `RouteTracer` and `GapDistance` use the same step.
- `core/score/FinalScore.kt` leaves mandatory pipes out of `unusedSections`.
- `core/level/LevelSolver.kt`: a breadth-first search over free cells, extended with an ordered waypoint search for mandatory channels. It is sound (never accepts an unwinnable level) because any path of free cells can be paved with straights and corners.
- `app`: `PlayingField.drawCell` draws the mandatory tint; `CellDescription` builds the accessibility text; `TipId`, `TipScheduler` and `TipOverlay` run the tips; the designer is `LevelDesignerScreen`, `DesignerGrid` and `LevelDesignerViewModel`.
- Levels 1 to 96 are `app/src/main/assets/levels/level_001.json` to `level_096.json`; the library puts 24 levels on a page.

## Goals / Non-Goals

**Goals:**
- Fixed pipes as level data, enforced in `core` so every rule is unit-testable on the JVM.
- A "placed by the level" notion on the board that valves (#136) and portals (#140) can reuse.
- A solver that may route through fixed pipes and still never accepts an unwinnable level.
- A chapter of 24 solvable levels that teaches the mechanic.

**Non-Goals:**
- Fixed pipes that rotate or move during play.
- Making fixed pipes required (that is what mandatory sections are).
- A new chapter unlock rule: the existing page star rule is used.

## Decisions

### Level format: a top-level `fixed` array
`"fixed": [ { "x": 3, "y": 2, "section": "LT" } ]`, optional, default `[]`. It parses into `Level.fixed: List<FixedPipe>` (position plus section, authored order kept so errors name `fixed[i]`). `section` parses as a `PipeSection`, so an unknown id is a parse error. The validator checks: inside the grid, not blocked, not on the start or finish, not on a mandatory cell, no duplicates. No cap on the number of entries (see Open Questions). `LevelWriter` writes `fixed` only when it is not empty.
*Alternative:* a `required: false` flag on `mandatory` entries. Not chosen as the default: it changes the meaning of an existing field, needs the `X`/`RES` rule and the 6-entry cap to depend on the flag, and mixes two different tile looks in one list. It is listed as an open question.

### Board: `Cell.Pipe` gets `fixed: Boolean = false` and a `placedByLevel` property
`Board.from(level)` adds `Cell.Pipe(section, fixed = true)` for each entry. `Cell.Pipe.placedByLevel` is `mandatory || fixed`, and every rule that today asks `mandatory` only because the player didn't place the section switches to it:
- `Placement.check`: `placedByLevel` returns `Rejected(FIXED)` before the replace checks, so hover, drop and tap agree.
- `FinalScore.of`: filters `placedByLevel` out of `unusedSections`, so the removal sequence never touches a fixed pipe.
Rules that are about being *required* (`MandatoryProgress`, the finish refusal, the route check's `MANDATORY_MISSED`) keep asking `mandatory`. Because a fixed pipe is a `Cell.Pipe`, `FlowStep`, `FlowSimulation`, `RouteTracer`, `GapDistance`, points and the reservoir speed need no change; unit tests prove it.
*Alternative:* a new `Cell.Fixed` type. Rejected for the same reason as for mandatory sections: every `is Cell.Pipe` branch in core and app would need a second case.

### Points: a fixed pipe scores its normal points
The issue says a fixed pipe behaves like a placed section "with the same points", and `Points` already awards per `Cell.Pipe`. So nothing changes in the scoring code; the spec makes it explicit. See Open Questions.

### Solver: fixed pipes are one-way cells for the search
Today `shortestPaths` is a cell BFS over free cells. It becomes a BFS over search nodes:
- a free cell is one node, entered from any side, left through any other side;
- a fixed pipe is one node per channel (two for a fixed cross, one otherwise). It can be entered only through an open side of that channel and is left through `exitFor(entry)`.
Each node is visited at most once per search, so a returned path never reuses a free cell or a fixed channel; a fixed cross can appear twice, once per channel, like a mandatory cross. Segments end when the step out of the last node enters the target (a mandatory channel's entry side or the finish's open side), so a fixed pipe can also be the cell right before a waypoint or the finish. The start's facing cell is entered from the start's open side, so a fixed pipe there that has no opening on that side makes the level unsolvable with a reason that names the start. Fixed channels used by earlier segments go into the `used` set like ordinary cells.
*Trade-off:* a free cell is marked when it is first reached, from whichever side. That keeps the search linear in the grid size, but in rare layouts the side it was first reached from is not the one a later fixed pipe needs, so the solver can reject a level a player could win. It can never accept an unwinnable one, because every returned route is a real self-avoiding route that enters every fixed pipe through an open side. `docs/level-format.md` states this next to the existing mandatory-section caveat.

### Rendering: a bolted steel tile under the pipe art
In `PlayingField.drawCell`, a fixed `Cell.Pipe` first draws its tile: fill `steel.700` at 35 %, a 1.5 dp `steel.300` edge, and four bolts (radius 0.05 c, `steel.300` with a `steel.100` highlight, centred 0.12 c in from each corner). Then the usual pipe art, liquid and glass are drawn on top. The bolts sit in the corners, where no section's pipe art reaches, so they show on every section. No new colour is needed: all tokens exist in the design system's steel palette. `docs/design-system.md` gets a "Fixed pipe" row in the cell states table (6.2), and the designer grid reuses the same draw function.
*Alternative:* a brass frame. Rejected: brass is the start valve's colour and would suggest an endpoint.

### Accessibility
`CellDescription` reads a fixed pipe as "Fixed <section name>, empty" or "Fixed <section name>, filled", reusing the existing section names. A cell that is both mandatory and fixed cannot exist (the validator forbids it).

### First-time tip
A new `TipId.Fixed("fixed")` is stored like the others, so no migration is needed. It is due when a level with fixed pipes starts and the tip has not been seen, and is ordered after the mandatory tip and before the cross tip: `Intro, Mandatory, Fixed, Cross, Reservoir`. The overlay uses one spotlight cut-out per fixed cell and the existing pan-to-target behaviour. Title "Fixed Pipes", text "These pipes are bolted down by the level. You can't remove or replace them, but the liquid runs through them like any other pipe, and your route doesn't have to use them." The strings go into `values/strings.xml`; Play's automatic translation covers the other supported languages, as for every other string (spec `app-localization`). A row is added to the tip table in `docs/design-system.md`.

### Level designer
A Fixed pipe tool with a section picker (the eight section icons, using the stack's pipe art). Tap places, removes or changes as specified; a drag that starts on a fixed pipe moves it, and the drop target is checked with the same "free cell" rule. `LevelDraft` gets the matching operations, so they are unit-tested in `core`. The Start, Finish, Block and mandatory tools skip fixed cells, and shrinking the grid drops fixed pipes that fall outside it, as for blocked cells. `LevelExporter` and test play already go through `Level`, so they pick up `fixed` from `LevelWriter` and `Board.from`. The view model's saved state stores the fixed list and the chosen section.

### The chapter: 24 levels on the next free page
`level_097.json` to `level_120.json` by default (see Open Questions). The curve: levels 97 to 100 put one or two helpful fixed straights on small grids; 101 to 106 add corners that point the wrong way; 107 to 114 combine fixed pipes with blocked cells and the cross and reservoir on the stack; 115 to 120 add mandatory sections and larger grids. Star thresholds use the designer's formula. Each level is built in the designer or by hand, then checked by `BundledLevelsTest`, `LevelSolvabilityTest` and the round-trip test. Levels 1 to 96 are not touched.

### Achievements
`AttemptSummary` takes its route facts from `GameState.flowPath`, so a fixed pipe the liquid runs through counts for route-shape achievements, as a mandatory section does today. `sectionsPlaced` counts placements only, so fixed pipes never count as placed.

## Risks / Trade-offs

- [Solver false negatives with fixed corners] → Documented; the failure names the endpoint or mandatory section that could not be reached, and moving a fixed pipe or a blocked cell fixes it.
- [Players mistake a fixed pipe for a mandatory one] → Different tile (steel and bolts versus the red tint), its own tip and its own accessibility text.
- [Many fixed pipes make the tip's spotlight busy] → One cut-out per cell is what the mandatory tip does; chapter levels keep the count small early on.
- [Achievements or tests that assume 96 levels] → `AchievementReachabilityTest` and the library tests are run in full; anything that hard-codes 96 is fixed to read the bundled count.
- [Sibling changes from #102 add fields to the same files] → Each sibling adds its own optional field; whichever lands later merges `LevelDto`, `LevelWriter` and the docs. Chapter level numbers are only fixed when the order in #102 is decided.

## Migration Plan

No migration. The `fixed` field is optional, existing levels parse and play unchanged, level history is keyed by level id, and the new tip key is simply absent from existing installs.

## Open Questions

1. **Does a fixed pipe score?** Recommended default: yes, its normal points, as the issue's proposed behaviour says. Alternative: 0 points because the player did not place it, which would need a points rule and changes the star maths.
2. **New `fixed` list or `required: false` on `mandatory`?** Recommended default: a new `fixed` list, which keeps the meaning and rules of `mandatory` intact. The alternative would also allow mandatory straights and corners but changes an existing field.
3. **Which chapter, which levels?** Recommended default: the first new chapter, page 5, levels 97 to 120, as suggested in #135. Final order depends on #102.
4. **How does the chapter unlock?** Recommended default: the existing page star rule (50 of 72 stars on page 4), as #102 assumes unless it decides otherwise.
5. **Cap on fixed pipes per level?** Recommended default: no cap beyond the free cells, since fixed pipes don't add waypoints to the solver. Alternative: a cap like the 6 for mandatory sections.
6. **Look of a fixed pipe.** Recommended default: darker steel tile with four corner bolts, as in this design. The maintainer may prefer another treatment from the design system.
7. **Do fixed cells count as free for the designer's star thresholds?** Recommended default: yes, as mandatory cells do, so thresholds stay comparable with existing levels.
8. **Fixed pipe on the start's or finish's facing cell?** Recommended default: allowed by the format and left to the solver, which can make for a nice "the first pipe is given" opener.
