# Spec Delta

## Purpose

Describes how the Leak Out score server is provisioned and run in Azure: reproducible infrastructure as code, image publishing, telemetry, database, configuration and cost-conscious scaling.

## ADDED Requirements

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

### Requirement: The API image is published to the existing container registry
The API SHALL be packaged as a container image and pushed to the existing container registry identified by `ACR_ENDPOINT`, authenticating with `ACR_USERNAME` and `ACR_PASSWORD`. The infrastructure SHALL NOT create a container registry.

#### Scenario: Image is pushed on deployment
- **WHEN** the deployment pipeline runs for a commit
- **THEN** an image of the API tagged with that commit's SHA is present in the existing registry

#### Scenario: Container app pulls from the registry
- **WHEN** the container app is deployed with that image tag
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

#### Scenario: No secrets in source control
- **WHEN** the repository is searched for the registry password, the database password and the application secrets
- **THEN** none of their values are found

### Requirement: The API scales with HTTP traffic and down to zero
The container app SHALL scale its replica count on concurrent HTTP requests, with a minimum of 0 replicas and a configurable maximum, and SHALL accept external HTTPS traffic only.

#### Scenario: Idle app scales to zero
- **WHEN** the API receives no requests for the scale-down period
- **THEN** the number of running replicas drops to 0

#### Scenario: Traffic wakes the app
- **WHEN** a request arrives while 0 replicas run
- **THEN** a replica starts and the request is answered

#### Scenario: Load scales out
- **WHEN** concurrent requests per replica exceed the configured threshold
- **THEN** replicas are added up to the configured maximum

### Requirement: Health probes use the service default endpoints
The container app SHALL use `/alive` as its liveness probe and `/health` as its readiness probe.

#### Scenario: Unhealthy dependency keeps a replica out of rotation
- **WHEN** `/health` reports unhealthy on a replica
- **THEN** that replica receives no traffic until it reports healthy again
