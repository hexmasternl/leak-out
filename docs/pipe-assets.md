# Pipe assets

The artwork of every pipe section is an SVG file in `src/app/src/main/assets/pipes/`, one per section, named after its id in lower case: `h.svg`, `v.svg`, `lt.svg`, `lb.svg`, `rt.svg`, `rb.svg`, `x.svg`, `res.svg`. Each section also has a **glass overlay**, `<id>-glass.svg` (`h-glass.svg` … `res-glass.svg`), with translucent highlights that make the pipe look like glass. These files are the only source of the pipe art. Open them in any SVG editor. The app reads them at runtime (`PipeArtParser`), draws the art, puts the liquid on top along the channels marked in the file, and lays the glass over everything.

The stack, the section under the finger while dragging, the playing field and the Pipe Lab all draw pipes with this same art (`PipeTile` and `drawPipe`), so an edit shows up everywhere.

The look follows [design-system.md](design-system.md) sections 6.3 and 6.4.

## File layout

```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <defs> gradients </defs>
  <g id="under"> drawn below the liquid: shadow side, body, specular, bore, tank </g>
  <path id="channel-main" data-from="left" data-to="right" d="M0 50 L100 50" fill="none" stroke="none"/>
  <circle id="tank" cx="50" cy="50" r="26" fill="none" stroke="none"/>          <!-- reservoir only -->
  <rect id="bridge" data-hides="channel-v" x="0" y="27" width="100" height="46"/> <!-- cross only -->
  <g id="over"> drawn above the liquid: flanges, tank rim, badge </g>
</svg>
```

- **View box**: always `0 0 100 100`. One unit is 1/100 of a cell.
- **`under` and `over`**: both groups are required. All visible art lives in one of them.
- **Channels**: one `path` per liquid channel, with an id starting with `channel-` and `data-from` / `data-to` set to `left`, `right`, `top` or `bottom`. The path runs along the centre of the bore from the middle of the `data-from` edge to the middle of the `data-to` edge. It is not drawn; the liquid is drawn along it, from whichever side the liquid enters. Channels are drawn in file order, so a later channel's liquid lies on top.
  - `H`, `V`, the corners and `RES` have one channel, `channel-main`.
  - `X` has `channel-v` (top–bottom) and then `channel-h` (left–right).
- **Tank** (`RES` only): the circle `tank` is the inside of the tank. The tank liquid rises in it from the bottom. The reservoir channel must be the straight line `M0 50 L100 50`.
- **Bridge** (`X` only): the rectangle `bridge` is where the liquid of the channel in `data-hides` is not drawn, because that channel passes under the horizontal pipe.

## Glass overlay

```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
  <g id="glass"> drawn above everything: tint, rim lines, specular streak </g>
</svg>
```

- **View box**: `0 0 100 100`, the same cell as the section asset.
- **`glass`**: exactly one group, required. All the overlay's art lives in it. It uses the same SVG subset and colours as the section assets, usually white at a low opacity.
- No channels, tank or bridge: those belong in the section asset, and loading an overlay that has one fails.

## Layer order

Bottom to top: the section's `under` group, the liquid (channels in file order), the `over` group, the glass overlay.

## Sizes

| Part | Size (view box units) |
|------|------|
| Body stroke | 36 |
| Bore stroke | 25 |
| Liquid (drawn by the app) | 21 |
| Specular highlight | 4, 15 from the centre toward the top-left |
| Flange | 10 deep, 40 across, drawn either side of the bore |
| Corner elbow | quarter circle of radius 50 around the cell corner |
| Cross bridge gap | 5 around the horizontal body |
| Reservoir tank | radius 30 outside, 26 inside |

## Colours

Only design-system tokens, written as hex: `steel.100`–`steel.900`, `surface.deep` (the cross's bridge gap), `reservoir.300`–`reservoir.700` and white (`line.highlight`). A unit test (`PipeAssetsTest`) fails on any other colour. Liquid colours are not used in the files: the app draws the liquid.

## Supported SVG

Elements: `svg`, `g`, `defs`, `path`, `rect`, `circle`, `linearGradient`, `stop`. `title`, `desc` and `metadata` are ignored.

Attributes: `id`, `data-*`, `d`, `x`, `y`, `width`, `height`, `rx`, `cx`, `cy`, `r`, `fill`, `stroke`, `stroke-width`, `stroke-linecap`, `stroke-linejoin`, `opacity`, `fill-opacity`, `stroke-opacity`; on gradients `x1`, `y1`, `x2`, `y2` and `gradientUnits="userSpaceOnUse"`; on stops `offset`, `stop-color`, `stop-opacity`.

Paint: `#RRGGBB`, `#RGB`, `none` or `url(#gradient-id)`.

Anything else, such as `text`, `style="…"` or `transform="…"`, makes loading fail with an error that names it. Elements and attributes in other namespaces (what Inkscape and other editors add) are ignored, so saving from an editor is fine as long as it writes plain attributes. If an editor writes `style="…"`, switch it to presentation attributes (in Inkscape: *Preferences → Input/Output → SVG output → Style properties → Presentation attributes*).

After editing an asset, run `./gradlew :app:testDebugUnitTest` from `src/`; `PipeAssetsTest` checks the format, the channels, the flanges and the colours of the section assets and the glass overlays.

## Previewing

Debug builds have a second launcher entry, **Pipe Lab**, that shows every section, with its glass, and plays the liquid with controls for direction, flow speed and reservoir speed.
