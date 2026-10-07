# Proposal

## Why

Tracked in [#18](https://github.com/hexmasternl/leak-out-server/issues/18).

The telemetry plumbing exists: OTLP export to the Aspire dashboard, `UseAzureMonitor()` when `APPLICATIONINSIGHTS_CONNECTION_STRING` is set, and the container app sets that variable. Coverage is uneven, though. Only `SignInPlayerCommandHandler` and `SubmitScoreCommandHandler` create spans and count outcomes. The two leaderboard queries, `UserResolver` (which runs on every authenticated request) and `PlayerProfileService` produce no telemetry at all. No handler records its duration, and apart from `GooglePlayGamesClient` no code logs anything. Telemetry carries no `service.version`, so a regression can't be tied to a deployment. Unhandled exceptions are not recorded on the request span, and nothing proves that the `/alive` and `/health` probes stay out of Application Insights, where they would eat the 1 GB daily ingestion cap. ADR 0008 requires a span, an outcome counter and a duration per handler, plus structured logs, so the server is not compliant yet.

## What Changes

- **Spans**: every command and query handler, plus `UserResolver` and `PlayerProfileService`, creates a span from its module's `ActivitySource`. The span carries low-cardinality domain tags, sets its status and records exceptions. Cancellation is not an error. New spans: `players.resolve_user`, `players.get_display_names`, `scores.get_level_top_scores` and `scores.get_level_scores_around_player`.
- **Metrics**: three duration histograms in seconds: `leakout.players.sign_in.duration`, `leakout.scores.submission.duration` and `leakout.scores.leaderboard.duration`. Two new counters: `leakout.players.user_resolutions` (outcome `cache_hit` / `cache_miss` / `unknown`) and `leakout.scores.leaderboard.requests` (`leaderboard.kind` = `top` / `around_player`, plus outcome). Metric attributes never carry player ids, levels, scores or names.
- **Logs**: source-generated `[LoggerMessage]` logs at meaningful outcomes: a new player registered, a sign-in rejected, a new best score, an unexpected handler failure. Logs are correlated with the active trace. They never contain the Google Play Games player id, auth codes, tokens or display names. The internal player id is allowed.
- **ServiceDefaults**: configure the probe filter and `RecordException = true` through `AspNetCoreTraceInstrumentationOptions`, so both also apply to the Azure Monitor registration.
- **Infrastructure**: a new `serviceVersion` Bicep parameter sets `OTEL_RESOURCE_ATTRIBUTES=service.version=<version>` on the container app. `deploy.yml` passes the GitVersion `SemVer` as `SERVICE_VERSION`.
- **Docs**: `README.md` explains where to find traces, metrics and logs, both locally (Aspire dashboard) and in Azure (Application Insights).
- **API contract**: unchanged.

## Capabilities

### New Capabilities
- `observability`: the traces, metrics and logs the API emits for its operations, the privacy rules they follow, the version they carry and where they are exported.

### Modified Capabilities

None. The in-flight `platform-azure-infrastructure` change already requires telemetry to reach Application Insights (`azure-hosting`). This change adds requirements on what that telemetry contains, and leaves that requirement as it is.

## Impact

- **Code**: `Players/HexMaster.LeakOut.Players` (`Observability/PlayersMetrics.cs`, `SignInPlayerCommandHandler`, `UserResolver`, `PlayerProfileService`, plus new log methods). `Scores/HexMaster.LeakOut.Scores` (`Observability/ScoresMetrics.cs` and all three handlers). `HexMaster.LeakOut.Aspire.ServiceDefaults/Extensions.cs`.
- **Tests**: `Players.Tests` and `Scores.Tests` (the `TelemetryCollector` helpers also record `double` histogram measurements) and `HexMaster.LeakOut.Aspire.ServiceDefaults.Tests`.
- **Infrastructure / CI**: `infrastructure/modules/api.bicep`, `resources.bicep`, `main.bicep`, `main.bicepparam`, `.github/workflows/deploy.yml`, `infrastructure/README.md`.
- **Dependencies**: none new. The Players module may need `Microsoft.Extensions.Logging.Abstractions` if it isn't already referenced transitively.
- **Out of scope**: Azure dashboards, workbooks and alert rules; changes to sampling or the daily ingestion cap; telemetry from the game client.
