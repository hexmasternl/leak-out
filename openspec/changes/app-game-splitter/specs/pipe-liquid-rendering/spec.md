# Spec Delta

## ADDED Requirements

### Requirement: Splitter divides the liquid at its centre
For a splitter, progress 0 to 0.5 SHALL fill the arm from the entry side to the centre, and progress 0.5 to 1 SHALL fill both other open arms from the centre outwards at the same rate. The entry side SHALL be one of the splitter's three open sides. The closed side SHALL never show liquid. This SHALL work for liquid entering through each of the three open sides, in all four orientations.

#### Scenario: Splitter half filled
- **WHEN** a splitter closed at the bottom, entered from the left, is drawn at progress 0.5
- **THEN** the left arm is filled up to the centre and the top and right arms are empty

#### Scenario: Splitter three quarters filled
- **WHEN** the same splitter is drawn at progress 0.75
- **THEN** the left arm is full and the top and right arms are each filled halfway from the centre

#### Scenario: Entry through the closed side
- **WHEN** a fill is resolved for a splitter closed at the bottom with entry side bottom
- **THEN** resolving the fill fails

### Requirement: One SVG asset per splitter orientation
The app SHALL have a pipe asset and a glass overlay for each of the four splitter orientations, drawn by the design system's pipe recipe, with the three bores marked as liquid channels and the closed side capped.

#### Scenario: Splitter assets present
- **WHEN** the pipe assets are loaded
- **THEN** each of the four splitter orientations has its artwork and glass overlay, and each marks three bore ends
