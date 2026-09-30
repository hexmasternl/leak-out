# pipe-section-assets Specification

## Purpose
TBD - created by archiving change app-game-play-pipe-assets. Update Purpose after archive.
## Requirements
### Requirement: One SVG asset per pipe section
The app SHALL ship one SVG file per pipe section in `app/src/main/assets/pipes/`, named after the section id in lower case: `h.svg`, `v.svg`, `lt.svg`, `lb.svg`, `rt.svg`, `rb.svg`, `x.svg` and `res.svg`. Every file SHALL use the view box `0 0 100 100`, which maps to one grid cell. The SVG files SHALL be the only source of the pipe artwork; the app SHALL NOT draw pipe bodies, flanges or tanks from geometry written in Kotlin.

#### Scenario: Every section has an asset
- **WHEN** the assets are loaded for every value of `PipeSection`
- **THEN** each one loads from its own file without an error

#### Scenario: Asset covers one cell
- **WHEN** any pipe asset is read
- **THEN** its view box is `0 0 100 100`

### Requirement: Artwork follows the design system
Each asset SHALL follow the pipe recipe in `docs/design-system.md` (sections 6.3 and 6.4), using only the design system's steel, surface and reservoir colours: a steel body with a shadow side and a specular highlight, a dark bore, and a flange at every open port. The cross SHALL draw its vertical pipe first and its horizontal pipe on top with a bridge gap, so it reads as two separate channels. The reservoir SHALL show a round violet tank in the centre of a horizontal pipe and a small "slow" badge in the top-right corner.

#### Scenario: Flanges at open ports only
- **WHEN** the asset of a section is drawn
- **THEN** there is a flange at every side the section is open on, and none at a closed side

#### Scenario: Cross reads as two pipes
- **WHEN** the cross asset is drawn
- **THEN** the horizontal pipe lies on top of the vertical pipe, separated by a gap in the background colour

### Requirement: Liquid channels are marked in the asset
Each asset SHALL contain one `path` element per liquid channel, with an id starting with `channel-` and the attributes `data-from` and `data-to` naming the two sides the channel connects (`left`, `right`, `top`, `bottom`). The path SHALL run along the centre of the bore from the `data-from` side to the `data-to` side, starting and ending on the edge of the cell. These channel paths SHALL NOT be drawn as artwork. The channels SHALL be exactly the channels of the section: one for `H`, `V`, `RES` and each corner, and two for the cross (left–right and top–bottom).

#### Scenario: Channels match the section
- **WHEN** the channels of every asset are read
- **THEN** `H` and `RES` have one channel left–right, `V` one top–bottom, `LT` left–top, `LB` left–bottom, `RT` right–top, `RB` right–bottom, and `X` two channels, left–right and top–bottom

#### Scenario: Channel ends lie on the cell edge
- **WHEN** a channel path is read
- **THEN** its start point lies on the middle of the edge of its `data-from` side and its end point on the middle of the edge of its `data-to` side

#### Scenario: Channel paths are invisible
- **WHEN** a section is drawn without liquid
- **THEN** the channel paths are not visible

### Requirement: Reservoir marks its tank
The reservoir asset SHALL contain a `circle` element with the id `tank` that describes the inside of the tank. The liquid in the tank SHALL be clipped to this shape. No other asset SHALL contain a `tank` element.

#### Scenario: Reservoir has a tank
- **WHEN** the reservoir asset is read
- **THEN** it has a tank circle centred in the cell

#### Scenario: Other sections have no tank
- **WHEN** any other asset is read
- **THEN** it has no tank

### Requirement: Cross marks its bridge
The cross asset SHALL contain a `rect` element with the id `bridge` and the attribute `data-hides="channel-v"`, covering the horizontal pipe and its gap. The liquid of the vertical channel SHALL NOT be drawn inside that rectangle, so it passes under the horizontal pipe. The cross SHALL list `channel-v` before `channel-h`, because channels are drawn in file order. No other asset SHALL contain a bridge.

#### Scenario: Vertical liquid passes under the bridge
- **WHEN** a cross is drawn with its vertical channel full
- **THEN** the vertical liquid shows above and below the horizontal pipe but not over it

### Requirement: Layers above and below the liquid
Each asset SHALL group its artwork into a group with the id `under` (drawn below the liquid: shadow, body, specular, bore, tank) and a group with the id `over` (drawn above the liquid: flanges and, for the reservoir, the tank rim and the badge). The renderer SHALL draw `under`, then the liquid, then `over`.

#### Scenario: Liquid sits inside the pipe
- **WHEN** a section is drawn with liquid
- **THEN** the liquid covers the bore and the flanges are drawn on top of the liquid

### Requirement: Supported SVG subset
The asset loader SHALL support the SVG subset the assets use: `svg`, `g`, `defs`, `path`, `rect`, `circle`, `linearGradient` with `stop`, the attributes `fill` (a colour, `none` or `url(#id)` of a gradient), `stroke`, `stroke-width`, `stroke-linecap`, `stroke-linejoin`, `opacity`, `fill-opacity` and `stroke-opacity`. Loading an asset that uses an element outside this subset SHALL fail with an error naming the element, so an unsupported edit is found by the tests and not shown as missing art.

#### Scenario: Unsupported element
- **WHEN** an SVG with an element outside the subset, such as `text`, is loaded
- **THEN** loading fails with an error that names the element

