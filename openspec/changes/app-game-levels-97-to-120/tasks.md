## 1. Tests first

- [ ] 1.1 Extend `BundledLevelsTest`: 120 levels with ids 1 to 120; levels 97 to 120 have 6–12 columns, 5–8 rows, an initial delay of 28000–32000 ms, speed ramp 0, reservoir factor 0.1, and a base speed from 0.5 to 0.9 that never decreases
- [ ] 1.2 Add the doability check for levels 97 to 120 (shortest route `L` ≤ 30 and `delay + L / speed ≥ 1.5 × L`), using the solver's shortest route
- [ ] 1.3 Keep the portal rule: fewer than half of levels 97 to 120 have portals

## 2. Levels

- [ ] 2.1 Design levels 97 to 104 (0.5 to about 0.62 cells per second), stars from free cells
- [ ] 2.2 Design levels 105 to 112 (about 0.64 to 0.76)
- [ ] 2.3 Design levels 113 to 120 (about 0.78 to 0.9)
- [ ] 2.4 Run `LevelSolvabilityTest`, `BundledLevelsTest` and the headless tester until every level passes

## 3. Release

- [ ] 3.1 Mention the 24 new levels in the Play release notes

## 4. Verification

- [ ] 4.1 Run `:core:test` and `:app:testDebugUnitTest`
- [ ] 4.2 Play a sample of the new levels (at least 97, 108 and 120) on a device or emulator and check they are doable
