## Context

`GameScreen` lays out a `Column` with `windowInsetsPadding(WindowInsets.safeDrawing)`. It holds `GameHud` (fixed height `HudLayout.HEIGHT`, drawn with `Modifier.hudPanel()`) and below it a `Row` with the stack column and the playing field. `MainActivity` hides the system bars (immersive), so in landscape the horizontal part of `safeDrawing` is mostly the display cutout. The app targets SDK 37, so the window is edge-to-edge and may draw into the cutout area; the inset comes from our own padding.

`WindowInsets.displayCutout` in Compose only gives the summed inset per side, not where on that side the cutout is. A punch-hole camera in the middle of a short edge gives the same left or right inset as a cutout in a corner. The bounding rectangles (`DisplayCutoutCompat.boundingRects`, from `ViewCompat.getRootWindowInsets`) tell where the cutout actually is.

## Goals / Non-Goals

**Goals:**

- The HUD panel and its content use the full screen width when no cutout overlaps the HUD band.
- HUD content never sits under a cutout that does overlap the HUD band.
- The stack and the playing field keep their current cutout handling.

**Non-Goals:**

- Letting the stack or the playing field reach into the free areas next to the cutout.
- Changing the HUD height, items or panel style.
- Portrait game layouts (the game stays landscape-locked).

## Decisions

1. **Split the insets.** The outer column only applies the vertical `safeDrawing` insets. The HUD applies its own horizontal insets from decision 2. The stack and field row applies the horizontal `safeDrawing` insets as now. Alternative: draw only the HUD background full width and keep its content inset. Rejected because the issue asks for the HUD content to use the space too, and an empty strip inside the panel would look odd.
2. **Pure overlap check.** `hudHorizontalInsets(cutoutRects, hudTop, hudBottom, screenWidth): HudInsets(left, right)` (pixels, plain Kotlin types, no Android dependencies) returns, per side, how far a cutout that reaches into the HUD band `[hudTop, hudBottom)` extends from that edge, or 0 when no cutout overlaps the band. A rect that touches neither side edge (a top-centre notch) counts for the side it is closer to, and only when it overlaps the HUD band. The game screen reads the rects from the root window insets and recomputes when the insets or the configuration change (turning to the other landscape side).
3. **Fallback.** When there are no bounding rects (API below 28, or no cutout), the HUD uses no horizontal cutout inset, which is the full-width case. System gesture insets are not applied to the HUD, as now.

## Risks / Trade-offs

- [Cog wheel close to a cutout on some devices] → the overlap check uses the full HUD band, so any cutout reaching into the HUD height pushes the content in on that side.
- [Emulators cannot always simulate a side cutout] → the overlap logic is covered by JVM unit tests; the instrumented test uses the emulator's display cutout setting where it is available.

## Open Questions

- Should the HUD also use the full width when a cutout overlaps only the panel's rounded corner but none of its content? This design treats any overlap with the HUD band as "keep clear".
