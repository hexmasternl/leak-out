package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DropThrottleTest {
    private var nowMs = 5_000L
    private val throttle = DropThrottle { nowMs }

    @Test
    fun `the first drop plays`() {
        assertTrue(throttle.tryDrop())
    }

    @Test
    fun `the first drop plays at uptime zero`() {
        nowMs = 0
        assertTrue(throttle.tryDrop())
    }

    @Test
    fun `a drop 99 ms after the last is skipped`() {
        assertTrue(throttle.tryDrop())
        nowMs += 99
        assertFalse(throttle.tryDrop())
    }

    @Test
    fun `a drop 100 ms after the last plays`() {
        assertTrue(throttle.tryDrop())
        nowMs += 100
        assertTrue(throttle.tryDrop())
    }

    @Test
    fun `a skipped drop does not move the window`() {
        assertTrue(throttle.tryDrop())
        nowMs += 60
        assertFalse(throttle.tryDrop())
        nowMs += 40
        assertTrue(throttle.tryDrop())
    }

    @Test
    fun `awards every 40 ms drop at most once per 100 ms`() {
        val played = (0 until 25).map { step ->
            nowMs = 5_000L + step * 40L
            throttle.tryDrop()
        }
        // 0, 120, 240, … ms: every third award of the fast forward run, 9 of 25.
        assertEquals(List(25) { it % 3 == 0 }, played)
    }

    @Test
    fun `the injected clock is used`() {
        var calls = 0
        val counting = DropThrottle { calls++; nowMs }
        counting.tryDrop()
        counting.tryDrop()
        assertEquals(2, calls)
    }

    @Test
    fun `the minimum interval is 100 ms`() {
        assertEquals(100L, DropThrottle.DROP_MIN_INTERVAL_MS)
    }
}
