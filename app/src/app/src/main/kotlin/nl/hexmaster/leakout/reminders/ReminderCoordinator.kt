package nl.hexmaster.leakout.reminders

import nl.hexmaster.leakout.core.reminders.ReminderPlanner
import java.time.Instant
import java.time.ZoneId

/**
 * Keeps the pending reminders in step with what the player allowed (openspec/specs/daily-pipe-reminders).
 * Every change goes through [refresh]: cancel everything, then, only when reminders are switched on and
 * notifications are allowed, schedule what is still ahead in the window planned from the last app open.
 *
 * @param remindersEnabled the stored Reminders setting.
 * @param canNotify whether the app may post notifications right now (the Android 13+ permission).
 */
class ReminderCoordinator(
    private val remindersEnabled: suspend () -> Boolean,
    private val state: ReminderStateStore,
    private val scheduler: ReminderScheduler,
    private val canNotify: () -> Boolean,
    private val clock: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    /** The app was opened: remember it and plan a new window from now, replacing the old one. */
    suspend fun onAppOpened() {
        state.setLastOpened(clock())
        refresh()
    }

    /**
     * Cancels every reminder and schedules the part of the current window that is still ahead.
     * Also called after the setting or the permission changed, a reboot and a time or time zone change.
     */
    suspend fun refresh() {
        scheduler.cancelAll()
        if (!remindersEnabled() || !canNotify()) return
        val opened = state.lastOpened() ?: return
        scheduler.schedule(ReminderPlanner.pending(opened, zone(), clock()))
    }
}
