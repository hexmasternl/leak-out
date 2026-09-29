## 1. Resolver

- [ ] 1.1 Add `NextLevelResolver` (pure Kotlin) returning `NextLevel(levelId, page, isUnlocked)` from completed history, total level count, page size and the level-library unlock rule
- [ ] 1.2 Unit tests: fresh install, linear progress, gaps in history, page boundary (24 to 25), all completed, next level locked by star gate

## 2. Dialog

- [ ] 2.1 Create `PlayMenuDialog` in `ui/title/` with "Continue" and "Select level" cartoon buttons per the design system; dismissible via outside tap and back
- [ ] 2.2 Compose UI tests: dialog content, dismiss, each button invokes its callback once

## 3. Welcome screen wiring

- [ ] 3.1 Change `WelcomeScreen` Play to show the dialog (local `showPlayMenu` state) instead of emitting a navigation event
- [ ] 3.2 Replace host callbacks with `onStartLevel(levelId)` and `onOpenLevelLibrary(page)`; on Continue or Select level resolve the next level from a history snapshot, close the dialog, and call the callback
- [ ] 3.3 Continue falls back to `onOpenLevelLibrary` when the next level is locked

## 4. Level library entry

- [ ] 4.1 Add optional `initialPage` argument to the level library screen/view model, clamped to accessible pages; the default keeps the existing "useful page" rule
- [ ] 4.2 Test that opening from the dialog lands on the page of the next level (highest completed 30 gives page 2, 24 gives page 2, none gives page 1)

## 5. Verification

- [ ] 5.1 Manual check on a device or emulator in landscape: fresh install, mid-progress, page boundary, all levels done
