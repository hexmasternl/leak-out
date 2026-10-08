# Spec Delta

## MODIFIED Requirements

### Requirement: Points are awarded when the liquid exits a section
The system SHALL award points for a pipe section on the board, whether the player placed it or the level placed it as a mandatory section or a fixed pipe, at the moment the liquid exits that section through the other port of the channel it entered. The system SHALL NOT award points when the liquid enters a section. A section that the liquid never reaches, or that the liquid has entered but not yet exited, SHALL NOT give points.

#### Scenario: No points while the liquid is inside a section
- **WHEN** the liquid has entered a straight section and has not reached its exit port yet
- **THEN** the score does not change

#### Scenario: Points when the liquid leaves a section
- **WHEN** the liquid reaches the exit port of a straight section and leaves it
- **THEN** the score goes up by 10

#### Scenario: Sections off the route give nothing
- **WHEN** the level ends and the player has placed sections that the liquid never reached
- **THEN** those sections have added nothing to the score

#### Scenario: Fixed pipe scores like a placed one
- **WHEN** the liquid leaves a fixed corner
- **THEN** the score goes up by 15, as for a placed corner

#### Scenario: Unreached fixed pipe gives nothing
- **WHEN** the level is won and the liquid never entered one of its fixed pipes
- **THEN** that fixed pipe has added nothing to the score
