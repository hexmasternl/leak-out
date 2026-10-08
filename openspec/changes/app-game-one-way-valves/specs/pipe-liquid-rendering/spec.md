## ADDED Requirements

### Requirement: Valve tile shows its direction
A valve SHALL be drawn as the straight section of its axis with a valve marking that points to its outlet, following `docs/design-system.md`. The marking SHALL stay visible while the liquid fills the valve and after it is filled, at every tile size the game uses.

#### Scenario: Dry valve
- **WHEN** a valve with direction `LEFT` is drawn without liquid
- **THEN** the tile shows a horizontal straight with the valve marking pointing left

#### Scenario: Marking over the liquid
- **WHEN** a valve with direction `DOWN` is drawn with its channel filled
- **THEN** the valve marking pointing down is still visible over the liquid

### Requirement: Valve fills from its inlet
The liquid in a valve SHALL start at its inlet and move toward its outlet, following the same progress rules as any other channel.

#### Scenario: Half-filled valve
- **WHEN** a valve with direction `RIGHT` is drawn at progress 0.5
- **THEN** the left half of the bore is filled and the right half is empty
