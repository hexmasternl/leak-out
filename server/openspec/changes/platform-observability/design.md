# Design

## Context

See proposal.md (Why) for the gaps. The relevant current state:

- `ServiceDefaults/Extensions.cs` adds ASP.NET Core, HttpClient and runtime instrumentation. It filters `/health` and `/alive` through the `configure` callback of `AddAspNetCoreInstrumentation(...)`, then adds `UseOtlpExporter()` and/or `UseAzureMonitor()` depending on configuration. `UseAzureMonitor()` registers its own ASP.NET Core instrumentation on the same tracer provider.
- `Program.cs` adds the `HexMaster.LeakOut.Players` and `HexMaster.LeakOut.Scores` meters and activity sources. EF Core and Npgsql spans come from the Aspire `AddNpgsqlDbContext` integration.
- Each module has a metrics class (`Observability/PlayersMetrics.cs`, `Observability/ScoresMetrics.cs`). It owns the static `ActivitySource`, the instrument names and the outcome constants, and is registered as a singleton through `IMeterFactory`. `SignInPlayerCommandHandler` and `SubmitScoreCommandHandler` already follow the pattern: start an activity, then a `try` / `catch (Exception ex) when (ex is not OperationCanceledException)` block that sets the status, adds the exception, counts and rethrows.
- Each test project has a `TelemetryCollector` that listens to the module's activity source and to the `long` measurements of meters created by its own `IMeterFactory`.
- `api.bicep` sets `OTEL_SERVICE_NAME` and `APPLICATIONINSIGHTS_CONNECTION_STRING`. `deploy.yml` already computes the GitVersion `semVer` in the `publish` job and exposes it as `needs.publish.outputs.version`.

## Goals / Non-Goals

**Goals:**
- Bring all six operations in line with the existing `SubmitScoreCommandHandler` pattern, plus durations and logs.
- Keep each module's telemetry definitions (names, units, outcome values) in its metrics class, so tests and code share the constants.
- Make the probe filter and exception recording independent of which exporter is active.

**Non-Goals:**
- A generic handler decorator or pipeline behaviour (see Decisions).
- Changing sampling, the ingestion cap, or adding Azure dashboards and alerts.
- Instrumenting repositories by hand. Database calls are already covered by auto-instrumentation, and ADR 0008 forbids wrapping them.

## Decisions

### Explicit instrumentation per handler, not a decorator
Each handler instruments itself, as `SignInPlayerCommandHandler` and `SubmitScoreCommandHandler` already do. With six operations, the duplication is small. Explicit code also lets each operation set its own domain tags (`leaderboard.entries`, `player.has_score`, `score.new_best`) and map exceptions to outcomes (`rejected`, `unavailable`). A decorator would need a tagging hook for both.
*Alternative:* an `ICommandHandler`/`IQueryHandler` decorator in Core. Rejected for now: it would pull `System.Diagnostics` concerns into Core and still need per-handler hooks. Revisit if the number of handlers grows a lot.

### Durations recorded by the metrics class
Handlers take `Stopwatch.GetTimestamp()` at the start and pass `Stopwatch.GetElapsedTime(start)` to the metrics method that records the outcome. Examples: `metrics.SignInSucceeded(isNewPlayer, elapsed)` and `metrics.LeaderboardServed(kind, elapsed)`. Each method then increments the counter and records the `Histogram<double>` (unit `s`) with the same outcome tag, so the two always agree. Histogram names: `leakout.players.sign_in.duration`, `leakout.scores.submission.duration`, `leakout.scores.leaderboard.duration` (tags `leaderboard.kind`, `outcome`).
*Alternative:* deriving durations from span durations in the backend. Rejected: ADR 0008 asks for histograms, and Application Insights custom metrics are cheaper to chart than span queries.

### Leaderboard queries share one counter and one histogram
Both leaderboard queries are the same kind of operation and differ only in shape. One `leakout.scores.leaderboard.requests` counter and one `leakout.scores.leaderboard.duration` histogram, tagged `leaderboard.kind` = `top` / `around_player`, keep the metric set small and still allow a split per kind. Outcomes: `success`, `error`.

### `UserResolver` and `PlayerProfileService` get spans; the resolver also counts
`players.resolve_user` runs on every authenticated request and is the place where a cache problem would show up. It gets a span tagged `players.resolution` (`cache_hit` / `cache_miss` / `unknown`) and the counter `leakout.players.user_resolutions` with the same outcome (`error` on an exception). It gets no histogram: a cache hit is microseconds, and the span duration is enough. The `subjectId` is never tagged. On a resolved player, the span gets the internal `player.id`.
`players.get_display_names` is a cross-module call that does a database read. It gets a span tagged `players.requested` (count of distinct ids) and `players.found`, with no metric. An empty input returns before the span starts, like a trivial getter.

### Tags
- Span tags (may be high-cardinality, never identity data): `player.id` (internal GUID), `score.level`, `score.value`, `score.new_best`, `leaderboard.kind`, `leaderboard.entries`, `player.has_score`, `player.new`, `players.resolution`, `players.requested`, `players.found`.
- Metric tags: only `outcome`, `new_player` (bool), `leaderboard.kind`.
- Display names, the subject id, auth codes and tokens are never passed to `SetTag`, a metric or a log call.

### Logs through source-generated `[LoggerMessage]` methods
Each class that logs becomes `partial` and declares `[LoggerMessage]` methods, as `GooglePlayGamesClient` does. Planned events:
- `SignInPlayerCommandHandler`: Information "Player {PlayerId} registered"; Error "Sign-in failed unexpectedly" (with exception, only for the `error` outcome). The `rejected` and `unavailable` outcomes are already logged by `GooglePlayGamesClient`, so the handler does not log them twice.
- `SubmitScoreCommandHandler`: Information "Player {PlayerId} set a new best score {Score} on level {Level}"; Error "Score submission failed unexpectedly".
- Leaderboard handlers: Error "{LeaderboardKind} leaderboard request failed unexpectedly". Successful reads are not logged, because they are high-volume and the span and metric already cover them.
- `UserResolver`: Debug "Subject did not resolve to a registered player" (without the subject).

Trace correlation comes for free: the OpenTelemetry logging provider attaches the current `Activity`'s trace and span id.
The Scores module gets an explicit `Microsoft.Extensions.Logging` reference for the source generator, `ILogger<T>` and `AddLogging()`, which `AddScoresModule()` calls next to `AddMetrics()`. Players already receives it transitively through `Microsoft.Extensions.Http`. Add it explicitly if the build shows otherwise.

### Probe filter and exception recording via options
Replace the callback on `AddAspNetCoreInstrumentation(...)` with `builder.Services.Configure<AspNetCoreTraceInstrumentationOptions>(o => { o.Filter = ...; o.RecordException = true; })` and a plain `AddAspNetCoreInstrumentation()`. Every registration of the instrumentation, including the one inside `UseAzureMonitor()`, reads the same default named options, so the filter and exception recording hold whichever exporter is active. A `ServiceDefaults.Tests` test resolves `IOptionsMonitor<AspNetCoreTraceInstrumentationOptions>` with and without the Application Insights connection string, and asserts that the filter rejects `/alive` and `/health`, accepts `/api/scores`, and that `RecordException` is true.

### Service version through `OTEL_RESOURCE_ATTRIBUTES`
`api.bicep` gets `param serviceVersion string` and the env var `OTEL_RESOURCE_ATTRIBUTES` = `service.version=${serviceVersion}`. The parameter is passed through `resources.bicep` and `main.bicep`, and `main.bicepparam` reads it with `readEnvironmentVariable('SERVICE_VERSION')`. `deploy.yml` sets `SERVICE_VERSION: ${{ needs.publish.outputs.version }}` in the deploy step. The OpenTelemetry resource detector reads the variable, and the Azure Monitor exporter maps `service.version` to `application_Version`. The configuration stays in the environment, as ADR 0008 asks. Locally the variable is absent, and the Aspire dashboard shows no version, which is fine.
*Alternative:* `ConfigureResource(r => r.AddService(..., serviceVersion: assemblyInformationalVersion))` in code. Rejected: it duplicates `OTEL_SERVICE_NAME` handling and puts exporter configuration in code.

### Test helpers record histograms
Both `TelemetryCollector` helpers add a `SetMeasurementEventCallback<double>` that stores into a separate `DoubleMeasurements` bag, so tests can assert on histogram name, tags and that the value is ≥ 0. To test logs, handler tests inject a `FakeLogger<T>` from `Microsoft.Extensions.Diagnostics.Testing`, which is added to the test projects. If that package is unwanted, they inject a mocked `ILogger<T>` with `IsEnabled` returning true and verify `Log` calls. A privacy test per sign-in path asserts that no recorded tag value or log message contains the Google player id, the auth code, the token or the display name.

## Risks / Trade-offs

- [Azure Monitor's own ASP.NET Core instrumentation ignores the configured options in some distro version] → The options test covers registration, and the acceptance check after deployment (no `/alive` requests in the `requests` table) covers runtime. If the check fails, fall back to a sampler or processor that drops spans for those paths.
- [`RecordException` duplicates exceptions already recorded on the handler span] → Accepted. They are different spans, and the request span is what Application Insights shows as the failed request.
- [More custom metrics and logs raise ingestion against the 1 GB/day cap] → Only failure paths and rare events log at Information or above. Successful leaderboard reads log nothing. Histograms are pre-aggregated before export.
- [Explicit instrumentation drifts as new handlers are added] → The ADR 0008 review rule plus the TelemetryCollector tests per handler. Revisit the decorator once there are more than a handful of handlers.
- [`SERVICE_VERSION` missing on a manual deploy] → `readEnvironmentVariable` without a default fails the deployment early, which is consistent with the other parameters. The `deploy.yml` "Check required secrets" step does not need it, because it comes from a job output, not a secret.

## Migration Plan

Additive only: no schema, API or configuration contract changes. Deploying through `deploy.yml` sets the version automatically. Rollback is a redeploy of the previous image. After the first deployment, verify in Application Insights that requests, dependencies, custom spans (`players.resolve_user`, `scores.get_level_top_scores`), custom metrics and traces arrive under role `hexmaster-leakout-api` with `application_Version` set, and that no `/alive` or `/health` requests appear.
