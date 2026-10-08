# Tasks

## 1. Build

- [ ] 1.1 In `app/src/app/build.gradle.kts`, set `isMinifyEnabled = true` and `isShrinkResources = true` for the release build type. Verify that `./gradlew :app:bundleRelease` succeeds and writes `app/build/outputs/mapping/release/mapping.txt`.
- [ ] 1.2 Run the instrumented tests against a minified build (for example a temporary `testBuildType` or a minified test build type) and add a keep rule to `proguard-rules.pro` for every failure, each with a comment. Verify that the affected instrumented tests pass on the minified build.

## 2. Workflow

- [ ] 2.1 In `.github/workflows/deploy-play-internal.yml`, add a `MAPPING_PATH` env, a step that fails when the file is missing, `mappingFile: ${{ env.MAPPING_PATH }}` on the upload step, and include the mapping in the kept workflow artifact. Verify the workflow with `actionlint`.

## 3. Docs

- [ ] 3.1 Describe the mapping file, where to find it and the Play Console warning in `docs/deployment.md`. Verify the section and its links.
