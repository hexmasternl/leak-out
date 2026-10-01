## Why

The finished pipe art already exists: one SVG per section in `app/src/main/assets/pipes/` (drawn by `PipeTile`) and, since the last commit, a `*-glass.svg` overlay per section that makes the pipe look like glass. But only the debug Pipe Lab uses it. The game itself still draws the stand-in pipes from `PipeSectionGlyph.drawPipeSection` (a gradient line per open side and a circle in the middle), in the stack, under the finger while dragging and on the playing field. The player looks at pipes all game, so the game should show the real art.

## What Changes

- The pipe renderer draws a section in four layers: the asset's `under` group, the liquid, the `over` group and then the section's **glass overlay** (`<id>-glass.svg`, group `glass`). The glass overlay is loaded and cached with the rest of the pipe art.
- The renderer can also be called from inside an existing `Canvas` (a `DrawScope` function that draws a section into a given rectangle), so the playing field, which draws every cell on one canvas, uses the same drawing as the `PipeTile` composable.
- **Stack**: every section in the stack column is drawn with the pipe art and glass instead of the stand-in glyph.
- **Dragged copy**: the copy under the finger is drawn with the pipe art and glass.
- **Playing field**: every placed section is drawn with the pipe art and glass, at the cell size, following pan and zoom. The existing cell background (and its tint once liquid has entered) stays.
- **Pipe Lab** shows the glass overlay too, since it uses `PipeTile`, so the lab keeps previewing exactly what the game shows.
- The stand-in drawing (`drawPipeSection` and the procedural `PipeSectionGlyph` body) is removed. No pipe body is drawn from Kotlin geometry any more.
- `docs/pipe-assets.md` documents the glass overlay files and the layer order.

Non-goals: drawing the liquid in the game's pipes (the playing field still only exposes the filled channels and liquid head as semantics; that is a separate change), and restyling the start, finish, blocked and empty cells.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `pipe-section-assets`: every section also has a glass overlay asset `<id>-glass.svg` with a `glass` group, in the same view box and SVG subset, drawn above everything else.
- `pipe-liquid-rendering`: the tile draws the glass overlay as its top layer (above the `over` group), and the same drawing is available for a rectangle inside another canvas.
- `game-play-stack`: stack sections and the dragged copy are drawn with the pipe assets and their glass overlay.
- `game-play-playing-field`: placed sections are drawn with the pipe assets and their glass overlay, scaled to the cell.

## Impact

- `app/src/main/kotlin/nl/hexmaster/leakout/ui/pipes/`: `PipeArtParser` (parse a glass overlay), `PipeArtLibrary` / `PreparedPipeArt` (load and prepare the overlay), `PipeTile` (draw the glass layer; extract a `DrawScope` drawing function).
- `app/src/main/kotlin/nl/hexmaster/leakout/ui/game/`: `PipeSectionGlyph.kt` (stand-in drawing removed, glyph uses the pipe art), `StackPanel.kt`, `GameScreen.kt` (dragged copy), `PlayingField.kt` (placed sections).
- Tests: JVM tests for parsing the glass overlay and its layer; instrumented tests that the stack, the dragged copy and the field draw the pipe art for every section.
- Assets are unchanged: the `*-glass.svg` files already exist and `PipeAssetsTest` already checks them.
- `docs/pipe-assets.md` updated. No new dependencies; `core` does not change.
