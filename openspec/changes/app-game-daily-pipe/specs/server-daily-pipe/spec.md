## ADDED Requirements

### Requirement: A daily pipe is generated every day at midnight UTC
The system SHALL run a scheduled generator every day at 00:00 UTC that creates the daily pipe for that UTC date. The generator SHALL produce a random level in the level-format JSON and a random stack seed.

#### Scenario: Scheduled run publishes today's pipe
- **WHEN** the generator runs at 00:00 UTC on 2026-11-02
- **THEN** a daily pipe for date 2026-11-02 is published with a level and a stack seed

#### Scenario: Two days get different pipes
- **WHEN** the generator has published the pipes for 2026-11-02 and 2026-11-03
- **THEN** the two pipes have different levels or different stack seeds

### Requirement: Only a feasible level is published
The generator SHALL check every candidate level with the level validation rules and the solvability simulation before publishing it. A candidate that is invalid or unsolvable SHALL be discarded and a new candidate generated. When no candidate passes within the attempt limit, the run SHALL fail without publishing anything and SHALL report the failure in telemetry.

#### Scenario: Unsolvable candidate is discarded
- **WHEN** the first candidate has no route from the start to the finish and the second candidate is solvable
- **THEN** the second candidate is published and the first is not

#### Scenario: No feasible candidate
- **WHEN** no candidate passes within the attempt limit
- **THEN** no daily pipe is published for that date, the run ends with a failure and an error is logged

#### Scenario: Published level is accepted by the app
- **WHEN** a level produced by the generator is parsed, validated and solved by the app's `LevelParser`, `LevelValidator` and `LevelSolver`
- **THEN** it parses, passes validation and is solvable

### Requirement: Publication is idempotent per date
The system SHALL publish at most one daily pipe per UTC date. A rerun of the generator for a date that already has a daily pipe SHALL leave that pipe unchanged.

#### Scenario: Job retried after success
- **WHEN** the generator runs a second time on 2026-11-02 after the pipe for that date was published
- **THEN** the published pipe for 2026-11-02 is unchanged

### Requirement: Today's daily pipe can be downloaded
The system SHALL serve today's daily pipe to an authenticated player at `GET /api/daily-pipes/today`, with its UTC date, the level JSON, the stack seed, the player's streak and the player's best score on that pipe (or none). Requests without a valid access token for a known player SHALL be rejected with `401 Unauthorized`.

#### Scenario: Pipe available
- **WHEN** an authenticated player requests today's daily pipe and the pipe for today's UTC date is published
- **THEN** the system responds with `200 OK` containing the date, the level, the stack seed, the streak and the player's best score

#### Scenario: Pipe not yet published
- **WHEN** an authenticated player requests today's daily pipe and no pipe is published for today's UTC date
- **THEN** the system responds with `404 Not Found` and does not return an older pipe

#### Scenario: Not signed in
- **WHEN** a client requests today's daily pipe without an access token
- **THEN** the system responds with `401 Unauthorized`

### Requirement: The day is decided by the server's UTC clock
The system SHALL decide which daily pipe is current from the server's UTC date only. No client-supplied date or time SHALL select or change the current daily pipe.

#### Scenario: Day boundary
- **WHEN** a player requests today's daily pipe at 23:59 UTC and again at 00:01 UTC after the new pipe was published
- **THEN** the first response holds the previous date's pipe and the second response holds the new date's pipe
