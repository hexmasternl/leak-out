## ADDED Requirements

### Requirement: Replacing starts a replacement sequence
When the player drops a section on a tile whose placed section accepts a replacement, the system SHALL start a replacement sequence on that tile instead of swapping the sections at once. When the sequence starts, the old section SHALL leave the board, the tile SHALL be empty, and the dropped section SHALL leave the stack, which moves down. The sequence SHALL last the level's `rules.replacePenaltyMs` of game time. When that time has passed, the dropped section SHALL land on the tile. At most one replacement sequence SHALL run at a time.

#### Scenario: Old section leaves at once
- **WHEN** the player drops `V` on a tile that holds an `H` the liquid has not entered, in a level with `rules.replacePenaltyMs` 1500
- **THEN** the tile is empty right away, the `H` is gone, and the stack has moved down

#### Scenario: New section lands after the penalty
- **WHEN** 1500 ms of game time have passed since that replacement started
- **THEN** the tile holds `V` and the sequence has ended

#### Scenario: Not yet landed just before the penalty ends
- **WHEN** 1499 ms of game time have passed since that replacement started
- **THEN** the tile is still empty

#### Scenario: Tick length does not matter
- **WHEN** the same replacement runs once with 1 ms ticks and once with one 1500 ms tick
- **THEN** both attempts end with the same board, flow, score and outcome

#### Scenario: Zero penalty
- **WHEN** the player replaces a section in a level with `rules.replacePenaltyMs` 0
- **THEN** the new section is on the tile right away and no sequence is running

### Requirement: No player interaction during a replacement sequence
While a replacement sequence runs, the system SHALL NOT accept any placement on any tile. The player SHALL NOT be able to pick up a section from the stack. A tap or drop on the field SHALL be ignored and SHALL NOT play the placement failed sound. The fast forward button SHALL NOT change the speed. The pause button SHALL stay available.

#### Scenario: Drop during the sequence
- **WHEN** a replacement sequence runs and the player drops a section on an empty tile
- **THEN** nothing is placed, no tile was highlighted, and no sound plays

#### Scenario: Tap during the sequence
- **WHEN** a replacement sequence runs and the player taps an empty tile
- **THEN** nothing is placed and no sound plays

#### Scenario: Stack cannot be dragged
- **WHEN** a replacement sequence runs and the player starts dragging the next section in the stack
- **THEN** no section is picked up

#### Scenario: Fast forward is ignored
- **WHEN** a replacement sequence runs and the player taps the fast forward button
- **THEN** the flow speed does not change

#### Scenario: Pause stays available
- **WHEN** a replacement sequence runs and the player taps pause
- **THEN** the game pauses

#### Scenario: Interaction returns after landing
- **WHEN** the new section has landed
- **THEN** tiles accept sections again by the placement rule and the stack can be dragged

### Requirement: Replacement sequence runs on the game clock
The replacement sequence and its animation SHALL use the game clock. While the game is paused, the sequence SHALL NOT move on and its animation SHALL stand still. Fast forward SHALL NOT speed up the sequence. The liquid SHALL keep flowing during the sequence.

#### Scenario: Pause freezes the sequence
- **WHEN** the player pauses 500 ms into a 1500 ms sequence, waits 10 seconds and resumes
- **THEN** the new section lands 1000 ms of game time after resuming

#### Scenario: Liquid keeps flowing
- **WHEN** a replacement sequence runs while the liquid is flowing
- **THEN** the liquid moves on during the sequence as it would without the sequence

### Requirement: Liquid reaching the tile during the sequence leaks
While a replacement sequence runs, its tile SHALL count as an empty tile for the liquid. When the liquid reaches that tile before the new section lands, it SHALL leak there by the normal leak rules, the attempt SHALL be lost, and the new section SHALL NOT land.

#### Scenario: Liquid arrives too early
- **WHEN** the liquid flows into the tile of a replacement sequence before the new section has landed
- **THEN** the liquid leaks at that tile, the level is lost, and the tile stays empty

### Requirement: Replacement sequence ends with the attempt
When an attempt is won, lost or restarted while a replacement sequence runs, the sequence SHALL end without the new section landing and without the landing sound. A restart SHALL start without any replacement sequence.

#### Scenario: Restart during the sequence
- **WHEN** the player restarts the level during a replacement sequence
- **THEN** no sequence is running, the board is the level's starting board, and no landing sound plays later

#### Scenario: Lost elsewhere during the sequence
- **WHEN** the liquid leaks at another tile during a replacement sequence
- **THEN** the sequence ends and the new section does not land

### Requirement: Replacement sequence is animated
When a replacement sequence starts, the field SHALL show the old section on its tile in three phases that fill the sequence: in a 1500 ms sequence, the section SHALL break apart into pieces over the first 600 ms, the pieces SHALL lie still and fully visible for the next 400 ms, and the pieces SHALL fade out over the last 500 ms, after which the new section lands. A sequence of another length SHALL scale the three phases in the same proportions. The field SHALL NOT show a progress indicator. When the new section lands, the field SHALL show it appearing on the tile. The tile SHALL NOT be highlighted as a drop target during the sequence.

#### Scenario: Break animation at the start
- **WHEN** a 1500 ms replacement sequence has run for 300 ms
- **THEN** the old section is drawn breaking apart on the tile

#### Scenario: Broken pieces hold
- **WHEN** a 1500 ms replacement sequence has run for 800 ms
- **THEN** the broken pieces are drawn still and fully visible, and no progress indicator is shown

#### Scenario: Pieces fade out
- **WHEN** a 1500 ms replacement sequence has run for 1250 ms
- **THEN** the broken pieces are drawn at about half their opacity

#### Scenario: Landing
- **WHEN** the new section lands
- **THEN** the broken pieces are gone and the field shows the new section appearing on the tile

### Requirement: Replacement sequence sounds
When a replacement sequence starts, the system SHALL play the section destroyed sound (`section_destroy`) once. When the new section lands, the system SHALL play the section placed sound (`section_placement`) once. Both sounds SHALL use the stored sound volume. With a penalty of 0, both sounds SHALL play at the drop, destroy first.

#### Scenario: Destroy sound at the start
- **WHEN** the player replaces a section
- **THEN** the section destroyed sound plays once and the section placed sound does not play yet

#### Scenario: Placement sound at landing
- **WHEN** the new section lands
- **THEN** the section placed sound plays once

#### Scenario: Zero penalty plays both
- **WHEN** the player replaces a section in a level with `rules.replacePenaltyMs` 0
- **THEN** the section destroyed sound plays, followed by the section placed sound
