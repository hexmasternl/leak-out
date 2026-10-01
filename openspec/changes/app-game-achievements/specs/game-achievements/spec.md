## ADDED Requirements

### Requirement: Achievement catalogue
The system SHALL define exactly 25 achievements, each with a stable key, a name, a description, a group (progress, skill, lifetime or hidden), a type (standard, or incremental with a number of steps), a hidden flag and an XP value. The achievements, steps, hidden flags and XP values SHALL be:

| Key | Name | Type | Hidden | XP |
|---|---|---|---|---|
| `first_leak_fixed` | First Leak Fixed | standard | no | 10 |
| `halfway` | Halfway Down the Drain | incremental, 12 | no | 30 |
| `master_plumber` | Master Plumber | incremental, 24 | no | 100 |
| `star_collector_1` | Star Collector I | incremental, 24 | no | 20 |
| `star_collector_2` | Star Collector II | incremental, 48 | no | 40 |
| `star_collector_3` | Star Collector III | incremental, 60 | no | 100 |
| `perfectionist` | Perfectionist | incremental, 24 | no | 100 |
| `zero_waste` | Zero Waste | standard | no | 30 |
| `double_cross` | Double Cross | standard | no | 20 |
| `crossroads_king` | Crossroads King | standard | no | 50 |
| `ahead_of_the_flow` | Ahead of the Flow | standard | no | 40 |
| `full_throttle` | Full Throttle | standard | no | 15 |
| `measure_twice` | Measure Twice | standard | no | 25 |
| `steady_hands` | Steady Hands | standard | no | 15 |
| `long_way_round` | The Long Way Round | standard | no | 40 |
| `big_spender` | Big Spender | standard | no | 40 |
| `apprentice` | Apprentice | incremental, 100 | no | 15 |
| `journeyman` | Journeyman | incremental, 1000 | no | 40 |
| `guildmaster` | Guildmaster | incremental, 5000 | no | 80 |
| `never_give_up` | Never Give Up | standard | no | 30 |
| `personal_best` | Personal Best | standard | no | 20 |
| `wet_feet` | Wet Feet | standard | no | 10 |
| `instant_flood` | Instant Flood | standard | yes | 30 |
| `wall_to_wall` | Wall to Wall | standard | yes | 50 |
| `photo_finish` | Photo Finish | standard | yes | 50 |

#### Scenario: XP budget
- **WHEN** the XP values of all achievements are added up
- **THEN** the sum is 1000 and every value is a multiple of 5

#### Scenario: Keys are unique
- **WHEN** the catalogue is read
- **THEN** it holds 25 achievements with 25 different keys

#### Scenario: Steps within Play Games limits
- **WHEN** an achievement is incremental
- **THEN** its number of steps is between 2 and 10000

#### Scenario: Catalogue matches the level library
- **WHEN** the bundled level library is loaded
- **THEN** it holds 24 levels, the number of steps of Master Plumber and Perfectionist

### Requirement: Progress achievements follow the level history
Progress achievements SHALL be decided from the level history alone. A level counts as finished when it has a history record, with any number of stars from 0 to 3. The progress of an incremental progress achievement SHALL be: finished levels for Halfway Down the Drain and Master Plumber, the sum of stored stars for Star Collector I to III, and the number of levels stored with 3 stars for Perfectionist. First Leak Fixed SHALL unlock when level 1 has a record. An incremental achievement SHALL unlock when its progress reaches its number of steps.

#### Scenario: First level finished
- **WHEN** the history holds a record for level 1 with 0 stars
- **THEN** First Leak Fixed is unlocked and Halfway Down the Drain has progress 1 of 12

#### Scenario: Star collector progress
- **WHEN** the history holds records with 3, 2 and 3 stars
- **THEN** Star Collector I has progress 8 of 24 and Perfectionist has progress 2 of 24

#### Scenario: Halfway reached
- **WHEN** the history holds 12 records
- **THEN** Halfway Down the Drain is unlocked

### Requirement: Skill achievements follow one attempt
Skill achievements SHALL be decided from the won attempt in which they are earned, using the attempt as it was when the level was won, before unused sections are removed. They SHALL unlock as follows:
- Zero Waste: the final score has no unused sections.
- Double Cross: the liquid left at least one cross section twice.
- Crossroads King: the liquid left at least three different cross sections twice each.
- Ahead of the Flow: the route was complete while the flow was still waiting to start.
- Full Throttle: fast forward was on when the liquid entered the finish.
- Measure Twice: no section was replaced during the attempt.
- Steady Hands: the game was never paused during the attempt, including the automatic pause when the app goes to the background.
- The Long Way Round: the liquid left 30 or more different placed tiles.
- Big Spender: the final total is 500 or more.

Double Cross and Crossroads King SHALL also be decided on a lost attempt, because the liquid can leave a cross twice before it leaks. All other skill achievements SHALL need a win.

#### Scenario: Zero waste win
- **WHEN** a level is won and the final score lists no unused sections
- **THEN** Zero Waste is unlocked

#### Scenario: Unused section
- **WHEN** a level is won with one unused section
- **THEN** Zero Waste is not unlocked

#### Scenario: Cross passed twice on a loss
- **WHEN** the liquid leaves a cross through its second channel and the pipe leaks later in the same attempt
- **THEN** Double Cross is unlocked

#### Scenario: Route ready before the flow
- **WHEN** the route is completed while the countdown is still running and the level is won
- **THEN** Ahead of the Flow is unlocked

#### Scenario: Route completed after the flow started
- **WHEN** the route is completed after the liquid has started flowing
- **THEN** Ahead of the Flow is not unlocked

#### Scenario: Replacement blocks Measure Twice
- **WHEN** a level is won and one section was replaced during the attempt
- **THEN** Measure Twice is not unlocked

#### Scenario: Pause blocks Steady Hands
- **WHEN** the game was paused once and resumed, and the level is won
- **THEN** Steady Hands is not unlocked

#### Scenario: Fast forward into the finish
- **WHEN** fast forward is on when the liquid enters the finish
- **THEN** Full Throttle is unlocked

#### Scenario: Long route
- **WHEN** a level is won and the liquid left 30 different placed tiles
- **THEN** The Long Way Round is unlocked

#### Scenario: Big score
- **WHEN** a level is won with a final total of 500
- **THEN** Big Spender is unlocked

### Requirement: Lifetime achievements follow player stats
Lifetime achievements SHALL be decided from player stats that last across attempts and are kept when progress is reset:
- Apprentice, Journeyman and Guildmaster: progress is the number of sections ever placed, counting every accepted placement including replacements and placements in attempts that were restarted or exited.
- Never Give Up: a level is won when its last 3 or more attempts that ended in a win or loss were all losses.
- Personal Best: recording a win replaced an existing highscore with a higher one.
- Wet Feet: the first loss ever.

#### Scenario: Placements add up
- **WHEN** the player has placed 98 sections so far and places 3 more in an attempt that is then restarted
- **THEN** Apprentice is unlocked and Journeyman has progress 101 of 1000

#### Scenario: Win after three losses
- **WHEN** level 5 was lost three times in a row and is then won
- **THEN** Never Give Up is unlocked

#### Scenario: Streak is per level
- **WHEN** level 5 was lost twice, level 6 was lost once, and level 5 is then won
- **THEN** Never Give Up is not unlocked

#### Scenario: Highscore beaten
- **WHEN** recording a win reports a new highscore
- **THEN** Personal Best is unlocked

#### Scenario: First completion is not a personal best
- **WHEN** recording a win reports a first completion
- **THEN** Personal Best is not unlocked

#### Scenario: First loss
- **WHEN** the player loses a level for the first time
- **THEN** Wet Feet is unlocked

### Requirement: Hidden achievements
Hidden achievements SHALL unlock as follows:
- Instant Flood: a level is lost before the liquid has left any placed section.
- Wall to Wall: a level is won while every free cell (not blocked, not the start, not the finish) holds a placed section.
- Photo Finish: a level is won and the placement that completed the route was made while the liquid was flowing, on the tile the liquid head's current channel exits into.

#### Scenario: Leak at the start
- **WHEN** the liquid leaves the start valve into an empty tile
- **THEN** the level is lost and Instant Flood is unlocked

#### Scenario: Full board
- **WHEN** a level is won and every free cell holds a section
- **THEN** Wall to Wall is unlocked

#### Scenario: Last-second completion
- **WHEN** the liquid head is in a corner section whose exit points at an empty tile, the player places the section that completes the route on that tile, and the level is then won
- **THEN** Photo Finish is unlocked

### Requirement: Achievements are evaluated by a pure function
The system SHALL decide achievements in `core` with a pure function that takes an optional attempt summary, the player stats after the attempt, the level history and the achievements already unlocked. It SHALL return the achievements newly unlocked by that call and the progress of every incremental achievement. Progress SHALL be limited to the number of steps and SHALL never be lower than the progress already stored. An achievement that is already unlocked SHALL never be returned as newly unlocked again. Without an attempt summary, only progress and lifetime achievements SHALL be evaluated.

#### Scenario: Already unlocked
- **WHEN** Zero Waste is already unlocked and another zero-waste win is evaluated
- **THEN** Zero Waste is not in the newly unlocked achievements

#### Scenario: Progress never goes down
- **WHEN** Star Collector I has stored progress 20 and the history now sums to 12 stars
- **THEN** the progress of Star Collector I stays 20

#### Scenario: Evaluation without an attempt
- **WHEN** the evaluation runs without an attempt summary and the history holds a record for level 1
- **THEN** First Leak Fixed is newly unlocked and no skill achievement is unlocked

### Requirement: Achievements are reachable
Every skill and hidden achievement SHALL be possible on at least one bundled level. This SHALL be proven by an automated test that plays a scripted attempt through the game engine for each of them.

#### Scenario: Crossroads King is reachable
- **WHEN** the reachability test runs
- **THEN** a scripted attempt on a bundled level that can draw crosses unlocks Crossroads King
