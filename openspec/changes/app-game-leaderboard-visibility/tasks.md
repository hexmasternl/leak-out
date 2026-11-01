## 1. Core

- [ ] 1.1 Expose the point values (per section type, the cross second pass, the unused-section penalty) from the `core` scoring rules as one read-only model
- [ ] 1.2 Unit test that the model matches the points the scoring code awards

## 2. Scoring guide

- [ ] 2.1 Add `ScoringGuideDialog` (Deep Flow dialog, scrolling content, Close always visible) formatting the values from 1.1; mention the Top 10 only when it is available
- [ ] 2.2 Add translatable strings in every supported language
- [ ] 2.3 Add a Scoring button to the pause dialog that opens the guide while the game stays paused
- [ ] 2.4 Store a "scoring guide seen" flag in the settings DataStore; show the guide once after the first library win's tally, never for a test play
- [ ] 2.5 Compose UI tests: content, open and close from the pause dialog, Back, first win shows it once, flag survives an app restart, 640 × 360 dp at 130 % font

## 3. Level complete dialog

- [ ] 3.1 Add the `around-me` call to the score server client (with the single re-sign-in on 401) and unit test it
- [ ] 3.2 After the win's score was accepted, load the place and show "You are #n on level x"; leave it out while loading and on failure
- [ ] 3.3 Add the Top 10 invitation line and the highlighted Top 10 button when the top 10 is available
- [ ] 3.4 Add the "How scoring works" help button next to the score rows
- [ ] 3.5 Compose UI tests: place shown, left out offline and in a test play, invitation only with the top 10, help button opens the guide, dialog still fits 640 × 360 dp

## 4. Docs

- [ ] 4.1 Add the scoring guide dialog, the place line and the invitation to `docs/design-system.md`

## 5. Verification

- [ ] 5.1 Run `:core:test`, `:app:testDebugUnitTest` and the affected instrumented tests
