# Tasks

## 1. Infrastructure

- [ ] 1.1 Add `minReplicas` (`int`, `@minValue(0)`, default `1`) to `server/infrastructure/modules/api.bicep`, `resources.bicep` and `main.bicep`, pass it through, set `param minReplicas = 1` in `main.bicepparam`, and use it in the scale block instead of `0`. Add `@minValue(1)` to `maxReplicas` and a check that fails when `minReplicas > maxReplicas`. Verify with `az bicep build --file server/infrastructure/main.bicep` and `az bicep build-params --file server/infrastructure/main.bicepparam`.
- [ ] 1.2 Run the server pull-request workflow's infrastructure validation and check that the only change to the container app is `minReplicas: 1`.

## 2. Docs

- [ ] 2.1 Update `server/infrastructure/README.md` (parameter table gets `minReplicas`, default `1`; the `modules/api.bicep` row says 1 to `maxReplicas` replicas) and the header comment in `modules/api.bicep`. Verify with a grep for "from 0 replicas" and "0 to `maxReplicas`".
