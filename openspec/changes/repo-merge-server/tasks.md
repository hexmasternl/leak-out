# Tasks

## 1. Move the app from `src/` to `app/src/`

- [x] 1.1 `mkdir app`, `git mv src app/src` and `git mv GitVersion.yml app/GitVersion.yml` (add `tag-prefix: '[vV]?'`, see design decision 5); verify `git log --follow app/src/app/build.gradle.kts` lists commits from before the move and `./gradlew :core:test :app:testDebugUnitTest` passes from `app/src/`
- [x] 1.2 Update `deploy-play-internal.yml`: `working-directory: app/src`, `BUNDLE_PATH: app/src/app/build/...`, GitVersion `configFilePath: app/GitVersion.yml`, and `on.push.paths` `app/**` plus the workflow file and the release scripts it runs; verify with `actionlint` if available and by checking every `src` reference in the workflow is gone
- [x] 1.3 Change `scripts/generate-release-notes.sh` to consider only commits that change `app/` (a merge counts when it changes `app/` compared with its first parent) and add a server-only merge case to `scripts/generate-release-notes.test.sh`; check `scripts/compute-release-version.sh` for `src`/tag assumptions; verify `bash scripts/generate-release-notes.test.sh` and `bash scripts/compute-release-version.test.sh` pass
- [x] 1.4 Replace every `src/` reference to the app with `app/src/` in `CLAUDE.md` (sync to `AGENTS.md` and `.github/copilot-instructions.md`), `README.md`, `docs/`, `openspec/config.yaml`, `.claude/skills`, `.claude/agents` and `scripts/`; run `node scripts/sync-agent-config.mjs --check` (in sync) and verify `grep -rn "src/" --include=*.md --include=*.yaml --include=*.yml --include=*.mjs --include=*.sh . | grep -v "^./app/src/\|^./server/\|openspec/changes/archive"` shows no app-location references

## 2. Import the server into `server/`

- [x] 2.1 In a fresh clone of `hexmasternl/leak-out-server`, run `git filter-repo --to-subdirectory-filter server`, then `git merge --allow-unrelated-histories` it into the feature branch; verify `git log --oneline -- server/src | tail -1` shows the server's first commit and `dotnet build server/src/leak-out-server.slnx` succeeds and the Bicep templates are in `server/infrastructure/`
- [x] 2.2 Rearrange per design.md decision 3: keep the solution and projects in `server/src/`, the Bicep templates in `server/infrastructure/`, and `global.json`, `Directory.Build.props`, `CodeCoverage.settings.xml`, `GitVersion.yml`, `README.md`, `docs/` and `.gitignore` in `server/`; drop `server/LICENSE` if identical to the root one; verify `dotnet test` runs from `server/` (Testcontainers needs Docker; record if skipped)
- [x] 2.3 Add `tag-prefix: 'server-v'` to `server/GitVersion.yml`; verify on a scratch branch with a `v9.9.9` tag that GitVersion with `server/GitVersion.yml` ignores it, and with a `server-v9.9.9` tag that GitVersion with `app/GitVersion.yml` ignores it
- [x] 2.4 Move the server's OpenSpec specs to `openspec/specs/server-<name>/`, its archived changes to `openspec/changes/archive/` (prefix clashes with `server-`) and update cross-references; rewrite `server-ci-cd` for the new workflow names, paths and environment; verify `openspec validate --specs` passes
- [x] 2.5 Keep `server/CLAUDE.md` and add `server/AGENTS.md` with the same content; move server skills that do not exist here to `.claude/skills/` (keep this repository's `openspec-*` and `triage-github-issues`), drop `.agents/` and the server's `.github/skills|prompts`, merge `.mcp.json`; run `node scripts/sync-agent-config.mjs` and verify `--check` reports in sync

## 3. Server workflows

- [x] 3.1 Move the server workflows to `.github/workflows/server-build-test.yml`, `server-pull-request.yml` and `server-deploy.yml`, and `coverage-gate.sh` to `.github/scripts/server-coverage-gate.sh`; point restore, build, test, coverage, `dotnet msbuild`/`publish` and artifact paths at `server/src/`, `global-json-file` at `server/global.json`, and the Bicep build and `az deployment` at `server/infrastructure/main.bicep` and `main.bicepparam`; verify with `actionlint` if available
- [ ] 3.2 In `server-pull-request.yml` add a `changes` job (`git diff --name-only` against the base) and run `build-test` and `bicep` only when `server/` or a server workflow changed; verify with a pull request into `development` that only changes `app/` (checks pass without building) and one that changes `server/` (server builds and tests)
- [x] 3.3 In `server-deploy.yml` use GitVersion `configFilePath: server/GitVersion.yml`, keep `environment: production` and the concurrency group, and for now only enable `workflow_dispatch` (cut-over step 1); verify the workflow file has no `push` trigger yet

## 4. Secrets, environment and Azure

- [ ] 4.1 Create the `production` environment in `hexmasternl/leak-out`, limited to `main`, and add the ten server secrets as environment secrets with values from their sources; verify `gh secret list -R hexmasternl/leak-out -e production` lists all ten
- [x] 4.2 Add a federated credential for `repo:hexmasternl/leak-out:environment:production` to the Azure app registration behind `AZURE_PROD_CLIENT_ID` (keep the old one); verify in the Azure portal that the credential exists _(verified by use rather than in the portal: `server-deploy` on `main` signed in to Azure through OIDC with `environment: production` and succeeded, which needs this credential)_
- [ ] 4.3 Update branch protection on `development`: require `server / build-test` and `server / bicep` (the new check names); verify an app-only pull request is mergeable

## 5. Documentation

- [x] 5.1 Write `docs/repository-layout.md` (folders, which workflow builds and releases what, tags `v*` and `server-v*`, where each side's secrets are documented) and link it from `CLAUDE.md` and `README.md`; add the server's secrets, environment and federated credential setup to `server/docs` or `server/README.md`; verify every secret name in the server workflows appears in that guide and vice versa
- [x] 5.2 Update `docs/deployment.md` for `app/src/` paths, `app/GitVersion.yml` and the path-filtered trigger; verify every path it mentions exists

## 6. Cut-over and integration

- [x] 6.1 Run `./gradlew :core:test :app:testDebugUnitTest :app:assembleRelease` from `app/src/` and `dotnet build` + `dotnet test` on `server/src/`; verify both succeed, then open the pull request into `development` listing the feature branches that still use `src/`
- [x] 6.2 After `development` is merged into `main`: verify the app deploy runs (the merge changes `app/`), push the tag `server-v0.3.2` on the server import merge commit (design decision 5: without it the server's GitVersion fails), then run `server-deploy.yml` by hand and verify the Azure sign-in, the deployment and the `/alive` smoke test succeed _(the merge of #121 ran the app deploy successfully; `server-v0.3.2` is on the import commit `eeb2165`; `server-deploy`, run by hand after #124 and again after `a497732`, succeeded including Azure sign-in, deployment and the smoke test)_
- [ ] 6.3 Enable the `push` trigger (`server/**` and the server workflows) in `server-deploy.yml` and disable `deploy.yml` in `leak-out-server`; verify a server-only push to `main` deploys the server and does not start the app deploy
- [ ] 6.4 Remove the old federated credential, add a README pointer to this repository in `leak-out-server` and archive it; verify the repository shows as archived
