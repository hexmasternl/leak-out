# Design

## Context

See proposal.md. The app already has a daily pipe screen, a Settings screen with stored settings, and `SupportedLanguages`. Game rules stay pure in `core`; `app` renders and wires Android services.

## Goals / Non-Goals

**Goals:**
- One pure, testable function that turns (now, time zone) into the list of reminder instants.
- Cancel-and-replace on every open, so state is always derivable from the last open moment.

**Non-Goals:**
- Server push (FCM), skipping the reminder for an already-played day, per-player reminder times.

## Decisions

- **Planner in `core`**: `ReminderPlanner.plan(openedAt: Instant, zone: ZoneId): List<Reminder>` returns three `DailyPipe` reminders and one `WeMissYou`, using `java.time` so DST is handled by `ZonedDateTime` (9:00 local, not "+24h"). Alternative: compute in `app`; rejected as untestable without Android.
- **Scheduling with `AlarmManager.setAndAllowWhileIdle` (inexact)**: needs no special permission; a few minutes of drift around 9:00 is acceptable for a reminder. Alternatives: exact alarms (`SCHEDULE_EXACT_ALARM`, special permission and Play policy review), `WorkManager` (more drift, heavier).
- **Cancel-and-replace**: each reminder gets a fixed request code (daily 1..3, miss-you 4); scheduling replaces them, cancelling clears them. The last open instant is stored so boot and time change receivers can re-plan without the player opening the app.
- **Receivers**: an alarm receiver posts the notification on a dedicated channel; a boot/time-zone/time-set receiver re-plans from the stored open instant. Reminders whose moment is already past are dropped.
- **Trigger point**: `MainActivity` calls the scheduler in `onStart`. The permission is requested from the welcome screen, not at first frame; the result triggers a re-plan.
- **Tap targets**: the daily reminder carries an intent extra that routes to the daily pipe; "We miss you" opens the launcher activity.
- **Setting**: `remindersEnabled` (default true) in the existing settings store, with a switch in a new Reminders panel on the Settings screen.

## Risks / Trade-offs

- [Inexact alarms drift by minutes, and aggressive OEM battery savers may delay them] → Accepted.
- [Re-planning on every `onStart` may reset alarms often] → Cheap and idempotent through fixed request codes.
- [Player denies the permission permanently] → Nothing is scheduled; the Settings switch explains how to enable it.

## Open Questions

- Final notification wording (placeholder texts are used until the maintainer decides).
- Whether a reminder should be skipped for a day the player already completed the daily pipe (would need a re-plan after finishing it).
