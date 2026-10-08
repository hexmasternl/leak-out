## MODIFIED Requirements

### Requirement: Mandatory sections behave like placed sections for the liquid
For the liquid, the route check, points and the flow speed, a mandatory section SHALL behave exactly like a placed section of the same kind: the liquid enters it through an open side, leaves it through the other end of the channel it entered, earns the section's points when it leaves, runs at the reservoir speed inside a mandatory reservoir, and runs faster after it has left a mandatory pump.

#### Scenario: Liquid passes a mandatory cross
- **WHEN** the liquid enters a mandatory cross from the left
- **THEN** it leaves the cross on the right and the cross's points are awarded

#### Scenario: Mandatory reservoir slows the liquid
- **WHEN** the liquid is inside a mandatory reservoir
- **THEN** the liquid moves at the level's speed times `flow.reservoirSpeedFactor`

#### Scenario: Leak at a mandatory section
- **WHEN** the liquid leaves a mandatory reservoir to the right and the tile to its right is empty
- **THEN** the level is lost by a leak, as with a placed reservoir

#### Scenario: Mandatory pump boosts the liquid
- **WHEN** the liquid leaves a mandatory pump in a level with a pump speed factor of 1.5
- **THEN** the pump's 40 points are awarded and the flow speed is multiplied by 1.5, as with a placed pump

### Requirement: A mandatory section is used when every channel is passed
A mandatory section SHALL count as **used** once the liquid has run through every channel it has: both the horizontal and the vertical channel of a cross, and the single left-to-right channel of a reservoir or a pump, in either direction. A channel the liquid is still running through SHALL count as passed once the liquid has left it.

#### Scenario: Reservoir passed once
- **WHEN** the liquid has run through a mandatory reservoir from right to left
- **THEN** the reservoir is used

#### Scenario: Cross passed on one channel
- **WHEN** the liquid has run through a mandatory cross from left to right only
- **THEN** the cross is not used

#### Scenario: Cross passed on both channels
- **WHEN** the liquid has run through a mandatory cross from left to right and later from bottom to top
- **THEN** the cross is used

#### Scenario: Pump passed once
- **WHEN** the liquid has run through a mandatory pump from left to right
- **THEN** the pump is used
