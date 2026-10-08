# Spec Delta

## Purpose

Gives the public website read-only, anonymous access to the best scores of a level, without exposing anything about players beyond their display name.

## ADDED Requirements

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
The public top scores endpoint SHALL allow cross-origin requests from the website's configured origins and SHALL NOT allow them from other origins.

#### Scenario: Request from the website
- **WHEN** a browser sends a request from the website's origin
- **THEN** the response carries a CORS header that allows that origin

#### Scenario: Request from another site
- **WHEN** a browser sends a request from an origin that is not configured
- **THEN** the response carries no CORS header that allows that origin

### Requirement: Public endpoint protection
The public top scores endpoint SHALL be rate limited per client and its responses SHALL be cacheable for a short time.

#### Scenario: Too many requests
- **WHEN** one client exceeds the rate limit
- **THEN** further requests get 429 Too Many Requests until the limit window passes
