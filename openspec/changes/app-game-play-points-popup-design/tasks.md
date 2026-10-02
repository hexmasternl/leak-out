# Tasks

Tracking issue: [#51 Section score indicator is too large](https://github.com/hexmasternl/leak-out/issues/51).

## 1. Design system (gate)

- [x] 1.1 Add **Points popup** to `docs/design-system.md` §7.2: the existing timing (150 ms fade-in at 80 → 100 % size, 500 ms hold, 350 ms fade-out, rise of 0.25 c over 1 000 ms), text 0.4 c, and the look: `surface.deep` fill over a 2 dp `liquid.glow` stroke (a 1 dp visible edge), `liquid.glow` glow (6 dp blur at 70 %), all at the popup alpha (design decisions 1–4); verify every token named exists in §2 and the timings match `PopupTiming`
- [x] 1.2 Add the popup to §7.4 (keeps its fade and rise, static glow) and add a "Points popup" row to the §12 *What changed from the slim version* table (white fill with a 3 dp `InkDark` outline → `surface.deep` fill, 1 dp `liquid.glow` edge and glow); verify the section reads consistently with §1.1 ("no 3 px ink borders")
- [x] 1.3 Add a popup sample to the `docs/design-system.html` gallery: "+10" over an empty tile, "+15" over a filled corner and "+50" over a filled cross, at 44 dp and 120 dp cells; verify in a browser that the number reads at 44 dp over all three (else adjust the glow radius/alpha and record the values in 1.1) — reads at 44 dp over all three; the glow values stay at 6 dp / 70 %. A 1 dp stroke on top outlined the overlapping contours inside Fredoka's "+" and "1", so the stroke moved under the fill at 2 dp (design decision 1)
- [ ] 1.4 Attach screenshots of the gallery sample to issue #51 and get the product owner's approval there, including whether the 0.4 c size stays (design, Open Questions); verify approval is noted on the issue before starting group 2

## 2. Style and drawing

- [ ] 2.1 Add `PopupStyle` to `app/.../ui/game/PointsPopup.kt` (text fraction 0.4, fill `SurfaceDeep`, stroke `LiquidGlow` 2 dp (1 dp visible), glow `LiquidGlow` 6 dp at 0.7) and a pure `glowColor(alpha)` that scales the glow alpha by the popup alpha; verify JVM unit tests in `PointsPopupTest`: the tokens equal the design-system values, the fill is opaque, `glowColor(1f)` has alpha 0.7, `glowColor(0.5f)` 0.35 and `glowColor(0f)` 0
- [ ] 2.2 Rewrite `drawPopups` in `PlayingField.kt`: measure once, then draw the glow pass (text shadow), the 2 dp `liquid.glow` stroke and the `surface.deep` fill on top, all at the frame alpha and inside the existing scale and rise; remove `POPUP_OUTLINE`, `POPUP_TEXT_FRACTION` and the `InkDark`/white passes; add a `@Preview` of a popup over a filled straight; verify `./gradlew :app:testDebugUnitTest` passes and the preview matches the gallery

## 3. Instrumented tests

- [ ] 3.1 Add a screenshot-style check (as in `EndpointScreenshotTest`) to `PointsPopupTest`: a popup fully visible over a filled straight; verify the pixel at the centre of a digit stroke is `surface.deep` (not white, not the pipe), a pixel just outside the glyph is lighter than the bare tile (the glow), and the image is saved for the reviewer; run it on the emulator and verify it passes
- [ ] 3.2 Run the existing `PointsPopupTest` cases on the emulator (popups appear, follow the tile, pause, restart, do not block input); verify they pass unchanged

## 4. Regression

- [ ] 4.1 Run `./gradlew :core:test :app:testDebugUnitTest` from `src/`; verify both pass
- [ ] 4.2 Run the affected instrumented tests (`PointsPopupTest`, `PlayingFieldTest`); verify they pass, and attach the 3.1 screenshot to issue #51
