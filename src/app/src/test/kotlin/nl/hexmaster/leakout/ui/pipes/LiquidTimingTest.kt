package nl.hexmaster.leakout.ui.pipes

import nl.hexmaster.leakout.core.level.PipeSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LiquidTimingTest {
    @Test
    fun `straight at 2 cells per second is half full after 250 ms`() {
        assertEquals(0.5f, LiquidTiming.progressAt(250f, 2f, PipeSection.H, 0.3f), 1e-6f)
    }

    @Test
    fun `reservoir is slowed by its factor`() {
        assertEquals(0.25f, LiquidTiming.progressAt(1000f, 1f, PipeSection.RES, 0.25f), 1e-6f)
        assertEquals(1f, LiquidTiming.progressAt(4000f, 1f, PipeSection.RES, 0.25f), 1e-6f)
        assertEquals(4000f, LiquidTiming.millisToFill(0f, 1f, PipeSection.RES, 0.25f), 1e-3f)
        assertEquals(1000f, LiquidTiming.millisToFill(0f, 1f, PipeSection.X, 0.25f), 1e-3f)
    }

    @Test
    fun `only the reservoir uses the factor`() {
        for (section in PipeSection.entries) {
            val expected = if (section == PipeSection.RES) 0.4f else 1f
            assertEquals(section.name, expected, LiquidTiming.speedFactor(section, 0.4f))
        }
    }

    @Test
    fun `progress is clamped to one`() {
        assertEquals(1f, LiquidTiming.progressAt(5000f, 1f, PipeSection.H, 0.3f))
        assertEquals(1f, LiquidTiming.advance(0.9f, 500f, 1f, PipeSection.H, 0.3f))
    }

    @Test
    fun `many small steps equal one big step`() {
        var progress = 0f
        repeat(60) { progress = LiquidTiming.advance(progress, 1000f / 120f, 1.3f, PipeSection.RES, 0.3f) }
        assertEquals(LiquidTiming.progressAt(500f, 1.3f, PipeSection.RES, 0.3f), progress, 1e-5f)
    }

    @Test
    fun `speeds of zero or less are refused`() {
        assertThrows(IllegalArgumentException::class.java) { LiquidTiming.progressAt(100f, 0f, PipeSection.H, 0.3f) }
        assertThrows(IllegalArgumentException::class.java) { LiquidTiming.progressAt(100f, -1f, PipeSection.H, 0.3f) }
        assertThrows(IllegalArgumentException::class.java) { LiquidTiming.advance(0f, 10f, 1f, PipeSection.RES, 0f) }
        assertThrows(IllegalArgumentException::class.java) { LiquidTiming.advance(0f, -10f, 1f, PipeSection.H, 0.3f) }
    }
}
