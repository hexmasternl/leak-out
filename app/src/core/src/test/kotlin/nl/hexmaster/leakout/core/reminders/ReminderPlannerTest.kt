package nl.hexmaster.leakout.core.reminders

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderPlannerTest {
    private val amsterdam = ZoneId.of("Europe/Amsterdam")
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val newYork = ZoneId.of("America/New_York")

    private fun at(zone: ZoneId, text: String): Instant = LocalDateTime.parse(text).atZone(zone).toInstant()

    private fun local(reminder: Reminder, zone: ZoneId): LocalDateTime = reminder.at.atZone(zone).toLocalDateTime()

    private fun List<Reminder>.localTimes(zone: ZoneId): List<String> = map { local(it, zone).toString() }

    @Test
    fun `opened after 9 the window starts tomorrow`() {
        // 2026-11-16 is a Monday.
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-11-16T14:00:00"), amsterdam)
        assertEquals(
            listOf("2026-11-17T09:00", "2026-11-18T09:00", "2026-11-19T09:00", "2026-11-23T09:00"),
            plan.localTimes(amsterdam),
        )
        assertEquals(
            listOf(ReminderKind.DailyPipe, ReminderKind.DailyPipe, ReminderKind.DailyPipe, ReminderKind.WeMissYou),
            plan.map { it.kind },
        )
    }

    @Test
    fun `opened before 9 the window starts today`() {
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-11-16T07:30:00"), amsterdam)
        assertEquals(
            listOf("2026-11-16T09:00", "2026-11-17T09:00", "2026-11-18T09:00", "2026-11-23T09:00"),
            plan.localTimes(amsterdam),
        )
    }

    @Test
    fun `opened exactly at 9 does not remind today`() {
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-11-16T09:00:00"), amsterdam)
        assertEquals("2026-11-17T09:00", local(plan.first(), amsterdam).toString())
    }

    @Test
    fun `opened just before 9 reminds today`() {
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-11-16T08:59:59"), amsterdam)
        assertEquals("2026-11-16T09:00", local(plan.first(), amsterdam).toString())
    }

    @Test
    fun `slots are stable so a new window replaces the old one`() {
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-11-16T14:00:00"), amsterdam)
        assertEquals(listOf(0, 1, 2, 3), plan.map { it.slot })
        assertEquals(ReminderPlanner.WE_MISS_YOU_SLOT, plan.last().slot)
    }

    @Test
    fun `the window rolls over month and year ends`() {
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-12-30T10:00:00"), amsterdam)
        assertEquals(
            listOf("2026-12-31T09:00", "2027-01-01T09:00", "2027-01-02T09:00", "2027-01-06T09:00"),
            plan.localTimes(amsterdam),
        )
    }

    @Test
    fun `a leap day is a day like any other`() {
        val plan = ReminderPlanner.plan(at(amsterdam, "2028-02-27T10:00:00"), amsterdam)
        assertEquals(
            listOf("2028-02-28T09:00", "2028-02-29T09:00", "2028-03-01T09:00", "2028-03-05T09:00"),
            plan.localTimes(amsterdam),
        )
    }

    @Test
    fun `reminders stay at 9 local across the spring forward change`() {
        // Europe/Amsterdam springs forward on 2026-03-29 (02:00 to 03:00).
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-03-27T10:00:00"), amsterdam)
        assertEquals(
            listOf("2026-03-28T09:00", "2026-03-29T09:00", "2026-03-30T09:00", "2026-04-03T09:00"),
            plan.localTimes(amsterdam),
        )
        // Only 23 hours pass between 9:00 on the 28th and 9:00 on the 29th.
        assertEquals(23L * 3600, plan[1].at.epochSecond - plan[0].at.epochSecond)
    }

    @Test
    fun `reminders stay at 9 local across the fall back change`() {
        // Europe/Amsterdam falls back on 2026-10-25 (03:00 to 02:00).
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-10-23T10:00:00"), amsterdam)
        assertEquals(
            listOf("2026-10-24T09:00", "2026-10-25T09:00", "2026-10-26T09:00", "2026-10-30T09:00"),
            plan.localTimes(amsterdam),
        )
        assertEquals(25L * 3600, plan[1].at.epochSecond - plan[0].at.epochSecond)
    }

    @Test
    fun `the week ending in a different offset still fires at 9`() {
        // "We miss you" lands after the autumn change, the open before it.
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-10-22T10:00:00"), amsterdam)
        assertEquals("2026-10-29T09:00", local(plan.last(), amsterdam).toString())
    }

    @Test
    fun `the same moment gives different reminders in different time zones`() {
        val moment = Instant.parse("2026-11-16T23:30:00Z")
        // 00:30 on the 17th in Amsterdam, 08:30 in Tokyo, 18:30 on the 16th in New York.
        assertEquals("2026-11-17T09:00", local(ReminderPlanner.plan(moment, amsterdam).first(), amsterdam).toString())
        assertEquals("2026-11-17T09:00", local(ReminderPlanner.plan(moment, tokyo).first(), tokyo).toString())
        assertEquals("2026-11-17T09:00", local(ReminderPlanner.plan(moment, newYork).first(), newYork).toString())
        // Amsterdam: before 9 on the 17th, so the window starts on the 17th. Tokyo: 08:30 on the 17th, same.
        // New York: after 9 on the 16th, so tomorrow, the 17th, as well.
        assertEquals(
            ZonedDateTime.of(2026, 11, 17, 9, 0, 0, 0, tokyo).toInstant(),
            ReminderPlanner.plan(moment, tokyo).first().at,
        )
    }

    @Test
    fun `we miss you is the seventh day after the open`() {
        val plan = ReminderPlanner.plan(at(tokyo, "2026-06-01T20:00:00"), tokyo)
        assertEquals("2026-06-08T09:00", local(plan.last(), tokyo).toString())
        assertEquals(ReminderKind.WeMissYou, plan.last().kind)
    }

    @Test
    fun `no daily reminder is planned after the third`() {
        val plan = ReminderPlanner.plan(at(amsterdam, "2026-11-16T14:00:00"), amsterdam)
        assertEquals(3, plan.count { it.kind == ReminderKind.DailyPipe })
        assertEquals(1, plan.count { it.kind == ReminderKind.WeMissYou })
        assertTrue(plan.dropLast(1).all { it.at < plan.last().at })
    }

    @Test
    fun `pending keeps only reminders still ahead`() {
        val opened = at(amsterdam, "2026-11-16T14:00:00")
        val now = at(amsterdam, "2026-11-18T12:00:00")
        val pending = ReminderPlanner.pending(opened, amsterdam, now)
        assertEquals(listOf("2026-11-19T09:00", "2026-11-23T09:00"), pending.localTimes(amsterdam))
    }

    @Test
    fun `pending is empty once the whole window has passed`() {
        val opened = at(amsterdam, "2026-11-16T14:00:00")
        assertTrue(ReminderPlanner.pending(opened, amsterdam, at(amsterdam, "2026-11-24T00:00:00")).isEmpty())
    }

    @Test
    fun `a time zone change keeps the open moment and moves the 9 o'clock times`() {
        val opened = Instant.parse("2026-11-16T13:00:00Z")
        val inAmsterdam = ReminderPlanner.plan(opened, amsterdam)
        val inNewYork = ReminderPlanner.plan(opened, newYork)
        assertEquals("2026-11-17T09:00", local(inAmsterdam.first(), amsterdam).toString())
        // 13:00Z is 08:00 in New York, still before 9, so the window starts that same day.
        assertEquals("2026-11-16T09:00", local(inNewYork.first(), newYork).toString())
        assertTrue(inNewYork.first().at.isBefore(inAmsterdam.first().at))
    }
}
