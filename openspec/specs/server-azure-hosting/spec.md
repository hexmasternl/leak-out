# server-azure-hosting Specification

## Purpose

Describes how the Leak Out score server is provisioned and run in Azure: reproducible infrastructure as code, image publishing, telemetry, database, configuration and cost-conscious scaling.

## Requirements

### Requirement: Infrastructure is defined as code
All Azure resources the server needs SHALL be defined in Bicep templates in the repository and deployed with a single subscription-scope deployment. That deployment SHALL create the resource group and place every resource inside it. Deploying the same templates and parameters again SHALL not change or duplicate resources.

#### Scenario: Deploy into a subscription without the resource group
- **WHEN** the templates are deployed at subscription scope with valid parameters and the named resource group does not exist
- **THEN** the resource group is created, and the Log Analytics workspace, Application Insights, PostgreSQL server with both databases, Container Apps environment and container app exist inside it

#### Scenario: Resource group already exists
- **WHEN** the templates are deployed at subscription scope and the named resource group already exists
- **THEN** the deployment succeeds and the resources are created or updated inside that resource group

#### Scenario: Redeploy without changes
- **WHEN** the templates are deployed again with the same parameters
- **THEN** the deployment succeeds and no resources are added, removed or recreated

### Requirement: The API runs from the existing container registry
The API SHALL be packageable as a container image without a Dockerfile, and the container app SHALL pull the image given as a deployment parameter from the existing container registry identified by `ACR_LOGIN_SERVER`, authenticating with `ACR_LOGIN_USERNAME` and `ACR_LOGIN_PASSWORD`. The infrastructure SHALL NOT create a container registry.

#### Scenario: Image can be built from the project
- **WHEN** the API project is published as a container
- **THEN** a runnable image of the API is produced that serves `/alive` on port 8080

#### Scenario: Container app pulls from the registry
- **WHEN** the container app is deployed with an image tag present in the registry
- **THEN** it pulls the image from the existing registry using the registry credentials and starts a revision

### Requirement: Telemetry goes to Application Insights and Log Analytics
The container app SHALL send its OpenTelemetry traces, metrics and logs to a workspace-based Application Insights component, and the Container Apps environment SHALL send console and system logs to the Log Analytics workspace.

#### Scenario: Request telemetry is visible
- **WHEN** a request is made to the hosted API
- **THEN** the request appears in Application Insights for the API

#### Scenario: Console logs are visible
- **WHEN** the API writes to the console
- **THEN** the output is queryable in the Log Analytics workspace

### Requirement: PostgreSQL is provisioned at low cost
The server SHALL use an Azure Database for PostgreSQL Flexible Server on the burstable tier with the smallest compute size, without high availability, hosting the `playersdb` and `scoresdb` databases, and requiring TLS for connections.

#### Scenario: Databases exist
- **WHEN** the infrastructure is deployed
- **THEN** the server hosts the databases `playersdb` and `scoresdb`

#### Scenario: Unencrypted connections are refused
- **WHEN** a client connects to the server without TLS
- **THEN** the connection is refused

### Requirement: Configuration and secrets are injected, never committed
The container app SHALL receive its connection strings and application secrets as container app secrets exposed through environment variables, using the same configuration keys the Aspire AppHost provides locally. No secret value SHALL be stored in the repository.

#### Scenario: Connection strings use the Aspire names
- **WHEN** the API starts in the container app
- **THEN** it finds connection strings named `playersdb` and `scoresdb` and connects to the Azure PostgreSQL databases

#### Scenario: Google Play Games sign-in is configured
- **WHEN** the templates are deployed with the secure parameters `googleClientId`, `googleClientSecret` and `authSigningKey`
- **THEN** the container app holds them as the secrets `google-client-id`, `google-client-secret` and `auth-signing-key`, exposes them as `GooglePlayGames__ClientId`, `GooglePlayGames__ClientSecret` and `Authentication__SigningKey`, and the API starts and accepts `POST /api/players/sessions`

#### Scenario: Signing key too short
- **WHEN** the templates are deployed with an `authSigningKey` shorter than 32 characters
- **THEN** the deployment is rejected before any resource changes

#### Scenario: No secrets in source control
- **WHEN** the repository is searched for the registry password, the database password and the application secrets
- **THEN** none of their values are found

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

### Requirement: Health probes use the service default endpoints
The container app SHALL use `/alive` as its liveness probe and `/health` as its readiness probe.

#### Scenario: Unhealthy dependency keeps a replica out of rotation
- **WHEN** `/health` reports unhealthy on a replica
- **THEN** that replica receives no traffic until it reports healthy again

### Requirement: The daily pipe generator runs as a scheduled job
The infrastructure SHALL define an Azure Container Apps job in the existing Container Apps environment that runs the daily pipe generator on the schedule `0 0 * * *` (00:00 UTC), with the same database connection, secrets injection and telemetry as the API, a retry limit and a timeout. The job SHALL be deployed from the same container registry and by the same server deployment workflow as the API.

#### Scenario: Job deployed
- **WHEN** the server infrastructure is deployed
- **THEN** a Container Apps job for the daily pipe generator exists with a schedule trigger of `0 0 * * *`

#### Scenario: Job failure is retried and visible
- **WHEN** a run of the generator fails
- **THEN** Container Apps retries it up to the retry limit and the failure is visible in Application Insights
