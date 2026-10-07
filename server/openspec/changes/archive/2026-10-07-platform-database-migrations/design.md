# Design

## Context

`PlayersDatabaseMigrator` and `ScoresDatabaseMigrator` are `IHostedService`s whose `StartAsync` calls `Database.MigrateAsync()`. `Program.cs` registers them only inside `if (builder.Environment.IsDevelopment())`. The Azure deployment (`platform-azure-infrastructure`, `platform-github-actions-workflows`) deploys infrastructure and the API image in a single `az deployment sub create`. The PostgreSQL firewall only allows Azure services, and the API connects as the server administrator. The container app has a startup probe on `/alive` (up to 50 seconds), uses single-revision mode, and scales from zero. EF Core and Npgsql are on 10.x.

`LeakOutApiFactory` (API tests) removes every `IHostedService`, so the API tests never touch a database.

## Goals / Non-Goals

**Goals:**
- One code path for applying migrations in every environment: local Aspire, tests that host the API, and Azure.
- A broken migration must never let a new revision take traffic.

**Non-Goals:**
- Zero-downtime handling of destructive schema changes (dropping or renaming columns the old revision still reads). Future migrations have to stay backwards-compatible with the previous release. That is a review rule, not something this change enforces.
- A separate least-privilege runtime database user.

## Decisions

**Migrate in the API at startup, in every environment.** Remove the `IsDevelopment()` guard in `Program.cs` and keep the two existing migrators unchanged apart from their comments.
- *Alternative, EF Core migration bundle run by the deploy workflow:* needs a temporary firewall rule for the GitHub runner. It also conflicts with the single Bicep deployment: the server does not exist yet on the first deployment, and on later deployments the bundle runs either before the new image is known to be good or after the new revision is already serving. Rejected.
- *Alternative, Container Apps job running the bundle inside the environment:* solves the firewall problem but adds an image, a job resource and ordering in Bicep. Too much for two small schemas and a low-traffic API. It can be revisited if migrations get long-running.

**Rely on the host's startup order to gate traffic.** With `WebApplication`, hosted services registered by the application start before the Kestrel server (`GenericWebHostService`) starts listening. So `/alive` is not answered until both `MigrateAsync` calls return. An exception in `StartAsync` stops the host, the startup probe fails, and Container Apps keeps the previous revision. No custom health check is needed.

**Rely on EF Core's migration lock for concurrency.** Since EF Core 9, `Migrate` takes a database-wide lock (Npgsql: a lock on `__EFMigrationsHistory` inside a transaction) and re-reads the applied migrations after it gets the lock. Replicas that start together therefore apply each migration once. No custom advisory lock.

**Keep the migrators as two hosted services.** Each module owns its own database and migrator. They run one after the other in registration order, which is fine because the databases are independent.

**Test the registration, not EF Core.** An API test hosts `Program` in `Production`, inspects the service collection before `LeakOutApiFactory` strips the hosted services, and asserts both migrators are registered. A second assertion covers Development. The existing `PlayersMigrationsTests` and `ScoresMigrationsTests` already cover that the migrations themselves apply against PostgreSQL.

## Risks / Trade-offs

- [Every cold start from zero runs one `__EFMigrationsHistory` query per database] → A few milliseconds per database, well within the 50-second startup probe.
- [A long-running future migration could exceed the startup probe window and get the revision killed mid-migration] → Each migration runs in a transaction, so a killed migration rolls back. If a migration needs more time, switch it to a Container Apps job (see Decisions).
- [The old revision briefly runs against the new schema while the new revision takes over] → Migrations must stay additive and backwards-compatible with the previous release (Non-Goals).
- [The API needs DDL rights on both databases] → It already connects as the administrator; a least-privilege split is a separate change.

## Migration Plan

1. Merge and deploy. The first new revision creates the missing `players` and `level_scores` tables and records the migrations in `__EFMigrationsHistory`.
2. If the tables were already created by hand with `dotnet ef database update`, the history table already lists the migrations and startup changes nothing.
3. Rollback: redeploy the previous image. The tables stay, and the previous image still works against them.
