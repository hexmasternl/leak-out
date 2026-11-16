## 1. Core rule

- [ ] 1.1 Add `AdSessionGate` in `core/ads` (`shouldShow(fromMenu, adReady)`) as pure, immutable Kotlin
- [ ] 1.2 Unit tests: menu start with ad ready shows; level-to-level never shows; no ad ready skips; Daily Pipe counts as a menu start

## 2. AdMob setup (maintainer, manual)

- [ ] 2.1 Create the AdMob app (`nl.hexmaster.leakout`) and an interstitial ad unit; copy the app ID and ad unit ID
- [ ] 2.2 Link the app to its Play listing in AdMob and set up the GDPR message under Privacy & messaging
- [ ] 2.3 Add GitHub secrets/variables `LEAKOUT_ADMOB_APP_ID` and `LEAKOUT_ADMOB_INTERSTITIAL_ID`

## 3. Build and manifest

- [ ] 3.1 Add `play-services-ads` to `libs.versions.toml` with an exact version and reference it from `app/build.gradle.kts`; check transitive fragment/lifecycle versions
- [ ] 3.2 Add the `APPLICATION_ID` meta-data (`${admobAppId}`) and the `AD_ID` permission to `AndroidManifest.xml`
- [ ] 3.3 Per build type: debug uses Google's test IDs; release reads `LEAKOUT_ADMOB_*` and falls back to test IDs; expose the ad unit ID as a `BuildConfig` field
- [ ] 3.4 Pass the new variables in `.github/workflows/deploy-play-internal.yml`

## 4. App code

- [ ] 4.1 Add `AdProvider` (`preload`, `showIfReady(activity, onFinished)`), `AdMobAdProvider` and a fake provider
- [ ] 4.2 Add `ConsentManager` (UMP request, form if required, `canRequestAds`, privacy options status and form) and initialise `MobileAds` on a background thread once allowed, then preload
- [ ] 4.3 In `MainActivity` ask the gate and provider before a menu-started level (Continue, Select level) and the Daily Pipe (button and reminder) open; keep `onNextLevel` and designer test play ad-free
- [ ] 4.4 Add the "Privacy options" row to Settings, shown only when required, with strings in every locale

## 5. Tests

- [ ] 5.1 Unit tests for the `ConsentManager` decisions and the start flow with the fake provider
- [ ] 5.2 Instrumented tests with the fake provider: ad on Continue, Select level and Daily Pipe; no ad on next level or restart; level opens at once when no ad is ready; Privacy options row visibility

## 6. Website, docs and release

- [ ] 6.1 Add `website/src/static/app-ads.txt` with the AdMob publisher line and confirm the website serves it at the domain root
- [ ] 6.2 Document the AdMob setup, app verification, the `LEAKOUT_ADMOB_*` variables and ad testing rules in `docs/deployment.md`
- [ ] 6.3 Update the privacy policy, the Play Console Ads declaration and the Data safety form (maintainer)

## 7. Verification

- [ ] 7.1 Run `:core:test`, `:app:testDebugUnitTest` and the affected instrumented tests
- [ ] 7.2 On a device with a debug build: test ad on Play and Daily Pipe, none between levels, again after the menu, game normal in airplane mode
