## ADDED Requirements

### Requirement: Submitting a daily pipe score requires an authenticated player
The system SHALL accept daily pipe score submissions at `POST /api/daily-pipes/{date}/scores` only from a request with a valid access token for a known player. Any other request SHALL be rejected with `401 Unauthorized` and SHALL NOT store anything.

#### Scenario: No access token
- **WHEN** a client posts a daily pipe score without an access token
- **THEN** the system responds with `401 Unauthorized` and nothing is stored

### Requirement: Only the current daily pipe accepts scores
The system SHALL accept a submission only when `{date}` is the date of the currently published daily pipe. A submission for any other date SHALL be rejected with `409 Conflict` and SHALL NOT store anything.

#### Scenario: Submission after the day ended
- **WHEN** the pipe for 2026-11-02 is current and a player submits a score for 2026-11-01
- **THEN** the system responds with `409 Conflict` and nothing is stored

### Requirement: Daily pipe submission input is validated
The system SHALL accept a submission with a score of at least 0 and at most the maximum score possible on the level, and stars from 0 to 3. Any other submission SHALL be rejected with `400 Bad Request` and validation problem details naming the invalid fields.

#### Scenario: Stars out of range
- **WHEN** an authenticated player submits score 300 with 4 stars for the current pipe
- **THEN** the system responds with `400 Bad Request` naming the stars field

#### Scenario: Negative score
- **WHEN** an authenticated player submits score -5 with 1 star for the current pipe
- **THEN** the system responds with `400 Bad Request` naming the score field

### Requirement: One best score per player per daily pipe
The system SHALL keep at most one score per player per daily pipe. The first submission SHALL be stored; a later submission SHALL replace it only when its score is higher. The response SHALL contain the player's best score, rank and streak.

#### Scenario: Higher score replaces
- **WHEN** a player with a best of 300 on today's pipe submits 420
- **THEN** the stored best score becomes 420

#### Scenario: Lower score is ignored
- **WHEN** a player with a best of 420 on today's pipe submits 300
- **THEN** the stored best score stays 420

### Requirement: Top scores of a daily pipe
The system SHALL return, at `GET /api/daily-pipes/{date}/scores/top`, the 10 best scores of the daily pipe of that date with each player's display name, ranked by score descending and then by earliest achievement, together with the caller's own rank and score when the caller has one. The endpoint SHALL require an authenticated player.

#### Scenario: Ranking with a tie
- **WHEN** two players both scored 400 on today's pipe and the first achieved it earlier
- **THEN** the player who achieved 400 first is ranked above the other

#### Scenario: Unknown date
- **WHEN** a player requests the top scores for a date without a published pipe
- **THEN** the system responds with `404 Not Found`

### Requirement: Older highscores are removed when a new pipe is published
When a new daily pipe is published, the system SHALL delete the scores of every daily pipe with an earlier date, in the same transaction as the publication.

#### Scenario: New day clears yesterday's highscores
- **WHEN** the pipe for 2026-11-02 is published while scores exist for 2026-11-01
- **THEN** no scores for 2026-11-01 remain

#### Scenario: Streaks survive the cleanup
- **WHEN** the scores for 2026-11-01 are deleted
- **THEN** every player's streak is unchanged
