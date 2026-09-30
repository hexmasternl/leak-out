## Why

The game engine change (`app-game-play-game-engine`) moves the liquid through the board, but leaves how pipes and liquid look to "a separate change". There is no pipe art at all yet: the stack and the playing field still draw stand-ins. The pipes are the thing the player looks at all game, so they need finished artwork, and the liquid animation needs to be seen and tuned (speed, direction, the reservoir slow-down, the cross) before the engine is wired to it.

## What Changes

- Add an **SVG asset for every pipe section**: `H`, `V`, `LT`, `LB`, `RT`, `RB`, `X` (cross) and `RES` (reservoir). The art follows the design system's pipe recipe (steel body, shadow side, specular, bore, flanges at every open port; the cross's bridge; the reservoir's violet tank and "slow" badge). The SVG files are the single source of the pipe art: designers can open and edit them in any SVG editor.
- Each SVG also marks, by id, the **centre line of every liquid channel** and which two sides it connects, and for the reservoir the **tank shape**. The app draws the liquid along those lines, so art and liquid always line up.
- Add a **pipe tile renderer** in `app` that draws a section from its SVG at any cell size, with the liquid of each channel on top at a given progress (0 to 1) and **entry side**. Liquid can flow through every channel **in both directions**: it always starts at the side it entered through.
- **Cross**: its two channels (left–right and top–bottom) each have their own liquid, own progress and own direction. Both can be filled in one level, in any order and any direction, and they never mix.
- **Reservoir**: the liquid runs through the inlet stub, fills the tank bottom-up, then runs through the outlet stub (progress 0–0.1, 0.1–0.9 and 0.9–1). It works from left to right and from right to left. Its speed is the flow speed times a **reservoir speed factor**.
- Add a **fluid animation clock**: a pure function that turns elapsed time, a flow speed (cells per second), the section and a reservoir speed factor into a channel progress. The renderer never eases progress; the same rule is used by the engine later.
- Add a **Pipe Lab** screen, in debug builds only, with its own launcher entry. It shows all eight sections and lets the developer:
  - pick the flow direction for every channel (for the cross, for each of its two channels separately),
  - set the **flow speed** with a slider,
  - set the **reservoir speed factor** with a slider,
  - **start**, **pause** and **reset** the fluid animation.
- `docs/project-plan.md` section 2.8 and `docs/design-system.md` section 6.3 are updated: the pipe art comes from the SVG assets instead of being drawn procedurally from paths in code; the liquid is still drawn in code.

## Capabilities

### New Capabilities
- `pipe-section-assets`: The SVG asset for every pipe section: which files exist, what they show, the ids that mark the liquid channels, their sides and the reservoir tank, and the rule that every open port has a channel end.
- `pipe-liquid-rendering`: Drawing a pipe section with its liquid: progress per channel, the entry side and flow in both directions, the cross's two independent channels, the reservoir's inlet–tank–outlet phases, and the timing rule that turns flow speed and the reservoir speed factor into progress.
- `pipe-lab`: The debug-only Pipe Lab screen for previewing the sections and controlling the fluid animation: direction per channel, flow speed, reservoir speed factor, start, pause and reset.

### Modified Capabilities

None. `openspec/specs/` holds only `landscape-orientation` and `welcome-screen`, which do not change. The playing field, stack and engine specs are still in open changes; they can use the tile renderer from this change but their requirements stay the same.

## Impact

- New assets in `app/src/main/assets/pipes/`: `h.svg`, `v.svg`, `lt.svg`, `lb.svg`, `rt.svg`, `rb.svg`, `x.svg`, `res.svg`.
- New Compose code in `app` (`nl.hexmaster.leakout.ui.pipes`): a small SVG loader for the subset the assets use, `PipeArt`, `PipeTile`, `ChannelFill`, `LiquidTiming`, with JVM unit tests (parsing, progress mapping, timing) and instrumented tests (every asset loads and draws, the tile renders filled and empty).
- New debug-only code in `app/src/debug/` (`PipeLabActivity`, `PipeLabScreen`) with instrumented tests. Release builds do not contain the lab.
- No new dependencies: the SVG subset is parsed with the platform's XML parser and drawn with Compose `Path`s.
- `core` does not change. The renderer uses `core`'s `PipeSection` and `Side`.
- `docs/project-plan.md` (2.8) and `docs/design-system.md` (6.3) are updated.
