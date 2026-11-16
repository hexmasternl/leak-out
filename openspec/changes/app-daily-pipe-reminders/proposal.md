# Proposal

## Why

The app has a daily pipe, but nothing invites players back to it. Local reminders keep players engaged without server push, while a short, self-limiting schedule lets players stay offline for a while without being nagged. Tracked in [#171](https://github.com/hexmasternl/leak-out/issues/171).

## What Changes

- Schedule local notifications as a rolling window, rebuilt on every app open: three "play the daily pipe" reminders (one per each of the next three days) and one "We miss you" notification 7 days out, all at 9:00 AM device-local time.
- Opening the app cancels every pending reminder and schedules a fresh window from that moment, so "We miss you" only fires after 7 days without opening the app.
- Request the Android 13+ notification permission at a sensible moment; schedule nothing while it is denied.
- Re-schedule after a device reboot and after a timezone or clock change.
- Add a "Reminders" switch to the Settings screen (stored setting, on by default); switching it off cancels all pending reminders.
- Tapping a daily reminder opens the daily pipe; tapping "We miss you" opens the app.
- The scheduling rules are pure Kotlin in `core`; `app` only wires them to Android.

## Capabilities

### New Capabilities
- `daily-pipe-reminders`: when reminders are scheduled and cancelled, their times, content, permission handling and tap targets.

### Modified Capabilities
- `settings-screen`: adds a Reminders panel with an on/off switch.
- `settings-persistence`: adds the stored `remindersEnabled` setting.

## Impact

- `app/src/core`: new pure scheduling rules (plan of reminder instants for a given "now" and time zone) with unit tests.
- `app/src/app`: notification channel, alarm scheduler, receivers (alarm, boot, time change), permission request, Settings panel, strings in every supported language, manifest entries.
- No server change. No new third-party dependency.
