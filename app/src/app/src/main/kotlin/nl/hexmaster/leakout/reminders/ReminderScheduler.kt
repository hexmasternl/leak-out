package nl.hexmaster.leakout.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import nl.hexmaster.leakout.core.reminders.Reminder
import nl.hexmaster.leakout.core.reminders.ReminderKind
import nl.hexmaster.leakout.core.reminders.ReminderPlanner

/** Sets and clears the alarms behind the reminders. Slots are replaced, never stacked. */
interface ReminderScheduler {
    /** Schedules every reminder; one already set for the same slot is replaced. */
    fun schedule(reminders: List<Reminder>)

    /** Cancels every reminder slot, whether or not it is set. */
    fun cancelAll()
}

/**
 * Reminders as inexact alarms ([AlarmManager.setAndAllowWhileIdle]): no special permission is
 * needed, and a few minutes of drift around 9:00 is fine for a reminder. Each slot has a fixed
 * request code, so scheduling a slot again replaces its alarm.
 */
class AlarmReminderScheduler(context: Context) : ReminderScheduler {
    private val appContext = context.applicationContext
    private val alarms = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(reminders: List<Reminder>) {
        for (reminder in reminders) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.at.toEpochMilli(), pendingIntent(reminder.slot, reminder.kind))
        }
    }

    override fun cancelAll() {
        for (slot in 0..ReminderPlanner.WE_MISS_YOU_SLOT) {
            alarms.cancel(pendingIntent(slot, kindOf(slot)))
        }
    }

    private fun pendingIntent(slot: Int, kind: ReminderKind): PendingIntent {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).putExtra(ReminderAlarmReceiver.EXTRA_KIND, kind.name)
        return PendingIntent.getBroadcast(appContext, REQUEST_CODE_BASE + slot, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun kindOf(slot: Int): ReminderKind =
        if (slot == ReminderPlanner.WE_MISS_YOU_SLOT) ReminderKind.WeMissYou else ReminderKind.DailyPipe

    private companion object {
        /** Request codes of the reminder alarms: this plus the slot. */
        const val REQUEST_CODE_BASE = 7100
    }
}

/** A scheduler that only records what it was asked, for tests. */
class FakeReminderScheduler : ReminderScheduler {
    /** The reminders currently set, by slot. */
    val scheduled = mutableMapOf<Int, Reminder>()

    /** What happened, in order: "cancelAll" and "schedule:<slot>". */
    val calls = mutableListOf<String>()

    override fun schedule(reminders: List<Reminder>) {
        for (reminder in reminders) {
            calls += "schedule:${reminder.slot}"
            scheduled[reminder.slot] = reminder
        }
    }

    override fun cancelAll() {
        calls += "cancelAll"
        scheduled.clear()
    }
}
