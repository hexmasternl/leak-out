## Context

Levels 1 to 96 are bundled as JSON in `app/src/main/assets/levels/`. Their base speed rises from 0.13 to 0.4 cells per second and their initial delay sits between 20 and 30 seconds; since `app-game-level-grid-limit` the late levels are up to 25 by 15. The library shows 24 levels per page and opens page `p+1` once page `p` holds 50 stars. `BundledLevelsTest` checks that every level parses, validates and is solvable, and has rules about where portals appear. The flow speed is capped at 10 cells per second by the level model.

## Goals / Non-Goals

**Goals:**
- A fifth page whose difficulty comes from speed, not grid size.
- Levels a player can finish: short routes, enough head start.
- A rising curve within the pack.

**Non-Goals:**
- New mechanics, new achievements (for example `pack_5_*`) or a new library layout.
- Re-tuning levels 1 to 96.
- A speed ramp per cell; the pack keeps `speedRampPerCell` at 0 so the speed shown in the HUD stays the speed the player plays against.

## Decisions

- **Grid size 6–12 by 5–8.** Small enough that the whole field reads at a glance in landscape, large enough for a few walls, a mandatory section and a portal pair. The grids grow slightly through the pack.
- **Initial delay 28–32 seconds.** Around the 30 seconds asked for; slightly longer on the levels with the longest routes.
- **Speed 0.5 → 0.9 cells per second**, rising in even steps (about 0.017 per level). At 0.9 a cell fills in just over a second, so a placed route is consumed almost as fast as it is built.
- **Reservoir factor 0.1.** With fast liquid a reservoir is the main tool to win time; the issue for reservoir types (#163) asks for the strongest slow-down on the fastest levels. The value is valid today, so this change does not depend on #163 landing first.
- **Doability rule.** For each level the shortest route (from the solver) has length `L`. The level counts as doable when `initialDelayMs / 1000 + L / cellsPerSecond >= 1.5 × L`, so a player placing one section every 1.5 seconds stays ahead of the liquid along that route, ignoring reservoirs. `BundledLevelsTest` checks this for levels 97 to 120. Shortest routes are kept at 30 cells or fewer.
- **Features.** Cross and reservoir weights on most levels, mandatory sections on about half, portals on fewer than half (as for levels 50 to 96).
- **Stars.** Calculated with the designer's rule from free cells, like every other level.
- **Authoring.** Levels are built with the level designer or by hand and checked with the solver and the headless tester, in batches of eight.

## Risks / Trade-offs

- [Too hard on real devices despite the rule] → Play a sample on a device; the rule leaves room to lower speeds per level.
- [Too easy because grids are small] → The speed and the 1.5-second rule keep pressure; tune after play-testing.
- [Pack achievement expectations] → Only page 1 has pack achievements; adding `pack_5_*` would need Play Console setup and fits the XP cap only with care, so it is left out (see Open Questions).

## Open Questions

- Is 0.9 cells per second on level 120 the right ceiling, or should the pack go faster?
- Should page 5 get its own pack achievements (`pack_5_complete`, `pack_5_perfect`)?
- Should the page unlock threshold for page 5 stay at 50 stars on page 4?
