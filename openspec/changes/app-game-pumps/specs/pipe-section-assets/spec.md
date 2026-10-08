## MODIFIED Requirements

### Requirement: One SVG asset per pipe section
The app SHALL ship one SVG file per pipe section in `app/src/main/assets/pipes/`, named after the section id in lower case: `h.svg`, `v.svg`, `lt.svg`, `lb.svg`, `rt.svg`, `rb.svg`, `x.svg`, `res.svg` and `pump.svg`. Every file SHALL use the view box `0 0 100 100`, which maps to one grid cell. The SVG files SHALL be the only source of the pipe artwork; the app SHALL NOT draw pipe bodies, flanges or tanks from geometry written in Kotlin.

#### Scenario: Every section has an asset
- **WHEN** the assets are loaded for every value of `PipeSection`
- **THEN** each one loads from its own file without an error

#### Scenario: Asset covers one cell
- **WHEN** any pipe asset is read
- **THEN** its view box is `0 0 100 100`

### Requirement: Artwork follows the design system
Each asset SHALL follow the pipe recipe in `docs/design-system.md` (sections 6.3 and 6.4), using only the design system's steel, surface, reservoir and pump colours: a steel body with a shadow side and a specular highlight, a dark bore, and a flange at every open port. The cross SHALL draw its vertical pipe first and its horizontal pipe on top with a bridge gap, so it reads as two separate channels. The reservoir SHALL show a round violet tank in the centre of a horizontal pipe and a small "slow" badge in the top-right corner. The pump SHALL show a round steel impeller housing with impeller blades in the centre of a horizontal pipe and a small "fast" badge in the pump colour in the top-right corner.

#### Scenario: Flanges at open ports only
- **WHEN** the asset of a section is drawn
- **THEN** there is a flange at every side the section is open on, and none at a closed side

#### Scenario: Cross reads as two pipes
- **WHEN** the cross asset is drawn
- **THEN** the horizontal pipe lies on top of the vertical pipe, separated by a gap in the background colour

#### Scenario: Pump differs from the reservoir by shape
- **WHEN** the pump and the reservoir assets are drawn in greyscale
- **THEN** the pump shows impeller blades and a "fast" badge, the reservoir a tank and a "slow" badge, and they can be told apart

### Requirement: Liquid channels are marked in the asset
Each asset SHALL contain one `path` element per liquid channel, with an id starting with `channel-` and the attributes `data-from` and `data-to` naming the two sides the channel connects (`left`, `right`, `top`, `bottom`). The path SHALL run along the centre of the bore from the `data-from` side to the `data-to` side, starting and ending on the edge of the cell. These channel paths SHALL NOT be drawn as artwork. The channels SHALL be exactly the channels of the section: one for `H`, `V`, `RES`, `PUMP` and each corner, and two for the cross (left–right and top–bottom).

#### Scenario: Channels match the section
- **WHEN** the channels of every asset are read
- **THEN** `H`, `RES` and `PUMP` have one channel left–right, `V` one top–bottom, `LT` left–top, `LB` left–bottom, `RT` right–top, `RB` right–bottom, and `X` two channels, left–right and top–bottom

#### Scenario: Channel ends lie on the cell edge
- **WHEN** a channel path is read
- **THEN** its start point lies on the middle of the edge of its `data-from` side and its end point on the middle of the edge of its `data-to` side

#### Scenario: Channel paths are invisible
- **WHEN** a section is drawn without liquid
- **THEN** the channel paths are not visible

#### Scenario: Pump has its own asset
- **WHEN** the assets are listed
- **THEN** `pump.svg` and its glass overlay are among them, and only `res.svg` contains a `tank` element
### Requirement: Glass overlay per pipe section
The app SHALL ship one glass overlay SVG per pipe section in `app/src/main/assets/pipes/`, named after the section id in lower case followed by `-glass`: `h-glass.svg`, `v-glass.svg`, `lt-glass.svg`, `lb-glass.svg`, `rt-glass.svg`, `rb-glass.svg`, `x-glass.svg`, `res-glass.svg` and `pump-glass.svg`. Every overlay SHALL use the view box `0 0 100 100` and put all its artwork in one group with the id `glass`. The overlay SHALL use the same supported SVG subset and the same design-system colours as the section assets, and SHALL contain no channels, tank or bridge. Loading an overlay without a `glass` group, or with an element outside the subset, SHALL fail with an error that names the problem.

#### Scenario: Every section has a glass overlay
- **WHEN** the glass overlays are loaded for every value of `PipeSection`
- **THEN** each one loads from its own `-glass.svg` file without an error and draws at least one shape

#### Scenario: Missing glass group
- **WHEN** an overlay without a group with the id `glass` is loaded
- **THEN** loading fails with an error that names the missing `glass` group
