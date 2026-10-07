# Spec Delta

## Purpose

Lets the Leak Out game submit a level score for the signed-in player, and keeps exactly one best score per player per level for the leaderboards.

## ADDED Requirements

### Requirement: Submitting a score requires an authenticated player
The system SHALL accept score submissions only from a request that carries a valid access token for a known player. Any other request SHALL be rejected with `401 Unauthorized` and SHALL NOT store anything.

#### Scenario: No access token
- **WHEN** a client posts a score without an access token
- **THEN** the system responds with `401 Unauthorized` and no score is stored

#### Scenario: Token for an unknown player
- **WHEN** a client posts a score with a valid access token whose player is not registered
- **THEN** the system responds with `401 Unauthorized` and no score is stored

### Requirement: Score submission input is validated
The system SHALL accept a submission that contains a level number of at least 1 and a score of at least 0. Any other submission SHALL be rejected with `400 Bad Request` and validation problem details naming the invalid fields, and SHALL NOT change any stored score.

#### Scenario: Level below 1
- **WHEN** an authenticated player submits level 0 with score 100
- **THEN** the system responds with `400 Bad Request` naming the level field and no score is stored

#### Scenario: Negative score
- **WHEN** an authenticated player submits level 3 with score -5
- **THEN** the system responds with `400 Bad Request` naming the score field and no score is stored

#### Scenario: Zero score is valid
- **WHEN** an authenticated player submits level 3 with score 0 and has no score on level 3
- **THEN** the system stores 0 as the player's best score on level 3

### Requirement: The first score on a level is stored
When the authenticated player has no stored score on the submitted level, the system SHALL store the submitted score as the player's best score on that level, together with the moment it was achieved.

#### Scenario: First submission for a level
- **WHEN** an authenticated player without a score on level 4 submits level 4 with score 520
- **THEN** the system stores 520 as the player's best score on level 4
- **AND** responds with `200 OK`, level 4, best score 520 and new best `true`

### Requirement: Only a higher score replaces the best score
When the authenticated player already has a score on the submitted level, the system SHALL replace it only when the submitted score is strictly higher, and then record the new moment it was achieved. An equal or lower score SHALL leave the stored score and its moment unchanged.

#### Scenario: Higher score replaces the best score
- **WHEN** a player whose best score on level 4 is 520 submits level 4 with score 540
- **THEN** the player's best score on level 4 becomes 540
- **AND** the system responds with `200 OK`, best score 540 and new best `true`

#### Scenario: Equal score is ignored
- **WHEN** a player whose best score on level 4 is 520 submits level 4 with score 520
- **THEN** the stored score and the moment it was achieved stay unchanged
- **AND** the system responds with `200 OK`, best score 520 and new best `false`

#### Scenario: Lower score is ignored
- **WHEN** a player whose best score on level 4 is 520 submits level 4 with score 300
- **THEN** the stored score stays 520
- **AND** the system responds with `200 OK`, best score 520 and new best `false`

### Requirement: One best score per player per level
The system SHALL keep at most one stored score per player per level, also when several submissions for the same player and level arrive at the same time. Scores of different levels and of different players SHALL NOT affect each other.

#### Scenario: Concurrent submissions
- **WHEN** a player without a score on level 2 submits scores 100 and 200 for level 2 at the same time
- **THEN** exactly one score is stored for that player on level 2 and its value is 200

#### Scenario: Levels are independent
- **WHEN** a player whose best score on level 1 is 900 submits level 2 with score 100
- **THEN** the best score on level 1 stays 900 and the best score on level 2 becomes 100
