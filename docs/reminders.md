# Daily pipe reminders

Leak Out reminds players to play the daily pipe with local notifications. Nothing is sent by a server and nothing leaves the device. The behaviour is specified in `openspec/specs/daily-pipe-reminders`.

## The rolling window

Every time the app is opened (cold start or return to the foreground), the app cancels every pending reminder and plans a new window from that moment:

- **Three daily-pipe reminders**, one for each of the next three days, at 9:00 local time. When the app is opened at or after 9:00, today gets no reminder and the window starts tomorrow; before 9:00 it starts today.
- **One "We miss you" reminder** at 9:00 local time on the seventh day after the open.

A player who keeps opening the app never sees "We miss you", because every open moves it a week ahead. A player who stays away gets three reminders, then silence, then one nudge after a week. This lets players stay offline for a while without being nagged.

The times are built from local dates, never by adding 24 hours, so daylight saving changes keep the reminders at 9:00.

## Where the code lives

| What | Where |
| ---- | ----- |
| The planning rules (which moments, past-9:00 rule, DST) | `core`: `nl.hexmaster.leakout.core.reminders.ReminderPlanner`, tested by `ReminderPlannerTest` |
| Cancel-and-replace, permission and setting checks | `app`: `reminders/ReminderCoordinator` |
| Alarms | `app`: `reminders/ReminderScheduler` (`AlarmManager.setAndAllowWhileIdle`) |
| Last app open, "permission asked" flag | `app`: `reminders/ReminderStateStore`, in the settings DataStore |
| Notification channel, texts, tap targets | `app`: `reminders/ReminderNotifications` |
| Alarm, restart and clock change receivers | `app`: `reminders/ReminderReceivers.kt`, declared in the manifest |
| The on/off switch | Settings screen, Reminders panel; stored as `remindersEnabled` (default on) |

## Behaviour worth knowing

- **Inexact alarms.** The alarms are inexact, so a reminder can arrive a few minutes after 9:00. This avoids the exact alarm permission and its Play review.
- **Permission.** From Android 13 the app needs the `POST_NOTIFICATIONS` permission. It is asked for once, when the welcome screen shows (never on the first frame), and again when the player switches Reminders on in Settings. While it is denied nothing is scheduled; granting it schedules the window from the stored last open.
- **Restart and clock changes.** Alarms are lost when the device restarts. A receiver for boot, time zone and clock changes plans the window again from the stored last open, dropping reminders that have passed.
- **Tapping a reminder.** A daily-pipe reminder opens the app and then the daily pipe, as soon as the Daily Pipe button is usable (signed in with Play Games and online; the app waits up to 5 seconds). "We miss you" opens the app on the welcome screen.
- **Texts.** The notification texts are ordinary string resources, translated by Play Console like the rest.
