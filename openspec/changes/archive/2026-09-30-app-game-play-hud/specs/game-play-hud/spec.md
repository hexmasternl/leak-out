## ADDED Requirements

### Requirement: HUD bar at the top of the game screen
The game screen SHALL show a HUD bar along the top edge, across the full width of the screen, in landscape. The stack column and the board SHALL be placed below the HUD and SHALL NOT be covered by it. The HUD SHALL have a fixed height, so that the space for the stack and the board does not change while playing.

#### Scenario: HUD is shown when a level starts
- **WHEN** the player starts a level
- **THEN** the HUD bar is shown at the top of the game screen, and the stack column and the board are shown below it

#### Scenario: Largest stack still fits below the HUD
- **WHEN** the game screen is shown on a 360 dp tall landscape screen for a level with a `stack.visibleSize` of 10
- **THEN** the HUD and all 10 stack sections are fully visible, and nothing scrolls or overlaps

### Requirement: HUD item order and alignment
The HUD SHALL show five items. The current level SHALL be at the far left. The current flow speed SHALL be directly to the right of the level. The cog wheel button SHALL be at the far right. The current score SHALL be right-aligned, directly to the left of the cog wheel. The three stars SHALL be directly to the left of the score. The space between the flow speed and the stars SHALL stay empty.

#### Scenario: Items are in the right place
- **WHEN** the HUD is shown
- **THEN** from left to right it shows the level, the flow speed, an empty gap, the stars, the score and the cog wheel, with the level against the left edge and the cog wheel against the right edge

#### Scenario: Score grows to the left
- **WHEN** the score goes from 90 to 1,250
- **THEN** the right edge of the score stays next to the cog wheel, the text grows towards the left and the stars move left with it

### Requirement: HUD shows the current level
The HUD SHALL show the number of the level that is being played, as "Level" followed by the level's `id`.

#### Scenario: Level number is shown
- **WHEN** the player plays level 7
- **THEN** the HUD shows "Level 7"

### Requirement: HUD shows the current flow speed
The HUD SHALL show the current flow speed in cells per second, with one decimal. When a level starts, the speed SHALL be the level's `flow.cellsPerSecond`. When the speed changes during play, the HUD SHALL show the new speed.

#### Scenario: Speed at the start of a level
- **WHEN** a level with a `flow.cellsPerSecond` of 0.8 starts
- **THEN** the HUD shows the flow speed as 0.8 cells per second

#### Scenario: Speed changes during play
- **WHEN** the current flow speed changes from 0.8 to 0.9 cells per second
- **THEN** the HUD shows 0.9 cells per second

### Requirement: HUD shows the current score
The HUD SHALL show the player's current score for this attempt. The score SHALL be 0 when a level starts or restarts. When the score changes, the HUD SHALL show the new score. Scores of 1,000 and more SHALL use the device's digit grouping.

#### Scenario: Score at the start of a level
- **WHEN** a level starts
- **THEN** the HUD shows a score of 0

#### Scenario: Score is updated
- **WHEN** the score changes from 40 to 50
- **THEN** the HUD shows 50

### Requirement: HUD shows the stars earned so far
The HUD SHALL show three stars for the level being played. The number of earned stars SHALL be the number of the level's star thresholds (`stars.one`, `stars.two`, `stars.three`) that the current score has reached: 0 while the score is below `stars.one`, and 3 once it is at or above `stars.three`. The stars SHALL be earned from left to right: the first star first, then the second, then the third. A star that is not earned SHALL be drawn in the design system's muted grey (`text.muted`) without a glow. An earned star SHALL be drawn filled in brass (`brass.300`) with a brass glow, as the design system draws an active star. When the score changes, the stars SHALL show the new count at the same moment as the new score. When a level starts or restarts, no star SHALL be earned.

#### Scenario: Three grey stars at the start
- **WHEN** a level with star thresholds 100, 200 and 350 starts
- **THEN** the HUD shows three grey stars next to the score of 0

#### Scenario: First star is earned
- **WHEN** the score goes from 90 to 100 on a level with star thresholds 100, 200 and 350
- **THEN** the first (leftmost) star turns glowing brass and the other two stay grey

#### Scenario: Second and third star are earned
- **WHEN** the score reaches 200 and later 350 on a level with star thresholds 100, 200 and 350
- **THEN** the first two stars are glowing brass at 200, and all three are glowing brass at 350

#### Scenario: Score between two thresholds
- **WHEN** the score is 199 on a level with star thresholds 100, 200 and 350
- **THEN** only the first star is glowing brass

#### Scenario: Restart clears the stars
- **WHEN** the player has earned two stars and restarts the level
- **THEN** all three stars are grey again

### Requirement: Earned star pops in
When a star becomes earned, it SHALL pop in: it SHALL scale up and settle to its normal size with the design system's `motion.pop` spring, and its glow SHALL appear with it. The glow SHALL be static, not pulsing. When the system "remove animations" setting is on, the star SHALL change to earned without the pop.

#### Scenario: Star pops when earned
- **WHEN** the score crosses `stars.one`
- **THEN** the first star scales up and settles over about 320 ms and ends filled in brass with a static glow

#### Scenario: Reduced motion
- **WHEN** the system "remove animations" setting is on and the score crosses `stars.one`
- **THEN** the first star is shown filled in brass with a static glow at once, without scaling

### Requirement: Cog wheel opens the pause menu
The HUD SHALL show a cog wheel button at the far right. Tapping it SHALL pause the game and open the pause dialog. The button SHALL have a touch target of at least 48 dp by 48 dp.

#### Scenario: Tapping the cog wheel
- **WHEN** the player taps the cog wheel during a level
- **THEN** the game pauses and the pause dialog opens

### Requirement: HUD items are accessible
The level, flow speed, stars and score SHALL each have a content description that names the value, such as "Level 7", "Flow speed 0.8 cells per second", "1 of 3 stars" and "Score 150". The three stars SHALL be one item for a screen reader, not three. The cog wheel SHALL have the content description "Pause menu".

#### Scenario: Screen reader reads the HUD
- **WHEN** a screen reader moves through the HUD of level 7 with a speed of 0.8, a score of 150 and star thresholds 100, 200 and 350
- **THEN** it reads "Level 7", "Flow speed 0.8 cells per second", "1 of 3 stars", "Score 150" and "Pause menu"
