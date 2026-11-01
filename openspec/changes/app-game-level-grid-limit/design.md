## Context

`LevelValidator.MAX_GRID_SIZE` is 50 and applies to both columns and rows; `LevelDraft` and the designer view model reuse it. The bundled levels grow steadily in size: levels 55 to 96 range from 19 by 16 up to 24 by 24. The game is played in landscape, so the playing field has far more room across than down; the rows are what makes the cells small. Star thresholds of the bundled levels follow the designer's rule (three stars = free cells times 10), and the flow speed and initial delay were tuned per level.

## Goals / Non-Goals

**Goals:**
- Enforce at most 25 columns and 15 rows everywhere a level is validated or designed.
- Bring every bundled level within that limit and keep all of them solvable.
- Keep the difficulty curve from level 55 to 96 rising, using the density of blocked cells, mandatory sections, portals and flow speed instead of grid size.

**Non-Goals:**
- Changing the minimum grid size, the level file format or any other validation rule.
- Clearing, migrating or flagging online scores of the redesigned levels.
- Re-tuning levels 1 to 54, which already fit.

## Decisions

- **Separate column and row limits.** Replace `MAX_GRID_SIZE` with `MAX_GRID_COLUMNS = 25` and `MAX_GRID_ROWS = 15`, and give each dimension its own range in the validator and in `LevelDraft`. One shared limit cannot express 25 by 15.
- **Redesign, not crop.** Cutting a 24 by 24 level down to 15 rows would usually remove the finish or a mandatory section and break the intended route. Each oversized level is rebuilt at the new size (in the designer or by editing its JSON), keeping its id, name, theme and features, then verified with the solver.
- **Stars from free cells.** Star thresholds of the redesigned levels are recalculated with the designer's rule, so they stay consistent with every other level.
- **Flow and stack re-tuned.** A smaller grid means shorter routes; the initial delay and cells per second are adjusted so each level still gives roughly the same pressure, and the difficulty still rises from 55 to 96.
- **Oversized designs shrink on restore.** A design kept in saved state from before this change that is larger than 25 by 15 goes through the existing shrink rules when it is restored, so the designer never shows a grid outside the range.

## Risks / Trade-offs

- [Scores on redesigned levels no longer compare with the new layouts] → Accepted by the maintainer in #159; no server change.
- [Redesigning 42 levels is a lot of work and may flatten the difficulty curve] → Work in batches, verify each batch with the solver and the headless tester, and play a sample on a device.
- [Level history and achievements on the device refer to level ids] → Ids do not change, so history and achievements stay valid; only a stored best score may be harder or easier to beat.

## Open Questions

- Should the redesigned levels keep their best scores and stars in the local level history, or should those be reset for levels 55 to 96?
- Should the 25 by 15 limit apply to the designer too, or only to bundled levels so the designer can still be used for larger experiments? This change applies it everywhere, as the issue asks.
