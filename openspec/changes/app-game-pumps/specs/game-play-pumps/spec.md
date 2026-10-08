# Spec Delta

## Purpose

Adds the pump, a pipe section that makes the flow faster for the rest of the attempt once the liquid has left it, so the player weighs its extra points against the extra pressure.

## ADDED Requirements

### Requirement: A pump connects left and right
A pump (`PUMP`) SHALL have one channel, between its left and its right side. Liquid SHALL be able to pass it in either direction. Liquid that reaches a pump from the top or the bottom SHALL leak, as at any closed side.

#### Scenario: Pump passed from left to right
- **WHEN** the liquid enters a pump from the left
- **THEN** it leaves the pump on the right

#### Scenario: Pump passed from right to left
- **WHEN** the liquid enters a pump from the right
- **THEN** it leaves the pump on the left

#### Scenario: Pump reached from the top
- **WHEN** the liquid leaves a section downwards into a pump
- **THEN** the level is lost by a leak at that section

### Requirement: The liquid runs at normal speed inside a pump
Inside a pump the liquid SHALL move at the current flow speed, without a slow-down or a boost. The boost SHALL start only once the liquid has left the pump.

#### Scenario: Speed while passing a pump
- **WHEN** the flow speed is 1.0 cells per second, no pump has been left yet and the head is halfway through a pump
- **THEN** the head moves at 1.0 cells per second

### Requirement: Leaving a pump boosts the flow for the rest of the attempt
Each time the liquid leaves a pump, the number of pumps left in this attempt SHALL go up by one, after the pump's points are awarded. The boost SHALL last until the attempt ends, and SHALL be reset when the level restarts.

#### Scenario: Speed after one pump
- **WHEN** a level has 1.0 cells per second, no speed ramp and a pump speed factor of 1.5, and the liquid leaves a pump
- **THEN** the flow speed is 1.5 cells per second in every following section

#### Scenario: Speed after two pumps
- **WHEN** the same level's liquid has left two pumps
- **THEN** the flow speed is 2.25 cells per second

#### Scenario: Restart resets the boost
- **WHEN** the liquid has left a pump and the player restarts the level
- **THEN** the flow speed is the level's cells per second again

### Requirement: A pump never makes the flow faster than the speed cap
The speed cap SHALL be 10 cells per second, the highest `flow.cellsPerSecond` a level may have. A pump boost SHALL NOT raise the flow speed above the cap. When the speed without pumps is already above the cap, a pump SHALL NOT change it.

#### Scenario: Boost capped
- **WHEN** a level has 5.0 cells per second, a pump speed factor of 3 and the liquid has left two pumps
- **THEN** the flow speed is 10 cells per second, not 45

#### Scenario: Ramp above the cap
- **WHEN** the speed ramp has brought the flow to 11 cells per second and the liquid leaves a pump
- **THEN** the flow speed stays 11 cells per second

### Requirement: Fast forward runs on top of the pump boost
Fast forward SHALL scale the flow time as before. The pump boost SHALL scale the flow speed. Both SHALL apply together, so with fast forward on after a pump the liquid moves 10 times as far per tick as at the boosted speed.

#### Scenario: Fast forward after a pump
- **WHEN** the boosted flow speed is 1.5 cells per second, fast forward is on and a tick of 100 ms is sent
- **THEN** the head moves 1.5 cells

### Requirement: The pump has its own look
The pump SHALL be drawn from its own asset with its glass overlay, on the field, in the stack and in the dragged copy. It SHALL be a horizontal pipe with an impeller housing in the centre and a small "fast" badge in the pump colour in the top-right corner, so it can be told apart from the reservoir by shape and badge, not only by colour. The liquid SHALL fill its channel from the entry side like a straight, in the liquid colour.

#### Scenario: Pump in the stack
- **WHEN** the stack holds a pump
- **THEN** its slot shows the pump tile with its impeller housing, "fast" badge and glass overlay

#### Scenario: Liquid in a pump
- **WHEN** a pump entered from the right is drawn at progress 0.5
- **THEN** the right half of its channel is filled with liquid and the left half is empty

### Requirement: The HUD shows the boost
When a pump boost changes the flow speed shown in the HUD, the speed number SHALL pulse once in the pump colour over about 600 ms. With the system "remove animations" setting on, the number SHALL change without a pulse.

#### Scenario: Pulse after a pump
- **WHEN** the HUD shows a flow speed of 10 and the liquid leaves a pump with a factor of 1.5
- **THEN** the HUD shows 15 and the number pulses once in the pump colour

#### Scenario: No pulse with reduced motion
- **WHEN** animations are turned off and the liquid leaves a pump
- **THEN** the HUD shows the new speed without a pulse

### Requirement: The pump is described for accessibility
A cell with a pump SHALL be read as a pump, with the same placed, filled and mandatory states as the other sections. A pump in the stack SHALL be named "Pump".

#### Scenario: Placed pump read aloud
- **WHEN** a screen reader reaches a placed pump the liquid has filled
- **THEN** it reads "Placed Pump section, filled", built from string resources

#### Scenario: Next section is a pump
- **WHEN** the next section in the stack is a pump
- **THEN** its content description names the pump and says that it is the next section

### Requirement: Pump tip
The first time a pump in the stack is fully visible on screen during play, or a level starts with a mandatory pump, whichever comes first, the game SHALL freeze and show the pump tip, highlighting that pump. The tip SHALL say that a pump only connects left and right, that the flow runs faster for the rest of the level once the liquid leaves it, shown as a percentage, and that it scores 40 points. Full visibility SHALL be judged as for the cross tip.

#### Scenario: Pump drawn onto the stack
- **WHEN** the pump tip has not been seen, the level's pump speed factor is 1.5 and a placement draws a `PUMP` into a fully visible slot
- **THEN** the game freezes, the `PUMP` in the stack is highlighted and the tip says the flow runs 50 % faster after a pump and that it scores 40 points

#### Scenario: Mandatory pump on the board
- **WHEN** the pump tip has not been seen and a level with a mandatory pump and a `PUMP` weight of 0 starts
- **THEN** the pump tip is shown before the countdown runs, highlighting the mandatory pump on the board

#### Scenario: Pump tip shown once
- **WHEN** the pump tip has been seen and a `PUMP` is drawn onto the stack
- **THEN** no tip is shown and the game keeps running

### Requirement: Pump chapter of bundled levels
The game SHALL ship a chapter of 24 new levels that introduce the pump, filling one whole level library page right after the last page of existing levels. Its first level SHALL deal pumps or hold a mandatory pump. Every level in the chapter SHALL parse, validate and pass the solver check. The page SHALL open by the existing page star rule.

#### Scenario: Chapter fills one page
- **WHEN** the level library shows the page after the last page of existing levels
- **THEN** it holds 24 levels and each of them deals pumps or holds a mandatory pump

#### Scenario: Chapter levels are solvable
- **WHEN** the headless solvability suite runs over the bundled levels
- **THEN** every level of the pump chapter passes

#### Scenario: Existing levels unchanged
- **WHEN** the pump chapter is added
- **THEN** levels 1 to 96 have the same files, scores and saved progress as before, and none of them deals a pump
