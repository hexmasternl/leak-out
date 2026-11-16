package nl.hexmaster.leakout.core.reminders

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** The two kinds of reminder notification (openspec/specs/daily-pipe-reminders). */
enum class ReminderKind { DailyPipe, WeMissYou }

/**
 * One reminder to show at [at]. [slot] identifies it inside a window: 0..2 for the daily-pipe
 * reminders and 3 for "We miss you", so scheduling the same slot again replaces the earlier alarm.
 */
data class Reminder(val kind: ReminderKind, val at: Instant, val slot: Int)

/**
 * Plans the rolling reminder window from the moment the app was opened. Every app open cancels the
 * old window and plans a new one, so "We miss you" only fires after a full week without opening the app.
 *
 * All reminders fire at 9:00 in the given time zone. Times are built from local dates, never by
 * adding 24 hours, so daylight saving changes keep them at 9:00.
 */
object ReminderPlanner {
    const val HOUR: Int = 9
    const val DAILY_PIPE_COUNT: Int = 3
    const val WE_MISS_YOU_AFTER_DAYS: Long = 7
    const val WE_MISS_YOU_SLOT: Int = DAILY_PIPE_COUNT

    private val reminderTime: LocalTime = LocalTime.of(HOUR, 0)

    /**
     * The whole window for an app open at [openedAt]: three daily-pipe reminders on consecutive days,
     * starting today when it is still before 9:00 and tomorrow otherwise, and one "We miss you" on the
     * seventh day after the open.
     */
    fun plan(openedAt: Instant, zone: ZoneId): List<Reminder> {
        val opened = openedAt.atZone(zone)
        val today = opened.toLocalDate()
        val firstDay = if (opened.toLocalTime() < reminderTime) today else today.plusDays(1)
        val daily = (0 until DAILY_PIPE_COUNT).map { index ->
            Reminder(ReminderKind.DailyPipe, nineAm(firstDay.plusDays(index.toLong()), zone), index)
        }
        val missYou = Reminder(ReminderKind.WeMissYou, nineAm(today.plusDays(WE_MISS_YOU_AFTER_DAYS), zone), WE_MISS_YOU_SLOT)
        return daily + missYou
    }

    /**
     * The part of the window that is still ahead at [now]. Used when the window is restored after a
     * reboot or a time zone change, where some reminders may have passed already.
     */
    fun pending(openedAt: Instant, zone: ZoneId, now: Instant): List<Reminder> =
        plan(openedAt, zone).filter { it.at.isAfter(now) }

    private fun nineAm(date: LocalDate, zone: ZoneId): Instant =
        ZonedDateTime.of(date, reminderTime, zone).toInstant()
}
