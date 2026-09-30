## Context

The plan (section 2.8) and the design system (6.3–6.5, 7.3) describe how pipes and liquid look, but no pipe art exists in the app yet. The open changes draw stand-ins: `app-game-play-stack` and `app-game-play-playing-field` show section ids, and `app-game-play-game-engine` lists "the liquid drawing inside the pipes (`LiquidDrawer`)" as a separate change. This is that change, plus the artwork itself.

The plan says pipes are drawn procedurally in Kotlin. The request for this change is to make the art SVG files instead, so it can be edited in an SVG editor, and to have a way to watch and tune the fluid: its speed, both directions, the reservoir slow-down and the cross.

Current state that matters:
- `core` has `PipeSection` (`H`, `V`, `LT`, `LB`, `RT`, `RB`, `X`, `RES`) and `Side` (`LEFT`, `RIGHT`, `TOP`, `BOTTOM`). It has no channel model yet; `app-game-play-fast-forward` and `app-game-play-level-lost` add `exitFor` and a `Channel` later.
- `app` does not depend on `core` yet.
- `LeakOutColors` has the liquid tokens; the steel and reservoir tokens from `design-system.md` 2.5/2.6 are not in it yet.
- Android has no SVG renderer. `VectorDrawable` is a different format, and Compose's `PathParser` turns SVG path data into a `Path`.

## Goals / Non-Goals

**Goals:**
- Eight hand-editable SVG files that are the single source of the pipe art.
- Liquid that is drawn along channel lines stored in the same SVG files, per channel, in both directions, with the reservoir's three phases.
- A small, strict loader and renderer with no new dependency.
- A pure, JVM-tested mapping from progress and entry side to what is filled, and from time and speed to progress.
- A debug-only Pipe Lab to start, pause and reset the animation with speed, reservoir speed and direction controls.

**Non-Goals:**
- Using the tile in the stack or the playing field. Those changes are open; they switch from their stand-ins to `PipeTile` when they are implemented (or in a follow-up).
- The start valve and finish drain art, leaks and droplet bursts, bubbles behind the head, the 400 ms glow fade and reduced-motion handling (design system 6.5, 7.4).
- Driving the liquid from the game engine. The lab has its own clock.
- A general SVG renderer: only the subset the assets use is supported.

## Decisions

**1. SVG in `assets/`, parsed at runtime, instead of converted to `VectorDrawable`.**
The files live in `app/src/main/assets/pipes/<id>.svg` and are read once with `javax.xml.parsers.DocumentBuilder`, which exists on Android and on the JVM, so the parser is unit tested without an emulator. *Other options:* (a) convert to `VectorDrawable` XML with Android Studio or a build script. Not chosen: there would be two copies of the art, and a `VectorDrawable` cannot carry the channel and tank markers, so they would have to live in Kotlin and could drift from the art. (b) A third-party SVG library (AndroidSVG, Coil SVG). Not chosen: it adds a dependency for eight small files, and we would still need our own markers and layering.

**2. The asset format.** Every file follows one shape:
```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <defs><linearGradient id="tank" gradientUnits="userSpaceOnUse" x1="50" y1="20" x2="50" y2="80">…</linearGradient></defs>
  <g id="under"> shadow, body, specular, bore (and the tank fill for RES) </g>
  <path id="channel-h" data-from="left" data-to="right" d="M0 50 H100" fill="none" stroke="none"/>
  <circle id="tank" cx="50" cy="50" r="26" fill="none" stroke="none"/>   <!-- RES only -->
  <g id="over"> flanges (and the tank rim and slow badge for RES) </g>
</svg>
```
Sizes follow the recipe in cell units × 100: body stroke 36, bore 25, liquid 21, specular 4, flange 10 deep and 40 wide. Corners are an edge-to-centre-to-edge path with a quarter arc of radius 50 around the cell corner (`M0 50 A50 50 0 0 0 50 0` for `LT`), which gives the round elbow and a channel of known length. The cross draws the vertical pipe, then a `surface.deep` stroke 5 wider than the horizontal body as the bridge gap, then the horizontal pipe. Colours are written as the design-system hex values; the SVG cannot refer to Kotlin tokens, and a unit test checks that every colour in the assets is one of the design-system tokens. Channel paths and the tank have `stroke="none" fill="none"` so the files still look right in an SVG editor.

**3. Parsed model, then Compose objects.**
```kotlin
data class PipeArt(val under: List<SvgShape>, val over: List<SvgShape>,
                   val channels: List<ArtChannel>, val tank: TankCircle?)
data class ArtChannel(val id: String, val from: Side, val to: Side, val pathData: String)
sealed interface SvgShape  // PathShape(d, style), RectShape, CircleShape; style = fill/stroke/width/cap/join/opacity
```
`PipeArtParser.parse(InputStream): PipeArt` is pure Kotlin (JVM tested). Unknown elements throw `UnsupportedSvgException(element)`. `PipeArtLibrary(assets)` loads and caches one `PipeArt` per `PipeSection`, and turns each into a `PreparedArt` with Compose `Path`s (via `PathParser().parsePathString(d).toPath()`) and `Brush`es, in the 0–100 space. Drawing scales the canvas by `size / 100`, so one prepared set serves every cell size and nothing is allocated per frame except the liquid segment. *Other option:* build `ImageVector`s. Not chosen: an `ImageVector` is drawn as a whole, and the liquid must go between the `under` and `over` layers.

**4. Liquid as a sub-segment of the channel path.**
`ChannelFill(progress: Float, entry: Side)` per channel. The pure `LiquidSpans.forChannel(channel, fill)` returns the fraction range along the path as it is written (from `data-from`): `[0, p]` when `entry == from`, `[1 - p, 1]` when `entry == to`, and throws when `entry` is neither. The renderer takes that range from a `PathMeasure` (`getSegment`) and strokes it with the liquid gradient at width 21 with round caps, draws the head as a `liquid.glow` circle at the leading end (the `p` end in the entry direction) while `0 < p < 1`, and a blurred wider stroke underneath as the glow while filling. This one rule gives both directions for every straight, corner and cross channel. *Other option:* reverse the path for the other direction. Not chosen: `Path` reversal is not in Compose, and a range on one path is simpler to test.

**5. Reservoir phases in the same pure mapping.**
`LiquidSpans.forReservoir(channel, tank, fill)` returns `ReservoirFill(inlet: FloatRange, tankLevel: Float, outlet: FloatRange)`. With the tank's left and right edge at path fractions `a = (50 - r) / 100` and `b = (50 + r) / 100` (the channel is the straight `M0 50 H100`):
- `p ≤ 0.1`: inlet stub filled over `p / 0.1` of its length, from the entry edge;
- `0.1 < p < 0.9`: inlet full, `tankLevel = (p - 0.1) / 0.8`;
- `p ≥ 0.9`: tank full, outlet filled over `(p - 0.9) / 0.1` of its length, from the tank.
From the left the inlet is `[0, a]` and the outlet `[b, 1]`; from the right they are mirrored. The tank liquid is a rectangle from the bottom of the tank up to `tankLevel × 2r`, clipped to the tank circle, with the liquid gradient. It is drawn on top of the `under` tank fill and below the `over` rim.

**6. Timing is one pure function.**
```kotlin
object LiquidTiming {
    fun speedFactor(section: PipeSection, reservoirSpeedFactor: Float): Float  // RES → factor, else 1
    fun advance(progress: Float, dtMs: Long, cellsPerSecond: Float, section: PipeSection, reservoirSpeedFactor: Float): Float
}
```
`advance` adds `dtMs / 1000 × cellsPerSecond × factor` and clamps to 1; it requires both speeds > 0. The lab integrates with `advance` every frame instead of computing `elapsed × speed`, so a slider change mid-flow continues from the current progress (spec: no jump). For a constant speed both give the same value, which a test checks. The engine change can use the same `speedFactor` rule; nothing here depends on the engine.

**7. `PipeTile` composable.**
`PipeTile(section, fills: Map<String, ChannelFill>, modifier)`, keyed by channel id (`channel-h`, `channel-v`, `channel-main`), drawn in a `Canvas`: `under` → channel liquid in file order → tank liquid → `over`. The cross lists `channel-v` before `channel-h`, so the horizontal liquid lies over the vertical liquid, as the horizontal pipe lies on top in the art. Because the horizontal body is in `under`, the vertical liquid would be drawn over it in the middle of the cell; the cross SVG therefore marks a `bridge` rectangle (`data-hides="channel-v"`, the horizontal body plus its gap) and the renderer clips the vertical liquid out of it, so the liquid passes under the bridge like the vertical bore does. The rectangle lives in the asset, not in Kotlin, so art and clip cannot drift apart. The art comes from `rememberPipeArtLibrary()` (overridable with `LocalPipeArtLibrary`). The tile has a `testTag("pipe-tile-<ID>")` and a semantics property with the fill state, so instrumented tests can check it without pixel matching, plus a few pixel checks on a captured bitmap for the key scenarios (half-filled `H` from each side, cross channels).

**8. Pipe Lab in the `debug` source set.**
`app/src/debug/AndroidManifest.xml` adds `PipeLabActivity` with a second `MAIN`/`LAUNCHER` intent filter, label "Pipe Lab", `sensorLandscape`, `Theme.LeakOut`. Code lives in `app/src/debug/kotlin/nl/hexmaster/leakout/lab/`: `PipeLabState` (pure: per-tile entry sides, per-channel progress, running flag, speeds; `tick(dtMs)`, `start()`, `pause()`, `reset()`, `setEntry(...)`) and `PipeLabScreen` (a 4 × 2 grid of labelled `PipeTile`s, a direction button under each tile — two for the cross — two `Slider`s with value labels, and Start/Pause/Reset buttons). The frame loop is `LaunchedEffect(running) { withFrameNanos … state.tick(dt) }`. The cross ticks its horizontal channel and, once it is full, passes the rest of the frame time to the vertical one. Unit tests for `PipeLabState` go in `app/src/testDebug/`. *Other option:* a hidden entry on the welcome screen. Not chosen: it would ship in release and touch a screen with its own open changes.

**9. `app` depends on `core`.** `implementation(project(":core"))` is added so the renderer uses `PipeSection` and `Side` instead of copies. Other open changes add the same line; whichever merges second drops the duplicate.

**10. Colour tokens.** The steel (`steel.100`–`steel.900`), `surface.deep` (already present), `reservoir.300/500/700` and `liquid.900` tokens are added to `LeakOutColors`, because the renderer draws the liquid and the lab uses them.

## Risks / Trade-offs

- [An SVG edit in an editor adds unsupported markup (Inkscape namespaces, `style=""` attributes, transforms)] → The parser ignores attributes in foreign namespaces and `sodipodi`/`inkscape` elements, but fails on unknown SVG elements and on `style`/`transform` attributes, and a unit test loads every asset. The asset format is documented in `docs/pipe-assets.md`.
- [Parsing XML at runtime costs time on the first frame] → Eight files of a few kB are parsed once per process and cached; the lab and later the game screen load them in `remember`. An instrumented test measures that loading all eight takes well under 100 ms on the emulator.
- [Glow via blur is not free] → The glow uses a wider, semi-transparent stroke instead of a blur mask filter, only on the channel that is filling.
- [The plan and design system say "procedural"] → Both docs are updated in this change.

## Migration Plan

Nothing to migrate: no screen shows pipes yet. The stack and playing-field changes can adopt `PipeTile` when implemented. Rollback is removing the assets, `ui/pipes` and the debug lab.

## Open Questions

None blocking. The exact look of the "slow" badge (an hourglass drawn from paths) can be refined in the SVG later without code changes.
