## 1. Load the glass overlays

- [x] 1.1 Add `PipeArtParser.parseGlass(input): List<SvgShape>` that reads the supported subset, requires one `glass` group, and fails with a named error on a missing `glass` group, an unsupported element, or a channel, tank or bridge
- [x] 1.2 JVM tests in `PipeArtParserTest`: a valid overlay parses to its shapes with their styles; a missing `glass` group, a `text` element and a `channel-` path each fail with an error naming the problem
- [x] 1.3 `PipeArtLibrary`: add `glassFileName(section)` (`pipes/<id>-glass.svg`), load the overlay with the section's art, and add `glass: List<PreparedShape>` to `PreparedPipeArt`
- [x] 1.4 Extend `PipeAssetsTest` so every section's overlay loads through `parseGlass` and draws at least one shape

## 2. One pipe drawing

- [x] 2.1 Move the drawing out of `PipeTile` into `internal fun DrawScope.drawPipe(art, rect, fills, scratch)`: translate to the rect, scale by `rect.minDimension / 100` (centred), draw `under`, liquid, `over`, then `glass`; make `LiquidScratch` internal
- [x] 2.2 `PipeTile` calls `drawPipe` with the whole canvas; keep its tag and `pipeFills` semantics
- [x] 2.3 Instrumented tests in `PipeTileTest`: every section still loads and draws; the glass is drawn (the specular streak region of `H` is brighter than the same art drawn without glass); liquid tests still pass with the glass on top

## 3. Stack and dragged copy

- [x] 3.1 `PipeSectionGlyph` draws `PipeTile(section)` inside its card background; delete `drawPipeSection` and the stand-in geometry
- [x] 3.2 Call `PipeArtLibrary.preloadAll()` when the game screen starts
- [x] 3.3 Instrumented tests: every stack slot contains the pipe tile of its section (`pipe-tile-<SECTION>`), and the dragged copy contains the pipe tile of the dragged section; existing stack and drag tests still pass (written and compiled; emulator run skipped at the user's request)

## 4. Playing field

- [x] 4.1 `PlayingField` reads the library with `rememberPipeArtLibrary()` and keeps one `LiquidScratch`; `drawCell` draws a `Cell.Pipe` with `drawPipe(library.prepared(section), rect)` inside `clipRect(rect)`, over the existing cell background and tint
- [x] 4.2 Add a test-only `pipeSection` semantics property on placed field cells
- [x] 4.3 Instrumented tests in `PlayingFieldTest`: a placed section's cell reports its `pipeSection`; a placed `H` shows the steel body band across the cell centre; after zooming in, the art still fills the (larger) cell (written and compiled; emulator run skipped at the user's request)
- [ ] 4.4 Existing game screen tests (`GameScreenTest`, `GameEngineScreenTest`, `LevelLostTest`, `LevelCompleteTest`) still pass

## 5. Docs and verification

- [ ] 5.1 Update `docs/pipe-assets.md`: the `<id>-glass.svg` overlay files, their `glass` group, and the layer order under → liquid → over → glass; mention that the game and the Pipe Lab draw the same art
- [ ] 5.2 Run `./gradlew :app:testDebugUnitTest` and `./gradlew :core:test` from `src/` and make sure they pass
- [ ] 5.3 Run the affected instrumented tests (`PipeTileTest`, `PipeLabScreenTest`, `PlayingFieldTest`, `GameScreenTest` and the other game screen tests touched above) on the emulator and make sure they pass
- [ ] 5.4 Check the game and the Pipe Lab on the emulator: stack, dragged copy and placed pipes show the new art with the glass
