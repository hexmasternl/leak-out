# Spec Delta

## Purpose

Lets players of the Leak Out game prove their identity to the score server with their Google Play Games account, so the server can attribute scores to a player and protect its endpoints.

## ADDED Requirements

### Requirement: Sign in with a Google Play Games server auth code
The system SHALL expose an anonymous sign-in endpoint that accepts a one-time Google Play Games server auth code. When Google confirms the code, the system SHALL return 200 with an access token, its expiry time, the player's internal id and the player's display name.

#### Scenario: Valid server auth code
- **WHEN** the game posts a server auth code that Google accepts for the player "Ann"
- **THEN** the response is 200 with an access token, an expiry time one hour from now, Ann's internal player id and the display name "Ann"

#### Scenario: Invalid or used server auth code
- **WHEN** the game posts a server auth code that Google rejects (invalid, expired or already used)
- **THEN** the response is 401 and no access token is returned

#### Scenario: Missing server auth code
- **WHEN** the game posts a sign-in request with an empty or missing server auth code
- **THEN** the response is 400 with a validation problem and Google is not called

### Requirement: Google unavailable during sign-in
When Google cannot be reached or returns a server error during sign-in, the system SHALL respond with 503 and SHALL NOT register or change a player.

#### Scenario: Google is down
- **WHEN** the game signs in while the Google token endpoint returns server errors after retries
- **THEN** the response is 503 and no player record is created or changed

### Requirement: Player registration on first sign-in
The system SHALL register a new player, with a new internal id, the first time a Google Play Games player signs in. Later sign-ins of the same Google Play Games player SHALL return the same internal id.

#### Scenario: First sign-in registers the player
- **WHEN** a Google Play Games player who never signed in before signs in
- **THEN** a player is registered and the response contains its new internal id

#### Scenario: Later sign-in returns the same player
- **WHEN** the same Google Play Games player signs in again on another day or device
- **THEN** the response contains the same internal player id as the first sign-in

### Requirement: Display name kept up to date
The system SHALL store the display name Google reports for the player and SHALL replace it with the current one on every sign-in.

#### Scenario: Player changed their Play Games name
- **WHEN** a registered player renamed themselves from "Ann" to "Annie" in Play Games and signs in
- **THEN** the response contains "Annie" and the stored display name is "Annie"

### Requirement: Endpoints require a valid access token
Every API endpoint except sign-in, health checks and the OpenAPI document SHALL require a valid, unexpired access token issued by this server, and SHALL respond with 401 otherwise.

#### Scenario: No token
- **WHEN** a client calls a protected endpoint without an access token
- **THEN** the response is 401

#### Scenario: Expired token
- **WHEN** a client calls a protected endpoint with an access token whose expiry time has passed
- **THEN** the response is 401

#### Scenario: Token from another issuer
- **WHEN** a client calls a protected endpoint with a token not signed by this server
- **THEN** the response is 401

### Requirement: Requests are attributed to the internal player id
For an authenticated request, the system SHALL determine the internal player id from the access token before handling the request. When the token belongs to no registered player, the system SHALL respond with 401.

#### Scenario: Token of a registered player
- **WHEN** a registered player calls a protected endpoint with a valid token
- **THEN** the request is handled on behalf of that player's internal id

#### Scenario: Token of an unknown player
- **WHEN** a client calls a protected endpoint with a validly signed token whose player is not registered
- **THEN** the response is 401

### Requirement: Google Play Games player id is not exposed
The system SHALL NOT return the Google Play Games player id as a field of any API response body. Response bodies SHALL identify a player only by the internal player id; the access token is opaque to the client.

#### Scenario: Sign-in response
- **WHEN** a player signs in
- **THEN** the response body contains the internal player id and does not contain the Google Play Games player id
