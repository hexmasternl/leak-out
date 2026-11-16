## Why

Leak Out is free and has no income. Showing one interstitial ad when a play session starts, and never in the middle of a run of levels, pays for the app without interrupting the puzzle flow.

Tracked in [#176](https://github.com/hexmasternl/leak-out/issues/176).

## What Changes

- Google AdMob is added to the app. One full-screen interstitial ad is shown when a game session starts from the menu: after "Continue" or a level pick in "Select level", and when the Daily Pipe opens (button or tapped reminder).
- A session is the stretch from entering a level from the menu until the player returns to the menu. Moving from level to level inside a session shows **no** ad, so the ad appears once, at the start only.
- An ad never blocks the player: if none is loaded or it fails to show, the level starts at once. The next ad is preloaded in the background.
- Ads are requested only when the Google UMP consent flow allows it. The Settings screen gets a "Privacy options" entry where the rules require one.
- Debug builds always use Google's test ad IDs. Release builds read the real AdMob app ID and ad unit ID from build variables (GitHub secrets in CI) and fall back to the test IDs when they are missing.
- The decision "show an ad now?" is a pure Kotlin rule in `core`; the AdMob SDK sits behind an `AdProvider` interface in `app`, so the UI and tests use a fake.
- The website serves `app-ads.txt` so AdMob can verify the app, and the deployment docs describe linking the app to its Play listing and the new variables.
- No ads in the level designer's test play, on other screens, or between levels. No banner, rewarded or app-open ads.

## Capabilities

### New Capabilities

- `session-start-ads`: when the interstitial is shown (session start only), when it is skipped, preloading, test versus real ad IDs.
- `ad-consent`: UMP consent before any ad request and the privacy options entry point.

### Modified Capabilities

- `settings-screen`: adds the "Privacy options" row, shown only when consent rules require it.

## Impact

- `core/ads/AdSessionGate.kt` (new) and its unit tests.
- `app/ads/` (new): `AdProvider`, `AdMobAdProvider`, `ConsentManager`, fake for tests.
- `app/MainActivity.kt`: asks the gate before a menu-started level or Daily Pipe opens; consent and SDK initialisation run at start.
- `app/ui/settings/SettingsScreen.kt`: "Privacy options" row.
- `app/src/gradle/libs.versions.toml`, `app/build.gradle.kts`, `AndroidManifest.xml`: `play-services-ads` (brings in UMP), AdMob app ID placeholder, `AD_ID` permission, per-build-type IDs.
- `.github/workflows/deploy-play-internal.yml`, `docs/deployment.md`: new `LEAKOUT_ADMOB_*` variables, AdMob setup and app verification steps.
- `website/src/static/app-ads.txt` (new): AdMob verification file.
- Privacy policy and Play Console (Ads declaration, Data safety): updated by the maintainer before the release that ships this.
