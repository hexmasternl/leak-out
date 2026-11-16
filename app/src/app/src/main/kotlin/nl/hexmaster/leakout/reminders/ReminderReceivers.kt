package nl.hexmaster.leakout.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.appContainer
import nl.hexmaster.leakout.core.reminders.ReminderKind

/** A reminder alarm went off: shows its notification. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } } ?: return
        ReminderNotifications.show(context, kind)
    }

    companion object {
        const val EXTRA_KIND = "kind"
    }
}

/**
 * The device restarted (alarms are lost), or its clock or time zone changed (the 9:00 moments move):
 * plans the window again from the last app open.
 */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESTORE_ACTIONS) return
        val container = context.appContainer
        val pending = goAsync()
        container.scope.launch {
            try {
                container.reminders.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val RESTORE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )
    }
}
