package nl.hexmaster.leakout.reminders

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.core.reminders.ReminderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderCoordinatorTest {
    private val amsterdam = ZoneId.of("Europe/Amsterdam")
    private val newYork = ZoneId.of("America/New_York")

    private var now: Instant = at("2026-11-16T14:00:00") // a Monday afternoon
    private var zone: ZoneId = amsterdam
    private var enabled = true
    private var permitted = true
    private val state = InMemoryReminderStateStore()
    private val scheduler = FakeReminderScheduler()

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(amsterdam).toInstant()

    private fun coordinator() = ReminderCoordinator(
        remindersEnabled = { enabled },
        state = state,
        scheduler = scheduler,
        canNotify = { permitted },
        clock = { now },
        zone = { zone },
    )

    private fun local(slot: Int): String = scheduler.scheduled.getValue(slot).at.atZone(zone).toLocalDateTime().toString()

    @Test
    fun `opening the app schedules three daily reminders and one we miss you`() = runTest {
        coordinator().onAppOpened()
        assertEquals(setOf(0, 1, 2, 3), scheduler.scheduled.keys)
        assertEquals(listOf("2026-11-17T09:00", "2026-11-18T09:00", "2026-11-19T09:00", "2026-11-23T09:00"), (0..3).map(::local))
        assertEquals(ReminderKind.WeMissYou, scheduler.scheduled.getValue(3).kind)
        assertEquals(3, scheduler.scheduled.values.count { it.kind == ReminderKind.DailyPipe })
    }

    @Test
    fun `everything is cancelled before the new window is scheduled`() = runTest {
        coordinator().onAppOpened()
        assertEquals("cancelAll", scheduler.calls.first())
        assertEquals(4, scheduler.calls.count { it.startsWith("schedule:") })
    }

    @Test
    fun `opening the app again replaces the window from the new open`() = runTest {
        val coordinator = coordinator()
        coordinator.onAppOpened()
        now = at("2026-11-18T20:00:00")
        coordinator.onAppOpened()
        assertEquals(4, scheduler.scheduled.size)
        assertEquals("2026-11-19T09:00", local(0))
        assertEquals("2026-11-25T09:00", local(3))
        assertEquals(2, scheduler.calls.count { it == "cancelAll" })
    }

    @Test
    fun `after three days away only we miss you is left`() = runTest {
        val coordinator = coordinator()
        coordinator.onAppOpened()
        now = at("2026-11-20T10:00:00")
        coordinator.refresh()
        assertEquals(setOf(3), scheduler.scheduled.keys)
        assertEquals("2026-11-23T09:00", local(3))
    }

    @Test
    fun `after a full week away nothing is left`() = runTest {
        val coordinator = coordinator()
        coordinator.onAppOpened()
        now = at("2026-11-24T10:00:00")
        coordinator.refresh()
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `nothing is scheduled while the permission is denied`() = runTest {
        permitted = false
        coordinator().onAppOpened()
        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(at("2026-11-16T14:00:00"), state.lastOpened())
    }

    @Test
    fun `granting the permission later schedules the window from the stored open`() = runTest {
        val coordinator = coordinator()
        permitted = false
        coordinator.onAppOpened()
        permitted = true
        coordinator.refresh()
        assertEquals(setOf(0, 1, 2, 3), scheduler.scheduled.keys)
    }

    @Test
    fun `switching reminders off cancels everything pending`() = runTest {
        val coordinator = coordinator()
        coordinator.onAppOpened()
        enabled = false
        coordinator.refresh()
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `switching reminders on schedules the window again`() = runTest {
        val coordinator = coordinator()
        enabled = false
        coordinator.onAppOpened()
        assertTrue(scheduler.scheduled.isEmpty())
        enabled = true
        coordinator.refresh()
        assertEquals(setOf(0, 1, 2, 3), scheduler.scheduled.keys)
    }

    @Test
    fun `a restart restores the same reminders that were pending`() = runTest {
        coordinator().onAppOpened()
        val before = scheduler.scheduled.toMap()
        scheduler.cancelAll() // the alarms are lost when the device restarts
        coordinator().refresh() // a new process reads the stored open
        assertEquals(before, scheduler.scheduled)
    }

    @Test
    fun `refresh before the app was ever opened schedules nothing`() = runTest {
        coordinator().refresh()
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `a time zone change moves the reminders to 9 in the new zone and keeps the open`() = runTest {
        val coordinator = coordinator()
        coordinator.onAppOpened()
        zone = newYork
        coordinator.refresh()
        // 14:00 in Amsterdam is 08:00 in New York: before 9, so the window starts that day.
        assertEquals("2026-11-16T09:00", local(0))
        assertEquals("2026-11-23T09:00", local(3))
        assertEquals(at("2026-11-16T14:00:00"), state.lastOpened())
    }
}
