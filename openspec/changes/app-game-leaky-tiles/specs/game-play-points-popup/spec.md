## MODIFIED Requirements

### Requirement: Points are shown on the tile
Each time points are awarded for a section, the system SHALL show the number of points on the tile of that section, as a plus sign followed by the number, such as "+10", "+15", "+25" or "+50". The text SHALL be centred on the tile, SHALL be drawn above the section and the liquid, and SHALL be sized to the tile so it stays readable at every zoom level. The glyphs SHALL be filled solid with the design system's `surface.deep` colour, edged with a thin `liquid.glow` stroke, over a soft `liquid.glow` glow, as described in the design system. No thick dark outline SHALL be drawn. The fill, stroke and glow SHALL fade together with the popup. When a leak penalty is taken, the popup SHALL show a minus sign followed by the penalty, such as "−20", with `leak.500` in place of `liquid.glow` for the stroke and the glow.

#### Scenario: Popup for a corner
- **WHEN** the liquid exits a corner section
- **THEN** "+15" appears centred on the tile of that corner

#### Scenario: Popup for the second pass of a cross
- **WHEN** the liquid exits a cross for the second time
- **THEN** "+50" appears on the tile of that cross

#### Scenario: Popup look
- **WHEN** a popup is fully visible over a filled straight section
- **THEN** its digits are filled with opaque `surface.deep`, edged with a thin `liquid.glow` stroke and surrounded by a soft `liquid.glow` glow, and the pipe and liquid do not show through the digits

#### Scenario: Look fades with the popup
- **WHEN** a popup has been shown for 850 ms, halfway through its fade-out
- **THEN** its fill, stroke and glow are all drawn at the popup's current alpha, so none of them stays visible after the popup is removed

#### Scenario: Popup for a leak penalty
- **WHEN** the liquid exits a straight on a leaky tile
- **THEN** "−20" appears centred on that tile, edged and glowing in `leak.500`, with the same timing as any other popup
