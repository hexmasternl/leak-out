# Spec Delta

## Purpose

Invites players to play the daily pipe with local reminders at 9:00 AM, and nudges lapsed players once after a week, without nagging players who stay away.

## ADDED Requirements

### Requirement: Reminder window is rebuilt on every app open
Each time the app is opened (cold start or return to the foreground), the app SHALL cancel all pending reminders and then schedule three daily-pipe reminders and one "We miss you" reminder, all relative to that moment.

#### Scenario: First open
- **WHEN** the app is opened and reminders are enabled and permitted
- **THEN** three daily-pipe reminders and one "We miss you" reminder are pending

#### Scenario: Open within the 7 days
- **WHEN** the app is opened again while earlier reminders are still pending
- **THEN** the earlier reminders are cancelled and a new set of three daily-pipe reminders and one "We miss you" reminder is scheduled from the new open

### Requirement: Daily-pipe reminders fire at 9:00 local time
The three daily-pipe reminders SHALL fire at 9:00 AM in the device's local time zone on each of the next three calendar days. When the app is opened at or after 9:00 AM, today SHALL NOT get a reminder and the window starts tomorrow; when it is opened before 9:00 AM, the window starts today.

#### Scenario: Opened after 9:00
- **WHEN** the app is opened on Monday at 14:00
- **THEN** reminders are scheduled for Tuesday, Wednesday and Thursday at 9:00

#### Scenario: Opened before 9:00
- **WHEN** the app is opened on Monday at 07:30
- **THEN** reminders are scheduled for Monday, Tuesday and Wednesday at 9:00

### Requirement: "We miss you" reminder after 7 days
The "We miss you" reminder SHALL fire at 9:00 AM local time on the seventh calendar day after the app was opened.

#### Scenario: Player stays away
- **WHEN** the app is opened on Monday and not opened again
- **THEN** the "We miss you" reminder fires the next Monday at 9:00, and no daily-pipe reminder fires after the third

### Requirement: Permission and setting gate scheduling
The app SHALL schedule no reminders while the notification permission is denied or the Reminders setting is off. It SHALL request the notification permission only after the player has seen the welcome screen, never at the first frame.

#### Scenario: Permission denied
- **WHEN** the app is opened and the notification permission is denied
- **THEN** no reminder is scheduled

#### Scenario: Permission granted later
- **WHEN** the player grants the permission and opens the app again
- **THEN** the reminder window is scheduled

#### Scenario: Reminders switched off
- **WHEN** the player switches Reminders off in Settings
- **THEN** all pending reminders are cancelled

### Requirement: Schedule survives reboot and clock changes
The app SHALL restore its pending reminders after a device reboot, and SHALL re-plan the 9:00 times after a time zone or clock change, keeping the original open moment.

#### Scenario: Reboot
- **WHEN** the device restarts while reminders are pending
- **THEN** the same reminders that were pending before the restart are pending again

#### Scenario: Time zone change
- **WHEN** the device time zone changes
- **THEN** the pending reminders fire at 9:00 in the new time zone

### Requirement: Reminder content and tap targets
Reminders SHALL be localized in every language the app supports. Tapping a daily-pipe reminder SHALL open the daily pipe; tapping the "We miss you" reminder SHALL open the app on the welcome screen.

#### Scenario: Tap daily reminder
- **WHEN** the player taps a daily-pipe reminder
- **THEN** the app opens on the daily pipe

#### Scenario: Tap "We miss you"
- **WHEN** the player taps the "We miss you" reminder
- **THEN** the app opens on the welcome screen
