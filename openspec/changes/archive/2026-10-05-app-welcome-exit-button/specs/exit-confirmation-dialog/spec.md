## ADDED Requirements

### Requirement: Exit asks for confirmation
Tapping "Exit" on the welcome screen SHALL open a modal dialog asking whether the player wants to leave the app, offering "Yes" and "Cancel" actions and a "Never show this again" checkbox, unless the player has previously opted out of the dialog.

#### Scenario: Tap Exit with dialog enabled
- **WHEN** the user taps "Exit" and has not opted out of the dialog
- **THEN** a modal dialog is shown asking whether to leave the app, with the buttons "Yes" and "Cancel" and an unchecked "Never show this again" checkbox
- **AND** the app has not exited yet

#### Scenario: Dismiss without choosing
- **WHEN** the dialog is open and the user taps outside it or presses back
- **THEN** the dialog closes, the app stays open, and no preference is saved

### Requirement: Yes exits the app
Tapping "Yes" SHALL close the app.

#### Scenario: Confirm exit
- **WHEN** the dialog is open and the user taps "Yes"
- **THEN** the app exits

### Requirement: Cancel closes the dialog
Tapping "Cancel" SHALL close the dialog and leave the welcome screen unchanged.

#### Scenario: Cancel exit
- **WHEN** the dialog is open and the user taps "Cancel"
- **THEN** the dialog closes and the app remains on the welcome screen

### Requirement: Never show this again is remembered
When the player taps "Yes" or "Cancel" with "Never show this again" checked, the app SHALL persist a setting that suppresses the dialog. While that setting is on, tapping "Exit" SHALL exit the app immediately without showing the dialog. The setting SHALL survive app restarts. The checkbox SHALL start unchecked every time the dialog opens.

#### Scenario: Opt out and exit
- **WHEN** the user checks "Never show this again" and taps "Yes"
- **THEN** the setting is saved and the app exits
- **AND** on the next launch, tapping "Exit" exits the app immediately without a dialog

#### Scenario: Opt out and cancel
- **WHEN** the user checks "Never show this again" and taps "Cancel"
- **THEN** the setting is saved and the dialog closes
- **AND** the next tap on "Exit" exits the app immediately without a dialog

#### Scenario: Checkbox not checked
- **WHEN** the user taps "Yes" or "Cancel" with "Never show this again" unchecked
- **THEN** no setting is saved and the dialog is shown again on the next tap on "Exit"

#### Scenario: Checkbox resets
- **WHEN** the dialog is reopened after a cancel with the checkbox unchecked
- **THEN** the checkbox is unchecked
