## ADDED Requirements

### Requirement: Tips resets are stored in the player stats
The player stats SHALL also hold the number of times the seen tips were reset. Each reset from the settings screen SHALL add one to it, evaluate the achievements and store the result in one transaction. The count SHALL survive app restarts and a progress reset.

#### Scenario: Reset is counted
- **WHEN** the player taps "Show tips again" twice
- **THEN** the stored number of tips resets is 2

#### Scenario: Count survives a progress reset
- **WHEN** the tips were reset once and the player then resets progress
- **THEN** the stored number of tips resets is still 1 and Tips Addict is still unlocked

### Requirement: Upgrade keeps achievement data
The database upgrade that adds the tips-reset count SHALL keep every level history record, every stored achievement and the existing player stats, and SHALL start the tips-reset count at 0.

#### Scenario: Migration with achievements
- **WHEN** a database of the previous version with 3 history records, Zero Waste unlocked and 250 sections placed is upgraded
- **THEN** the records, the unlock and the 250 sections are unchanged and the tips-reset count is 0
