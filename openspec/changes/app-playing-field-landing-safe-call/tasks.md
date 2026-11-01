## 1. Fix

- [ ] 1.1 In `app/src/main/kotlin/nl/hexmaster/leakout/ui/game/PlayingField.kt`, replace `landing?.at` with `landing.at` in the landing pop condition of the cell drawing loop

## 2. Verification

- [ ] 2.1 Run `./gradlew :app:compileReleaseKotlin` and confirm the "Unnecessary safe call on a non-null receiver of type 'Landing'" warning is gone
- [ ] 2.2 Run `./gradlew :app:testDebugUnitTest` and confirm it passes
- [ ] 2.3 Run the affected instrumented tests for the playing field and placement (or skip device runs if the maintainer waives them)
