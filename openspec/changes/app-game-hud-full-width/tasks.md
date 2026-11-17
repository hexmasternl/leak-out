## 1. HUD insets

- [x] 1.1 Add a pure `hudHorizontalInsets(cutoutRects, hudTop, hudBottom, screenWidth)` helper in `app/ui/game` that returns the left and right inset the HUD needs (0 when no cutout overlaps the HUD band).
- [x] 1.2 JVM unit tests: no cutout; cutout in the middle of the left or right edge below the band; corner cutout overlapping the band on each side; top-centre notch; cutouts on both sides.

## 2. Game screen layout

- [x] 2.1 In `GameScreen`, apply only the vertical `safeDrawing` insets to the outer column and give the stack and field row the horizontal `safeDrawing` insets.
- [x] 2.2 Read the display cutout bounding rects from the root window insets, derive the HUD band from the top inset and `HudLayout.HEIGHT`, and pad the HUD with the helper's result; recompute on inset and rotation changes.
- [x] 2.3 Check the HUD panel shape and the cog wheel button at the screen edges on both landscape sides. (checked on the emulator by the maintainer)
- [x] 2.4 Keep the HUD content inside the display's rounded top corners (`RoundedCorner`, Android 12+): the panel stays full width, the content moves in past the curve plus a margin.

## 3. Tests

- [x] 3.1 Instrumented test: with a side cutout, the HUD spans the full screen width and the stack and field stay clear of the cutout, on both landscape sides. (`HudCutoutTest`, which fakes the cutout through `LocalCutoutRects`; compiled, not run on a device yet)
- [ ] 3.2 Run `:app:testDebugUnitTest` and the affected instrumented tests. (`:app:testDebugUnitTest` passes; the instrumented test was compiled but not run: no device attached)
