# Spec Delta

## MODIFIED Requirements

### Requirement: Anonymous top 25 of a level
The score server SHALL serve the public top scores of a level without authentication. The response SHALL contain the level and at most 25 entries in the level ranking order, each with its position, the player's display name, the score and the moment the server received that score, and SHALL NOT contain player ids or any other player data.

#### Scenario: Request without a token
- **WHEN** a client without an access token requests the public top scores of level 3, which has 40 scores
- **THEN** the response is 200 OK with level 3 and the entries with positions 1 to 25

#### Scenario: Level without scores
- **WHEN** a client requests the public top scores of level 7, which has no scores
- **THEN** the response is 200 OK with level 7 and an empty list of entries

#### Scenario: Invalid level
- **WHEN** a client requests the public top scores of level 0
- **THEN** the response is 400 Bad Request with a validation error for the level

#### Scenario: Entry carries the moment the score was received
- **WHEN** the server received a player's best score of 540 on level 3 at 2026-10-01 14:30 UTC and a client requests the public top scores of level 3
- **THEN** that player's entry has score 540 and the moment `2026-10-01T14:30:00+00:00`

#### Scenario: Lower score does not change the moment
- **WHEN** that player later submits 300 on level 3 and a client requests the public top scores of level 3 after the cache has expired
- **THEN** that player's entry still has score 540 and the moment `2026-10-01T14:30:00+00:00`
