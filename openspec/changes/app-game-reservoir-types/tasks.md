## 1. Reservoir types in core

- [ ] 1.1 Add `ReservoirType` (`RED_10`, `BLUE_20`, `PURPLE_30`, `ORANGE_40`) with its factor and a lookup from a factor in `core/level`
- [ ] 1.2 Restrict `flow.reservoirSpeedFactor` in `LevelValidator` to 0.1, 0.2, 0.3 and 0.4 (tolerance 1e-9); the error names the field and the allowed values
- [ ] 1.3 Add snapping to the nearest type (ties to the higher factor) in `LevelDraft`
- [ ] 1.4 Tests: `LevelValidatorTest` (each type accepted; 0, 0.25, 0.5 and 1.2 rejected), `LevelDraftTest` (snapping), `ReservoirType` lookup

## 2. Rendering

- [ ] 2.1 Add the red, blue and orange reservoir tokens (300/500/700) to `docs/design-system.md` and the app's colour definitions; document that violet is the 30 % type
- [ ] 2.2 Tint the reservoir in `PipeArt` from the level's `ReservoirType`, on the playing field and in the stack
- [ ] 2.3 Show the percentage on the "slow" badge (string resource, all supported languages)
- [ ] 2.4 Unit tests for the colour mapping and the badge text; check the four types side by side in the pipe lab

## 3. Level designer

- [ ] 3.1 Replace the reservoir speed factor field with a four-way type selector
- [ ] 3.2 Snap a restored design to the nearest type
- [ ] 3.3 Update `LevelDesignerViewModelTest`

## 4. Bundled levels

- [ ] 4.1 Set `flow.reservoirSpeedFactor` per bundled level from its base speed: below 0.2 → 0.4, 0.2 to below 0.3 → 0.3 (left out), 0.3 to below 0.37 → 0.2, from 0.37 → 0.1
- [ ] 4.2 Add a `BundledLevelsTest` check that every bundled level uses an allowed type and that no faster level uses a higher factor than a slower one
- [ ] 4.3 Run `LevelSolvabilityTest` and `BundledLevelsTest`

## 5. Docs

- [ ] 5.1 Document the four reservoir types in `docs/level-format.md`

## 6. Verification

- [ ] 6.1 Run `:core:test` and `:app:testDebugUnitTest`
- [ ] 6.2 Play a level of each reservoir type on a device or emulator and check the colours and badges
