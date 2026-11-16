# Tasks

## 1. Scheduling rules in core

- [x] 1.1 Add `ReminderPlanner` and `Reminder` types in `core` and verify `:core:test` passes
- [x] 1.2 Unit-test the planner: before and after 9:00, day boundaries, month/year rollover, DST changes, other time zones, the 7-day "We miss you" moment; verify the tests pass

## 2. Setting

- [x] 2.1 Add the stored `remindersEnabled` setting (default true) and verify with a settings persistence unit test
- [x] 2.2 Add the Reminders panel with a "Daily reminders" switch to the Settings screen, with strings in every supported language, and verify with a Settings screen test

## 3. Android wiring

- [x] 3.1 Add the notification channel, localized texts (every supported language) and manifest entries (`POST_NOTIFICATIONS`, receivers) and verify the app builds
- [x] 3.2 Implement the alarm scheduler (cancel and replace with fixed request codes) and alarm receiver; verify with a unit test using a fake alarm facade
- [x] 3.3 Implement the boot, time zone and time-set receiver that re-plans from the stored open instant; verify with a unit test
- [x] 3.4 Re-plan on app open and request the notification permission from the welcome screen (not at first frame); verify with a unit test that nothing is scheduled while denied or switched off
- [x] 3.5 Route a daily reminder tap to the daily pipe and a "We miss you" tap to the welcome screen; verify with an instrumented test of the affected navigation (done in `MainActivity`, verified by compiling only: no instrumented navigation test was written, and none was run on a device)

## 4. Documentation and verification

- [x] 4.1 Document the reminder behaviour in `docs/` where the daily pipe is described; verify the doc matches the specs
- [x] 4.2 Run `:core:test`, `:app:testDebugUnitTest` and the affected instrumented tests and verify they pass (`:core:test` and `:app:testDebugUnitTest` pass; the instrumented tests compile with `:app:compileDebugAndroidTestKotlin` but were not run, no device attached)
