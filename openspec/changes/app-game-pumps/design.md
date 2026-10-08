# Design

## Context

See proposal.md for the motivation. The pieces a pump touches already exist for the reservoir, which is its mirror image:

- `PipeSection` (core) lists the sections with their open sides. `exitFor` gives the exit side for an entry side, and the route check (`RouteTracer`/`FlowStep`) and the flow both use it. `RES` is open left and right only.
- `GameState.flowSpeed` is `cellsPerSecond × (1 + speedRampPerCell × sectionsLeft)`. `FlowSimulation.speedIn` multiplies it by `reservoirSpeedFactor` inside a reservoir. The HUD reads `flowSpeed`.
- `LevelDefaults` holds the defaults and the stack weights. `LevelDto`/`LevelParser`/`LevelValidator`/`LevelWriter` read, check and write the level file. `LevelValidator.MAX_CELLS_PER_SECOND` is 10.
- Mandatory sections (`Level.mandatory`, `MandatoryProgress`, `LevelSolver`) accept only `X` and `RES` today. The solver treats a mandatory `RES` as one left–right channel.
- `Dealer` deals by weight and forces only the straights and corners by the drought limit.
- `Points.forExit` is an exhaustive `when` over `PipeSection`. So are a few app-side `when`s (cell descriptions, tip scheduler, liquid spans, Pipe Lab), and `AttemptSummary` in core. The compiler will point at each of them.
- Pipe art is one SVG plus a `-glass.svg` per section in `assets/pipes/`, drawn by the pipe tile. Liquid follows the `channel-*` path marked in the asset.
- Tips are scheduled by `TipScheduler` and stored as seen in the settings DataStore (`SettingsRepository`). Tip texts are English string resources. English is the only resource language; Play Console translates them automatically (`app-localization`).
- The level library has 4 pages of 24 levels (1 to 96). A page opens at 50 stars on the page before.

## Goals / Non-Goals

**Goals:**
- A pump section that the stack can deal and a level can make mandatory, with a permanent, multiplying speed boost that is capped.
- Keep core pure: the boost is derived from game state, not stored in the renderer.
- Levels 1 to 96 play exactly as before, in score and in timing.
- 24 bundled pump levels that pass the solver check.

**Non-Goals:**
- Pumps that slow the flow, pumps with a limited reach, or a vertical pump.
- A new sound effect for the pump. The pump uses the existing placement and points sounds; a sound can follow later.
- New achievements for pumps.
- Changing how chapters unlock. The pump page uses the existing page star rule.
- The order of the mechanic chapters (#102). This change only adds the next page.

## Decisions

### 1. One `PUMP` section, open left and right, two-way
`PipeSection.PUMP(alwaysOnStack = false, openSides = {LEFT, RIGHT})`. `exitFor` needs no change: a single channel gives the other open side. This mirrors `RES`, so the route check, the solver's channel model and the mandatory progress work without a new shape.
*Alternatives:* a horizontal and a vertical pump (`PUMP_H`, `PUMP_V`) gives more routes but doubles the asset, the weight keys, the designer tools and the tip; a one-way pump overlaps with the valve (#136). Left as an open question.

### 2. The boost is counted, not stored as a speed
`GameState` gets `pumpsLeft: Int`, derived like `sectionsLeft` from the score sheet's passes: the passes on tiles that hold a `PUMP` (placed or mandatory). A pass is recorded in the "leaving a tile" step when the points are awarded, so the boost starts once the liquid has left the pump. `flowSpeed` becomes:

```
ramped = cellsPerSecond × (1 + speedRampPerCell × sectionsLeft)
flowSpeed = if (pumpsLeft == 0) ramped
            else min(ramped × pumpSpeedFactor^pumpsLeft, max(MAX_CELLS_PER_SECOND, ramped))
```

With `pumpsLeft == 0` the expression is exactly today's, so a level without a pump is bit-for-bit unchanged. The cap reuses `LevelValidator.MAX_CELLS_PER_SECOND` (10) and never lowers a ramped speed that is already higher. The reservoir factor still applies on top in `speedIn`. Fast forward keeps scaling flow time, not speed, so it stacks on the boost. Restart clears the score sheet, so `pumpsLeft` starts at 0.
*Alternatives:* storing a "current speed" in state would duplicate the ramp formula and risk drift between the HUD and the flow. A boost for a number of cells after the pump is an open question; the counter can grow into "cells left of boost" if needed.

### 3. `flow.pumpSpeedFactor`, default 1.5, range (1, 3]
A new optional `Double` in `LevelFlow`, `LevelDefaults.PUMP_SPEED_FACTOR = 1.5`. Validator error on `flow.pumpSpeedFactor` when it is not greater than 1 or above 3. `LevelWriter` writes it only when it differs from the default, like the other optional flow fields, so existing level files and their round trip stay unchanged.

### 4. Weight `PUMP`, default 0, never forced
`STACK_WEIGHTS[PUMP] = 0`. The parser accepts `stack.weights.PUMP`; the validator allows 0 or more, as for `X` and `RES`. `Dealer.droughtLimit` already returns `null` for sections that are not `alwaysOnStack`, so `PUMP` is never forced; a test pins this down.

### 5. 40 points
`Points.PUMP = 40`. The pump is the best-paying single pass after the second pass of a cross (50), which matches the risk. The final-score unused-section penalty applies to a pump like any other section.

### 6. Mandatory pump
`LevelValidator` accepts `X`, `RES` and `PUMP` as mandatory. `MandatoryProgress` and `LevelSolver` treat `PUMP` exactly like `RES` (one left–right channel). The mandatory section tip already highlights every mandatory section; the pump tip is then shown too when not yet seen.

### 7. Look: impeller housing and a "fast" badge in a new pump colour
`pump.svg`: a horizontal steel pipe with a round steel impeller housing (radius about 0.3 c, like the reservoir tank) holding four curved impeller blades, flanges left and right, and a "fast" badge (a pill with a double chevron) in the top-right corner. `pump-glass.svg` follows the glass recipe. The design system gets a `pump.300/500/700` colour family in a warm orange (proposal: `#FDBA74`, `#F97316`, `#C2410C`), used only for the badge and the blades, never for the liquid, the same way violet means "slow" for the reservoir. Orange stays clear of `leak.500` red and `brass.500`. The liquid fills the single channel like `H`, with progress factor 1 (`LiquidTiming` needs no new factor).
The faster flow is visible in two ways: the liquid itself moves faster, and the HUD flow speed number (which already reads `flowSpeed`) jumps and pulses once in `pump.500` over about 600 ms. Reduced motion skips the pulse.
*Alternative:* spinning the impeller while the liquid passes. Nice, but it needs a rotating group in the SVG renderer, which the supported SVG subset does not have. Left out.

### 8. Pump tip
A `PUMP` tip in `TipScheduler`, due when a `PUMP` is in a fully visible stack slot (same rule as the reservoir tip) or when the level starts with a mandatory pump. It comes last in the order intro, mandatory, cross, reservoir, pump. Text (English resource, percentage = `(pumpSpeedFactor − 1) × 100`, rounded half up): **Pump** — "A pump only connects left and right. Once the liquid has left it, the flow runs 50 % faster for the rest of the level. It scores 40 points." A new "pump tip seen" key in the settings DataStore; a missing key reads as not seen, so existing installs see the tip once.

### 9. Designer
A Pump speed factor field next to the reservoir speed factor, a `PUMP` weight field (nine weights), and a Mandatory pump tool next to the cross and reservoir tools, sharing their rules. `LevelDraft` gets the pump kind; live validation and the solver need no designer-specific work.

### 10. The pump chapter
24 files on the next free page. If the pump is the first mechanic chapter to land, that is `level_097.json` to `level_120.json` (page 5). Each is made in the designer or by hand, checked by `LevelSolvabilityTest`, `BundledLevelsTest` and a new chapter test that asserts every level of the page deals a pump (`PUMP` weight > 0) or has a mandatory pump. A gentle curve: the first levels have a mandatory pump on a straight route and a low factor (1.25 to 1.5), later levels deal pumps from the stack, mix them with reservoirs, and use factors up to 2. Star thresholds follow the designer's calculated thresholds, adjusted upward for pump points where play-testing shows it.

## Risks / Trade-offs

- [The boost makes a level unwinnable in practice although the solver says it is solvable] → The solver only checks the route's shape, as for every level. Keep chapter factors at 2 or less and play-test each level; the cap stops runaway speed.
- [Changing `flowSpeed` shifts the timing of existing levels] → The formula is unchanged when `pumpsLeft == 0`; a test replays scripted attempts on levels 1 to 96 and compares the score and the tick of the outcome with the current engine.
- [Exhaustive `when`s over `PipeSection` in places not listed here] → The compiler flags them; the task list asks for a full build of both modules.
- [Chapter level ids clash with a sibling mechanic that lands first] → The chapter takes the next free page at implementation time; ids are fixed only when the levels are written.
- [Orange badge too close to the red leak warning for some colour-blind players] → The pump is told apart by shape (blades, chevron badge) as well as colour; checked in the Pipe Lab in greyscale.

## Migration Plan

No data migration. The new tip key defaults to not seen. Existing level files, level history and top scores are untouched. Rollback is removing the chapter's level files and the `PUMP` section; no stored data refers to them except history rows of the new level ids, which an older build ignores as unknown levels.

## Open Questions

Each has a recommended default that the specs and tasks already follow, so the change can be implemented without waiting.

1. **Permanent boost or for a number of cells?** (issue) Recommended: permanent for the rest of the attempt, as specified. A limited reach is out of scope in the issue.
2. **Dealt from the stack or only placed by the level?** (issue) Recommended: both. Dealt with its own weight (`PUMP`, default 0), and also possible as a mandatory section.
3. **How many points?** (issue) Recommended: 40.
4. **Left-right only, or a horizontal and a vertical variant?** (issue) Recommended: left-right only, like the reservoir. A vertical variant can be added later as its own section id without changing this one.
5. **Default `flow.pumpSpeedFactor` and its range?** Recommended: 1.5, greater than 1 and at most 3, as proposed in the issue.
6. **Cap.** Recommended: 10 cells per second, the existing maximum level speed; a pump never lowers a ramped speed that is already higher.
7. **Chapter position.** (issue, #102) Recommended: after the fixed pipe (#135) and valve (#136) chapters. Until #102 settles the order, the chapter takes the next free page when it is implemented (97 to 120 if it is the first).
8. **How do chapters unlock?** (#102) Recommended: the existing page star rule (50 of 72 stars on the page before).
9. **Pump colour.** Recommended: a new `pump.*` orange family (`#FDBA74`, `#F97316`, `#C2410C`) for the badge, the blades and the HUD pulse; the liquid keeps its colour.
10. **A pump sound?** Recommended: none in this change; reuse the existing sounds.
