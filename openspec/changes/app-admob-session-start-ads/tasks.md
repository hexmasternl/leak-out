## 1. Core rule

- [x] 1.1 Add `AdSessionGate` in `core/ads` (`shouldShow(fromMenu, adReady)`) as pure, immutable Kotlin
- [x] 1.2 Unit tests: menu start with ad ready shows; level-to-level never shows; no ad ready skips; Daily Pipe counts as a menu start

## 2. AdMob setup (maintainer, manual)

- [ ] 2.1 Create the AdMob app (`nl.hexmaster.leakout`) and an interstitial ad unit; copy the app ID and ad unit ID
- [ ] 2.2 Link the app to its Play listing in AdMob and set up the GDPR message under Privacy & messaging
- [ ] 2.3 (optional) Override the release IDs with `LEAKOUT_ADMOB_APP_ID` and `LEAKOUT_ADMOB_INTERSTITIAL_ID`; not needed, the build file holds the Leak Out IDs

## 3. Build and manifest

- [x] 3.1 Add `play-services-ads` to `libs.versions.toml` with an exact version and reference it from `app/build.gradle.kts`; check transitive fragment/lifecycle versions
- [x] 3.2 Add the `APPLICATION_ID` meta-data (`${admobAppId}`) and the `AD_ID` permission to `AndroidManifest.xml`
- [x] 3.3 Per build type: debug uses Google's test IDs; release uses the Leak Out IDs (overridable by `LEAKOUT_ADMOB_*`); expose the ad unit ID as a `BuildConfig` field
- [x] 3.4 No workflow change needed: the release IDs are defaults in the build file

## 4. App code

- [x] 4.1 Add `AdProvider` (`preload`, `showIfReady(activity, onFinished)`), `AdMobAdProvider` and a fake provider
- [x] 4.2 Add `ConsentManager` (UMP request, form if required, `canRequestAds`, privacy options status and form) and initialise `MobileAds` on a background thread once allowed, then preload
- [x] 4.3 In `MainActivity` ask the gate and provider before a menu-started level (Continue, Select level) and the Daily Pipe (button and reminder) open; keep `onNextLevel` and designer test play ad-free
- [x] 4.4 Add the "Privacy options" row to Settings, shown only when required, with strings in every locale

## 5. Tests

- [x] 5.1 Unit tests for the session start flow with a fake provider, the build and manifest wiring, and the gate
- [x] 5.2 Instrumented test for the Privacy options row (shown and opening the form when required, absent otherwise)

## 6. Website, docs and release

- [x] 6.1 Add `website/src/static/app-ads.txt` with the AdMob publisher line and smoke-test it in the website deploy
- [x] 6.2 Document the AdMob setup, app verification, the `LEAKOUT_ADMOB_*` variables and ad testing rules in `docs/deployment.md`
- [ ] 6.3 Update the privacy policy, the Play Console Ads declaration and the Data safety form (maintainer)
- [ ] 6.4 After the website deploys, check `https://leakout.hexmaster.nl/app-ads.txt` and that the Play listing website is that domain (maintainer)

## 7. Verification

- [ ] 7.1 Run `:core:test`, `:app:testDebugUnitTest` and the affected instrumented tests (JVM tests pass; instrumented tests not run yet)
- [ ] 7.2 On a device with a debug build: test ad on Play and Daily Pipe, none between levels, again after the menu, game normal in airplane mode
