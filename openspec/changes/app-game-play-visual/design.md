## Context

`app-game-play-pipe-assets` added the SVG art for every section (`assets/pipes/<id>.svg`), a small parser for the SVG subset (`PipeArtParser` → `PipeArt`), a cache that turns it into Compose paths and brushes (`PipeArtLibrary` → `PreparedPipeArt`) and the `PipeTile` composable that draws `under`, liquid, `over`. Only the debug Pipe Lab uses it.

The game draws pipes with the older stand-in `drawPipeSection(section, rect)` in `ui/game/PipeSectionGlyph.kt`:

- `StackPanel` draws every slot with `PipeSectionGlyph(section)`.
- `GameScreen` draws the dragged copy with `PipeSectionGlyph(section)`.
- `PlayingField.drawCell` calls `drawPipeSection(cell.section, rect)` for `Cell.Pipe`, inside the one `Canvas` that draws every visible cell, with pan and zoom applied through `FieldViewport.cellRect`.

A glass overlay per section (`assets/pipes/<id>-glass.svg`) was committed after that change. Each file has one `<g id="glass">` with translucent white strokes: a faint tint over the whole body, rim lines and a specular streak. `PipeAssetsTest` already checks their view box, subset and colours; nothing loads or draws them yet. The overlays span the whole pipe including the flange area, and their comments say they are "drawn on top of `<id>.svg` and its liquid".

## Goals / Non-Goals

**Goals:**
- One pipe drawing for the whole app: stack, dragged copy, playing field and Pipe Lab all draw a section from its SVG asset plus its glass overlay.
- The field keeps drawing all cells on one canvas (no composable per cell), so large levels and pan/zoom stay cheap.
- Remove the stand-in pipe geometry from Kotlin.

**Non-Goals:**
- Drawing liquid in the game's pipes. `PlayingField` keeps passing `head` and `filled` only as semantics; the liquid drawing is its own change. The renderer already accepts fills, so that change only has to build them.
- Restyling start, finish, blocked or empty cells, or the cell background under a pipe.
- Changing the SVG assets or the glass overlays.

## Decisions

### Glass overlay is a separate asset, parsed into its own shape list
`PipeArtParser` gets a second entry point, `parseGlass(input): List<SvgShape>`, that reads the same subset but requires exactly one `glass` group and rejects channels, tank and bridge. `PipeArtLibrary` loads `<id>-glass.svg` next to `<id>.svg` (`glassFileName(section)`), and `PreparedPipeArt` gets a `glass: List<PreparedShape>` prepared with the existing `prepare` function.

*Alternative:* merge the glass into the section SVG as a third group. Rejected: the overlays are already separate files that a designer edits on their own, and keeping them apart lets the base art be previewed without glass.

### Layer order: under, liquid, over, glass
The glass is drawn last, above the flanges. The overlay's comment says it goes on top of the art and the liquid, and its rim lines stop at 10 and 90 to leave the flanges clear while the faint tint covers them, which only reads right on top. Drawing it above the liquid is what makes the liquid look like it is behind glass.

### One `DrawScope` function, used by the tile and the field
The body of `PipeTile`'s `Canvas` moves into `fun DrawScope.drawPipe(art: PreparedPipeArt, rect: Rect, fills: Map<String, ChannelFill> = emptyMap(), scratch: LiquidScratch)`: it translates to `rect.topLeft`, scales by `rect.minDimension / 100` (centred when the rect is not square) and draws the four layers. `PipeTile` calls it with the whole canvas; `PlayingField.drawCell` calls it with the cell rect. `LiquidScratch` becomes `internal` so the field can keep one.

The field reads the library with `rememberPipeArtLibrary()` in the composable and passes it into `drawCell`. Art is prepared lazily and cached per section, so the first frame that shows a section parses it once; `PipeArtLibrary.preloadAll()` is called when the game screen starts so no frame pays for it mid-game.

*Alternative:* one `PipeTile` composable per placed cell, positioned with offsets. Rejected: the field draws hundreds of cells, pans and zooms every frame, and already has one canvas with its own clipping and layering (highlight, leak, finish ring, popups on top).

### Clipping the art to the cell
`PipeTile` clips to its bounds. In the field, a section is drawn with `clipRect(rect)` so a stroke cap or the cross's shadow offset (the `under` shadow is shifted by 1 unit) cannot bleed into the neighbour cell. Channels end on the cell edge, so neighbouring pipes still meet.

### Stack and dragged copy use `PipeTile`
`PipeSectionGlyph` keeps its role (the rounded card background) but draws `PipeTile(section)` inside it instead of the stand-in. `drawPipeSection` is deleted. Stack slots and the dragged copy therefore get the glass for free, and their existing tags and content descriptions stay.

### Tests see what is drawn through semantics and pixels
- `PipeTile` already publishes `pipeFills`; it also keeps its `pipe-tile-<SECTION>` tag, so stack and drag tests can find the tile per section.
- The field cell semantics get a `pipeSection` property on placed cells (the section drawn there), matching the existing test-only `filledChannels` hook.
- Pixel checks confirm the glass is really drawn: at the specular streak (view box y ≈ 42 on `H`) the tile with glass is brighter than the same art without it, and a placed `H` in the field shows the steel body colour band across the cell centre, not the stand-in gradient.

## Risks / Trade-offs

- [Drawing many translucent strokes per cell on large zoomed-out fields could cost frame time] → The prepared paths are cached and reused; only visible cells are drawn (`viewport.visibleCells()`). If profiling shows a problem, glass can be skipped below a minimum cell size, but that is not planned now.
- [Pixel tests are sensitive to anti-aliasing and device density] → Compare colour bands and relative brightness in regions away from edges, as `PipeTileTest` already does, rather than exact pixels.
- [A missing or broken glass file now breaks every pipe] → `PipeAssetsTest` and a new parser test load every overlay in the JVM build, so it fails before it ships.

## Migration Plan

No data or settings change. Replacing the stand-in drawing is a pure rendering change, rolled back by reverting the change.

## Open Questions

None.
