# Design

## Context

`drawPopups` in `app/.../ui/game/PlayingField.kt` draws each award as Fredoka Bold text at 0.4 × the cell size, in two passes: a 3 dp `InkDark` (`#1E293B`, slim palette) stroke, then a white fill on top. A stroke is centred on the glyph outline, so half of its 3 dp eats into the white fill. At the smallest cells (44 dp, text ≈ 17.6 sp) that leaves little white inside the strokes of the digits, so "+10" reads as an outline, a heavy, hollow number (see proposal.md, Why). The timing (`popupFrame` in `PointsPopup.kt`) is pure and well tested, and the issue asks to keep it as it is.

The design system has no popup entry at all; the colours it does define for "alive" things are `liquid.glow` (`#9DF3FF`, the glow colour) and the `G` glow level (§4.3: `0 0 24` at 45 %).

## Goals / Non-Goals

**Goals:**
- A popup that reads at a glance at every zoom level, over an empty tile, steel pipe and bright liquid alike.
- The popup look is described in the design system and the app follows it, with tokens only.

**Non-Goals:**
- Timing, rise, scale, font, or position of the popup. The fade and rise stay exactly as they are.
- Popups anywhere other than the playing field (the result dialog tally is unaffected).
- A Daylight-theme variant: the game board is always drawn in the Deep palette.

## Decisions

1. **Draw order: glow, stroke, fill.** Three passes of the same `TextLayoutResult`: a glow pass, a 2 dp `liquid.glow` stroke, then the solid `surface.deep` fill on top. A stroke is centred on the glyph outline, so the fill hides its inner 1 dp and leaves a crisp 1 dp edge outside the glyph. *Alternative*: a 1 dp stroke drawn on top of the fill. Rejected on the gallery (task 1.3): Fredoka's "+" and "1" are built from overlapping contours, and a stroke on top outlines those overlaps inside the glyph, so "+" reads as "⊕". Under the fill, the overlaps are covered.

2. **Fill is `surface.deep`, fully opaque (times the popup alpha).** The issue asks for an "opaque surface.deep" number. Opaque means the pipe and liquid do not show through the digits, which is what makes the number solid instead of hollow. The fill is multiplied by the popup's alpha so it still fades in and out.

3. **The visible edge is 1 dp (a 2 dp stroke), a fixed dp width, not a fraction of the cell.** The issue asks for a *thin* border; a fixed 1 dp stays thin when the player zooms in (where a fraction would grow to a thick outline again) and stays visible at the 44 dp minimum cell. *Alternative*: 0.02 c. Rejected for the zoom-in reason.

4. **Glow is a `liquid.glow` blur, 6 dp radius at 70 %, not the 24 dp `G` level.** A dark number on the dark board needs a halo to separate it from the pipe art; the 24 dp `G` glow is sized for buttons and the finish ring and would cover a whole 44 dp tile and its neighbours. 6 dp at 70 % gives a tight, bright halo around the digits. It is drawn as a text shadow (`Shadow(LiquidGlow.copy(alpha = 0.7f × popup alpha), Offset.Zero, 6.dp.toPx())`) on a fill pass in `liquid.glow`, the same technique `ResultDialogLayout` uses for the score glow, so no native canvas or `BlurMaskFilter` is needed. The final values are settled on the design-system gallery (task 1.3) and recorded in the design system; the spec only requires "a soft glow".
   *Alternative*: `BlurMaskFilter` on the native canvas, as in `CountdownBar`. Rejected: more code for the same look, and `drawText` already supports shadows.

5. **Style lives in a pure `PopupStyle` object in `PointsPopup.kt`.** Colours (`SurfaceDeep`, `LiquidGlow`), stroke width, glow radius and glow alpha, next to `PopupTiming`. `drawPopups` only reads it. This makes the tokens unit-testable on the JVM and replaces the private `POPUP_OUTLINE` constant. `POPUP_TEXT_FRACTION` moves there too, reduced from 0.4 to 0.33 (decision 7).

6. **Reduced motion: no change.** The glow is static (it does not pulse), and the fade and rise are gameplay feedback that §7.4 already leaves alone; the design system says so explicitly.

7. **Text shrinks from 0.4 c to 0.33 c.** The issue title says the indicator is too large. On approving the gallery (task 1.4), the product owner asked for a smaller number as well as the thinner edge. 0.33 c (about 14.5 sp at the 44 dp minimum cell) keeps "+50" well inside its tile, also at full rise. It is still sized to the tile, so it scales with zoom.

## Risks / Trade-offs

- [The dark fill on a dark empty tile could read as a hole] → The glow and stroke carry the shape; verified on the gallery over an empty tile, a filled straight and a filled corner (task 1.3) and on the emulator screenshot (task 3.1) before the change is accepted on the issue.
- [The three passes multiply text drawing per popup] → At most a handful of popups are on screen at once and the `TextLayoutResult` is measured once and reused, so the cost is negligible.
- [Shadow blur radius is in px on `drawText`] → Converted from dp per frame with the field's density; covered by the unit test of `PopupStyle` (dp values) and the screenshot check (rendered).
