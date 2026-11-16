## ADDED Requirements

### Requirement: Reservoir shows the level's reservoir type
The reservoir SHALL be drawn with the accent colours of the level's reservoir type, both on the playing field and in the stack: red for 0.1, blue for 0.2, purple (the design system's violet) for 0.3 and orange for 0.4. The accent SHALL be applied when drawing, by replacing the reservoir colours of `res.svg`; there SHALL still be one reservoir asset. The "slow" badge SHALL show the reservoir's percentage of normal speed (10 %, 20 %, 30 % or 40 %) next to its icon, so colour is never the only cue. The liquid SHALL keep its own colour in every reservoir type.

#### Scenario: Red reservoir
- **WHEN** a level with a reservoir speed factor of 0.1 shows a reservoir on the playing field
- **THEN** its tank is drawn in the red reservoir accent and its badge reads "10 %"

#### Scenario: Default reservoir
- **WHEN** a level without a reservoir speed factor shows a reservoir in the stack
- **THEN** its tank is drawn in the purple reservoir accent and its badge reads "30 %"

#### Scenario: Liquid colour unchanged
- **WHEN** liquid fills an orange reservoir
- **THEN** the liquid is drawn in the normal liquid colour
