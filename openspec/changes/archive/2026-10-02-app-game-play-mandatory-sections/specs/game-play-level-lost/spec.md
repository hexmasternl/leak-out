# Spec Delta

## ADDED Requirements

### Requirement: Reaching the finish with an unused mandatory section loses the level
When the liquid leaves a section into the finish tile through the finish's open side while at least one mandatory section of the level is not used, the finish SHALL refuse the liquid and the system SHALL mark the current attempt as **lost** because a mandatory section was missed. The points for the section the liquid just left SHALL be awarded before the level is lost. The game SHALL stop exactly as for a leak.

#### Scenario: Mandatory reservoir skipped
- **WHEN** the liquid enters the finish through its open side and has never run through the level's mandatory reservoir
- **THEN** the level is lost because a mandatory section was missed, and it is not won

#### Scenario: Mandatory cross used on one channel
- **WHEN** the liquid enters the finish through its open side after passing the level's mandatory cross from left to right only
- **THEN** the level is lost because a mandatory section was missed

#### Scenario: Points before the loss
- **WHEN** the liquid leaves a corner section into the finish with a score of 40 and a mandatory section unused
- **THEN** the score is 55 and the level is lost

### Requirement: Missed mandatory sections are shown on the field
When the level is lost because a mandatory section was missed, the playing field SHALL show liquid spurting out of the finish tile on its open side for 1 second, and SHALL make every unused mandatory section pulse in the `leak` colour during that second. The effect SHALL move with the tiles when the field is panned or zoomed.

#### Scenario: Missed section effect
- **WHEN** the level is lost because the mandatory cross at x 3, y 2 was not used
- **THEN** liquid spurts out of the finish's open side and the cross at x 3, y 2 pulses red for 1 second

## MODIFIED Requirements

### Requirement: Level lost dialog appears after the leak
One second after the level is lost, the system SHALL show the level lost dialog: a modal dialog above the dimmed game screen. The dialog SHALL show a title that clearly says the level is lost ("Leaked!"), a short message, the score of the attempt, and two buttons: **Restart** and **Exit**. The message SHALL be "The pipe is leaking. Level lost." after a leak, and "The liquid missed a mandatory section. Level lost." when a mandatory section was missed. The dialog SHALL NOT show a Resume button.

#### Scenario: Dialog content
- **WHEN** the level is lost by a leak with a score of 85 and 1 second has passed
- **THEN** the level lost dialog shows "Leaked!", the message "The pipe is leaking. Level lost.", a score of 85, and the buttons Restart and Exit

#### Scenario: Dialog after a missed mandatory section
- **WHEN** the level is lost because a mandatory section was missed and 1 second has passed
- **THEN** the level lost dialog shows "Leaked!" and the message "The liquid missed a mandatory section. Level lost."

#### Scenario: Dialog waits for the leak effect
- **WHEN** the level has been lost for less than 1 second
- **THEN** the level lost dialog is not shown yet
