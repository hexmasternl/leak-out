# Tasks

## 1. ServiceDefaults: probe filter and exception recording

- [ ] 1.1 In `ServiceDefaults/Extensions.cs`, configure `AspNetCoreTraceInstrumentationOptions` through `builder.Services.Configure<...>`: `Filter` excludes `/health` and `/alive`, and `RecordException = true`. Switch to plain `AddAspNetCoreInstrumentation()`. Verify with new `ServiceDefaults.Tests` tests, with and without `APPLICATIONINSIGHTS_CONNECTION_STRING`, that the resolved options reject `/alive` and `/health`, accept `/api/scores` and have `RecordException` true.

## 2. Test helpers

- [ ] 2.1 Extend both `TelemetryCollector` helpers (Players.Tests, Scores.Tests) with a `double` measurement callback and a `DoubleMeasurements` bag. Add `Microsoft.Extensions.Diagnostics.Testing` (for `FakeLogger<T>`) to both test projects. Verify the existing tests still pass.

## 3. Players module

- [ ] 3.1 Extend `PlayersMetrics` with the `leakout.players.sign_in.duration` histogram (seconds), the `leakout.players.user_resolutions` counter, the activity names `players.resolve_user` and `players.get_display_names`, and the resolution outcome constants (`cache_hit`, `cache_miss`, `unknown`, `error`). Sign-in methods take the elapsed time and record counter and histogram with the same outcome. Verify with metrics unit tests.
- [ ] 3.2 Instrument `SignInPlayerCommandHandler`: record the duration on success and failure, log "Player {PlayerId} registered" for a new player and an Error log for the `error` outcome only. Verify `SignInPlayerCommandHandlerTests` cover the histogram per outcome, the logs, a cancellation that is not marked Error, and that no tag, metric attribute or log contains the Google player id, auth code, token or display name.
- [ ] 3.3 Instrument `UserResolver` with a `players.resolve_user` span (`players.resolution`, `player.id` when resolved, status, exception) and the `user_resolutions` counter. Add a Debug log for an unknown subject without the subject. Verify `UserResolverTests` cover cache hit, cache miss, unknown, repository failure (Error status, `error` outcome) and that the subject id never appears in tags or logs.
- [ ] 3.4 Instrument `PlayerProfileService` with a `players.get_display_names` span (`players.requested`, `players.found`, status, exception). No span for empty input. Verify `PlayerProfileServiceTests` cover the span tags, the empty-input case, and that no display name is tagged.

## 4. Scores module

- [ ] 4.1 Add `Microsoft.Extensions.Logging.Abstractions` to `HexMaster.LeakOut.Scores`. Extend `ScoresMetrics` with the histograms `leakout.scores.submission.duration` and `leakout.scores.leaderboard.duration`, the counter `leakout.scores.leaderboard.requests` (`leaderboard.kind` `top` / `around_player`, outcome `success` / `error`), and the activity names `scores.get_level_top_scores` and `scores.get_level_scores_around_player`. Verify with metrics unit tests that no metric carries player id, level or score.
- [ ] 4.2 Instrument `SubmitScoreCommandHandler`: record the duration on success and failure, log "Player {PlayerId} set a new best score {Score} on level {Level}" on a new best, and an Error log on unexpected failure. Verify `SubmitScoreCommandHandlerTests` cover the histogram outcomes, both logs, and no level or score in metric attributes.
- [ ] 4.3 Instrument `GetLevelTopScoresQueryHandler` with span (`score.level`, `player.id`, `leaderboard.kind`, `leaderboard.entries`, status, exception), counter, histogram and an Error log on failure. Verify `GetLevelTopScoresQueryHandlerTests` cover an empty and a filled leaderboard, a repository failure (Error status, `error` outcome) and a cancellation.
- [ ] 4.4 Instrument `GetLevelScoresAroundPlayerQueryHandler` the same way, plus `player.has_score`. Verify `GetLevelScoresAroundPlayerQueryHandlerTests` cover a player with and without a score, a failure and a cancellation.

## 5. Infrastructure and deployment

- [ ] 5.1 Add `param serviceVersion string` to `infrastructure/modules/api.bicep` and set `OTEL_RESOURCE_ATTRIBUTES` = `service.version=${serviceVersion}`. Pass it through `resources.bicep` and `main.bicep`, and read it in `main.bicepparam` with `readEnvironmentVariable('SERVICE_VERSION')`. Verify `az bicep build --file infrastructure/main.bicep` and `az bicep build-params --file infrastructure/main.bicepparam` (with `SERVICE_VERSION` set) succeed.
- [ ] 5.2 Set `SERVICE_VERSION: ${{ needs.publish.outputs.version }}` in the deploy step of `.github/workflows/deploy.yml`. Document the parameter and the env var in `infrastructure/README.md`. Verify the workflow YAML parses and every parameter in `main.bicepparam` has a matching env var in the deploy step.

## 6. Documentation

- [ ] 6.1 Add an "Observability" section to `README.md`: where traces, metrics and logs appear locally (Aspire dashboard) and in Azure (Application Insights, role `hexmaster-leakout-api`, `application_Version`), the custom span and metric names, and the privacy rule. Verify every name in it matches the constants in `PlayersMetrics` and `ScoresMetrics`.

## 7. Verification

- [ ] 7.1 Run all tests with coverage (git-workflow A3) and verify they pass with at least 80% line coverage.
- [ ] 7.2 Run the API through Aspire, sign in, submit a score and request both leaderboards. Verify in the dashboard that a leaderboard trace shows the HTTP span, `players.resolve_user`, the handler span and nested database spans, that the custom metrics appear, and that no `/alive` or `/health` traces appear.
- [ ] 7.3 After the first deployment, verify in Application Insights that requests, dependencies, custom spans, custom metrics and logs arrive under role `hexmaster-leakout-api` with the deployed version, and that no `/alive` or `/health` requests are recorded.
