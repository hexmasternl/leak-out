## 1. Welcome screen label

- [ ] 1.1 Add `versionLabel: String?` and `onVersionLabelShown: () -> Unit` parameters to `WelcomeScreen`; render the label bottom-right inside the safe area, non-interactive, in the design system's caption style
- [ ] 1.2 Keep the label fully visible for 5000 ms, then fade alpha to 0 over 300 ms (160 ms with reduced motion) and remove it from composition
- [ ] 1.3 Compose UI tests with the test clock: text "v1.4.0 (57)" shown at start, still visible at 4.9 s, gone after 5.3 s, buttons still clickable while it shows, `null` shows nothing, reduced motion fades in 160 ms

## 2. Host wiring

- [ ] 2.1 In `MainActivity`, format `AppInfo` as "v<versionName> (<versionCode>)" and pass it while a process-lifetime `versionLabelShown` flag is false; set the flag via `onVersionLabelShown` when the label first shows
- [ ] 2.2 Unit test the formatting; UI test that returning to the welcome screen from Settings does not show the label again

## 3. Docs

- [ ] 3.1 Add the version label (position, type, colour, 5 s hold, 300 ms fade, reduced-motion 160 ms) to `docs/design-system.md`

## 4. Verification

- [ ] 4.1 Run `:app:testDebugUnitTest` and the affected instrumented tests (`WelcomeScreenTest`, `PseudoLocaleLayoutTest`)
