package nl.hexmaster.leakout.core.level

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReservoirTypeTest {
    @Test
    fun `each factor maps to its type`() {
        assertEquals(ReservoirType.RED_10, ReservoirType.fromFactor(0.1))
        assertEquals(ReservoirType.BLUE_20, ReservoirType.fromFactor(0.2))
        assertEquals(ReservoirType.PURPLE_30, ReservoirType.fromFactor(0.3))
        assertEquals(ReservoirType.ORANGE_40, ReservoirType.fromFactor(0.4))
    }

    @Test
    fun `lookup tolerates floating point noise`() {
        assertEquals(ReservoirType.PURPLE_30, ReservoirType.fromFactor(0.1 + 0.2))
    }

    @Test
    fun `other factors have no type`() {
        for (factor in listOf(0.0, 0.25, 0.5, 1.0, 1.2, -0.1)) assertNull(ReservoirType.fromFactor(factor))
    }

    @Test
    fun `percent is the factor in percent`() {
        assertEquals(listOf(10, 20, 30, 40), ReservoirType.entries.map { it.percent })
    }

    @Test
    fun `default type is the default factor`() {
        assertEquals(LevelDefaults.RESERVOIR_SPEED_FACTOR, ReservoirType.DEFAULT.factor)
    }

    @Test
    fun `nearest snaps to the closest type`() {
        assertEquals(ReservoirType.RED_10, ReservoirType.nearest(0.0))
        assertEquals(ReservoirType.RED_10, ReservoirType.nearest(0.12))
        assertEquals(ReservoirType.BLUE_20, ReservoirType.nearest(0.17))
        assertEquals(ReservoirType.PURPLE_30, ReservoirType.nearest(0.31))
        assertEquals(ReservoirType.ORANGE_40, ReservoirType.nearest(0.5))
        assertEquals(ReservoirType.ORANGE_40, ReservoirType.nearest(1.0))
    }

    @Test
    fun `nearest sends a tie to the higher factor`() {
        assertEquals(ReservoirType.BLUE_20, ReservoirType.nearest(0.15))
        assertEquals(ReservoirType.PURPLE_30, ReservoirType.nearest(0.25))
        assertEquals(ReservoirType.ORANGE_40, ReservoirType.nearest(0.35))
    }
}
