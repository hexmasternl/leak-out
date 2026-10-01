## 1. Gradle: version code and release signing (`app/build.gradle.kts`)

- [x] 1.1 Read `versionCode` from `providers.gradleProperty("versionCode")`, falling back to the current literal; fail the build with a clear message when the value is not a positive integer
- [x] 1.2 Add an `upload` signing config created only when `LEAKOUT_UPLOAD_STORE_FILE` is set, reading `LEAKOUT_UPLOAD_STORE_PASSWORD`, `LEAKOUT_UPLOAD_KEY_ALIAS` and `LEAKOUT_UPLOAD_KEY_PASSWORD`; fail with a message naming any of the three that is missing
- [x] 1.3 Use the `upload` signing config for the `release` build type when it exists; leave `debug` untouched
- [x] 1.4 Verify locally without the variables: `./gradlew :app:bundleRelease` succeeds and `jarsigner -verify` reports the bundle as unsigned; `./gradlew :app:assembleDebug` still works
- [x] 1.5 Verify locally with a throwaway keystore made with `keytool` in the scratchpad (never in the repo): `bundleRelease` succeeds and `jarsigner -verify -verbose -certs` shows the throwaway certificate
- [x] 1.6 Verify with the store file set but the alias missing: the build fails and names `LEAKOUT_UPLOAD_KEY_ALIAS`
- [x] 1.7 Verify `-PversionCode=42` ends up in the merged release manifest (`app/build/intermediates/merged_manifest/release/...`), that no property keeps the file's version code, and that `-PversionCode=abc` and `-PversionCode=0` fail the build
- [x] 1.8 Run `./gradlew :core:test :app:testDebugUnitTest` and confirm they still pass

## 2. Deployment workflow (`.github/workflows/deploy-play-internal.yml`)

- [x] 2.1 Create the workflow: `on: push: branches: [main]` and `workflow_dispatch`; `concurrency: { group: deploy-play, cancel-in-progress: false }`; `permissions: contents: read`; one job on `ubuntu-latest` with `environment: play-store` and `defaults.run.working-directory: src`
- [x] 2.2 Resolve the current release commit SHAs of `actions/checkout`, `actions/setup-java`, `gradle/actions/setup-gradle`, `actions/upload-artifact` and `r0adkll/upload-google-play`, and pin each by full SHA with the version as a comment
- [x] 2.3 Add checkout, Temurin JDK 21 and Gradle setup steps
- [x] 2.4 Add the secret check: list every empty secret among the five by name, refer to `docs/deployment.md`, exit non-zero; never echo values
- [x] 2.5 Run `./gradlew :core:test :app:testDebugUnitTest`
- [x] 2.6 Compute `versionCode = github.run_number + (vars.VERSION_CODE_OFFSET || 1)`, export it to later steps and write it to `$GITHUB_STEP_SUMMARY`
- [x] 2.7 Decode `PLAY_UPLOAD_KEYSTORE_BASE64` to `$RUNNER_TEMP/upload.jks` and run `./gradlew :app:bundleRelease -PversionCode=…` with the four `LEAKOUT_UPLOAD_*` variables set from the secrets
- [x] 2.8 Verify the bundle with `jarsigner -verify` and fail if it is not signed
- [x] 2.9 Upload with `r0adkll/upload-google-play`: `serviceAccountJsonPlainText` from `PLAY_SERVICE_ACCOUNT_JSON`, `packageName: nl.hexmaster.leakout`, `releaseFiles` pointing at `src/app/build/outputs/bundle/release/app-release.aab`, `track: ${{ vars.PLAY_TRACK || 'internal' }}`, `status: ${{ vars.PLAY_RELEASE_STATUS || 'completed' }}`
- [x] 2.10 Upload the `.aab` as a workflow artifact with a short retention (for example 14 days)
- [x] 2.11 Add an `if: always()` step that deletes `$RUNNER_TEMP/upload.jks`
- [x] 2.12 If AGP cannot install Android platform 37 on the runner by itself, add an explicit `sdkmanager "platforms;android-37"` step (check the runner image's installed SDKs first) *Not needed: the `ubuntu-latest` (24.04) image already has `android-37.0` and build-tools 37.0.0.*
- [x] 2.13 Lint the workflow with `actionlint` (download the binary into the scratchpad, or run it with Docker) and fix every finding

## 3. Setup guide (`docs/deployment.md`)

- [ ] 3.1 Overview: what the workflow does, when it runs, and a short explanation (or diagram) of the upload key, the app signing key held by Google, and the service account
- [ ] 3.2 Prerequisites: Play Console developer account with admin rights, GitHub repository admin access, a JDK for `keytool`
- [ ] 3.3 Create the upload keystore: exact `keytool -genkeypair` command (RSA, 2048 bits or more, 25+ years validity, alias), where to keep it and the passwords (password manager, never the repo), and how to back it up
- [ ] 3.4 Play Console: create the app (package `nl.hexmaster.leakout`), opt in to Play App Signing, build a signed bundle locally with the `LEAKOUT_UPLOAD_*` variables and version code 1, and upload it by hand to the internal testing track; list the minimum setup the console requires before testers can install
- [ ] 3.5 Add testers to the internal testing track (email list) and share the opt-in link; explain how to use a named closed track instead and set `PLAY_TRACK`
- [ ] 3.6 Google Cloud: create or pick a project, enable the Google Play Android Developer API, create a service account and download its JSON key; warn to keep the key file out of the repo and delete the local copy once stored
- [ ] 3.7 Play Console: invite the service account's email under Users and permissions, grant access to this app only with permission to release to testing tracks; note that permissions can take time to apply
- [ ] 3.8 GitHub: create the `play-store` environment with deployment branches limited to `main` (optionally required reviewers); add each of the five secrets with what it holds and how to get it, including one-line base64 commands for Windows PowerShell, macOS and Linux
- [ ] 3.9 GitHub: add the repository variables `PLAY_TRACK`, `PLAY_RELEASE_STATUS` and `VERSION_CODE_OFFSET` with their defaults and when to change them (`draft` while the app has never been published; offset above the highest version code already uploaded)
- [ ] 3.10 Running it: merge `development` into `main` or use *Run workflow*; what a successful run and job summary look like; where to find the release in the Play Console
- [ ] 3.11 Troubleshooting: missing secret, version code already used, package not found, "Only releases with status draft may be created on draft app", service account has no permission, upload signed with the wrong key, unit test failure, Android SDK platform missing
- [ ] 3.12 Maintenance: rotate the service-account key, reset a lost or leaked upload key through Play Console support, change the track, raise the version-code offset, bump `versionName`, and an optional move to Workload Identity Federation
- [ ] 3.13 Check that the set of secret and variable names in the workflow and in the guide is identical (for example with `grep -o` on both files and a diff)

## 4. Project instructions

- [ ] 4.1 Add a short "Releases" section to `CLAUDE.md`: pushes to `main` deploy to the internal testing track via the workflow, signing material is never committed or needed locally, and the setup is in `docs/deployment.md`
- [ ] 4.2 Run `node scripts/sync-agent-config.mjs --check` and make sure `AGENTS.md` and `.github/copilot-instructions.md` are in sync

## 5. First deployment (after merge into `main`, with the repository owner)

- [ ] 5.1 The owner completes `docs/deployment.md` steps for the keystore, Play Console, service account, environment, secrets and variables, and confirms they are done
- [ ] 5.2 After `development` is merged into `main`, check the run: tests pass, the version code is in the summary, the bundle is signed and uploaded, and the keystore cleanup step ran
- [ ] 5.3 Confirm in the Play Console that the release is on the chosen track and installable by a tester; if the app is still a draft app, confirm `PLAY_RELEASE_STATUS=draft` was used and record when to switch it to `completed`
