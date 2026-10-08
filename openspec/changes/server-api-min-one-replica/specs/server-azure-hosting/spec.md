# Spec Delta

## REMOVED Requirements

### Requirement: The API scales with HTTP traffic and down to zero
**Reason**: Scaling to zero makes the first request after a quiet period wait for a cold start of about 15 seconds, and the app's requests time out (#131).
**Migration**: Replaced by "The API scales with HTTP traffic and keeps a warm replica". Scaling to zero is still possible by setting the minimum to 0.

## ADDED Requirements

### Requirement: The API scales with HTTP traffic and keeps a warm replica
The container app SHALL scale its replica count on concurrent HTTP requests between a configurable minimum and a configurable maximum, and SHALL accept external HTTPS traffic only. The minimum SHALL default to 1 replica and the maximum to 3, so a request does not wait for a cold start.

#### Scenario: Idle app keeps one replica
- **WHEN** the API receives no requests for the scale-down period
- **THEN** 1 replica keeps running

#### Scenario: First request after a quiet period
- **WHEN** a request arrives after a period without traffic
- **THEN** a running replica answers it without a cold start

#### Scenario: Load scales out
- **WHEN** concurrent requests per replica exceed the configured threshold
- **THEN** replicas are added up to the configured maximum

#### Scenario: Minimum set back to zero
- **WHEN** the deployment sets the minimum to 0
- **THEN** the app scales to 0 replicas when idle, and a request arriving while 0 replicas run starts a replica and is answered
