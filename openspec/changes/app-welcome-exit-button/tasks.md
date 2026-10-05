## 1. Preference

- [x] 1.1 Add `skipExitConfirmation: Flow<Boolean>` and `setSkipExitConfirmation(Boolean)` to the settings repository, backed by a DataStore Preferences key `skip_exit_confirmation` (default `false`)
- [x] 1.2 Unit tests: default false, set true persists, value survives recreating the DataStore

## 2. Dialog

- [x] 2.1 Create `ExitConfirmationDialog` in `ui/title/` with question text, "Never show this again" checkbox (unchecked on open), and "Yes" / "Cancel" cartoon buttons per the design system; dismissible via outside tap and back
- [x] 2.2 Compose UI tests: dialog content, Yes and Cancel each invoke their callback once with the checkbox state, dismiss invokes the dismiss callback

## 3. Welcome screen wiring

- [ ] 3.1 On Exit tap, read a snapshot of `skipExitConfirmation`: if true call `onExit()`, otherwise show the dialog (fall back to showing it if the read fails)
- [ ] 3.2 On Yes: persist the flag if checked (awaiting the write), then call `onExit()`. On Cancel: persist the flag if checked, close the dialog. On outside tap/back: close without persisting
- [ ] 3.3 Tests for the exit decision logic: flag off shows dialog, flag on exits immediately, checked Yes/Cancel persists, unchecked does not

## 4. Verification

- [ ] 4.1 Manual check on a device or emulator in landscape: dialog shown, Cancel, Yes, opt out via Yes and via Cancel, restart the app and confirm Exit is immediate
