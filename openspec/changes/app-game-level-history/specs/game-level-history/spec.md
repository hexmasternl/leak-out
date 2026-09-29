## ADDED Requirements

### Requirement: Finished levels are stored in a local database
The system SHALL keep a local on-device database with at most one history record per level. A record SHALL contain the level id, the personal highscore for that level and the number of stars earned with that highscore. A level SHALL count as finished if and only if it has a record.

#### Scenario: Record survives app restart
- **WHEN** a history record has been stored and the app is restarted
- **THEN** the record is read back with the same level id, highscore and stars

#### Scenario: Unplayed level has no record
- **WHEN** the history is read for a level that was never finished
- **THEN** no record is returned and the level is reported as not finished

#### Scenario: One record per level
- **WHEN** a level is finished several times
- **THEN** the history contains exactly one record for that level

### Requirement: First completion creates a record
The system SHALL create a history record when a level is finished successfully and no record exists for it, storing the score of that game as the highscore and the stars earned with it.

#### Scenario: First win is recorded
- **WHEN** level 3 is finished with a score of 240 and 2 stars and level 3 has no record
- **THEN** a record for level 3 is stored with highscore 240 and 2 stars
- **AND** the recording result is reported as a first completion

### Requirement: Highscore is only replaced by a better score
The system SHALL replace the stored highscore and stars of a level only when a later successful game scores strictly higher than the stored highscore. The highscore and stars SHALL be updated together.

#### Scenario: Better score replaces highscore
- **WHEN** level 3 has highscore 240 with 2 stars and is finished with a score of 380 and 3 stars
- **THEN** the record is updated to highscore 380 and 3 stars
- **AND** the recording result is reported as a new highscore

#### Scenario: Lower score keeps highscore
- **WHEN** level 3 has highscore 380 with 3 stars and is finished with a score of 200 and 1 star
- **THEN** the record still holds highscore 380 and 3 stars
- **AND** the recording result is reported as no improvement

#### Scenario: Equal score keeps record
- **WHEN** level 3 has highscore 240 with 2 stars and is finished with a score of 240
- **THEN** the record is unchanged and the result is reported as no improvement

### Requirement: Failed games are not recorded
The system SHALL NOT create or change any history record when a game ends in a leak.

#### Scenario: Leak on unplayed level
- **WHEN** a game on a level with no record ends in a leak
- **THEN** no record is created for that level

#### Scenario: Leak on finished level
- **WHEN** a game on a level with a record ends in a leak
- **THEN** the existing record is unchanged

### Requirement: Recorded values are validated
The system SHALL reject a recording whose score is negative or whose star count is not between 1 and 3, and SHALL leave the stored history unchanged.

#### Scenario: Negative score
- **WHEN** a result with a score of -5 is recorded
- **THEN** the recording is rejected and no record is created or changed

#### Scenario: Star count out of range
- **WHEN** a result with 0 stars or 4 stars is recorded
- **THEN** the recording is rejected and no record is created or changed

### Requirement: History can be read back
The system SHALL provide the record of a single level and the records of all finished levels, and SHALL notify observers when a record is created or changed.

#### Scenario: Read one level
- **WHEN** the history is read for level 3 that has a record
- **THEN** its level id, highscore and stars are returned

#### Scenario: Read all levels
- **WHEN** levels 1, 2 and 4 are finished and all history is read
- **THEN** exactly the records of levels 1, 2 and 4 are returned

#### Scenario: Observer sees a new highscore
- **WHEN** an observer watches level 3 and a higher score is recorded for it
- **THEN** the observer receives the updated record
