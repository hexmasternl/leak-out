# achievement-persistence Specification

## Purpose
TBD - created by archiving change app-game-achievements. Update Purpose after archive.

## Requirements

### Requirement: Achievements are stored on the device
The system SHALL store, in the local database, for every achievement its progress and, once unlocked, the time it was unlocked. It SHALL also store the player stats: the number of sections ever placed, the number of losses, and the current loss streak per level. Stored data SHALL be identified by achievement key, not by position in the catalogue. The data SHALL survive app restarts.

#### Scenario: Unlock survives a restart
- **WHEN** Zero Waste is unlocked and the app is closed and opened again
- **THEN** Zero Waste is still unlocked with the same unlock time

#### Scenario: Stats survive a restart
- **WHEN** the player has placed 250 sections and the app is restarted
- **THEN** the stored number of sections placed is 250

### Requirement: Attempts update achievements once
When an attempt ends in a win or a loss, the system SHALL update the player stats with that attempt, evaluate the achievements, and store the result in one transaction, exactly once per attempt. On a win this SHALL happen after the win is recorded in the level history. When an attempt is restarted or exited before it ends, the system SHALL add its placements to the player stats and evaluate the achievements, exactly once per attempt.

#### Scenario: Win is processed once
- **WHEN** a level is won and the game screen recomposes several times afterwards
- **THEN** the attempt's placements are added to the stats once

#### Scenario: Abandoned attempt counts placements
- **WHEN** the player places 4 sections and taps Restart in the pause dialog
- **THEN** the stored number of sections placed grows by 4

### Requirement: Existing players receive their progress achievements
When the app starts, including the first start after an update that adds levels or achievements, the system SHALL evaluate the progress and lifetime achievements from the stored level history and player stats and store any newly unlocked achievement. A player who finished levels before achievements existed SHALL receive the progress achievements those levels earn.

#### Scenario: Upgrade with history
- **WHEN** the app is updated on a device whose history holds 14 finished levels, and the app starts
- **THEN** First Leak Fixed (if level 1 is finished) and Getting the Hang of It are unlocked, and Pack 1 Complete has progress 14 of 24

### Requirement: Achievements outlast a progress reset
Resetting progress from the settings screen SHALL delete the level history only. Unlocked achievements, achievement progress, player stats and loss streaks SHALL be kept.

#### Scenario: Reset keeps achievements
- **WHEN** Getting the Hang of It is unlocked and the player confirms Reset progress
- **THEN** Getting the Hang of It is still unlocked and the level history is empty

### Requirement: Database upgrade keeps existing data
The database upgrade that adds the achievement tables SHALL keep every existing level history record.

#### Scenario: Migration with history
- **WHEN** a database of the previous version holding 3 history records is upgraded
- **THEN** the 3 records are unchanged and the achievement tables exist and are empty
