# observability Specification

## Purpose

Defines the traces, metrics and logs the score server emits for its operations, the privacy rules that telemetry follows, and where it is exported, so operators can follow a request end to end and tie a regression to a deployment.

## Requirements

### Requirement: Every operation is traced
The system SHALL create a named span for each of these operations, nested under the incoming request's span: `players.sign_in`, `players.resolve_user`, `players.get_display_names`, `scores.submit_score`, `scores.get_level_top_scores` and `scores.get_level_scores_around_player`. Each span SHALL carry low-cardinality domain tags describing its result.

#### Scenario: Leaderboard request shows the full call chain
- **WHEN** an authenticated player requests the top scores of a level
- **THEN** the trace contains the HTTP request span, a `players.resolve_user` span, a `scores.get_level_top_scores` span, and the database spans nested under them

#### Scenario: Around-player span describes its result
- **WHEN** a player requests the scores around their own score on a level where they have no score
- **THEN** the `scores.get_level_scores_around_player` span records that the player has no score on the level and that zero entries were returned

#### Scenario: User resolution records the cache outcome
- **WHEN** a token's subject is resolved to an internal player id that was already cached
- **THEN** the `players.resolve_user` span is tagged with the cache-hit outcome

### Requirement: Span status reflects the outcome
Every operation span SHALL have status Ok when the operation succeeds. When the operation fails, the span SHALL have status Error and record the exception. A cancelled operation SHALL NOT be marked as an error.

#### Scenario: Failed operation
- **WHEN** the database throws while the top scores of a level are read
- **THEN** the `scores.get_level_top_scores` span has status Error and contains an exception event

#### Scenario: Cancelled operation
- **WHEN** the client disconnects and the operation is cancelled
- **THEN** the operation span does not have status Error

### Requirement: Every operation counts its outcomes
The system SHALL count every attempt of each operation, tagged with its outcome: sign-ins (`leakout.players.sign_ins`), user resolutions (`leakout.players.user_resolutions`: `cache_hit`, `cache_miss`, `unknown`, `error`), score submissions (`leakout.scores.submissions`) and leaderboard requests (`leakout.scores.leaderboard.requests`, also tagged `leaderboard.kind` = `top` or `around_player`).

#### Scenario: Unknown subject
- **WHEN** a token's subject does not belong to any registered player
- **THEN** `leakout.players.user_resolutions` is incremented with outcome `unknown`

#### Scenario: Leaderboard request counted by kind
- **WHEN** a player requests the scores around their own score
- **THEN** `leakout.scores.leaderboard.requests` is incremented with `leaderboard.kind` = `around_player` and outcome `success`

### Requirement: Operation durations are measured
The system SHALL record the duration in seconds of every sign-in (`leakout.players.sign_in.duration`), score submission (`leakout.scores.submission.duration`) and leaderboard request (`leakout.scores.leaderboard.duration`, tagged with `leaderboard.kind`), each tagged with its outcome, for both successful and failed attempts.

#### Scenario: Failed submission is still timed
- **WHEN** storing a submitted score fails
- **THEN** `leakout.scores.submission.duration` records one measurement with outcome `error`

### Requirement: Metric attributes stay low-cardinality
Metric attributes SHALL only take values from small fixed sets, such as outcomes and leaderboard kinds. They SHALL NOT contain player ids, levels, scores, display names or any other per-request value.

#### Scenario: Submission metric carries no level or score
- **WHEN** a player submits score 540 on level 3
- **THEN** no attribute of any metric recorded for that submission contains the player id, `3` as a level or `540` as a score

### Requirement: Telemetry protects player identity
No span attribute, metric attribute or log record SHALL contain the Google Play Games player id, a server auth code, an access token or a player's display name. The internal player id MAY appear in span attributes and logs.

#### Scenario: Sign-in telemetry
- **WHEN** a player signs in
- **THEN** the spans, metrics and logs of that request contain the internal player id at most, and neither the Google Play Games player id, the auth code, the issued token nor the display name

### Requirement: Meaningful outcomes are logged
The system SHALL write a structured log record when a new player registers, when Google rejects a sign-in, when a submission becomes a player's new best score, and when an operation fails unexpectedly. Each log record SHALL be correlated with the active trace.

#### Scenario: New best score logged
- **WHEN** a submission becomes the player's new best score on a level
- **THEN** a log record states the internal player id, the level and the new best score, and carries the trace id of the request

#### Scenario: Unexpected failure logged
- **WHEN** an operation fails with an exception that is not a cancellation or an expected rejection
- **THEN** an error log record with the exception is written in the context of the request's trace

### Requirement: Unhandled exceptions are recorded on the request span
The incoming HTTP request span SHALL record an exception event when the request fails with an unhandled exception.

#### Scenario: Unhandled exception
- **WHEN** a request fails with an unhandled exception
- **THEN** the request span contains an exception event with the exception type

### Requirement: Health probes are not traced
Requests to `/alive` and `/health` SHALL NOT produce request spans, and the database calls of the health checks SHALL NOT produce spans either, so platform probes neither clutter traces nor consume telemetry ingestion. The endpoints SHALL keep answering in every environment.

#### Scenario: Liveness probe
- **WHEN** the platform calls `/alive`
- **THEN** no request span is recorded for it, locally or in Application Insights

#### Scenario: Readiness probe checks the databases
- **WHEN** the platform calls `/health` and the health checks query both databases
- **THEN** the endpoint answers with the health status, and neither a request span nor a database span is recorded for it

#### Scenario: Database calls of a request are kept
- **WHEN** a leaderboard request queries the database
- **THEN** its database spans are recorded under the request's trace

### Requirement: Telemetry identifies the service and its version
All telemetry SHALL carry the service name `hexmaster-leakout-api` and, when deployed, the service version of the deployed release.

#### Scenario: Deployed version visible
- **WHEN** version 1.4.0 is deployed and serves a request
- **THEN** the request's telemetry in Application Insights is attributed to role `hexmaster-leakout-api` with version `1.4.0`

### Requirement: Telemetry is exported locally and in Azure
When running under Aspire, the system SHALL export traces, metrics and logs to the Aspire dashboard. When deployed to Azure, it SHALL export requests, dependencies, custom spans, custom metrics and logs to Application Insights.

#### Scenario: Local run
- **WHEN** the API runs through the Aspire AppHost and serves a request
- **THEN** its trace, metrics and logs appear in the Aspire dashboard

#### Scenario: Azure deployment
- **WHEN** the deployed API serves a leaderboard request
- **THEN** the request, its database dependencies, the custom spans, the custom metrics and the logs appear in Application Insights
