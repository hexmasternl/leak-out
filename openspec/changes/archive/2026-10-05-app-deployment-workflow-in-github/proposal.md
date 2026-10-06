## Why

Getting a build of Leak Out to testers is a manual job today: someone has to bump the version, build a signed bundle on their own machine with a keystore that lives on that machine, and upload it through the Play Console. That is slow, easy to get wrong (a forgotten `versionCode` bump is rejected by Play) and ties releases to one person's laptop. `main` is already the production branch, so every merge into `main` should put a new build in the hands of the private testers automatically.

## What Changes

- **Deployment workflow**: a new GitHub Actions workflow, `.github/workflows/deploy-play-internal.yml`, runs on every push to `main` (and can be started by hand with `workflow_dispatch`). It runs the JVM unit tests, builds a signed release Android App Bundle (`.aab`) and uploads it to the Play Console's private testing track (the **internal testing** track by default). It never runs for pushes to `development` or feature branches, and only one deployment runs at a time.
- **Release signing in Gradle**: `app/build.gradle.kts` gets a `release` signing config that reads the upload keystore path, store password, key alias and key password from environment variables. When they are not set (a developer's machine), the release build stays unsigned exactly as today; the CI build fails instead of producing an unsigned bundle.
- **Automatic version code**: `versionCode` can be overridden with the Gradle property `-PversionCode=<n>`. The workflow sets it from the GitHub run number plus a configurable offset, so every upload has a higher version code than the last. Without the property the build keeps the version code in `build.gradle.kts`. `versionName` stays maintained by hand.
- **Secrets and variables**: the upload keystore (base64-encoded), its passwords and alias, and a Google Cloud service-account JSON key are stored as GitHub Actions secrets in a `play-store` environment. The target track, release status and version-code offset are repository variables, so they can change without editing the workflow.
- **Setup guide**: a new document, `docs/deployment.md`, describes step by step how to set everything up: creating the app in the Play Console and doing the one-time first manual upload, Play App Signing and the upload key, creating the upload keystore with `keytool`, creating the Google Cloud project and service account and granting it access in the Play Console, adding testers to the internal track, adding every GitHub secret and variable, running and troubleshooting the workflow, and rotating keys.

## Capabilities

### New Capabilities
- `app-deployment`: when and how a release is built and published to the Play Console's private testing track — the trigger, the checks before upload, signing, version codes, the secrets and variables the workflow needs, and the setup guide that documents them.

### Modified Capabilities
<!-- None: no existing spec covers building or publishing the app. -->

## Impact

- New files: `.github/workflows/deploy-play-internal.yml`, `docs/deployment.md`.
- `app/build.gradle.kts`: release signing config from environment variables and an optional `versionCode` Gradle property. Debug builds and local release builds behave as before.
- GitHub repository settings: a `play-store` environment with five secrets and three repository variables (set up by hand by following the guide; nothing is committed).
- External systems: the Google Play Console (app, internal testing track, API access for a service account) and a Google Cloud project with the Google Play Android Developer API enabled.
- Dependencies: no new app dependencies. The workflow uses pinned versions of `actions/checkout`, `actions/setup-java`, `gradle/actions/setup-gradle`, `actions/upload-artifact` and `r0adkll/upload-google-play`.
- No changes to `core` or to the game's behaviour.
