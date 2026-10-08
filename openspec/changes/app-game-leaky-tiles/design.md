# Design

## Context

See proposal.md for the motivation and specs/ for the behaviour. The parts of the current code this change touches:

- `Level` (core/level) holds concrete values. `LevelDto` is the JSON shape, `LevelParser` turns parse failures into field-path errors, `LevelValidator` collects every meaning error and `LevelWriter` writes only non-default fields. `blocked` and `mandatory` are lists in authored order, so the validator can name duplicates by index.
- `Board.from(level)` builds the starting board from `Cell` values. Placement (`Placement.check`) looks only at the board cell, so a leaky tile that is an ordinary empty cell on the board is placeable without any change.
- `FlowSimulation.leaveTile` awards points through `ScoreSheet.award(at, section)`, which uses `Points.forExit(section, pass)` and keeps a pass count per tile. `Award(position, points, atMs)` drives the popup.
- `FinalScore.of(earned, board)` already clamps the total at 0.
- `LevelSolver` searches free cells, with an ordered waypoint search when there are mandatory sections. Its result is `Solvable(path)` or `Unsolvable(reason)`.
- `StarBenchmarks.forFreeCells(level.freeCells)` gives the designer's thresholds. `AttemptSummary.freeCells` feeds the `WALL_TO_WALL` achievement.
- The only string resources are the default (English) ones in `app/src/main/res/values`; other languages come from Play Console's automatic translation (`app-localization`).

## Goals / Non-Goals

**Goals:**
- Leaky tiles as level data and one scoring rule, all in `core`, so every rule is JVM-unit-testable.
- No change to the flow path, speed or route check, so `FlowStepRouteAgreementTest` keeps holding without new cases beyond leaky boards.
- A chapter of 24 levels that the existing solver test guards.

**Non-Goals:**
- A liquid volume or level-wide reservoir that can run dry (out of scope in #138).
- Leaky tiles that slow or pause the liquid.
- A per-level penalty value. The penalty is one constant.
- A new sound effect or haptic for a leak penalty. The existing award sound plays as it does now; see Open Questions.
- Changing how chapters unlock or how the level library pages work.

## Decisions

### Level format: a top-level `leaky` array of cells
`"leaky": [ { "x": 3, "y": 2 } ]`, optional, default `[]`, the same cell shape as `blocked`. It goes into `Level.leaky: List<Position>` with `Level.isLeaky(position)`. The validator checks inside the grid, not blocked, not start or finish, not a mandatory section and no duplicates, each error naming `leaky[i]`. `LevelWriter` writes it only when non-empty, so every bundled level written today stays byte-identical.
*Alternative:* a `cells` map with a kind per cell (`blocked`, `leaky`). Rejected: it would change the existing `blocked` field and every bundled level.
*Alternative:* allow a mandatory section on a leaky tile. Rejected for now (see Open Questions): it doubles the cases in the designer and the tip for little puzzle value.

### Leaky is a level property, not a board cell type
The board keeps `Cell.Empty` and `Cell.Pipe` on a leaky tile. The engine and the renderer ask `level.isLeaky(position)`. So placement, replacement, the replace lock, the route tracer, `FlowStep` and the gap warning need no change, and the leaky flag survives a replacement and a restart by construction.
*Alternative:* `Cell.Pipe(leaky = true)` and a `Cell.Leaky` empty cell. Rejected: every `is Cell.Empty` / `is Cell.Pipe` branch in core and app would need care, and a replacement would have to copy the flag.

### Scoring: one rule in `ScoreSheet`
`ScoreSheet.award(at, section, leaky: Boolean = false)` still counts the pass, then returns `-Points.LEAKY_TILE_PENALTY` (20) when `leaky`, otherwise `Points.forExit(section, pass)`. `FlowSimulation.leaveTile` passes `state.level.isLeaky(flow.position)`. `Award.points` becomes negative for a penalty; the popup reads the sign to choose "+N" or "−N" and the colour. `ScoreSheet.total` may go below 0.
*Why 20:* it is more than a straight or corner scores, so a leaky pass always costs more than the cheapest detour cell earns (10 to 15), but small enough that one crossing on a long route still allows stars.
*Alternative:* clamp the running score at 0. Rejected: a leaky tile right after the start would then cost nothing, which rewards crossing it early.

### Final score, HUD and history
`FinalScore.of` already clamps the total at 0, so stars, the level history, achievements and online score submission never see a negative value. The HUD score bar clamps its fill to `0..1` and shows the signed number, using a real minus sign. The final-score dialog shows signed earned points.

### Solver: reachability unchanged, plus a leak-free probe
Leaky tiles are free for `LevelSolver`, so a level that is solvable today stays solvable. `SolverResult.Solvable` gains `leakFree: Boolean`: after a successful solve, the solver runs the same search once more on a copy of the level in which every leaky tile is treated as blocked. That second run reuses all existing code and its bound on work. The designer shows the note from this flag; the bundled-levels test does not require it.
*Alternative:* a weighted shortest-path that minimises leaky tiles. Rejected: more code than the designer note needs.

### Star thresholds without leaky tiles
`Level.freeCells` keeps its meaning for the `WALL_TO_WALL` achievement (a leaky tile is still a cell a pipe can go on). A new `Level.scoringCells` (free cells minus leaky tiles) feeds `StarBenchmarks.forFreeCells` in `LevelDraft`. So a leaky tile never inflates the three-star target with points that cannot be earned on it.

### Rendering
- **Tile** (`docs/design-system.md` 6.2): the empty-cell fill and border, plus two or three crack strokes in `line.strong` at 70 %, a damp blob in `liquid.700` at 25 % and, top-right, a drip badge (a drop glyph in `leak.500` on `surface.deep`, the same size and place as the reservoir's "slow" badge). The badge is drawn above the pipe art so it stays visible on a placed section; the cracks and the blob are drawn under it.
- **Drips** (6.5): while the head is in a section on a leaky tile, three drops in `liquid.500` fall from the bore to the cell's bottom edge over 600 ms each, staggered, on the game clock. After the head leaves, the damp blob switches to `liquid.700` at 45 % (the wet stain). Reduced motion: no drops, the stain appears when the head enters.
- **Popup**: the existing popup recipe with `leak.500` replacing `liquid.glow` for stroke and glow.
- **Semantics**: `CellSemantics` adds the leaky texts from string resources ("Leaky tile, empty", "… on a leaky tile").

### Onboarding
A new `TipId.LEAKY_TILE` with its own seen flag in the settings store, triggered on level start like the mandatory section tip and highlighting every leaky tile. The order becomes intro, mandatory, leaky, cross, reservoir. Its texts go into the default string resources only, which is how every supported language is served.

### Chapter of levels
24 new `level_NNN.json` files at the next free ids when the change is implemented (97 to 120 if this is the first #102 chapter to land). Difficulty rises over the page: the first levels have one leaky tile and an obvious detour; later levels mix in blocked cells, mandatory sections and finally routes where every path crosses a leaky tile. Star thresholds come from the designer's calculation (`scoringCells`), then are tuned by play-testing. `LevelSolvabilityTest`, `BundledLevelsTest` and the round-trip test cover them automatically.

## Risks / Trade-offs

- [A negative `Award.points` breaks code that assumes awards are positive, such as sounds, haptics or achievements that sum awards] → grep every `awards` and `points` use in app and core; add a unit test for the achievement rules with a negative award.
- [Chapter ids clash with sibling #102 chapters landing in a different order] → the ids are chosen at implementation time from the last bundled level; the design does not fix them.
- [The leak-free probe doubles solver time on crowded levels] → it only runs after a successful solve and shares the existing work bound; the 50 by 50 timing guard test covers it.
- [Players read cracks as blocked] → the drip badge and the tip; the instrumented test checks a drop on a leaky tile works.

## Migration Plan

No data migration. The `leaky` field is optional, the existing level files and saved history are not touched, and a new seen flag defaults to "not seen". Rollback is removing the chapter files and the field; no saved state depends on it.

## Open Questions

- **What does a leaky tile drain?** Points, time (the liquid pauses there) or a new liquid volume. *Recommended default:* points, as specified here: no section points and a 20-point penalty per pass. Time would overlap with the pump and reservoir mechanics, and a volume is out of scope.
- **Can the score of an attempt go below zero?** *Recommended default:* the running score in the HUD may go below 0; the final total of a won attempt is clamped at 0 as it is today, so stars, history and online scores never see a negative value.
- **Where does this chapter go in the chapter order?** The issue suggests after the valve (#136) and pump chapters. *Recommended default:* use the next free 24 ids when this change is implemented; the order is decided in #102.
- **Is 20 the right penalty?** *Recommended default:* 20, one constant (`Points.LEAKY_TILE_PENALTY`), retuned by play-testing the chapter without a spec change other than the number.
- **May a mandatory section lie on a leaky tile?** *Recommended default:* no, the validator rejects it. It can be allowed later without changing the scoring rule.
- **Should a penalty have its own sound or haptic?** *Recommended default:* no new asset; keep the existing award feedback. A dedicated drip sound can follow.
