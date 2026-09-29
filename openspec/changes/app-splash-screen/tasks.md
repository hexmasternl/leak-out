## 1. Prerequisites and dependencies

- [x] 1.1 Confirm the `app-welcome-screen` scaffolding exists (`app` module, `MainActivity`, `LeakOutTheme`); if not, apply that change first
- [x] 1.2 Add `androidx.core:core-splashscreen` to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [x] 1.3 Bundle `wendy_one_regular.ttf` (OFL) in `app/src/main/res/font/` if it is not already present
- [x] 1.4 Make sure the Deep color tokens `surfaceDeep`, `liquidGlow`, `liquid300`, `liquid500` and `liquid700` exist in `ui/theme` with the design-system hex values

## 2. System splash

- [ ] 2.1 Add `surface_deep` (`#0C1A2A`) to `res/values/colors.xml`
- [ ] 2.2 Add a transparent vector drawable `ic_splash_blank.xml` for the splash icon
- [ ] 2.3 Define `Theme.LeakOut.Splash` (parent `Theme.SplashScreen`) with `windowSplashScreenBackground=@color/surface_deep`, `windowSplashScreenAnimatedIcon=@drawable/ic_splash_blank`, `windowSplashScreenIconBackgroundColor=@color/surface_deep` and `postSplashScreenTheme` set to the app theme
- [ ] 2.4 Set the app theme's `android:windowBackground` to `@color/surface_deep`
- [ ] 2.5 Set `android:theme="@style/Theme.LeakOut.Splash"` on `MainActivity` in `AndroidManifest.xml`
- [ ] 2.6 Call `installSplashScreen()` in `MainActivity.onCreate` before `super.onCreate`

## 3. Liquid wordmark

- [ ] 3.1 Implement `ui/splash/LiquidWordmark.kt`: a single `Text` with centred lines, Wendy One, a vertical gradient `Brush` (stops 0 / 0.35 / 0.70 / 1.0 → glow / 300 / 500 / 700) and a `Shadow` glow (`liquid500` at 35 %, 6 dp offset, 24 dp blur)
- [ ] 3.2 Size the text from `BoxWithConstraints` (`min(h*0.30, w*0.22)`, at least 56 sp) so the system font scale does not change it
- [ ] 3.3 Compare against `docs/design-system.html` on the emulator; if the glow is too weak, add the blurred-copy fallback (`Modifier.blur` on API 31+)

## 4. In-app splash screen and transition

- [ ] 4.1 Implement `ui/splash/SplashScreen.kt`: a full-size `surface.deep` background (Deep palette forced) with `LiquidWordmark("LEAK\nOUT")` centred both ways
- [ ] 4.2 In `MainActivity`, call `enableEdgeToEdge` with dark, transparent status and navigation bar styles
- [ ] 4.3 Add splash → welcome state held with `rememberSaveable`, a `LaunchedEffect` enforcing the 1 200 ms minimum, and a 320 ms `Crossfade` into `WelcomeScreen`
- [ ] 4.4 Reduced motion: detect `ANIMATOR_DURATION_SCALE == 0` and use `snap()` instead of the fade, keeping the static glow
- [ ] 4.5 Add `@Preview`s at 800×360 dp and 640×320 dp, plus one with font scale 1.3

## 5. Tests and verification

- [ ] 5.1 JVM unit test for the minimum-duration logic (remaining delay = `max(0, 1200 − elapsed)`), extracted to a small pure function
- [ ] 5.2 Compose UI test: the splash shows "LEAK" and "OUT" (as one centred text node), then the welcome screen appears after the minimum duration (using the test clock)
- [ ] 5.3 Compose UI test: after recreating the activity once the welcome screen is shown, the splash is not shown again
- [ ] 5.4 Compose UI test: with animations disabled, the welcome screen replaces the splash without a cross-fade
- [ ] 5.5 Manual check on API 31+ and API 26–30 emulators: a dark `#0C1A2A` launch with no icon, no white flash, a seamless handoff to the wordmark, and dark system bars
- [ ] 5.6 Manual check with the Daylight theme selected: the splash still uses the Deep palette
