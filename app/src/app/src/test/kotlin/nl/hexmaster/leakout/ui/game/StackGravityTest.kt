package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackGravityTest {
    private val pitch = 46f // 40 dp preview slot plus a 6 dp gap

    @Test
    fun `offset starts at the distance and ends at rest`() {
        assertEquals(pitch, StackGravity.offsetAt(pitch, 0L), 0f)
        assertEquals(0f, StackGravity.offsetAt(pitch, StackGravity.settleDuration(pitch)), 0f)
        assertEquals(0f, StackGravity.offsetAt(pitch, 10_000L), 0f)
    }

    @Test
    fun `fall duration grows with the distance within its bounds`() {
        assertEquals(120L, StackGravity.fallDuration(1f))
        assertEquals(260L, StackGravity.fallDuration(500f))
        assertTrue(StackGravity.fallDuration(pitch) in 180L..230L)
        assertTrue(StackGravity.fallDuration(30f) < StackGravity.fallDuration(pitch))
    }

    @Test
    fun `the section speeds up while it falls`() {
        val fall = StackGravity.fallDuration(pitch)
        var lastOffset = pitch
        var lastStep = 0f
        for (t in 10L until fall step 10L) {
            val offset = StackGravity.offsetAt(pitch, t)
            val step = lastOffset - offset
            assertTrue("t=$t step $step after $lastStep", step > lastStep)
            lastOffset = offset
            lastStep = step
        }
    }

    @Test
    fun `the bounce peaks at most 4 dp and never goes below the slot`() {
        for (distance in listOf(10f, pitch, 92f, 300f)) {
            val samples = (0L..StackGravity.settleDuration(distance)).map { StackGravity.offsetAt(distance, it) }
            val bounce = samples.drop(StackGravity.fallDuration(distance).toInt())
            assertTrue(bounce.max() <= 4f + 0.001f)
            assertTrue(bounce.max() > 0f)
            assertTrue(samples.all { it >= 0f })
        }
        assertEquals(0.12f * 10f, StackGravity.bounceHeight(10f), 0.0001f)
        assertEquals(4f, StackGravity.bounceHeight(pitch), 0f)
    }

    @Test
    fun `a one slot fall rests within 400 ms for every section of the stack`() {
        for (onePitch in listOf(22f, 30.7f, pitch)) {
            for (indexFromBottom in 0 until 20) {
                val rest = StackGravity.startDelay(indexFromBottom, onePitch) + StackGravity.settleDuration(onePitch)
                assertTrue("pitch $onePitch index $indexFromBottom rests at $rest", rest <= StackGravity.MAX_REST_MS)
            }
        }
    }

    @Test
    fun `the bottom section starts first and the stagger is capped`() {
        val small = 22f
        assertEquals(0L, StackGravity.startDelay(0, small))
        assertEquals(24L, StackGravity.startDelay(1, small))
        assertEquals(48L, StackGravity.startDelay(2, small))
        for (index in 1 until 20) {
            assertTrue(StackGravity.startDelay(index, pitch) >= StackGravity.startDelay(index - 1, pitch))
            assertTrue(StackGravity.startDelay(index, pitch) <= StackGravity.STAGGER_MS * StackGravity.MAX_STAGGER_STEPS)
        }
    }

    @Test
    fun `retargeting keeps the section where it is`() {
        for (t in 0L..400L step 20L) {
            val current = StackGravity.offsetAt(pitch, t)
            val retargeted = StackGravity.retarget(current, pitch)
            // The new slot is one pitch lower, so the same screen position is one pitch more above it.
            assertEquals(current + pitch, retargeted, 0.0001f)
            assertEquals(retargeted, StackGravity.offsetAt(retargeted, 0L), 0f)
        }
    }

    @Test
    fun `reduced motion slides monotonically over 160 ms without a bounce`() {
        var last = Float.MAX_VALUE
        for (t in 0L..StackGravity.REDUCED_MS) {
            val offset = StackGravity.reducedOffsetAt(pitch, t)
            assertTrue(offset <= last)
            assertTrue(offset >= 0f)
            last = offset
        }
        assertEquals(pitch, StackGravity.reducedOffsetAt(pitch, 0L), 0f)
        assertEquals(0f, StackGravity.reducedOffsetAt(pitch, 160L), 0f)
        assertEquals(0f, StackGravity.reducedOffsetAt(pitch, 500L), 0f)
        assertEquals(0f, StackGravity.reducedAlphaAt(0L), 0f)
        assertEquals(1f, StackGravity.reducedAlphaAt(160L), 0f)
    }
}

class StackFallsTest {
    private val pitch = 46f
    private val ids = (14 downTo 0).toList() // a fresh stack of 15: top id 14, next id 0

    private fun placed(placements: Int) = (placements + 14 downTo placements).toList()

    @Test
    fun `a new stack is at rest`() {
        val falls = StackFalls.resting(ids, 0)
        assertFalse(falls.isActive(0L))
        assertTrue(ids.all { falls.offsetOf(it, 0L) == 0f })
    }

    @Test
    fun `after a placement every section is one pitch above its new slot and the new one enters from above`() {
        val falls = StackFalls.resting(ids, 0).sync(placed(1), 1, pitch, 1_000L, reduced = false).stamp(1_016L)

        for (id in placed(1)) assertEquals("id $id", pitch, falls.offsetOf(id, 1_016L), 0f)
        assertTrue(falls.isActive(1_016L))
        assertFalse(falls.isActive(1_016L + StackGravity.MAX_REST_MS))
        assertTrue(placed(1).all { falls.offsetOf(it, 1_016L + StackGravity.MAX_REST_MS) == 0f })
    }

    @Test
    fun `the section that becomes next falls first`() {
        val falls = StackFalls.resting(ids, 0).sync(placed(1), 1, 22f, 0L, reduced = false).stamp(0L)
        // id 1 is the new next section, id 2 is above it.
        assertTrue(falls.offsetOf(1, 30L) < falls.offsetOf(2, 30L))
    }

    @Test
    fun `a placement during a fall never moves a section up`() {
        var falls = StackFalls.resting(ids, 0).sync(placed(1), 1, pitch, 0L, reduced = false).stamp(0L)
        // Screen position of a section, measured downwards from the slot of id 5 before any placement.
        fun position(id: Int, slotsDown: Int, now: Long) = slotsDown * pitch - falls.offsetOf(id, now)

        val before = position(5, 1, 100L)
        falls = falls.sync(placed(2), 2, pitch, 100L, reduced = false).stamp(100L)
        assertEquals(before, position(5, 2, 100L), 0.0001f)

        var last = before
        val fallEnd = 100L + StackGravity.fallDuration(falls.falls.getValue(5).from)
        for (t in 100L..fallEnd) {
            val now = position(5, 2, t)
            assertTrue("t=$t", now >= last - 0.0001f)
            last = now
        }
        assertEquals(2 * pitch, position(5, 2, 100L + StackGravity.MAX_REST_MS + 200L), 0f)
    }

    @Test
    fun `reduced motion moves everything together and fades the new section in`() {
        val falls = StackFalls.resting(ids, 0).sync(placed(1), 1, pitch, 0L, reduced = true).stamp(0L)
        val newTop = placed(1).first()

        assertEquals(0f, falls.offsetOf(newTop, 0L), 0f)
        assertEquals(0f, falls.alphaOf(newTop, 0L), 0f)
        assertEquals(falls.offsetOf(1, 80L), falls.offsetOf(9, 80L), 0f)
        assertFalse(falls.isActive(160L))
        assertEquals(1f, falls.alphaOf(newTop, 160L), 0f)
        assertTrue(placed(1).all { falls.offsetOf(it, 160L) == 0f })
    }

    @Test
    fun `a new attempt starts at rest`() {
        val falling = StackFalls.resting(ids, 0).sync(placed(3), 3, pitch, 0L, reduced = false).stamp(0L)
        val restarted = falling.sync(ids, 0, pitch, 50L, reduced = false)
        assertFalse(restarted.isActive(50L))
    }

    @Test
    fun `falls wait for their first frame`() {
        val falls = StackFalls.resting(ids, 0).sync(placed(1), 1, pitch, 0L, reduced = false)
        assertEquals(pitch, falls.offsetOf(1, 5_000L), 0f)
        assertTrue(falls.isActive(5_000L))
    }
}
