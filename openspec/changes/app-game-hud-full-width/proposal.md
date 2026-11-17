## Why

In landscape the whole game screen (HUD, stack and playing field) is inset by the display cutout, because `GameScreen` applies `WindowInsets.safeDrawing` to the column that holds all three. On phones with a camera hole in the middle of a short edge, the cutout sits beside the stack or the field, not beside the HUD. The HUD still stops short of that edge, leaving an empty strip above the camera hole. The `game-play-hud` spec already asks for a HUD "across the full width of the screen", so the current layout does not meet it.

Tracked in [#184](https://github.com/hexmasternl/leak-out/issues/184).

## What Changes

- The HUD spans the full screen width whenever no display cutout overlaps the HUD's vertical band.
- When a cutout does overlap the HUD band (for example a corner cutout or a notch at the top), the HUD keeps its content clear of the cutout on that side only.
- The stack column and the playing field keep avoiding the cutout, as they do now.
- A small pure function decides whether a cutout overlaps the HUD, from the cutout's bounding rectangles, the HUD band and the screen width. It has unit tests.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `game-play-hud`: the "HUD bar at the top of the game screen" requirement spells out how the HUD relates to the display cutout.

## Impact

- `app/ui/game/GameScreen.kt`: the HUD gets its own horizontal insets instead of sharing the column's `safeDrawing` padding; the stack and field row keeps the full `safeDrawing` insets.
- New pure helper in `app/ui/game` (for example `HudInsets.kt`) that turns cutout rectangles into left and right insets for the HUD, with JVM unit tests.
- Instrumented test for the game screen with a cutout beside the field and beside the stack, where the emulator can simulate a cutout.
- No change to `core`, the level format or the server.
