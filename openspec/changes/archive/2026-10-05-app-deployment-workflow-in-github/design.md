## Context

Leak Out is built with Gradle from `src/` (AGP 9.4.1, JDK 21, `compileSdk`/`targetSdk` 37, application id `nl.hexmaster.leakout`). `app/build.gradle.kts` has a hard-coded `versionCode = 1` and `versionName = "0.1.0"`, and the `release` build type has no signing config. The repository has no `.github/workflows/` yet; `.github/` only holds Copilot configuration. `*.jks` and `*.keystore` are already in `.gitignore`.

`main` is production and only receives merges from `development`. The goal is that every such merge produces a build for the private testers without anyone touching a keystore or the Play Console.

Google Play has some fixed rules this design has to live with:

- The Play Developer API cannot create an app. The app, its store listing basics and the very first bundle have to be set up by hand in the Play Console.
- Every uploaded bundle needs a `versionCode` higher than any bundle uploaded before, on any track.
- With Play App Signing (mandatory for new apps), we sign with an **upload key**; Google re-signs with the app signing key it keeps. Losing the upload key is recoverable (Google can reset it); losing an app signing key would not be, which is why we let Google hold it.
- An app that has never been published (still a "draft app") only accepts releases with status `draft` through the API.

## Goals / Non-Goals

**Goals:**
- A push to `main` builds, tests, signs and uploads a release bundle to the internal testing track with no manual steps.
- No signing material or credentials in the repository; secrets live in GitHub and only the deployment job can read them.
- Version codes increase on their own.
- Local builds keep working with no keystore or extra setup.
- One setup guide, `docs/deployment.md`, that someone new to the project can follow from an empty Play Console to a green workflow run.

**Non-Goals:**
- Promoting builds to closed, open or production tracks. That stays a manual step in the Play Console.
- Release notes ("What's new") per upload, store listing or screenshot management.
- Running the instrumented (emulator) tests in CI. They stay a local step, as today.
- R8/minification and uploading mapping files (minification is off; when it is turned on, the mapping upload is a follow-up).
- A build workflow for pull requests or `development`.
- Automating `versionName`; it is bumped by hand when we want testers to see a new version label.

## Decisions

### 1. One workflow, triggered by push to `main` plus `workflow_dispatch`

`.github/workflows/deploy-play-internal.yml` with `on: push: branches: [main]` and `workflow_dispatch`. A `concurrency` group (`deploy-play`, `cancel-in-progress: false`) makes deployments run one after the other, so two quick merges cannot race for the same version code or have the older build land last.

Steps in one job on `ubuntu-latest`, working directory `src`:
1. `actions/checkout`
2. `actions/setup-java` (Temurin, JDK 21, matching `jdk` in `libs.versions.toml`)
3. `gradle/actions/setup-gradle` (Gradle cache; wrapper validation)
4. Check that every required secret is non-empty, failing with a message that names the missing one and points to `docs/deployment.md`.
5. `./gradlew :core:test :app:testDebugUnitTest`
6. Decode the keystore secret into `$RUNNER_TEMP/upload.jks`.
7. `./gradlew :app:bundleRelease -PversionCode=<computed>` with the signing environment variables set.
8. Verify the bundle is signed (`jarsigner -verify`) before uploading.
9. Upload with `r0adkll/upload-google-play`.
10. Delete the decoded keystore (`if: always()`), and attach the `.aab` as a workflow artifact for a short retention period so a build can be inspected or uploaded by hand.

*Alternatives:* separate build and deploy jobs passing the bundle as an artifact — cleaner separation, but the keystore would then be needed in the build job anyway and it doubles the setup time; not worth it for one app. Triggering on tags/releases instead of `main` — the user asked for push to `main`, and `main` already only moves on deliberate merges.

### 2. Upload with `r0adkll/upload-google-play`, pinned to a commit SHA

It is the de facto action for this, supports AAB, track, release status and service-account JSON, and is maintained. All actions are pinned to a full commit SHA with the version as a comment, since this job holds the signing key and Play credentials. The implementer resolves the SHAs of the current releases at implementation time.

*Alternatives:* Gradle Play Publisher plugin — adds a build dependency to the app and couples publishing to Gradle; fastlane `supply` — needs a Ruby toolchain. Both are heavier than one action step.

### 3. Authenticate with a service-account JSON key

A Google Cloud service account with the Google Play Android Developer API enabled, invited in the Play Console (Users and permissions) with release permissions for this app only. Its JSON key is the secret `PLAY_SERVICE_ACCOUNT_JSON`, passed to the action as `serviceAccountJsonPlainText`.

*Alternative:* Workload Identity Federation (no long-lived key). It is more secure but needs `google-github-actions/auth`, a workload identity pool and provider, and more Cloud setup that is easy to get wrong. The guide mentions it as a later hardening step; the key approach is documented first because it is what most people can set up without Cloud IAM experience.

### 4. Signing config read from environment variables, applied only when present

In `app/build.gradle.kts`:

```kotlin
val uploadStoreFile = System.getenv("LEAKOUT_UPLOAD_STORE_FILE")
signingConfigs {
    if (uploadStoreFile != null) create("upload") {
        storeFile = file(uploadStoreFile)
        storePassword = System.getenv("LEAKOUT_UPLOAD_STORE_PASSWORD")
        keyAlias = System.getenv("LEAKOUT_UPLOAD_KEY_ALIAS")
        keyPassword = System.getenv("LEAKOUT_UPLOAD_KEY_PASSWORD")
    }
}
buildTypes { release { signingConfigs.findByName("upload")?.let { signingConfig = it } } }
```

If `LEAKOUT_UPLOAD_STORE_FILE` is set but another of the three is missing, the build fails with a clear message rather than producing a half-configured signing attempt. Without it, `bundleRelease` produces an unsigned bundle as it does today, so developers and agents need no keystore. The CI job cannot silently ship an unsigned bundle because of the secret check (step 4) and the signature check (step 8).

*Alternatives:* a `keystore.properties` file — that is just another file to create on the runner from secrets; environment variables are simpler there and nothing extra is needed locally. Signing after the build with `jarsigner` in the workflow — works for AABs, but keeps signing outside Gradle so a local signed build would need different steps.

### 5. Version code = run number + offset, through a Gradle property

`defaultConfig.versionCode` reads `providers.gradleProperty("versionCode")`, parsed as a positive integer (an invalid value fails the build), falling back to the current literal `1`. The workflow passes `-PversionCode=$(( github.run_number + vars.VERSION_CODE_OFFSET ))` (offset defaults to `1`, so the first automated build is ≥ 2, above the manual first upload with code 1).

`run_number` only grows for this workflow and survives re-runs unchanged. A re-run after a *failed* upload reuses the number, which is fine because nothing was uploaded; a re-run after a *successful* upload is rejected by Play, which is the right outcome. If the workflow file is ever renamed or recreated (resetting `run_number`), or a bundle is uploaded by hand with a higher code, raising `VERSION_CODE_OFFSET` fixes it. The computed code is printed in the job summary.

*Alternatives:* committing a version bump back to `main` — the workflow would push to the production branch, which the project forbids; timestamp-based codes — exceed sensible ranges fast and are hard to read; querying Play for the highest code — extra API calls and tooling for little gain.

### 6. Secrets in a `play-store` environment, settings in repository variables

Secrets (environment `play-store`, deployment branches limited to `main`):

| Secret | Content |
| --- | --- |
| `PLAY_UPLOAD_KEYSTORE_BASE64` | the upload keystore (`.jks`), base64-encoded on one line |
| `PLAY_UPLOAD_KEYSTORE_PASSWORD` | keystore password |
| `PLAY_UPLOAD_KEY_ALIAS` | alias of the upload key in the keystore |
| `PLAY_UPLOAD_KEY_PASSWORD` | password of that key |
| `PLAY_SERVICE_ACCOUNT_JSON` | the service account's JSON key, as is |

Repository variables (non-secret):

| Variable | Default when unset | Meaning |
| --- | --- | --- |
| `PLAY_TRACK` | `internal` | Play track to upload to; a closed track's name to use a named private preview track instead |
| `PLAY_RELEASE_STATUS` | `completed` | `draft` while the app has never been published, then `completed` |
| `VERSION_CODE_OFFSET` | `1` | added to the run number |

An environment limited to `main` means a workflow on any other branch cannot read the secrets even if someone edits it there, and it allows adding required reviewers later as an approval gate. The defaults live in the workflow (`${{ vars.PLAY_TRACK || 'internal' }}`) so a fresh fork works with only the secrets set.

*Alternative:* repository-level secrets — readable by every workflow on every branch; rejected.

### 7. "Private preview test track" = internal testing by default

The Play Console's internal testing track is the private, invite-only track meant for fast distribution (up to 100 testers, available within minutes, no review). Because "private preview" could also mean a named closed testing track, the track is a variable rather than hard-coded; the guide explains both and how to switch.

### 8. The setup guide is `docs/deployment.md`, linked from `CLAUDE.md`

The guide is ordered so each step only needs the ones before it:

1. Overview: what the workflow does and a diagram of keys and accounts (upload key, app signing key, service account).
2. Prerequisites: Play developer account, repository admin access, JDK `keytool`.
3. Create the upload keystore (`keytool -genkeypair`, RSA 2048+, long validity, alias choice), where to store it and its passwords outside the repo (password manager), and back it up.
4. Create the app in the Play Console, set up Play App Signing, and do the first upload by hand (a locally signed `bundleRelease` with `versionCode` 1) to the internal track; fill in the minimum the console requires before testing.
5. Add testers to the internal track (email list) and share the opt-in link.
6. Google Cloud: create or pick a project, enable the Google Play Android Developer API, create a service account, create a JSON key.
7. Play Console: invite the service account under Users and permissions, grant it access to this app with release-to-testing-tracks permissions only.
8. GitHub: create the `play-store` environment with the `main` branch rule, add the five secrets (with exact commands to base64-encode the keystore on Windows PowerShell, macOS and Linux, without line breaks), add the three variables.
9. Run it: merge to `main` or use *Run workflow*, what a green run looks like, where to find the build in the Play Console.
10. Troubleshooting: the common Play API errors (version code already used, package not found, "only releases with status draft may be created on draft app", service account lacks permission, permission changes taking time to apply, wrong key / signature mismatch with the registered upload key) and what to do about each.
11. Maintenance: rotating the service-account key, resetting a lost upload key via Play support, changing the track, raising the version-code offset, and optionally moving to Workload Identity Federation.

`CLAUDE.md` gets a short "Releases" note pointing to the guide, so agents know `main` merges deploy and never need the keystore.

## Risks / Trade-offs

- [Signing material in GitHub] A leaked upload key lets someone upload builds only if they also have Play access; it can be reset by Google. → Environment-scoped secrets limited to `main`, actions pinned by SHA, keystore deleted after the build, the service account limited to this app and to testing tracks.
- [First run fails because the Play Console isn't ready] (app not created, no manual first upload, service account not yet granted). → The guide makes these explicit steps before the GitHub part, and the troubleshooting section maps each error to its fix.
- [Run number reset or manual uploads break version ordering] → `VERSION_CODE_OFFSET` variable; the computed code is shown in the job summary.
- [Runner lacks Android SDK 37 platform] `ubuntu-latest` has the SDK and accepted licenses, and AGP downloads a missing platform automatically. → If that fails, add an explicit `sdkmanager "platforms;android-37"` step; noted in tasks.
- [The workflow can't be fully tested before it is on `main`] `workflow_dispatch` and the trigger only exist once the file is on the default branch. → The YAML is linted with `actionlint`, the Gradle signing and version-code paths are verified locally with a throwaway keystore, and the first real run after merge is an explicit verification task.
- [Unit tests on `main` duplicate work already done on the feature branch] → A few minutes per deployment, and it guarantees nothing broken reaches testers.
- [Large bundle] About 61 MB of music assets; within Play's 200 MB base-module limit for AABs, but upload takes a little longer.

## Migration Plan

1. Merge the change into `development` (the workflow does nothing there).
2. Before merging `development` into `main`, follow `docs/deployment.md` steps 3–8 so the Play Console and secrets are ready. While the app is still a draft app, set `PLAY_RELEASE_STATUS=draft`.
3. Merge into `main`; the first run uploads automatically. Check the run and the internal track.
4. Once the app has been published to any track, set `PLAY_RELEASE_STATUS=completed`.

Rollback: disable the workflow in the Actions tab (or delete the file through `development` → `main`). A bad build on the internal track is replaced by uploading a newer one; nothing else in the app depends on this change.

## Open Questions

- Is "private preview" the internal testing track, or an existing named closed track? Handled by `PLAY_TRACK`; the default is `internal`.
- Does the app already exist in the Play Console, and has anything been uploaded (which sets the minimum version code)? The guide covers both cases with `VERSION_CODE_OFFSET`.
