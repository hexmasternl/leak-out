# Spec Delta

## Purpose

Leaky tiles are cells that accept any pipe section but drain the liquid that passes them, so a route over one costs points. This capability covers where they can be, how they score, how they look and how they are described.

## ADDED Requirements

### Requirement: Any section can be placed on a leaky tile
A leaky tile SHALL accept a section from the stack exactly like an empty cell, by drag or by tap. A section on a leaky tile SHALL follow the normal replace rules. The cell SHALL stay leaky for the whole attempt, also after its section is replaced, and a restart SHALL show the leaky tiles empty again.

#### Scenario: Drop on a leaky tile
- **WHEN** the player drops a corner section on an empty leaky tile
- **THEN** the corner is placed on that tile and the tile still shows as leaky under the pipe

#### Scenario: Replace a section on a leaky tile
- **WHEN** replacing is allowed and the player drops a straight on a dry corner that lies on a leaky tile
- **THEN** the straight replaces the corner and the tile is still leaky

#### Scenario: Restart
- **WHEN** the player has placed sections on leaky tiles and restarts the level
- **THEN** the leaky tiles are empty and still leaky

### Requirement: Passing a leaky tile costs points
Each time the liquid exits a section on a leaky tile, the attempt SHALL get no points for that section and SHALL lose 20 points instead, the leak penalty. This SHALL hold for every section type and for every pass: a cross on a leaky tile passed through both channels SHALL cost the penalty twice.

#### Scenario: Straight on a leaky tile
- **WHEN** the score is 40 and the liquid exits a straight on a leaky tile
- **THEN** the score is 20

#### Scenario: Cross on a leaky tile passed twice
- **WHEN** the score is 100 and the liquid exits a cross on a leaky tile through its horizontal channel and later through its vertical channel
- **THEN** the score is 80 after the first exit and 60 after the second, and neither the 10 nor the 50 cross points are awarded

#### Scenario: Leak penalty before a leak
- **WHEN** the liquid exits a corner on a leaky tile into an empty cell
- **THEN** the leak penalty is taken and the level is lost at that corner's exit side

### Requirement: A leaky tile does not change the flow
A leaky tile SHALL NOT change the flow speed, the channels of the section on it, the route check or where the liquid goes next. A route that the route check reports as complete SHALL also be won when it runs over leaky tiles.

#### Scenario: Same speed on a leaky tile
- **WHEN** the base speed is 1.0 cells per second and the head is in a straight on a leaky tile
- **THEN** the head moves at 1.0 cells per second

#### Scenario: Complete route over a leaky tile
- **WHEN** a complete route runs over two leaky tiles and ticks are sent until the flow ends
- **THEN** the level is won and the earned points include two leak penalties

### Requirement: Leaky tiles look cracked and damp
The field SHALL draw a leaky tile as the empty-cell tile with cracks, a damp stain and a small drip badge in the top-right corner, as described in the design system, so it is told apart from empty, blocked and mandatory cells. A section placed on it SHALL be drawn on top with its normal pipe art, and the cracks and the badge SHALL stay visible around the pipe. The look SHALL follow pan and zoom.

#### Scenario: Leaky tile at the start
- **WHEN** a level with a leaky tile starts
- **THEN** that tile shows cracks, a damp stain and a drip badge, and looks different from empty and blocked cells

#### Scenario: Section on a leaky tile
- **WHEN** the player places a straight on a leaky tile
- **THEN** the straight's pipe art is drawn on the tile and the drip badge is still visible

### Requirement: Liquid drips while it passes a leaky tile
While the liquid head is in a section on a leaky tile, the field SHALL draw drops in the liquid colour falling from the pipe into the cracks. When the head leaves, the drops SHALL stop and the tile SHALL keep a darker wet stain for the rest of the attempt. The drops SHALL follow game time, so they freeze while the game is paused. With the system setting to remove animations on, no drops SHALL be drawn and only the wet stain SHALL show.

#### Scenario: Drops while filling
- **WHEN** the head is halfway through a straight on a leaky tile
- **THEN** drops fall from the pipe on that tile and the liquid is drawn in the pipe as in any straight

#### Scenario: Wet stain afterwards
- **WHEN** the head has left a section on a leaky tile
- **THEN** no more drops fall there and the tile shows a wet stain

#### Scenario: Reduced motion
- **WHEN** animations are turned off on the device and the liquid passes a leaky tile
- **THEN** no drops are drawn and the tile shows the wet stain once the head has entered it

### Requirement: Leaky tiles are described to screen readers
A screen reader SHALL name a leaky tile as leaky: an empty one as "Leaky tile, empty" and one with a section by adding "on a leaky tile" to the section's description, for example "Placed Corner section on a leaky tile, filled". The texts SHALL come from string resources.

#### Scenario: Empty leaky tile
- **WHEN** a screen reader reaches an empty leaky tile
- **THEN** it reads "Leaky tile, empty"

#### Scenario: Section on a leaky tile
- **WHEN** a screen reader reaches a filled corner on a leaky tile
- **THEN** it reads "Placed Corner section on a leaky tile, filled"

### Requirement: Leaky tiles appear occasionally from level 25 onwards
The bundled levels 1 to 24 SHALL have no leaky tiles. Level 25 SHALL be the first bundled level with a leaky tile. From level 25 to 96, leaky tiles SHALL appear occasionally: some levels SHALL have leaky tiles and most SHALL have none. From level 49 onwards, some of the levels with leaky tiles SHALL offer a shortcut: their shortest route SHALL cross a leaky tile while a longer route crosses none, so the player chooses between losing points on the short route and placing more sections on the long one. Every bundled level with leaky tiles SHALL pass the solver check. No level SHALL be added, removed or renumbered by this change.

#### Scenario: No leaky tiles before level 25
- **WHEN** the bundled levels 1 to 24 are loaded
- **THEN** none of them has a leaky tile

#### Scenario: Level 25 introduces leaky tiles
- **WHEN** level 25 is loaded
- **THEN** it has at least one leaky tile and a route that avoids every leaky tile

#### Scenario: Leaky tiles are occasional
- **WHEN** the bundled levels 25 to 96 are loaded
- **THEN** some of them have leaky tiles and fewer than half of them do

#### Scenario: Shortcut over a leaky tile from level 49
- **WHEN** the bundled levels 49 to 96 without mandatory sections are solved once as they are and once with their leaky tiles treated as blocked
- **THEN** for at least four of them both solves succeed and the first route is shorter than the second and crosses a leaky tile

#### Scenario: Levels with leaky tiles pass the solver check
- **WHEN** the headless solvability suite runs over the bundled levels
- **THEN** every level with leaky tiles is reported solvable

### Requirement: Levels without leaky tiles and saved progress are unaffected
Adding leaky tiles SHALL NOT change levels 1 to 24, nor any later level that gets no leaky tile: their files, ids, star thresholds and play SHALL stay as before. Updating a level with leaky tiles SHALL keep its id. Saved level history, stars, achievements and settings SHALL be kept; stars already earned on an updated level SHALL NOT be taken away. An updated level's star thresholds SHALL still allow three stars on a route that avoids the leaky tiles.

#### Scenario: Unchanged level files
- **WHEN** the change is applied
- **THEN** the files for levels 1 to 24, and for every later level without leaky tiles, are byte-identical to before, and each parses to the same level as before

#### Scenario: Saved progress kept
- **WHEN** a player who has finished levels 1 to 60 with stars updates the app
- **THEN** the level history still holds those 60 results with their stars, including those of updated levels, and the library opens the same pages as before
