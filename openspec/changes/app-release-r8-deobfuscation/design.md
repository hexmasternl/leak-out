# Design

## Context

The release build type in `app/src/app/build.gradle.kts` sets `isMinifyEnabled = false` and already points at `proguard-android-optimize.txt` and an empty `proguard-rules.pro`. The deploy workflow (`.github/workflows/deploy-play-internal.yml`) builds `:app:bundleRelease` and uploads the bundle with `r0adkll/upload-google-play`, which accepts a `mappingFile` input. The Play Console warns about a missing deobfuscation file on every release (#130). With minification off the warning has no effect: stack traces are already readable.

## Goals / Non-Goals

**Goals:**
- A smaller release bundle through R8 code and resource shrinking.
- Readable crash and ANR stack traces in the Play Console for minified releases.
- No Play Console warning about a missing deobfuscation file.

**Non-Goals:**
- Native debug symbols: the app has no native code of its own.
- A crash reporting service other than the Play Console.
- Shrinking debug builds.

## Decisions

### 1. Turn R8 on rather than silence the warning
The alternative is to leave minification off and ignore the warning, which costs nothing. R8 is the Android default for release builds and the app pulls in large libraries (Compose, Room, Play Games), so the size and start-up gain is worth the one-off effort of keep rules. See Open Questions: the maintainer may still choose the do-nothing option.

### 2. Upload the mapping file with the bundle in the same step
`r0adkll/upload-google-play` takes `mappingFile` and attaches it to the uploaded version code. Doing it in the same step keeps the mapping tied to exactly the bundle it belongs to. The mapping is also kept as a workflow artifact so a crash can be retraced locally.

### 3. Keep rules only where a test shows they are needed
Libraries ship their own consumer rules (Room, Compose, Play Games, kotlinx.serialization's generated serializers). `proguard-rules.pro` only gets rules that a failing test on the minified build proves necessary, each with a comment saying why.

## Risks / Trade-offs

- [Reflection breaks at runtime only in release builds] → Run the instrumented tests against a minified build before finishing; smoke-test level loading, the level history (Room), settings (DataStore), score posting and Play Games sign-in.
- [Stack traces from minified builds are unreadable without the mapping] → The workflow fails if `mapping.txt` is missing, so no minified bundle is uploaded without its mapping.

## Open Questions

- Do you want R8 enabled at all, or would you rather keep the release unminified and ignore the warning (and close #130 as answered)?
