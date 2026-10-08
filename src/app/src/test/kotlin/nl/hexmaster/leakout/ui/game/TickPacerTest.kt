package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TickPacerTest {
    /** The deltas a fresh pacer returns for one second of frames at [hz], starting at [startNanos]; null for a skipped frame. */
    private fun oneSecondAt(hz: Int, startNanos: Long = 5_000_000_000L): List<Long?> {
        val pacer = TickPacer()
        return (0..hz).map { k -> pacer.onFrame(startNanos + k * 1_000_000_000L / hz) }
    }

    /** The ticks after the first frame's 0. */
    private fun List<Long?>.ticks(): List<Long> = drop(1).filterNotNull()

    @Test
    fun `the first frame sends 0`() {
        assertEquals(0L, TickPacer().onFrame(123_456_789L))
    }

    @Test
    fun `at 60 Hz every frame ticks`() {
        val deltas = oneSecondAt(60)
        assertTrue(deltas.all { it != null })
        assertEquals(60, deltas.ticks().size)
    }

    @Test
    fun `at 120 Hz every second frame ticks, 60 times a second`() {
        val deltas = oneSecondAt(120)
        for (k in 1..120) {
            if (k % 2 == 1) assertNull("frame $k", deltas[k]) else assertTrue("frame $k", deltas[k] != null)
        }
        assertEquals(60, deltas.ticks().size)
    }

    @Test
    fun `at 90 Hz mostly every second frame ticks, 50 times a second`() {
        // 11.1 ms frames: two make a tick, and the kept remainder lets every ninth frame tick alone.
        val ticks = oneSecondAt(90).ticks()
        assertEquals(50, ticks.size)
        assertTrue(ticks.all { it >= TickPacer.MIN_TICK_MS })
        assertEquals(40, ticks.count { it >= 22L })
    }

    @Test
    fun `at 144 Hz every second frame ticks, 72 times a second`() {
        val ticks = oneSecondAt(144).ticks()
        assertEquals(72, ticks.size)
        assertTrue(ticks.all { it in 13L..14L })
    }

    @Test
    fun `the deltas add up to the elapsed whole milliseconds at every tick`() {
        for (hz in listOf(30, 60, 90, 120, 144, 165, 240)) {
            val pacer = TickPacer()
            val start = 7_777_777L
            var sum = 0L
            for (k in 0..3 * hz) {
                val frame = start + k * 1_000_000_000L / hz
                val delta = pacer.onFrame(frame) ?: continue
                sum += delta
                assertEquals("$hz Hz frame $k", (frame - start) / 1_000_000, sum)
            }
            // What has not been ticked yet is less than one tick's worth.
            assertTrue("$hz Hz after 3 s: $sum", 3_000L - sum in 0L..TickPacer.MIN_TICK_MS)
        }
    }

    @Test
    fun `a new pacer after a pause starts at 0 again`() {
        val first = TickPacer()
        first.onFrame(0L)
        assertEquals(16L, first.onFrame(16_000_000L))
        // Ten seconds later the clock runs again with a new pacer: the pause is not ticked.
        val resumed = TickPacer()
        assertEquals(0L, resumed.onFrame(10_016_000_000L))
        assertEquals(16L, resumed.onFrame(10_032_000_000L))
    }
}
