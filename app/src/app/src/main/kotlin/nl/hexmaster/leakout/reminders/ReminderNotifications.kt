package nl.hexmaster.leakout.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import nl.hexmaster.leakout.MainActivity
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.reminders.ReminderKind

/** The reminder notifications: their channel, their texts and what tapping them opens. */
object ReminderNotifications {
    const val CHANNEL_ID = "reminders"

    /** Extra on the launch intent of a daily-pipe reminder: open the daily pipe once the welcome screen shows. */
    const val EXTRA_OPEN_DAILY_PIPE = "nl.hexmaster.leakout.OPEN_DAILY_PIPE"

    /** Creates the channel. Safe to call again; Android keeps the player's own channel settings. */
    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        manager.createNotificationChannel(channel)
    }

    /** Whether the app may post notifications: always before Android 13, the permission from then on. */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Posts the reminder of [kind]; does nothing without the permission. */
    fun show(context: Context, kind: ReminderKind) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        createChannel(context)
        val daily = kind == ReminderKind.DailyPipe
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(context.getString(if (daily) R.string.reminder_daily_title else R.string.reminder_miss_title))
            .setContentText(context.getString(if (daily) R.string.reminder_daily_body else R.string.reminder_miss_body))
            .setContentIntent(openAppIntent(context, kind))
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID_BASE + kind.ordinal, notification)
    }

    private fun openAppIntent(context: Context, kind: ReminderKind): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (kind == ReminderKind.DailyPipe) intent.putExtra(EXTRA_OPEN_DAILY_PIPE, true)
        return PendingIntent.getActivity(context, NOTIFICATION_ID_BASE + kind.ordinal, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private const val NOTIFICATION_ID_BASE = 7200
}
