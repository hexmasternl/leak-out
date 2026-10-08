package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeakEffectTest {
    @Test
    fun `the spurt has no length at the moment of the leak`() {
        assertEquals(0f, leakFrame(0).length, 0f)
    }

    @Test
    fun `the spurt grows and never shrinks during the effect`() {
        var previous = leakFrame(0).length
        for (ms in 1 until LeakTiming.EFFECT_MS) {
            val length = leakFrame(ms).length
            assertTrue("shrank at $ms ms", length >= previous)
            previous = length
        }
        assertTrue(leakFrame(LeakTiming.EFFECT_MS - 1).length > leakFrame(100).length)
    }

    @Test
    fun `alpha stays between 0 and 1`() {
        for (ms in -10L..LeakTiming.EFFECT_MS + 500) {
            val alpha = leakFrame(ms).alpha
            assertTrue("alpha $alpha at $ms ms", alpha in 0f..1f)
        }
    }

    @Test
    fun `from 1000 ms on the frame is the fixed puddle`() {
        assertEquals(LeakPuddle, leakFrame(1_000))
        assertEquals(LeakPuddle, leakFrame(1_001))
        assertEquals(LeakPuddle, leakFrame(60_000))
    }

    @Test
    fun `the missed section pulse peaks twice and is off outside the effect`() {
        assertEquals(0f, missedPulseAlpha(0), 1e-6f)
        assertEquals(1f, missedPulseAlpha(250), 1e-6f)
        assertEquals(0f, missedPulseAlpha(500), 1e-6f)
        assertEquals(1f, missedPulseAlpha(750), 1e-6f)
        assertEquals(0f, missedPulseAlpha(-1), 0f)
        assertEquals(0f, missedPulseAlpha(LeakTiming.EFFECT_MS), 0f)
        assertEquals(0f, missedPulseAlpha(60_000), 0f)
        for (ms in 0L until LeakTiming.EFFECT_MS step 10) assertTrue(missedPulseAlpha(ms) in 0f..1f)
    }
}
