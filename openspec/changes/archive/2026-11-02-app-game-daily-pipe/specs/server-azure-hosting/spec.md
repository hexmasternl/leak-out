## ADDED Requirements

### Requirement: The daily pipe generator runs as a scheduled job
The infrastructure SHALL define an Azure Container Apps job in the existing Container Apps environment that runs the daily pipe generator on the schedule `0 0 * * *` (00:00 UTC), with the same database connection, secrets injection and telemetry as the API, a retry limit and a timeout. The job SHALL be deployed from the same container registry and by the same server deployment workflow as the API.

#### Scenario: Job deployed
- **WHEN** the server infrastructure is deployed
- **THEN** a Container Apps job for the daily pipe generator exists with a schedule trigger of `0 0 * * *`

#### Scenario: Job failure is retried and visible
- **WHEN** a run of the generator fails
- **THEN** Container Apps retries it up to the retry limit and the failure is visible in Application Insights
