# Spec Delta

## MODIFIED Requirements

### Requirement: Liquid look
The liquid SHALL be drawn with the design system's liquid gradient (`liquid.300` → `liquid.500` → `liquid.700`), 0.21 of the cell wide, with a rounded head in `liquid.glow` at its leading edge while progress is between 0 and 1. A channel that is filling SHALL have a `liquid.glow` outer glow; a completely filled channel SHALL be drawn without the glow. A channel the tile is told is frozen SHALL be drawn as ice instead: the same width and path, with the design system's ice gradient (`ice.100` → `ice.300` → `ice.500`), no head and no glow. A freeze progress between 0 and 1 SHALL draw the first part of the channel, measured from its exit side, as ice and the rest as liquid.

#### Scenario: Head only while filling
- **WHEN** a channel is drawn at progress 0.4 and at progress 1
- **THEN** the first shows a glowing head at 40 % of the channel and the second shows no head

#### Scenario: Frozen channel
- **WHEN** an `H` entered from the left is drawn at progress 1 and frozen
- **THEN** the whole bore shows the ice gradient, with no head and no glow

#### Scenario: Freezing halfway
- **WHEN** an `H` entered from the left is drawn at progress 1 with a freeze progress of 0.5
- **THEN** the right half of the bore shows ice and the left half shows liquid
