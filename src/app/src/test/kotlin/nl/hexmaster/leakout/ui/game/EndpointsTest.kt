package nl.hexmaster.leakout.ui.game

import kotlin.math.hypot
import nl.hexmaster.leakout.core.level.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointsTest {
    @Test
    fun `the chevron points out of each open side`() {
        for (side in Side.entries) {
            val chevron = EndpointGeometry.chevron(side)
            val out = CellPoint(side.dx.toFloat(), side.dy.toFloat())
            fun along(p: CellPoint) = p.x * out.x + p.y * out.y
            assertEquals(EndpointGeometry.CHEVRON_TIP, along(chevron.tip), 1e-6f)
            assertTrue("$side legs behind the tip", along(chevron.legA) < along(chevron.tip) && along(chevron.legB) < along(chevron.tip))
            // The legs sit symmetrically either side of the stub's axis.
            fun across(p: CellPoint) = -p.x * out.y + p.y * out.x
            assertEquals(0f, across(chevron.tip), 1e-6f)
            assertEquals(-across(chevron.legA), across(chevron.legB), 1e-6f)
        }
    }

    @Test
    fun `the chevron sits on the stub between the wheel and the flange`() {
        val chevron = EndpointGeometry.chevron(Side.RIGHT)
        assertTrue(chevron.legA.x > EndpointGeometry.WHEEL_RADIUS - EndpointGeometry.CHEVRON_WIDTH)
        assertTrue(chevron.tip.x < 0.4f)
    }

    @Test
    fun `the checker has 36 squares spanning the ring's inner diameter, centred on the cell`() {
        val squares = EndpointGeometry.checker
        assertEquals(36, squares.size)
        val r = EndpointGeometry.RING_INNER_RADIUS
        assertEquals(-r, squares.minOf { it.left }, 1e-6f)
        assertEquals(-r, squares.minOf { it.top }, 1e-6f)
        assertEquals(r, squares.maxOf { it.left + it.size }, 1e-6f)
        assertEquals(r, squares.maxOf { it.top + it.size }, 1e-6f)
        assertEquals(0.215f, r, 1e-6f)
        // The centre is a corner shared by four squares.
        assertTrue(squares.any { kotlin.math.abs(it.left + it.size) < 1e-6f && kotlin.math.abs(it.top + it.size) < 1e-6f })
    }

    @Test
    fun `the top-left square is light and neighbours alternate`() {
        val squares = EndpointGeometry.checker
        val topLeft = squares.minWith(compareBy({ it.top }, { it.left }))
        assertTrue(topLeft.light)
        for (a in squares) for (b in squares) {
            val touching = kotlin.math.abs(a.left - b.left) + kotlin.math.abs(a.top - b.top)
            if (kotlin.math.abs(touching - a.size) < 1e-5f) assertNotEquals(a.light, b.light)
        }
        assertEquals(18, squares.count { it.light })
    }

    @Test
    fun `the checker reaches the ring, so no disc shows`() {
        val r = EndpointGeometry.RING_INNER_RADIUS
        val corner = EndpointGeometry.checker.maxOf { hypot(it.left, it.top) }
        assertTrue(corner >= r)
    }

    @Test
    fun `the valve pulses on 1_6 s while waiting`() {
        val at0 = EndpointGlow.valve(0, flowing = false, reducedMotion = false)
        val at800 = EndpointGlow.valve(800, flowing = false, reducedMotion = false)
        val at1600 = EndpointGlow.valve(1_600, flowing = false, reducedMotion = false)
        assertEquals(at0, at1600, 1e-6f)
        assertNotEquals(at0, at800, 1e-3f)
        assertEquals(0.45f, at0, 1e-6f)
        assertEquals(0.9f, at800, 1e-6f)
    }

    @Test
    fun `the valve is steady once the liquid flows and under reduced motion`() {
        for (t in listOf(0L, 400L, 800L, 1_234L)) {
            assertEquals(0.6f, EndpointGlow.valve(t, flowing = true, reducedMotion = false))
            assertEquals(0.6f, EndpointGlow.valve(t, flowing = false, reducedMotion = true))
        }
    }

    @Test
    fun `the finish pulses on 2 s until won`() {
        val at0 = EndpointGlow.finish(0, won = false, reducedMotion = false)
        assertEquals(at0, EndpointGlow.finish(2_000, won = false, reducedMotion = false), 1e-6f)
        assertEquals(0.9f, EndpointGlow.finish(1_000, won = false, reducedMotion = false), 1e-6f)
        assertNotEquals(at0, EndpointGlow.finish(800, won = false, reducedMotion = false), 1e-3f)
    }

    @Test
    fun `the finish is steady under reduced motion and off once won`() {
        assertEquals(0.6f, EndpointGlow.finish(1_000, won = false, reducedMotion = true))
        assertEquals(0f, EndpointGlow.finish(1_000, won = true, reducedMotion = false))
    }

    @Test
    fun `glows stay within their range`() {
        for (t in 0L..4_000L step 37) {
            assertTrue(EndpointGlow.valve(t, false, false) in 0.45f..0.9f)
            assertTrue(EndpointGlow.finish(t, false, false) in 0.45f..0.9f)
        }
    }
}
