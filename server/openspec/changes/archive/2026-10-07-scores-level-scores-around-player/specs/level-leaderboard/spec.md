# Spec Delta

## Purpose

Lets players see how they rank on a level: the best scores of the level and the scores around their own position, ranked by one shared rule.

## ADDED Requirements

### Requirement: Leaderboard window around the calling player
The system SHALL expose `GET /api/levels/{level}/scores/around-me` to authenticated players. It SHALL return the level number and up to 10 leaderboard entries around the caller's best score on that level: the 5 entries ranked directly above the caller, the caller, and the 4 entries ranked directly below. Each entry SHALL hold its position, the player's display name, the score, and whether it is the caller.

#### Scenario: Player in the middle of the leaderboard
- **WHEN** a level has 200 scores and the caller is ranked at position 128
- **THEN** the response holds 10 entries with positions 123 to 132 in ranked order, and only the entry at position 128 is marked as the caller

#### Scenario: Unauthenticated request
- **WHEN** the request has no valid session token
- **THEN** the system responds with `401 Unauthorized`

### Requirement: The window shifts at the edges of the leaderboard
When fewer than 5 entries are ranked above the caller, the system SHALL extend the window downwards; when fewer than 4 entries are ranked below the caller, it SHALL extend the window upwards. The window SHALL hold 10 entries, or every entry of the level when the level has fewer than 10 scores.

#### Scenario: Player near the top
- **WHEN** a level has 50 scores and the caller is ranked at position 3
- **THEN** the response holds the entries at positions 1 to 10, and the entry at position 3 is marked as the caller

#### Scenario: Player near the bottom
- **WHEN** a level has 50 scores and the caller is ranked at position 49
- **THEN** the response holds the entries at positions 41 to 50, and the entry at position 49 is marked as the caller

#### Scenario: Level with fewer than 10 scores
- **WHEN** a level has 6 scores and the caller is ranked at position 4
- **THEN** the response holds all 6 entries at positions 1 to 6

### Requirement: Window uses the leaderboard ranking rule
The window SHALL use the same ranking and positions as the level top 10: higher scores rank first, equal scores rank by the earliest time the score was achieved, and remaining ties by player identifier. Positions SHALL be 1-based and unique. A player without a display name SHALL be shown as `Unknown player`.

#### Scenario: Equal scores around the caller
- **WHEN** the caller and another player both scored 526 on a level and the other player achieved it earlier
- **THEN** the other player is ranked directly above the caller with the position one lower than the caller's

### Requirement: Caller without a score on the level
When the caller has no score on the requested level, the system SHALL respond with `200 OK`, the level number, and an empty list of entries.

#### Scenario: No score yet
- **WHEN** the caller has never submitted a score for level 7 and requests the window for level 7
- **THEN** the system responds with `200 OK` and an empty list of entries

### Requirement: Level number is validated
The system SHALL reject a level number below 1 with `400 Bad Request` and a validation problem response, without querying scores.

#### Scenario: Invalid level
- **WHEN** the caller requests the window for level 0
- **THEN** the system responds with `400 Bad Request`
