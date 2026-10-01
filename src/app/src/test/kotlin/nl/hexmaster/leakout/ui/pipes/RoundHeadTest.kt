package nl.hexmaster.leakout.ui.pipes

import nl.hexmaster.leakout.core.level.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundHeadTest {
    private val h = ArtChannel("channel-main", Side.LEFT, Side.RIGHT, "M0 50 L100 50")
    private val cap = 0.105f

    private fun headed(progress: Float, entry: Side) =
        LiquidSpans.withRoundHead(LiquidSpans.forChannel(h, ChannelFill(progress, entry)), cap)

    @Test
    fun `the cap front is on the head when flowing forwards`() {
        val span = headed(0.5f, Side.LEFT)
        assertEquals(0.5f - cap, span.capCenter!!, 1e-5f)
        assertEquals(0f, span.body.start, 1e-5f)
        assertEquals(0.5f - cap, span.body.end, 1e-5f)
    }

    @Test
    fun `the cap front is on the head when flowing backwards`() {
        val span = headed(0.5f, Side.RIGHT)
        assertEquals(0.5f + cap, span.capCenter!!, 1e-5f)
        assertEquals(0.5f + cap, span.body.start, 1e-5f)
        assertEquals(1f, span.body.end, 1e-5f)
    }

    @Test
    fun `just after entering only the cap shows, centred before the entry border`() {
        val forwards = headed(0.02f, Side.LEFT)
        assertTrue(forwards.body.isEmpty)
        assertEquals(0.02f - cap, forwards.capCenter!!, 1e-5f)

        val backwards = headed(0.02f, Side.RIGHT)
        assertTrue(backwards.body.isEmpty)
        assertEquals(1f - 0.02f + cap, backwards.capCenter!!, 1e-5f)
    }

    @Test
    fun `the front moves at the speed of the progress, so it is linear in time`() {
        val fronts = (1..9).map { headed(it / 10f, Side.LEFT).capCenter!! + cap }
        fronts.zipWithNext().forEach { (a, b) -> assertEquals(0.1f, b - a, 1e-5f) }
    }

    @Test
    fun `just before leaving the front reaches the exit border`() {
        assertEquals(1f, headed(0.9999f, Side.LEFT).capCenter!! + cap, 1e-3f)
        assertEquals(0f, headed(0.9999f, Side.RIGHT).capCenter!! - cap, 1e-3f)
    }

    @Test
    fun `full and empty spans have no cap and keep their extent`() {
        val full = headed(1f, Side.LEFT)
        assertNull(full.capCenter)
        assertEquals(PathSpan(0f, 1f), full.body)
        assertNull(headed(0f, Side.LEFT).capCenter)
    }

    @Test
    fun `a negative cap is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            LiquidSpans.withRoundHead(PathSpan(0f, 0.5f, head = 0.5f), -0.1f)
        }
    }
}
