## ADDED Requirements

### Requirement: Earned points are the points collected during play
The final score of a won attempt SHALL start from the **earned points**: the sum of all points awarded while the liquid flowed, as shown in the HUD at the moment of the win.

#### Scenario: Earned points equal the HUD score
- **WHEN** the level is won with the HUD showing a score of 120
- **THEN** the earned points of the final score are 120

### Requirement: Unused sections cost 3 points each
Every placed section that the liquid never entered SHALL count as an **unused section**. The penalty SHALL be 3 points per unused section. Sections the liquid entered, the start tile, the finish tile and blocked tiles SHALL NOT count as unused.

#### Scenario: Four unused sections
- **WHEN** the level is won and four placed sections were never entered by the liquid
- **THEN** the penalty is 12 points

#### Scenario: No unused sections
- **WHEN** the level is won and every placed section was entered by the liquid
- **THEN** the penalty is 0 points

#### Scenario: A cross used once is not unused
- **WHEN** the level is won and the liquid passed a cross through one channel only
- **THEN** that cross does not count as unused

### Requirement: Total is earned points minus the penalty, never below 0
The **total** SHALL be the earned points minus the penalty. When the penalty is larger than the earned points, the total SHALL be 0.

#### Scenario: Total with a penalty
- **WHEN** the earned points are 120 and the penalty is 12
- **THEN** the total is 108

#### Scenario: Penalty larger than the earned points
- **WHEN** the earned points are 10 and the penalty is 15
- **THEN** the total is 0

### Requirement: Stars come from the total
The stars of a won attempt SHALL be the level's star thresholds applied to the **total**, with the same rule the HUD uses during play, giving 0 to 3 stars.

#### Scenario: One star
- **WHEN** the level's thresholds are 70, 110 and 160 and the total is 108
- **THEN** the attempt earns 1 star

#### Scenario: Three stars
- **WHEN** the level's thresholds are 70, 110 and 160 and the total is 160
- **THEN** the attempt earns 3 stars

#### Scenario: No star
- **WHEN** the level's thresholds are 70, 110 and 160 and the total is 0
- **THEN** the attempt earns 0 stars

### Requirement: The score is revealed in order
The level complete dialog SHALL reveal the score in this order, one phase after the other, on the game clock:
1. The earned points SHALL be shown at once when the dialog opens, with the penalty at 0 and the total at 0.
2. After a short hold, the penalty SHALL count up in steps of 3, one step per unused section. With each step one unused section SHALL be removed from the playing field behind the dialog, until every unused section is gone. The penalty phase SHALL take at most 2 seconds. When there are no unused sections the penalty phase SHALL take no time.
3. The total SHALL run from 0 to the final total quickly (within 1 second), ending exactly on the final total.
4. The stars SHALL be revealed (see the next requirement).

#### Scenario: Earned points shown at once
- **WHEN** the level complete dialog opens with 120 earned points and 4 unused sections
- **THEN** the dialog shows 120 earned points, a penalty of 0 and a total of 0, and all 4 unused sections are still on the field

#### Scenario: Penalty and field in step
- **WHEN** the penalty phase has done 2 of 4 steps
- **THEN** the dialog shows a penalty of 6 and 2 of the 4 unused sections have been removed from the field

#### Scenario: Penalty done
- **WHEN** the penalty phase is done with 4 unused sections
- **THEN** the dialog shows a penalty of 12 and no unused section is left on the field, while the sections the liquid entered are still there

#### Scenario: Total counts up
- **WHEN** the total phase runs with a final total of 108
- **THEN** the shown total goes up over time, never goes down and ends exactly at 108

#### Scenario: Nothing to remove
- **WHEN** the dialog opens with no unused sections
- **THEN** the total starts counting up right after the earned points hold, with a penalty of 0

### Requirement: Stars are revealed one by one
When the total has been shown, all three stars SHALL first show as grey. Each earned star SHALL then light up in the earned star colour, one after the other, and SHALL pop to a larger size and settle back to its normal size. Stars that are not earned SHALL stay grey and SHALL NOT animate.

#### Scenario: Stars start grey
- **WHEN** the level complete dialog opens with 2 earned stars
- **THEN** all three stars are grey

#### Scenario: Stars light up in order
- **WHEN** the star phase runs with 2 earned stars
- **THEN** the first star lights up and pops, then the second star lights up and pops, and the third star stays grey

#### Scenario: No stars earned
- **WHEN** the star phase runs with 0 earned stars
- **THEN** all three stars stay grey

### Requirement: Animations run on the game clock and can be skipped
The score and star animations SHALL advance with the game clock only. When the animations are skipped, the dialog SHALL show the final earned points, penalty, total and stars, and every unused section SHALL be removed from the field.

#### Scenario: Clock does not move
- **WHEN** the level complete dialog is shown and no game time passes
- **THEN** the shown penalty, total and stars do not change

#### Scenario: Skipped animation
- **WHEN** the animations are skipped with 120 earned points, 4 unused sections and 1 earned star
- **THEN** the dialog shows 120 earned points, a penalty of 12, a total of 108 and 1 lit star, and no unused section is left on the field

### Requirement: Restart clears the final score
Restarting or leaving the level after a win SHALL discard the final score. The next attempt SHALL start with a score of 0 and no unused sections.

#### Scenario: Restart after a win
- **WHEN** the level was won with a total of 108 and the player restarts
- **THEN** the score is 0 and there is no final score until the level is won again
