# Proposal

## Why

Every level is currently a blank grid between a start and a finish, so the only puzzle is finding *a* route. Pre-placed sections the route must run through let a level designer force a detour, a crossing or a slow reservoir stretch, which gives levels a deliberate shape and a real puzzle instead of "shortest path, then pad for points".

## What Changes

- A level can list **mandatory sections**: pipe sections that are already on the board when the attempt starts. Only a cross (`X`) or a reservoir (`RES`) can be mandatory.
- A mandatory section works like a placed section for the liquid: it is entered, filled, scored and slowed down (reservoir) exactly the same way.
- A mandatory section can never be removed or replaced, whether or not the liquid has entered it.
- A mandatory section is **used** when the liquid has passed every channel of it: both the horizontal and the vertical channel of a cross, the single channel of a reservoir.
- The level is only won when the liquid enters the finish after every mandatory section is used. When the liquid reaches the finish with a mandatory section still unused, the finish refuses the liquid and the level is **lost**. The lost dialog says that a mandatory section was missed.
- The route only counts as complete (and fast forward only becomes available) when the route runs through every channel of every mandatory section.
- Mandatory sections are drawn with their normal pipe art on a red-tinted tile from the design system's `leak` palette, so the player can tell them apart from sections they placed.
- The level solver and the bundled-levels test only accept a level with mandatory sections when a route exists that runs through all of them.
- In code, the existing `mandatory` flag for straight and corner sections (sections that must have a stack weight of at least 1) is renamed, so "mandatory" only means a pre-placed section.

## Capabilities

### New Capabilities
- `game-play-mandatory-sections`: pre-placed mandatory sections during play: which sections can be mandatory, how they look, that they cannot be removed, when one counts as used, and that the level cannot be won while one is unused.

### Modified Capabilities
- `game-level-model`: adds the optional `mandatory` field to the level format with its validation.
- `game-play-placement`: a tile with a mandatory section never accepts a section.
- `game-play-fast-forward`: the route is only complete when it passes every channel of every mandatory section.
- `game-play-level-complete`: reaching the finish only wins when every mandatory section is used.
- `game-play-level-lost`: reaching the finish with an unused mandatory section loses the level, with its own message and field effect.
- `game-play-playing-field`: mandatory sections are drawn on a red-tinted tile.
- `level-solvability`: the solver requires a route through every mandatory section, in a direction the section allows.

## Impact

- `core/level`: `Level`, `LevelDto`, `LevelParser`, `LevelValidator`, `LevelSolver`, `PipeSection` (rename of the existing `mandatory` flag).
- `core/board`, `core/engine`, `core/route`: the board starts with the mandatory sections, placement refuses them, the route tracer and the flow simulation check that they are used, and the lost outcome carries a reason.
- `app`: the playing field draws the red tint, cell semantics describe mandatory sections, and the level lost dialog shows the missed-section message.
- `docs/level-format.md` documents the new field; `docs/design-system.md` documents the mandatory tile tint.
- Existing level files are unaffected: the field is optional and defaults to none.
- The in-flight `app-game-level-designer` change will need to write the `mandatory` field when it exports a level so that export still round-trips; editing mandatory sections in the designer is not part of this change.
