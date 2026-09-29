## 1. Project scaffolding

- [x] 1.1 Create the Android `app` module (Gradle Kotlin DSL, version catalog, Compose + Material 3, min SDK 26) if not present
- [x] 1.2 Add `MainActivity` and `AndroidManifest.xml` with launcher intent filter

## 2. Landscape orientation

- [x] 2.1 Set `android:screenOrientation="sensorLandscape"` on `MainActivity` in the manifest
- [ ] 2.2 Verify on device/emulator: launches landscape while held in portrait, never rotates to portrait, flips between both landscape sides

## 3. Theme and components

- [x] 3.1 Add design-system color tokens (`ui/theme/Color.kt`)
- [x] 3.2 Bundle Fredoka font in `res/font/` and define typography styles (Title/Hero, Button text) in `ui/theme/Type.kt`
- [x] 3.3 Implement `LeakOutTheme`
- [x] 3.4 Implement `CartoonButton` (ink border, 6dp bottom shadow, inner shine, pressed state translating down 4dp, min 48dp touch target) with primary / secondary / danger variants

## 4. Welcome screen

- [x] 4.1 Implement `WelcomeBackground` composable showing the prepared `assets/welcome` illustration (bundled as a downscaled drawable, `ContentScale.Crop`) so it fills any landscape size
- [x] 4.2 Implement title composable: "LEAK OUT" large, centered, outlined with drop shadow, scaled to available size
- [x] 4.3 Implement `WelcomeScreen(onPlay, onSettings, onExit)` with title centered and Play / Settings / Exit buttons below in that order
- [x] 4.4 Handle short landscape heights via `BoxWithConstraints` scaling and safe-drawing insets/cutouts
- [ ] 4.5 Enable edge-to-edge / immersive mode in `MainActivity`
- [x] 4.6 Add `@Preview`s for a standard (e.g. 800×360dp) and small (640×320dp) landscape size

## 5. Wiring

- [ ] 5.1 Set `WelcomeScreen` as the content of `MainActivity`
- [ ] 5.2 Exit calls `finishAffinity()`; Play and Settings are stubbed callbacks (TODO for future screens)

## 6. Tests and docs

- [ ] 6.1 Compose UI test: Play, Settings, Exit are displayed and each click invokes exactly its callback once
- [ ] 6.2 Compose UI test: title text is displayed
- [ ] 6.3 Update `docs/project-plan.md` orientation statements (sections 1.2 and 1.11) to landscape
