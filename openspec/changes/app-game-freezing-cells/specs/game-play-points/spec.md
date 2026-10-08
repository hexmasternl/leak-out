# Spec Delta

## MODIFIED Requirements

### Requirement: The last section before a leak still scores
When the liquid exits a placed section and the next tile does not accept it (an empty tile, a blocked tile, a closed side of a section, the edge of the grid, the wrong side of the finish, a used cross channel or a frozen section), the system SHALL still award the points for the section the liquid exited.

#### Scenario: Leak after a corner
- **WHEN** the liquid exits a corner section into an empty tile and leaks
- **THEN** the score goes up by 15 for the corner

#### Scenario: Leak into a frozen section
- **WHEN** the liquid exits a straight section into a frozen section and leaks
- **THEN** the score goes up by 10 for the straight
