# server-public-top-scores Specification

## Purpose
Gives the public website read-only, anonymous access to the best scores of a level, without exposing anything about players beyond their display name.

## Requirements
### Requirement: Anonymous top 25 of a level
The score server SHALL serve the public top scores of a level without authentication. The response SHALL contain the level and at most 25 entries in the level ranking order, each with its position, the player's display name and the score, and SHALL NOT contain player ids or any other player data.

#### Scenario: Request without a token
- **WHEN** a client without an access token requests the public top scores of level 3, which has 40 scores
- **THEN** the response is 200 OK with level 3 and the entries with positions 1 to 25

#### Scenario: Level without scores
- **WHEN** a client requests the public top scores of level 7, which has no scores
- **THEN** the response is 200 OK with level 7 and an empty list of entries

#### Scenario: Invalid level
- **WHEN** a client requests the public top scores of level 0
- **THEN** the response is 400 Bad Request with a validation error for the level

### Requirement: Website origin allowed
The public top scores endpoint SHALL allow cross-origin GET requests from the website's configured origins, which by default is `https://leakout.hexmaster.nl`, and SHALL NOT allow them from other origins.

#### Scenario: Request from the website
- **WHEN** a browser sends a request from `https://leakout.hexmaster.nl`
- **THEN** the response carries a CORS header that allows that origin

#### Scenario: Preflight from the website
- **WHEN** a browser sends a CORS preflight request from `https://leakout.hexmaster.nl` without an access token
- **THEN** the response allows that origin and the GET method

#### Scenario: Request from another site
- **WHEN** a browser sends a request from an origin that is not configured
- **THEN** the response carries no CORS header that allows that origin

### Requirement: Server-side cache of the public top scores
The score server SHALL keep the public top scores of each level in memory for 30 minutes after reading them, and SHALL answer requests for that level from memory during that time. A failed read SHALL NOT be kept. Responses SHALL be cacheable by browsers for a short time.

#### Scenario: Repeated requests
- **WHEN** the public top scores of level 3 are requested twice within 30 minutes
- **THEN** the scores are read from the database only once and both responses are the same

#### Scenario: Cache expired
- **WHEN** the public top scores of level 3 are requested more than 30 minutes after they were read
- **THEN** the scores are read from the database again

#### Scenario: Read failed
- **WHEN** reading the public top scores of level 3 fails and they are requested again
- **THEN** the scores are read from the database again
