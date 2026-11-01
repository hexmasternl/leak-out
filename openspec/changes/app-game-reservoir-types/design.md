## Context

`FlowSettings.reservoirSpeedFactor` (default 0.3, valid range (0, 1]) already multiplies the liquid speed inside a reservoir (`FlowSimulation`). No bundled level sets it, so every reservoir runs at 30 %. The reservoir artwork comes from `res.svg` with the design system's violet `reservoir.*` tokens and a "slow" badge. The reservoir tip already reads the factor and shows it as a percentage. The base flow speed of the bundled levels rises from 0.13 to 0.4 cells per second.

## Goals / Non-Goals

**Goals:**
- Four reservoir types (10, 20, 30, 40 %) that the player can tell apart by colour and by label.
- A reservoir type chosen per level, defaulting to 30 %.
- Bundled levels where faster flow means a stronger slow-down.

**Non-Goals:**
- Different reservoir types within one level (see Open Questions).
- Recolouring the liquid: it stays aqua.
- Changing reservoir points, the Snail achievement or star thresholds.

## Decisions

- **Per level, not per tile.** The issue asks for "this 30% configurable per level", and the field already exists on `flow`. Keeping it there keeps the level format, the stack (one `RES` kind) and the engine unchanged. A per-tile type would need four section kinds in the stack and the dealer.
- **A closed set of values.** The JSON keeps a number so existing files stay valid. The validator accepts the factor when it is within 1e-9 of 0.1, 0.2, 0.3 or 0.4, and the error names `flow.reservoirSpeedFactor` and the allowed values. A `ReservoirType` enum in `core/level` (`RED_10`, `BLUE_20`, `PURPLE_30`, `ORANGE_40`) maps a factor to its type, so the app picks the accent from the type, not from the number.
- **Accent tokens.** Add `reservoir.red.*`, `reservoir.blue.*` and `reservoir.orange.*` (300/500/700, like the violet set) to the design system; the existing violet `reservoir.*` set becomes the purple type. Proposed hues, to be checked in the design pass: red `#FF7A90`/`#F0506E`/`#B83250` (pinker than `leak.500`, so a reservoir does not read as a leak), blue `#7FA8FF`/`#4D7CFE`/`#2F56C9` (deeper than the liquid aqua), orange `#FFC48A`/`#FF9F43`/`#D9771F` (yellower than `portal.3`).
- **Tint at draw time.** The reservoir SVG keeps a single set of violet colours. `PipeArt` replaces the three violet tokens with the type's tokens when it draws a reservoir, so there is still one source file per section. The badge shows the percentage ("10 %") next to the hourglass.
- **Designer snaps.** The designer shows a four-way selector. A design restored from saved state with another factor (for example 0.5) snaps to the nearest type, ties going to the higher factor.
- **Bundled level tuning by base speed.** Below 0.2 cells per second: 40 % (orange). From 0.2 to below 0.3: 30 % (purple, field left out). From 0.3 to below 0.37: 20 % (blue). From 0.37: 10 % (red). The writer leaves out the field when it is 0.3, so only levels with another type change.

## Risks / Trade-offs

- [Colour clash with the leak red, the liquid aqua and portal orange] → Distinct hues plus the percentage on the badge; a design review is part of the tasks.
- [Late levels become easier with a 10 % reservoir] → Intended by the issue; scores already submitted online may be beaten more easily. Accepted, no server change.
- [Strict validation rejects designs or level files with other factors] → Only bundled levels and designer output exist; the designer snaps on restore.

## Open Questions

- Should one level be able to mix reservoir types (for example a red and a blue reservoir in the same stack)? This change assumes one type per level.
- Are the speed bands for the bundled levels (below 0.2 → 40 %, 0.2 to 0.3 → 30 %, 0.3 to 0.37 → 20 %, from 0.37 → 10 %) what the maintainer has in mind?
- Are the proposed accent hues acceptable next to the leak red and the portal colours?
