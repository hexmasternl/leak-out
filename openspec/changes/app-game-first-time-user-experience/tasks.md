# Tasks

Tracking issue: [#36 First time user experience](https://github.com/hexmasternl/leak-out/issues/36).

## 1. Design system proposal (gate)

- [x] 1.1 Add a "Tip overlay" component to `docs/design-system.md` §5 (scrim with spotlight cut-out, glow ring, callout card with pointer, step counter, Next / Skip tour / Let's go / Got it buttons) and its entry, step change and drag hint motion to §7.2 with reduced-motion variants in §7.4; verify every token named exists in §2–§4
- [x] 1.2 Write the tip texts (5 intro steps, cross, reservoir, mandatory) into the design system section; verify they state the facts in the `game-play-onboarding` spec (10/50 points, 25 points, reservoir percentage, left–right only, mandatory loss rule)
- [x] 1.3 Build static Compose `@Preview` mock-ups (start step, placing step with hint, cross tip on the stack) and attach screenshots to issue #36; verify the previews render
- [ ] 1.4 Get the product owner's approval on issue #36 and record requested changes in the design system and spec; verify approval is noted on the issue before starting group 3

## 2. Seen tips storage

- [ ] 2.1 Add `TipId` with stable keys and `seenTips`, `markTipSeen` and `resetTips` to `SettingsRepository`, `InMemorySettingsRepository` and `DataStoreSettingsRepository` (string set key, unknown keys ignored); verify unit tests for first launch, mark seen, mark twice, reset, unknown key, and that reset leaves settings alone pass with `./gradlew :app:testDebugUnitTest`
- [ ] 2.2 Verify that clearing the level history leaves the seen tips unchanged and that resetting tips leaves the history unchanged with an instrumented repository test

## 3. Pure tip scheduling

- [ ] 3.1 Add `TipScheduler.dueTips` with the fixed order and the level-start / after-placement triggers; verify unit tests for first level (any id), tour seen, `X` in starting stack, `X` drawn later, `RES`, everything due at once, nothing due while not playing
- [ ] 3.4 Make the cross and reservoir tips depend on `fullyVisibleSlots` and add `tipSlot`; verify unit tests for an `X` / `RES` in a cut-off slot (not due), the same section after it moves into a visible slot (due, that slot highlighted), a starting stack with the section only in cut-off slots, and the lowest visible slot chosen when several match
- [ ] 3.2 Add `TipText.percent` and `TipScheduler.hintCell`; verify unit tests for 0.3→30 %, 0.25→25 %, 0.333→33 % and the hint cell next to the start exit, skipping blocked cells
- [ ] 3.3 Add callout placement (side with most room, kept on screen); verify unit tests for targets at the left stack, a centre cell and a bottom-right cell on a 640 × 360 dp screen

## 4. Overlay and targets

- [ ] 4.1 Add `TipTargets` and register the stack column, stack slots, and start, finish and mandatory cells from `StackPanel` and `PlayingField`, and derive `fullyVisibleSlots` from the slot rects and the panel's window-clipped bounds; verify an instrumented test that the registered start cell rect matches the start tile's bounds and that, on a screen too short for the whole stack, the cut-off top slots are not in `fullyVisibleSlots`
- [ ] 4.2 Add `FieldViewport.revealCell` within the pan limits and restore the level-start view after the tour; verify instrumented tests for the finish step on a 16 × 12 level and the view after the tour
- [ ] 4.3 Build the `TipOverlay` composable per the approved design (scrim cut-out, callout, buttons, step counter, drag hint, reduced motion) consuming all other input; verify instrumented tests that no drag, pinch or pan reaches the game while it is shown and that the static arrow shows with animations off

## 5. Game wiring

- [ ] 5.1 Freeze via `Pause` for tips in `GameSession`, keep the pause dialog hidden for tip pauses, and resume after the last due tip; verify unit tests on `GameSession` and the instrumented "countdown stops during a tip" and "no pause dialog for a tip" scenarios
- [ ] 5.2 Evaluate `dueTips` at level start and after each accepted placement in `GameScreen`, re-evaluate when `fullyVisibleSlots` changes, show tips in order and mark each seen on close; verify instrumented tests for the first level being level 4, the tour not shown again, restart, cross and reservoir tips appearing once, and on a short screen a cross drawn onto the cut-off top of the stack only showing its tip after the placement that brings it fully into view
- [ ] 5.3 Implement Next / Skip tour / Let's go / Got it and Back behaviour; verify instrumented tests for finishing, skipping, Back skipping the tour, Back as Got it, and the liquid continuing where it froze
- [ ] 5.4 Handle backgrounding during a tip (pause dialog over the tip, Resume keeps the tip and freeze); verify the instrumented "coming back to the app during a tip" scenario and that the existing pause tests still pass
- [ ] 5.5 Add content descriptions and live-region announcements for tips; verify the instrumented screen reader scenario ("Step 1 of 5", buttons in order)

## 6. Settings

- [ ] 6.1 Add the "Show tips again" button and "Tips will show again when you play" text to the Progress panel and `SettingsViewModel`; verify instrumented tests for reset, disabled when nothing seen, accessibility, and that a level started afterwards shows the intro tour

## 7. Mandatory section tip (after `app-game-play-mandatory-sections` is on `development`)

- [ ] 7.1 Return the mandatory tip from `dueTips` for a level with mandatory sections and register their cells as targets; verify unit tests for the order with mandatory sections and instrumented tests that every mandatory cell is highlighted and the tip shows once

## 8. Integration

- [ ] 8.1 Run `./gradlew :core:test :app:testDebugUnitTest` and the affected game screen, pause, settings and onboarding instrumented tests on the emulator; verify all pass and post screenshots of each tip on issue #36
