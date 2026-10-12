# Design

## Context

`server/infrastructure/modules/api.bicep` hard-codes `minReplicas: 0` and takes `maxReplicas` (default 3, set to 3 in `main.bicepparam`). The `http-concurrency` rule adds replicas at 20 concurrent requests per replica. A cold start takes about 15 seconds and the app's requests time out during it (#131).

## Goals / Non-Goals

**Goals:**
- No cold start on the first request after a quiet period.
- The minimum can be changed per environment through a parameter.

**Non-Goals:**
- Changing the scale rule or its threshold.
- Changing the app's timeouts (that is `app-score-server-cold-start-timeout`).
- Making start-up itself faster.

## Decisions

### 1. `minReplicas` parameter, default 1
Add `param minReplicas int = 1` (with `@minValue(0)`) to `main.bicep`, `resources.bicep` and `modules/api.bicep`, set it in `main.bicepparam`, and use it in the scale block. A parameter keeps the option of scaling to zero for a test environment.

*Alternative:* hard-code 1. Rejected: the cost trade-off may change, and a parameter costs nothing.

### 2. Guard against min > max
Add `@minValue(1)` to `maxReplicas` and fail the deployment when `minReplicas > maxReplicas`. Container Apps rejects it too, but a Bicep error is easier to read.

## Risks / Trade-offs

- [An always-on replica costs money even when nobody plays] → Asked for in #131. Setting `minReplicas = 0` reverts it.
- [A new revision restarts the replica] → Container Apps starts the new revision before it switches traffic, so a deploy does not cause a cold start for players.

## Open Questions

- Should a future test or staging environment keep `minReplicas = 0` to save cost? Only the production parameters file exists today.
