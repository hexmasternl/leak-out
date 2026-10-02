package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.engine.Replacement
import nl.hexmaster.leakout.core.level.LevelDefaults
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplacementEffectTest {
    private fun replacement(startedAtMs: Long = 1_000, durationMs: Long = 1_500) =
        Replacement(Position(2, 1), PipeSection.H, PipeSection.V, startedAtMs, startedAtMs + durationMs)

    @Test
    fun `the sequence is a 600 ms break, a 400 ms hold and a 500 ms fade, the default penalty`() {
        assertEquals(600L, ReplacementTiming.BREAK_MS)
        assertEquals(400L, ReplacementTiming.HOLD_MS)
        assertEquals(500L, ReplacementTiming.FADE_MS)
        assertEquals(LevelDefaults.REPLACE_PENALTY_MS, ReplacementTiming.SEQUENCE_MS)
    }

    @Test
    fun `the section breaks over the first 600 ms`() {
        val replacement = replacement()
        assertEquals(0f, replacementFrame(replacement, 1_000).breakProgress)
        assertEquals(0.5f, replacementFrame(replacement, 1_300).breakProgress, 1e-5f)
        assertEquals(1f, replacementFrame(replacement, 1_600).breakProgress, 1e-5f)
    }

    @Test
    fun `the broken pieces lie still and fully visible for 400 ms`() {
        val replacement = replacement()
        for (ms in listOf(1_600L, 1_800L, 2_000L)) {
            val frame = replacementFrame(replacement, ms)
            assertEquals("scatter at $ms", 1f, frame.scatter, 1e-5f)
            assertEquals("alpha at $ms", 1f, frame.alpha, 1e-5f)
        }
    }

    @Test
    fun `the pieces fade out over the last 500 ms`() {
        val replacement = replacement()
        assertEquals(0.5f, replacementFrame(replacement, 2_250).alpha, 1e-4f)
        assertEquals(0f, replacementFrame(replacement, 2_500).alpha, 1e-5f)
        assertEquals(0f, replacementFrame(replacement, 9_000).alpha, 1e-5f)
        assertEquals(1f, replacementFrame(replacement, 2_250).scatter, 1e-5f)
    }

    @Test
    fun `a replacement of another length scales the phases along`() {
        val replacement = replacement(durationMs = 3_000)
        assertEquals(1f, replacementFrame(replacement, 2_200).breakProgress, 1e-5f)
        assertEquals(1f, replacementFrame(replacement, 3_000).alpha, 1e-5f)
        assertEquals(0.5f, replacementFrame(replacement, 3_500).alpha, 1e-4f)
    }

    @Test
    fun `a replacement without length shows nothing`() {
        assertEquals(0f, replacementFrame(replacement(durationMs = 0), 1_000).alpha)
    }

    @Test
    fun `the section cracks before the fragments fly`() {
        val replacement = replacement()
        val cracking = replacementFrame(replacement, 1_090) // 15 % of the break
        assertEquals(0.5f, cracking.crack, 1e-4f)
        assertEquals(0f, cracking.scatter)
        val flying = replacementFrame(replacement, 1_400)
        assertEquals(1f, flying.crack)
        assertTrue(flying.scatter in 0f..1f && flying.scatter > 0f)
    }

    @Test
    fun `the fragments never fly back`() {
        val replacement = replacement()
        var previous = 0f
        for (ms in 1_000L until 1_600L) {
            val scatter = replacementFrame(replacement, ms).scatter
            assertTrue("came back at $ms ms", scatter >= previous)
            previous = scatter
        }
    }

    @Test
    fun `a tile always breaks into the same four quarters, each thrown outward`() {
        val fragments = fragmentsFor(Position(3, 4))
        assertEquals(fragments, fragmentsFor(Position(3, 4)))
        assertEquals(setOf(0 to 0, 1 to 0, 0 to 1, 1 to 1), fragments.map { it.column to it.row }.toSet())
        for (fragment in fragments) {
            assertTrue(if (fragment.column == 0) fragment.dx < 0 else fragment.dx > 0)
            assertTrue(if (fragment.row == 0) fragment.dy < 0 else fragment.dy > 0)
        }
    }

    @Test
    fun `the landing pops in from small, overshoots and settles`() {
        assertEquals(0.6f, landingPopScale(0)!!, 1e-6f)
        assertTrue(landingPopScale(110)!! > 1f)
        assertNotNull(landingPopScale(ReplacementTiming.LANDING_POP_MS - 1))
        assertNull(landingPopScale(ReplacementTiming.LANDING_POP_MS))
        assertNull(landingPopScale(-1))
    }
}
