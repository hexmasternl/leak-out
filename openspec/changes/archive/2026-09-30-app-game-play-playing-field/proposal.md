## Why

The game screen has a stack and a HUD, but the board next to them is only a stand-in grid (`PlaceholderBoard` from `app-game-play-stack`). It always squeezes the whole grid into the space it gets. The bundled levels now grow to 16 × 12 cells, so on a phone the cells become too small to see or hit. The player also gets no feedback on where a section will land while dragging, and cannot replace a placed section. The playing field is where the game is played, so it needs to be readable, easy to move around and clear about where a drop is allowed.

## What Changes

- Replace the stand-in board with the real **playing field**. It fills all the space that is left on the game screen after the HUD (top) and the stack column (left).
- When a level starts, the field shows the **complete level**, centred, if the level fits at a readable cell size. If it does not fit, the field starts at the smallest allowed cell size and shows the part of the level around the start cell.
- **Pan**: when the level is larger than the field, the player presses on the field and drags to move the level around. The level cannot be moved further than its edges. When the level fits on an axis, it stays centred on that axis and cannot be moved on it.
- **Pinch to zoom**: the player zooms in and out with two fingers, around the point between the fingers. The zoom has a lower and an upper limit, so cells never get too small to read or hit, and never get so large that only a few cells are visible. The player cannot zoom out further than needed to see the whole level.
- The level can **never be rotated**. A two-finger twist does nothing.
- **Hover highlight**: while the player drags a section from the stack over the field, the tile under the section is highlighted when the section can be dropped there.
  - Empty tiles are highlighted.
  - Blocked tiles, the start tile and the finish tile are never highlighted.
  - A tile with a placed section is highlighted only when the liquid has not entered that section yet and the level allows replacing.
- **Replacing**: dropping a section on a tile that holds a placed section the liquid has not entered replaces it. The old section is gone. Once liquid has entered a section, it can never be replaced or removed. A replacement starts the level's short replace lock (`rules.replacePenaltyMs`), during which no drop is accepted.
- A drop is accepted on exactly the tiles that are highlighted. So the highlight and the result of the drop always agree.
- The board model moves into `core`: every cell is empty, blocked, start, finish or a placed pipe that knows whether liquid has entered it. The flow change will set that flag later.

## Capabilities

### New Capabilities
- `game-play-playing-field`: The playing field on the game screen: where it is placed, how the level is fitted and centred, panning, pinch to zoom and its limits, no rotation, and the drawing of the grid, start, finish, blocked cells and placed sections.
- `game-play-placement`: Dropping a dragged section on the field: which tiles are highlighted during a drag, which drops are accepted, replacing a placed section, the lock on sections the liquid has entered, and the replace lock.

### Modified Capabilities

None. The stack, HUD and pause specs are still in the open changes `app-game-play-stack` and `app-game-play-hud` and are not in `openspec/specs/`. Their requirements stay the same: the stack still changes only when the field accepts a drop, and pausing still cancels a drag, which now also clears the highlight.

## Impact

- New pure Kotlin code in `core` (`nl.hexmaster.leakout.core.board`): `Cell`, `Board` and a placement check (place, replace or reject), with JVM unit tests.
- New Compose code in `app` (`ui/game/`): `PlayingField`, which replaces `PlaceholderBoard`, and a pure `FieldViewport` for the fit, zoom limits, pan limits and screen-to-tile mapping, with unit and instrumented tests.
- `SectionDropTarget` (from `app-game-play-stack`) gets a `canPlace(section, position)` query next to `tryPlace`. The drag overlay asks it on every move to update the highlight.
- `GameSession` (from `app-game-play-hud`) holds a `Board` instead of a map of placed sections, and keeps the replace lock time.
- This change builds on `app-game-play-stack` and `app-game-play-hud` and must merge after both.
- `docs/project-plan.md` sections 1.2, 1.7 and 2.8 are updated: the board is panned and zoomed instead of always fitted, and a drop replaces an unfilled section.
