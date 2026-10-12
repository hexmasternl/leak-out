# game-play-gap-warning Specification

## Purpose
TBD - created by archiving change app-game-play-gap-warning. Update Purpose after archive.

## Requirements

### Requirement: Gap distance
While the liquid is flowing and the attempt is being played, the system SHALL measure the **gap distance**: how far, in sections, the liquid head is from the point where it would leak if nothing changes. The distance SHALL be the part of the head's current tile it still has to run through (1 minus its progress), plus 1 for every placed section or portal pair it would enter after that tile before it leaks. The walk ahead SHALL follow the same rules as the flow: an empty, blocked or start tile, the grid edge, a section without a matching opening, a cross channel the liquid already used, a portal entered through a closed side or a pair already passed, and a finish entered from the wrong side or with a mandatory section unused are all gaps. A tile whose replacement section has not yet landed SHALL count as empty. When the walk ahead reaches the finish and the liquid would win there, there SHALL be no gap. There SHALL be no gap while the flow waits to start and after the attempt is won or lost. The distance SHALL be measured again on every frame and after every placement.

#### Scenario: Gap right after the head's tile
- **WHEN** the liquid head is halfway through a straight whose exit faces an empty tile
- **THEN** the gap distance is 0.5

#### Scenario: Gap further ahead
- **WHEN** the liquid head has just entered a tile, and four connected sections follow it before an empty tile
- **THEN** the gap distance is 5

#### Scenario: Route into the finish
- **WHEN** the sections ahead of the liquid lead into the finish through its open side and every mandatory section is used on the way
- **THEN** there is no gap

#### Scenario: Mandatory section missed
- **WHEN** the sections ahead lead into the finish, but a mandatory section is not on the way
- **THEN** the gap is at the finish, and the distance counts the sections up to it

#### Scenario: Placement moves the gap
- **WHEN** the gap distance is 2.5 and the player places a connected section on the empty tile at the gap
- **THEN** the gap distance becomes 3.5 or more, depending on what lies beyond the new section

#### Scenario: Pending replacement
- **WHEN** a replacement is running on the tile right after the head's tile
- **THEN** that tile counts as a gap until the new section lands

#### Scenario: Countdown
- **WHEN** the flow is still counting down
- **THEN** there is no gap and no warning, however the field looks

#### Scenario: Gap behind a portal pair
- **WHEN** the liquid head is halfway through a straight whose exit leads into a portal, and the cell the other portal faces is empty
- **THEN** the gap distance is 1.5: half the straight plus one for the portal pair

#### Scenario: Head inside a portal pair
- **WHEN** the head is at progress 0.25 in a portal pair and the cell the exit portal faces is empty
- **THEN** the gap distance is 0.75

### Requirement: Score bar colour warns of the gap
While the gap distance is 5 or less, the score bar's fill SHALL be drawn in a colour between its normal brass and red (`leak.500`): brass at 5 sections, red at 3 sections and closer, and in between in proportion to the distance. While the gap distance is more than 5, or there is no gap, the score bar SHALL look as it does without a warning. The colour SHALL follow the distance on every frame, so it changes smoothly as the liquid flows. It SHALL go back at once when a placement moves the gap further away.

#### Scenario: Far from the gap
- **WHEN** the gap distance is 7
- **THEN** the score bar's fill is its normal brass

#### Scenario: Turning red
- **WHEN** the gap distance is 4
- **THEN** the score bar's fill is halfway between brass and `leak.500`

#### Scenario: Fully red
- **WHEN** the gap distance is 3 or less
- **THEN** the score bar's fill is `leak.500`

#### Scenario: Gap closed by building
- **WHEN** the score bar is red and the player completes the route to the finish
- **THEN** the score bar's fill is brass again at once

### Requirement: Score bar blinks close to the gap
While the gap distance is 3 or less, the score bar SHALL alternate between showing the score and showing the word "Warning", 500 ms each, starting with "Warning". The word SHALL use the same text style as the score, in capitals for the current locale. The fill, the markers and the sparkles SHALL keep working while the bar blinks. The timing SHALL follow the game clock, so the bar does not alternate while the game is paused. When the gap distance becomes more than 3 or there is no gap any more, the bar SHALL show the score again at once. With the system "remove animations" setting on, the text SHALL still alternate, without any fade.

#### Scenario: Blinking starts
- **WHEN** the gap distance drops to 3
- **THEN** the score bar shows "WARNING" for 500 ms, then the score for 500 ms, and so on

#### Scenario: Building stops the blinking
- **WHEN** the bar is blinking and the player places sections that put the gap 4 sections away
- **THEN** the bar shows the score and stops alternating, and its fill stays partly red

#### Scenario: Paused while blinking
- **WHEN** the game is paused while the bar shows "WARNING"
- **THEN** the bar keeps showing "WARNING" until the game is resumed and the 500 ms run out

#### Scenario: Attempt ends
- **WHEN** the liquid leaks while the bar is blinking
- **THEN** the bar stops blinking and shows the score

### Requirement: Score bar warning is accessible
While the score bar blinks, its content description SHALL add the warning and the gap distance in whole sections, rounded up, to the score description, such as "Score 150, 1 of 3 stars. Warning: leak in 3 sections". The description SHALL NOT change with every blink. The text SHALL come from string resources with a plural for the number of sections.

#### Scenario: Description while blinking
- **WHEN** the gap distance is 2.4 and the score is 150 with 1 of 3 stars
- **THEN** the score bar's content description is "Score 150, 1 of 3 stars. Warning: leak in 3 sections"

#### Scenario: Description without warning
- **WHEN** the gap distance is 4
- **THEN** the score bar's content description is the plain score description

### Requirement: Heartbeat as the liquid approaches the gap
While the gap distance is 5 or less, the game is not paused, the app is in the foreground and the attempt is being played, the system SHALL play the heartbeat sound in a loop. Its volume SHALL be the stored sound effect volume times a factor that grows linearly from 0.15 at a gap distance of 5 to 1.0 at a gap distance of 0. The volume SHALL follow the distance while the heartbeat plays. The heartbeat SHALL stop when the gap distance becomes more than 5, there is no gap, the game pauses, the app leaves the foreground, or the attempt is won, lost or restarted. It SHALL start again from the beginning of the file the next time the conditions hold. The heartbeat SHALL follow the sound effect settings like every other effect, SHALL NOT change the music, and SHALL be silent when no heartbeat file is bundled.

#### Scenario: Heartbeat starts quietly
- **WHEN** the sound volume is 0.8 and the gap distance drops to 5
- **THEN** the heartbeat starts looping at volume 0.12

#### Scenario: Heartbeat gets louder
- **WHEN** the heartbeat plays at sound volume 0.8 and the gap distance drops to 1
- **THEN** the heartbeat plays at volume 0.66 (0.8 × 0.83)

#### Scenario: Building stops the heartbeat
- **WHEN** the heartbeat plays and the player places sections that put the gap 6 sections away
- **THEN** the heartbeat stops

#### Scenario: Paused
- **WHEN** the player opens the pause dialog while the heartbeat plays
- **THEN** the heartbeat stops, and it starts again when the game is resumed and the gap is still 5 sections away or less

#### Scenario: Leak
- **WHEN** the liquid leaks while the heartbeat plays
- **THEN** the heartbeat stops before the level lost sound plays

#### Scenario: Sound effects off
- **WHEN** `soundEnabled` is off and the gap distance is 2
- **THEN** no heartbeat plays
