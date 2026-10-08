# Proposal

## Why

Every upload to the Play Console shows the warning "There is no deobfuscation file associated with this App Bundle". Today the release build does not run R8 (`isMinifyEnabled = false`), so there is nothing to deobfuscate and the warning is harmless. But it also means the release bundle ships every unused class and method of Compose, Room, kotlinx.serialization and Play Games, which makes the download larger and the app slower to start than it needs to be. Turning R8 on and uploading its mapping file with each bundle gives a smaller, faster app, keeps crash and ANR stack traces readable in the Play Console, and makes the warning go away.

Tracked in [#130](https://github.com/hexmasternl/leak-out/issues/130).

## What Changes

- The release build type enables R8 code shrinking and obfuscation (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`).
- `proguard-rules.pro` gets the keep rules the app needs, each proven by a failing test on the minified build.
- The deploy workflow uploads `app/src/app/build/outputs/mapping/release/mapping.txt` together with the bundle (the `mappingFile` input of the upload action) and keeps it as a workflow artifact next to the bundle.
- `docs/deployment.md` explains the mapping file and the warning.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `app-deployment`: the release bundle is minified with R8 and every upload carries its deobfuscation (mapping) file.

## Impact

- `app/src/app/build.gradle.kts` (release build type), `app/src/app/proguard-rules.pro`.
- `.github/workflows/deploy-play-internal.yml` (upload step, workflow artifact).
- `docs/deployment.md`.
- Risk of runtime breakage in code reached through reflection (serialization, Room, Play Games). Covered by running the instrumented tests against a minified build before the change is done.
