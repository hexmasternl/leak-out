## ADDED Requirements

### Requirement: HUD bar is a distinct panel
The HUD bar SHALL be drawn as the HUD panel described in section 5.3 of the design system, so that it is clearly set apart from the stack column and the board below it. The panel SHALL keep the fixed height that the game screen layout depends on.

#### Scenario: HUD stands out from the board
- **WHEN** the game screen is shown
- **THEN** the HUD is drawn as the design system's HUD panel, with its own surface, edge and depth, and is visually separate from the stack column and the board

### Requirement: HUD shows a countdown bar before the flow
While the flow is waiting to start, the HUD SHALL show a countdown bar in the space between the flow speed and the score bar. The bar's fill SHALL be the countdown's remaining time divided by the level's `flow.initialDelayMs`: full when the attempt starts and empty when the liquid starts. The bar SHALL follow the game's flow time, so it does not move while the game is paused and drains ten times as fast while fast forward is on.

#### Scenario: Countdown bar at the start
- **WHEN** a level with a `flow.initialDelayMs` of 10,000 starts
- **THEN** the HUD shows the countdown bar completely full

#### Scenario: Countdown bar drains
- **WHEN** the countdown of a level with a `flow.initialDelayMs` of 10,000 has 4,000 ms left
- **THEN** the countdown bar is 40 % full

#### Scenario: Countdown bar is frozen while paused
- **WHEN** the game is paused with 6,000 ms left on the countdown
- **THEN** the countdown bar stays 60 % full until the game resumes

#### Scenario: Restart refills the countdown bar
- **WHEN** the player restarts the level
- **THEN** the countdown bar is completely full again

### Requirement: Countdown bar warns before the flow starts
The countdown bar's fill SHALL be liquid blue (`liquid.500`) while more than 5 seconds are left, brass (`brass.500`) while 5 seconds or less and more than 2 seconds are left, and red (`leak.500`) while 2 seconds or less are left. The seconds SHALL be counted in flow time.

#### Scenario: Plenty of time
- **WHEN** 8,000 ms are left on the countdown
- **THEN** the countdown bar is liquid blue

#### Scenario: Brass warning
- **WHEN** 5,000 ms are left on the countdown
- **THEN** the countdown bar is brass

#### Scenario: Red warning
- **WHEN** 1,500 ms are left on the countdown
- **THEN** the countdown bar is red

### Requirement: Countdown bar disappears once the liquid flows
When the liquid starts, the countdown bar SHALL fade out over the design system's `motion.quick` and its space SHALL stay empty for the rest of the attempt. The other HUD items SHALL NOT move when the bar disappears. A level with a `flow.initialDelayMs` of 0 SHALL show no countdown bar. When the system "remove animations" setting is on, the bar SHALL disappear at once.

#### Scenario: Liquid starts
- **WHEN** the countdown reaches 0 and the liquid starts
- **THEN** the countdown bar fades out and the level, flow speed and score bar stay where they are

#### Scenario: No start delay
- **WHEN** a level with a `flow.initialDelayMs` of 0 starts
- **THEN** the HUD shows no countdown bar

### Requirement: HUD shows the score on a progress bar
The HUD SHALL show the score as a progress bar directly to the left of the cog wheel. The bar's maximum SHALL be the level's `stars.three`; its fill SHALL be the score divided by `stars.three`, and full once the score is at or above `stars.three`. The bar SHALL have a marker line at `stars.one` and one at `stars.two`, placed at that threshold divided by `stars.three` along the bar. The bar SHALL have a fixed width that does not depend on the score.

#### Scenario: Empty bar at the start
- **WHEN** a level with star thresholds 100, 200 and 350 starts
- **THEN** the score bar is empty, with marker lines at 2/7 and 4/7 of its length

#### Scenario: Bar fills with the score
- **WHEN** the score is 175 on a level with star thresholds 100, 200 and 350
- **THEN** the score bar is half full, past the first marker and before the second

#### Scenario: Bar stays full past three stars
- **WHEN** the score is 500 on a level with star thresholds 100, 200 and 350
- **THEN** the score bar is completely full

#### Scenario: Restart empties the bar
- **WHEN** the player has a score of 250 and restarts the level
- **THEN** the score bar is empty again

### Requirement: Score bar shows the actual score
The score bar SHALL show the player's current score for this attempt as a number centred on the bar. The number SHALL be the actual score, also when it is above `stars.three`. The score SHALL be 0 when a level starts or restarts. When the score changes, the number SHALL show the new score at once. Scores of 1,000 and more SHALL use the device's digit grouping. The number SHALL stay legible over both the filled and the empty part of the bar.

#### Scenario: Score at the start of a level
- **WHEN** a level starts
- **THEN** the score bar shows the number 0

#### Scenario: Score is updated
- **WHEN** the score changes from 40 to 50
- **THEN** the score bar shows 50

#### Scenario: Score above the maximum
- **WHEN** the score is 1,250 on a level with star thresholds 100, 200 and 350
- **THEN** the score bar is full and shows "1,250" in an English locale

### Requirement: Score bar fills smoothly
When the score changes, the score bar's fill SHALL grow from its old length to its new length over about 600 ms with an ease-out curve, instead of jumping. When the score changes again while the fill is still moving, the fill SHALL continue from where it is to the new length. When a level starts or restarts, the fill SHALL be empty at once, without animating. When the system "remove animations" setting is on, the fill SHALL change over 160 ms ease-out.

#### Scenario: Points are gained
- **WHEN** the score goes from 50 to 100 on a level with a `stars.three` of 350
- **THEN** the fill grows from 1/7 to 2/7 of the bar over about 600 ms while the number shows 100 at once

#### Scenario: Restart does not animate
- **WHEN** the player restarts the level with a partly filled score bar
- **THEN** the score bar is empty at once

### Requirement: Score bar sparkles at each star threshold
When the fill of the score bar reaches the position of `stars.one`, `stars.two` or `stars.three`, the bar SHALL sparkle briefly at that position, in brass, for about 600 ms. The sparkle SHALL start when the moving fill reaches that position, not when the score changes. Each threshold SHALL sparkle once per attempt; when one score change passes several thresholds, each of them SHALL sparkle as the fill passes it. The three-star sparkle SHALL be at the end of the bar. When the system "remove animations" setting is on, there SHALL be no sparkle.

#### Scenario: First star reached
- **WHEN** the score goes from 90 to 100 on a level with star thresholds 100, 200 and 350
- **THEN** the score bar sparkles at the one-star marker as the fill reaches it

#### Scenario: Two thresholds in one change
- **WHEN** the score goes from 190 to 360 on a level with star thresholds 100, 200 and 350
- **THEN** the bar sparkles at the two-star marker and then at the end of the bar as the fill passes them

#### Scenario: Points beyond three stars
- **WHEN** the score goes from 360 to 400 on a level with a `stars.three` of 350
- **THEN** the bar does not sparkle

#### Scenario: Reduced motion
- **WHEN** the system "remove animations" setting is on and the score crosses `stars.one`
- **THEN** the fill passes the one-star marker without a sparkle

## MODIFIED Requirements

### Requirement: HUD item order and alignment
The HUD SHALL show five items. The current level SHALL be at the far left. The current flow speed SHALL be directly to the right of the level. The cog wheel button SHALL be at the far right. The score bar SHALL be directly to the left of the cog wheel. The countdown bar SHALL be in the space between the flow speed and the score bar. The items SHALL keep their places for the whole attempt, also when the score grows or the countdown bar disappears.

#### Scenario: Items are in the right place
- **WHEN** the HUD is shown while the flow is waiting to start
- **THEN** from left to right it shows the level, the flow speed, the countdown bar, the score bar and the cog wheel, with the level against the left edge and the cog wheel against the right edge

#### Scenario: Score grows to the left
- **WHEN** the score goes from 90 to 1,250
- **THEN** the score bar keeps its position and width, and the number grows from its centre while staying centred on the bar; nothing to the left of the score bar moves

### Requirement: HUD shows the current flow speed
The HUD SHALL show the current flow speed as a whole number next to the flow speed icon: the speed in cells per second times 10, rounded to the nearest whole number with halves rounded up, and without a unit. This is for display only. When a level starts, the speed SHALL be the level's `flow.cellsPerSecond`. When the speed changes during play, the HUD SHALL show the new number.

#### Scenario: Speed at the start of a level
- **WHEN** a level with a `flow.cellsPerSecond` of 0.2 starts
- **THEN** the HUD shows the flow speed as 2

#### Scenario: Speed changes during play
- **WHEN** the current flow speed changes from 0.8 to 0.9 cells per second
- **THEN** the HUD shows 9

#### Scenario: Speed is rounded
- **WHEN** the current flow speed is 1.25 cells per second
- **THEN** the HUD shows 13

#### Scenario: Small speed change
- **WHEN** the current flow speed changes from 0.80 to 0.82 cells per second
- **THEN** the HUD keeps showing 8

### Requirement: HUD items are accessible
The level, flow speed, countdown bar and score bar SHALL each have a content description that names the value, such as "Level 7", "Flow speed 8", "Flow starts in 7 seconds" and "Score 150, 1 of 3 stars". The countdown SHALL be read in whole seconds, rounded up. The score bar SHALL be one item for a screen reader, including its markers. Once the countdown bar has disappeared, it SHALL NOT be read. The cog wheel SHALL have the content description "Pause menu".

#### Scenario: Screen reader reads the HUD
- **WHEN** a screen reader moves through the HUD of level 7 with a speed of 0.8, 6,200 ms left on the countdown, a score of 150 and star thresholds 100, 200 and 350
- **THEN** it reads "Level 7", "Flow speed 8", "Flow starts in 7 seconds", "Score 150, 1 of 3 stars" and "Pause menu"

#### Scenario: Screen reader after the flow started
- **WHEN** a screen reader moves through the HUD after the liquid has started
- **THEN** it reads the level, the flow speed, the score bar and the pause menu, and no countdown

## REMOVED Requirements

### Requirement: HUD shows the current score
**Reason**: The score is now shown on the score bar.
**Migration**: See "Score bar shows the actual score", which keeps the same rules for the start, updates and digit grouping.

### Requirement: HUD shows the stars earned so far
**Reason**: The three separate HUD stars are merged into the score bar (issue #38).
**Migration**: The one-star and two-star thresholds are marker lines on the score bar and the three-star threshold is its end; the number of stars earned is still read by screen readers as part of the score bar. The level complete dialog keeps its stars.

### Requirement: Earned star pops in
**Reason**: There are no separate HUD stars any more to pop in.
**Migration**: Reaching a star threshold is shown by the score bar's sparkle ("Score bar sparkles at each star threshold").
