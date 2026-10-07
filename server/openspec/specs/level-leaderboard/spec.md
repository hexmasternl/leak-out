# level-leaderboard Specification

## Purpose

Ranks the best scores of all players on a level and serves the leaderboard views built on that ranking, so players can compare their result with others.

## Requirements

### Requirement: Level ranking order
The system SHALL rank the best scores on a level by score, highest first. Equal scores SHALL be ordered by the moment the score was achieved, earliest first, and remaining ties by player in a stable order. Each ranked entry SHALL have a 1-based position equal to its place in this order; two entries SHALL NOT share a position.

#### Scenario: Higher score ranks first
- **WHEN** player A has 540 and player B has 600 on level 3
- **THEN** player B has position 1 and player A has position 2

#### Scenario: Equal scores ranked by earliest achievement
- **WHEN** player A achieved 500 on level 3 yesterday and player B achieved 500 on level 3 today
- **THEN** player A has position 1 and player B has position 2

#### Scenario: Positions are unique and consecutive
- **WHEN** three players have the same score on a level
- **THEN** they get positions 1, 2 and 3

### Requirement: Top 10 scores of a level
The system SHALL let an authenticated player request the top scores of a level. The response SHALL contain the level and at most 10 entries in ranking order, each with its position, the player's display name, the score, and whether the entry belongs to the requesting player.

#### Scenario: More than 10 scores
- **WHEN** 25 players have a score on level 3 and a player requests the top scores of level 3
- **THEN** the response contains the entries with positions 1 to 10, highest score first

#### Scenario: Fewer than 10 scores
- **WHEN** 4 players have a score on level 3 and a player requests the top scores of level 3
- **THEN** the response contains those 4 entries with positions 1 to 4

#### Scenario: Level without scores
- **WHEN** no player has a score on level 7 and a player requests the top scores of level 7
- **THEN** the request succeeds and the response contains level 7 and an empty list of entries

#### Scenario: Requesting player is in the top 10
- **WHEN** the requesting player has position 2 on level 3
- **THEN** the entry with position 2 is marked as the requesting player's and no other entry is

#### Scenario: Player without a display name
- **WHEN** a ranked player has no known display name
- **THEN** the entry shows the display name "Unknown player"

### Requirement: Top scores require authentication
The system SHALL reject a request for the top scores of a level that does not carry a valid access token with 401 Unauthorized.

#### Scenario: No access token
- **WHEN** a client requests the top scores of level 3 without an access token
- **THEN** the response is 401 Unauthorized

### Requirement: Top scores level validation
The system SHALL reject a request for the top scores of a level lower than 1 with 400 Bad Request and a validation problem that names the level.

#### Scenario: Level zero
- **WHEN** an authenticated player requests the top scores of level 0
- **THEN** the response is 400 Bad Request with a validation error for the level
