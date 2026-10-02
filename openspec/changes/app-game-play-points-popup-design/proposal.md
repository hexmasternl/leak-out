# Proposal

Tracking issue: [#51 Section score indicator is too large](https://github.com/hexmasternl/leak-out/issues/51).

## Why

When a section has run full, its points ("+10", "+15", "+25", "+50") pop up on the tile. Today the number is a white fill under a 3 dp `InkDark` outline, a leftover from the old slim cartoon style. At 40 % of the tile that outline is so thick it swallows the counters of the digits: the number reads as hollow, looks bigger and heavier than it is, and is hard to read over the steel pipes and the bright liquid. It is also the only element on the board that is not in the *Deep Flow* design system (§1.1: "No 3 px ink borders").

## What Changes

- **Design system first** (`docs/design-system.md`): the points popup gets its own entry, so far it is not described at all.
  - §7.2 *Specific animations* gains **Points popup**: the existing timing (150 ms fade-in from 80 % to 100 % size, 500 ms hold, 350 ms fade-out, a quarter-tile rise over the full 1 000 ms), unchanged.
  - Its look: the glyphs are **filled solid with `surface.deep`**, edged with a **thin 1 dp `liquid.glow` edge**, over a **soft `liquid.glow` glow** (blur 6 dp at 70 %). The glow carries the number on the dark board; the dark fill keeps the digits' counters open.
  - §7.4 *Reduced motion*: the popup is gameplay feedback; it keeps its fade and rise (no change), and its glow is static anyway.
  - The `docs/design-system.html` gallery gets a popup sample over an empty tile, a filled straight and a filled corner.
- **App complies with the design system**: `drawPopups` in `PlayingField` draws the glow, the `surface.deep` fill and the `liquid.glow` stroke, replacing the `InkDark` outline and white fill. Text size (0.4 c), font, position, timing, rise and scale stay as they are.
- No `core` change: awards, points and their timing are unchanged.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `game-play-points-popup`: "Points are shown on the tile" now specifies how the number is drawn: an opaque `surface.deep` fill, a thin `liquid.glow` stroke and a `liquid.glow` glow, all fading with the popup.

## Impact

- `docs/design-system.md` (§7.2, §7.4) and `docs/design-system.html` (gallery sample).
- `app/src/main/kotlin/nl/hexmaster/leakout/ui/game/PlayingField.kt` (`drawPopups`, popup constants) and `PointsPopup.kt` (a pure `PopupStyle` with the colours and sizes).
- Tests: JVM unit test for `PopupStyle`; the instrumented `PointsPopupTest` gets a screenshot-style check of a popup over a filled section.
- No dependency, level-format or engine change.
