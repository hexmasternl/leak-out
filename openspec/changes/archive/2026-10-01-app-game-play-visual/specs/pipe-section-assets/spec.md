## ADDED Requirements

### Requirement: Glass overlay per pipe section
The app SHALL ship one glass overlay SVG per pipe section in `app/src/main/assets/pipes/`, named after the section id in lower case followed by `-glass`: `h-glass.svg`, `v-glass.svg`, `lt-glass.svg`, `lb-glass.svg`, `rt-glass.svg`, `rb-glass.svg`, `x-glass.svg` and `res-glass.svg`. Every overlay SHALL use the view box `0 0 100 100` and put all its artwork in one group with the id `glass`. The overlay SHALL use the same supported SVG subset and the same design-system colours as the section assets, and SHALL contain no channels, tank or bridge. Loading an overlay without a `glass` group, or with an element outside the subset, SHALL fail with an error that names the problem.

#### Scenario: Every section has a glass overlay
- **WHEN** the glass overlays are loaded for every value of `PipeSection`
- **THEN** each one loads from its own `-glass.svg` file without an error and draws at least one shape

#### Scenario: Missing glass group
- **WHEN** an overlay without a group with the id `glass` is loaded
- **THEN** loading fails with an error that names the missing `glass` group

## MODIFIED Requirements

### Requirement: Layers above and below the liquid
Each asset SHALL group its artwork into a group with the id `under` (drawn below the liquid: shadow, body, specular, bore, tank) and a group with the id `over` (drawn above the liquid: flanges and, for the reservoir, the tank rim and the badge). The section's glass overlay SHALL be drawn last, above the `over` group. The renderer SHALL draw `under`, then the liquid, then `over`, then the glass overlay.

#### Scenario: Liquid sits inside the pipe
- **WHEN** a section is drawn with liquid
- **THEN** the liquid covers the bore and the flanges are drawn on top of the liquid

#### Scenario: Glass lies on top
- **WHEN** a section is drawn, with or without liquid
- **THEN** its glass overlay is drawn above the liquid and above the `over` group
