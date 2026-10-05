## MODIFIED Requirements

### Requirement: Achievement catalogue
The system SHALL define a catalogue of achievements, each with a stable key, a name, a description, a group (progress, skill, lifetime or hidden), a type (standard, or incremental with a number of steps), a hidden flag and an XP value. The catalogue SHALL hold these 39 achievements, the 25 launch achievements first and the later additions after them:

| Key | Name | Type | Hidden | XP |
|---|---|---|---|---|
| `first_leak_fixed` | First Leak Fixed | standard | no | 10 |
| `levels_finished_1` | Getting the Hang of It | incremental, 10 | no | 15 |
| `star_collector_1` | Star Collector I | incremental, 25 | no | 15 |
| `star_collector_2` | Star Collector II | incremental, 60 | no | 30 |
| `perfectionist_1` | Perfectionist I | incremental, 10 | no | 30 |
| `pack_1_complete` | Pack 1 Complete | incremental, 24 | no | 30 |
| `pack_1_perfect` | Pack 1 Perfect | incremental, 24 | no | 40 |
| `zero_waste` | Zero Waste | standard | no | 30 |
| `double_cross` | Double Cross | standard | no | 20 |
| `crossroads_king` | Crossroads King | standard | no | 40 |
| `ahead_of_the_flow` | Ahead of the Flow | standard | no | 30 |
| `full_throttle` | Full Throttle | standard | no | 15 |
| `measure_twice` | Measure Twice | standard | no | 20 |
| `steady_hands` | Steady Hands | standard | no | 15 |
| `long_way_round` | The Long Way Round | standard | no | 30 |
| `big_spender` | Big Spender | standard | no | 30 |
| `apprentice` | Apprentice | incremental, 100 | no | 10 |
| `journeyman` | Journeyman | incremental, 1000 | no | 25 |
| `guildmaster` | Guildmaster | incremental, 5000 | no | 40 |
| `never_give_up` | Never Give Up | standard | no | 20 |
| `personal_best` | Personal Best | standard | no | 15 |
| `wet_feet` | Wet Feet | standard | no | 5 |
| `instant_flood` | Instant Flood | standard | yes | 20 |
| `wall_to_wall` | Wall to Wall | standard | yes | 30 |
| `photo_finish` | Photo Finish | standard | yes | 30 |
| `tips_addict` | Tips Addict | standard | no | 5 |
| `long_way_round_2` | The Long Way Round II | standard | no | 10 |
| `long_way_round_3` | The Long Way Round III | standard | no | 15 |
| `big_spender_2` | Big Spender II | standard | no | 15 |
| `crossroad_master` | Crossroad Master | standard | no | 15 |
| `efficiency_master` | Efficiency Master | standard | no | 15 |
| `highway` | Highway | standard | no | 10 |
| `snake` | Snake | standard | no | 15 |
| `eraser` | Eraser | standard | no | 5 |
| `dramatic_failure` | Dramatic Failure | standard | yes | 5 |
| `on_your_nerves` | On Your Nerves | standard | yes | 5 |
| `star_collector_3` | Star Collector III | incremental, 100 | no | 10 |
| `star_collector_4` | Star Collector IV | incremental, 250 | no | 15 |
| `snail` | Snail | standard | no | 10 |

`tips_addict` SHALL be in the lifetime group, `dramatic_failure` and `on_your_nerves` in the hidden group, `star_collector_3` and `star_collector_4` in the progress group counting stars like the earlier Star Collector tiers, and the other additions in the skill group.

#### Scenario: XP budget
- **WHEN** the XP values of the catalogue are added up
- **THEN** the sum is 745, every value is a multiple of 5, and 255 of the 1000 XP Play Games allows is left

#### Scenario: Launch entries unchanged
- **WHEN** the catalogue is compared with the 25 launch achievements
- **THEN** each of them is present with the same key, type, steps, hidden flag and XP, Big Spender still needs 500 and The Long Way Round still needs 30 tiles

#### Scenario: Keys are unique
- **WHEN** the catalogue is read
- **THEN** every achievement has a different key

#### Scenario: Steps within Play Games limits
- **WHEN** an achievement is incremental
- **THEN** its number of steps is between 2 and 10000

## ADDED Requirements

### Requirement: Higher tiers of skill achievements
The Long Way Round II and III SHALL unlock on a win where the liquid left at least 50 and at least 100 different placed tiles; a cross SHALL count once. Big Spender II SHALL unlock on a win with a final total, after the unused-section penalty, of 1000 or more. Each tier SHALL be decided on its own, so one win can unlock several tiers at once.

#### Scenario: One win unlocks every tier it reaches
- **WHEN** a level is won after the liquid left 104 different placed tiles
- **THEN** The Long Way Round, The Long Way Round II and The Long Way Round III are all newly unlocked

#### Scenario: Between two tiers
- **WHEN** a level is won after the liquid left 49 different placed tiles
- **THEN** The Long Way Round is unlocked and The Long Way Round II is not

#### Scenario: Penalty counts for Big Spender II
- **WHEN** a level is won with 1009 points earned and 4 unused sections, a final total of 997
- **THEN** Big Spender is unlocked and Big Spender II is not

#### Scenario: A loss is no long way round
- **WHEN** a level is lost after the liquid left 60 different placed tiles
- **THEN** none of The Long Way Round tiers is unlocked

### Requirement: Route shape achievements
Route shape achievements SHALL be decided from the sections the liquid left, in the order it left them; a cross left twice SHALL appear twice in that order. Their rules SHALL be:
- Crossroad Master: a win in which the liquid left 3 or more cross sections one directly after the other.
- Highway: a win in which the liquid left 7 or more straight sections of the same direction one directly after the other.
- Snake: a win in which the liquid left 12 or more corner sections one directly after the other.

Mandatory sections SHALL count like placed sections.

#### Scenario: Three crosses in a row
- **WHEN** a level is won and the liquid left a straight, then 3 crosses one after the other
- **THEN** Crossroad Master is newly unlocked

#### Scenario: Three crosses in a row and a leak
- **WHEN** the liquid leaves 3 crosses one after the other and then leaks
- **THEN** Crossroad Master is not unlocked

#### Scenario: A straight between crosses
- **WHEN** a level is won and the liquid left 2 crosses, a straight and another cross
- **THEN** Crossroad Master is not unlocked

#### Scenario: Highway of seven
- **WHEN** a level is won and the liquid left 7 horizontal straights one after the other
- **THEN** Highway is newly unlocked

#### Scenario: A cross breaks the highway
- **WHEN** a level is won and the liquid left 4 horizontal straights, a cross and 3 horizontal straights
- **THEN** Highway is not unlocked

#### Scenario: Snake of twelve
- **WHEN** a level is won and the liquid left 12 corner sections one after the other
- **THEN** Snake is newly unlocked

#### Scenario: Eleven corners are no snake
- **WHEN** a level is won and the longest run of corners the liquid left is 11
- **THEN** Snake is not unlocked

### Requirement: Snail
Snail SHALL unlock on a win in which the liquid ran through 3 or more reservoirs. Mandatory reservoirs SHALL count like placed ones.

#### Scenario: Three reservoirs and a win
- **WHEN** a level is won after the liquid left 3 reservoirs
- **THEN** Snail is newly unlocked

#### Scenario: Two reservoirs
- **WHEN** a level is won after the liquid left 2 reservoirs
- **THEN** Snail is not unlocked

#### Scenario: Three reservoirs and a leak
- **WHEN** the liquid leaves 3 reservoirs and then leaks
- **THEN** Snail is not unlocked

### Requirement: Higher Star Collector tiers
Star Collector III and IV SHALL count the stars of every finished level, like Star Collector I and II, and unlock at 100 and 250 stars.

#### Scenario: Between the tiers
- **WHEN** the stored stars add up to 180
- **THEN** Star Collector III is unlocked and Star Collector IV shows 180 of 250

### Requirement: Efficiency Master
Efficiency Master SHALL unlock on a win when the number of sections on the board at the moment of the win, mandatory and unused sections included, is no more than the number of cells of the level's shortest route as the level solver finds it. A level the solver cannot solve SHALL never unlock it.

#### Scenario: Shortest route
- **WHEN** a level whose shortest route has 9 cells is won with exactly 9 sections on the board
- **THEN** Efficiency Master is newly unlocked

#### Scenario: One extra section
- **WHEN** the same level is won with 9 sections on the route and 1 unused section elsewhere on the board
- **THEN** Efficiency Master is not unlocked

#### Scenario: Replacements do not count against it
- **WHEN** the same level is won with 9 sections on the board after 2 replacements
- **THEN** Efficiency Master is newly unlocked

### Requirement: Eraser
Eraser SHALL unlock on a win with 20 or more replacements in that attempt.

#### Scenario: Twenty replacements and a win
- **WHEN** an attempt with 20 replacements is won
- **THEN** Eraser is newly unlocked

#### Scenario: Twenty replacements and a leak
- **WHEN** an attempt with 20 replacements is lost
- **THEN** Eraser is not unlocked

#### Scenario: Nineteen replacements
- **WHEN** an attempt with 19 replacements is won
- **THEN** Eraser is not unlocked

### Requirement: Tips Addict
Tips Addict SHALL unlock the first time the player resets the seen tips with "Show tips again" on the settings screen. The reset SHALL be counted in the player stats, so it also unlocks when the app is restarted before the evaluation runs.

#### Scenario: Show tips again
- **WHEN** the player taps "Show tips again" on the settings screen for the first time
- **THEN** Tips Addict is newly unlocked

#### Scenario: Unlocks once
- **WHEN** Tips Addict is unlocked and the player resets the tips again
- **THEN** nothing is newly unlocked

### Requirement: Further hidden achievements
These hidden achievements SHALL unlock as follows:
- Dramatic Failure: a level is lost after the liquid left more than 50 sections; a cross left twice SHALL count twice.
- On Your Nerves: during one running replacement, the player tapped the tile being replaced 5 or more times before its new section landed. It SHALL be decided when the attempt ends, won or lost.

#### Scenario: Long route lost
- **WHEN** a level is lost after the liquid left 51 sections
- **THEN** Dramatic Failure is newly unlocked

#### Scenario: Fifty is not enough
- **WHEN** a level is lost after the liquid left exactly 50 sections
- **THEN** Dramatic Failure is not unlocked

#### Scenario: Impatient taps
- **WHEN** the player taps the tile of a running replacement 5 times before it lands and later wins the level
- **THEN** On Your Nerves is newly unlocked

#### Scenario: Taps spread over replacements
- **WHEN** the player taps the tiles of two running replacements 3 times each
- **THEN** On Your Nerves is not unlocked

#### Scenario: Taps on another tile
- **WHEN** the player taps 5 tiles other than the one being replaced during a running replacement
- **THEN** On Your Nerves is not unlocked

### Requirement: Later additions are reachable
Every skill and hidden achievement this change adds SHALL be possible on at least one bundled level, proven by a scripted attempt played through the game engine. Tips Addict SHALL be proven by an evaluation of player stats with one tips reset. Achievements that no bundled level can reach SHALL NOT be added to the catalogue.

#### Scenario: Large-grid tiers are reachable
- **WHEN** the reachability test runs
- **THEN** a scripted attempt on a bundled 16 × 12 level unlocks The Long Way Round III and Big Spender II

#### Scenario: Snail is reachable
- **WHEN** the reachability test runs
- **THEN** a scripted attempt on a bundled level that draws reservoirs runs the liquid through 3 of them and unlocks Snail

#### Scenario: Star milestones fit the bundled levels
- **WHEN** the star milestones are compared with the bundled levels
- **THEN** 250 stars is no more than 3 stars for every bundled level
