package nl.hexmaster.leakout.ui.game

import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HudSparkleTest {
    @Test
    fun `sparkle lasts 600 ms`() {
        assertNotNull(sparkleFrame(0))
        assertNotNull(sparkleFrame(599))
        assertNull(sparkleFrame(600))
        assertNull(sparkleFrame(-1))
    }

    @Test
    fun `starts with a full flash and particles at the centre`() {
        val frame = sparkleFrame(0)!!
        assertEquals(1f, frame.flashAlpha, 0.001f)
        assertEquals(HudSparkle.PARTICLES, frame.particles.size)
        frame.particles.forEach {
            assertEquals(0f, hypot(it.dx, it.dy), 0.001f)
            assertEquals(1f, it.alpha, 0.001f)
        }
    }

    @Test
    fun `flash is gone after 300 ms`() {
        assertEquals(0.5f, sparkleFrame(150)!!.flashAlpha, 0.001f)
        assertEquals(0f, sparkleFrame(300)!!.flashAlpha, 0.001f)
    }

    @Test
    fun `particles fly out 6 to 14 dp and fade`() {
        val late = sparkleFrame(599)!!
        val reaches = late.particles.map { hypot(it.dx, it.dy) }
        assertTrue(reaches.all { it in 5.9f..14f })
        assertEquals(14f, reaches.max(), 0.1f)
        assertEquals(6f, reaches.min(), 0.1f)
        assertTrue(late.particles.all { it.alpha < 0.01f })
        assertEquals(HudSparkle.PARTICLES / 2, late.particles.count { it.glow })
    }
}
