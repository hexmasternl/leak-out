# database-migrations Specification

## Purpose

Keeps the server's PostgreSQL schemas (`playersdb` and `scoresdb`) up to date with the code that uses them, in every environment, so a running API never queries a table that does not exist yet.

## Requirements

### Requirement: Schemas are brought up to date before the API serves requests
In every environment, the API SHALL apply all pending schema migrations of `playersdb` and `scoresdb` when it starts, and SHALL NOT accept HTTP requests, including `/alive` and `/health`, until both databases are up to date.

#### Scenario: First start against empty databases
- **WHEN** the API starts in Production against empty `playersdb` and `scoresdb` databases
- **THEN** the `players` and `level_scores` tables exist before the API answers its first request, and `POST /api/players/sessions` does not fail with `relation "players" does not exist`

#### Scenario: Start against up-to-date databases
- **WHEN** the API starts and both databases already have every migration applied
- **THEN** no schema change is made and the API starts serving requests

#### Scenario: New migration shipped in a new release
- **WHEN** a release containing a new migration starts against databases created by an earlier release
- **THEN** the new migration is applied once, and existing rows are kept

### Requirement: A failed migration prevents the API from serving
If a migration cannot be applied, the API SHALL stop starting up and SHALL NOT serve requests against a partly migrated schema.

#### Scenario: Migration fails
- **WHEN** applying a migration fails at startup, for example because the database is unreachable
- **THEN** the API process exits with an error, `/alive` is never answered, and in Azure the previous revision keeps serving traffic

### Requirement: Concurrent starts migrate safely
When several API instances start against the same databases at the same time, each migration SHALL be applied exactly once, and every instance SHALL start successfully.

#### Scenario: Two replicas start together
- **WHEN** two API replicas start at the same time against databases with a pending migration
- **THEN** the migration is applied once and both replicas start serving requests
