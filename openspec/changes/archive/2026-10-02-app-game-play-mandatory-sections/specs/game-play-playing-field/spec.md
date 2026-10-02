# Spec Delta

## ADDED Requirements

### Requirement: Mandatory sections are drawn on a red tint
The field SHALL draw a mandatory section with the same pipe art as a placed section of its kind, on a tile tinted with the design system's `leak.tint` and edged in `leak.500`, so that it stands apart from the sections the player placed and from blocked cells. The tint SHALL stay the same while and after the liquid runs through the section, and SHALL follow pan and zoom.

#### Scenario: Mandatory reservoir is drawn
- **WHEN** a level with a mandatory reservoir starts
- **THEN** the field draws the reservoir's pipe art on a red-tinted tile

#### Scenario: Tint stays when filled
- **WHEN** the liquid runs through a mandatory cross
- **THEN** the cross's tile keeps its red tint and the liquid is drawn in the pipe as in any placed cross

#### Scenario: Placed section next to a mandatory one
- **WHEN** the player places a cross next to a mandatory cross
- **THEN** only the mandatory cross has the red-tinted tile
