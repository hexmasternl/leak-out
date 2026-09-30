## 1. Setup

- [ ] 1.1 Add `implementation(project(":core"))` to `app/build.gradle.kts` and check that `./gradlew :app:assembleDebug` still builds.
- [ ] 1.2 Add the missing design-system tokens to `LeakOutColors`: `steel.100/300/500/700/900`, `reservoir.300/500/700` and `liquid.900`.

## 2. SVG assets

- [ ] 2.1 Draw `h.svg` and `v.svg` in `app/src/main/assets/pipes/` in the asset format from the design (view box `0 0 100 100`, `under` group, `channel-*` path with `data-from`/`data-to`, `over` group with flanges at both open ports).
- [ ] 2.2 Draw the four corners `lt.svg`, `lb.svg`, `rt.svg`, `rb.svg` with a radius-50 quarter-arc elbow and flanges at their two open ports.
- [ ] 2.3 Draw `x.svg`: vertical pipe, `surface.deep` bridge gap, horizontal pipe on top, channels `channel-h` (left→right) and `channel-v` (top→bottom), flanges at all four ports.
- [ ] 2.4 Draw `res.svg`: horizontal pipe, violet tank circle with the `reservoir.700 → reservoir.500` gradient in `under`, the `tank` circle marker, the steel rim and the "slow" badge in `over`, channel left→right.
- [ ] 2.5 Document the asset format and the supported SVG subset in `docs/pipe-assets.md`.

## 3. Asset loader

- [ ] 3.1 Add `PipeArt`, `SvgShape` (path, rect, circle with a style), `ArtChannel`, `TankCircle` and `SvgGradient` in `nl.hexmaster.leakout.ui.pipes`.
- [ ] 3.2 Add the pure `PipeArtParser.parse(InputStream): PipeArt` for the subset in the spec; unknown SVG elements and `style`/`transform` attributes throw `UnsupportedSvgException` naming them; foreign-namespace (editor) elements and attributes are ignored.
- [ ] 3.3 Unit test the parser on small inline SVGs: layers, styles, gradients, channels and their sides, tank, missing `under`/`over`, bad `data-from` value, unsupported element.
- [ ] 3.4 Unit test the real assets (read from `src/main/assets/pipes` in the JVM test): every section loads, view box is `0 0 100 100`, the channels per section match the spec, channel paths start with `M` at the middle of their `data-from` edge and end at the middle of their `data-to` edge, only `RES` has a tank, and every colour used is a design-system token.
- [ ] 3.5 Add `PipeArtLibrary` (loads from `AssetManager`, caches per `PipeSection`, prepares Compose `Path`s and `Brush`es) and `rememberPipeArtLibrary()`.
- [ ] 3.6 Run `./gradlew :app:testDebugUnitTest` and check that everything passes.

## 4. Liquid mapping and timing

- [ ] 4.1 Add `ChannelFill(progress, entry)` and the pure `LiquidSpans.forChannel` (range along the path for both entry sides, clamped progress, error for a side not on the channel).
- [ ] 4.2 Add `LiquidSpans.forReservoir` with the inlet (0–0.1), tank (0.1–0.9) and outlet (0.9–1) phases for both entry sides.
- [ ] 4.3 Add `LiquidTiming.speedFactor` and `LiquidTiming.advance`, refusing speeds of 0 or less.
- [ ] 4.4 Unit test the mapping: `H` 0.5 from the left is `[0, 0.5]` and 0.25 from the right is `[0.75, 1]`; corners from both sides; progress below 0 and above 1 is clamped; reservoir at 0.05 from the right fills only the outer half of the right stub, at 0.5 from the left has a full inlet and a half tank, at 0.95 has a half outlet; the boundaries 0.1 and 0.9.
- [ ] 4.5 Unit test the timing: 250 ms at 2 cells/s is 0.5; a reservoir at factor 0.25 is 0.25 after 1 s and full after 4 s; integrating with many small `advance` steps equals one big step; speed 0 and factor 0 are refused.
- [ ] 4.6 Run `./gradlew :app:testDebugUnitTest` and check that everything passes.

## 5. Pipe tile

- [ ] 5.1 Add `PipeTile(section, fills, modifier)`: draw `under`, vertical liquid (clipped out of the cross's bridge band), horizontal liquid, tank liquid, `over`; liquid gradient width 21, round caps, glowing head while filling, glow stroke only while filling.
- [ ] 5.2 Add the `pipe-tile-<ID>` test tag and a semantics property with the fill state.
- [ ] 5.3 Instrumented tests: all eight tiles draw at 48 dp and 96 dp without errors; an empty `H` has no liquid-coloured pixels in the bore; `H` at 0.5 from the left has liquid pixels left of the centre and none right of it, and the mirror from the right; a cross with only the horizontal channel filled has no liquid in the top and bottom legs; a reservoir at 0.5 has liquid in the lower half of the tank only; loading all eight assets takes less than 100 ms.

## 6. Pipe Lab (debug only)

- [ ] 6.1 Add the pure `PipeLabState` in `app/src/debug/kotlin/nl/hexmaster/leakout/lab/`: entry side per channel (default `data-from`), progress per channel, running flag, flow speed (0.1–4, default 1) and reservoir speed factor (0.1–1, default 0.3); `start`, `pause`, `reset`, `setEntry` (empties that tile), `tick(dtMs)` using `LiquidTiming.advance`, with the cross passing leftover time from its horizontal to its vertical channel.
- [ ] 6.2 Unit test `PipeLabState` in `app/src/testDebug/`: tick does nothing before start and while paused; start/pause/start continues; reset empties everything and stops; a straight fills in 0.5 s at 2 cells/s; a reservoir takes 4 s at factor 0.25 while a straight takes 1 s; changing the speed mid-flow does not move the progress; the cross fills horizontal in the first second and vertical in the second; changing a direction empties only that tile.
- [ ] 6.3 Add `PipeLabScreen`: a 4 × 2 grid of labelled `PipeTile`s, a direction toggle under each tile (two for the cross), flow speed and reservoir speed sliders with value labels, Start, Pause and Reset buttons, and the `withFrameNanos` frame loop while running.
- [ ] 6.4 Add `PipeLabActivity` and `app/src/debug/AndroidManifest.xml` with a "Pipe Lab" launcher entry, `sensorLandscape` and `Theme.LeakOut`, hiding the system bars like `MainActivity`.
- [ ] 6.5 Instrumented tests: the lab shows eight labelled tiles without liquid; Start fills `H` and pressing Pause freezes it; Reset empties every tile; reversing `H` makes the liquid enter from the right; the cross's two direction toggles are independent; moving the flow speed slider changes the shown value.
- [ ] 6.6 Check that the release build does not contain the lab: run `./gradlew :app:processReleaseMainManifest` and check the merged release manifest has no `PipeLabActivity`.

## 7. Docs and verification

- [ ] 7.1 Update `docs/project-plan.md` section 2.8 and `docs/design-system.md` section 6.3: the pipe art comes from the SVG assets in `assets/pipes/`, the liquid is drawn in code along the channels marked in them.
- [ ] 7.2 Run `./gradlew :core:test :app:testDebugUnitTest` and `./gradlew :app:connectedDebugAndroidTest` on the emulator and check that everything passes.
- [ ] 7.3 Open the Pipe Lab on the emulator, start the animation, and check by eye (screenshot) that every section fills in the chosen direction, the reservoir tank fills bottom-up and is slower, and the cross's channels fill one after the other.
