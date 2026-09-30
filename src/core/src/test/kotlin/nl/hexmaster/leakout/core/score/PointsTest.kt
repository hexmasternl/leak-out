package nl.hexmaster.leakout.core.score

import kotlin.test.Test
import kotlin.test.assertEquals
import nl.hexmaster.leakout.core.level.PipeSection

class PointsTest {
    @Test
    fun `straight sections give 10`() {
        assertEquals(10, Points.forExit(PipeSection.H, 1))
        assertEquals(10, Points.forExit(PipeSection.V, 1))
    }

    @Test
    fun `corner sections give 15`() {
        for (corner in listOf(PipeSection.LT, PipeSection.LB, PipeSection.RT, PipeSection.RB)) {
            assertEquals(15, Points.forExit(corner, 1), "$corner")
        }
    }

    @Test
    fun `reservoir gives 25`() {
        assertEquals(25, Points.forExit(PipeSection.RES, 1))
    }

    @Test
    fun `cross gives 10 for the first pass and 50 for the second`() {
        assertEquals(10, Points.forExit(PipeSection.X, 1))
        assertEquals(50, Points.forExit(PipeSection.X, 2))
    }

    @Test
    fun `only the cross depends on the pass`() {
        for (section in PipeSection.entries - PipeSection.X) {
            assertEquals(Points.forExit(section, 1), Points.forExit(section, 2), "$section")
        }
    }
}
