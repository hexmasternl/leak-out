package nl.hexmaster.leakout.core.level

import kotlin.test.Test
import kotlin.test.assertEquals

class PipeSectionTest {
    @Test
    fun `sections expose their open sides`() {
        val expected = mapOf(
            PipeSection.H to setOf(Side.LEFT, Side.RIGHT),
            PipeSection.V to setOf(Side.TOP, Side.BOTTOM),
            PipeSection.LT to setOf(Side.LEFT, Side.TOP),
            PipeSection.LB to setOf(Side.LEFT, Side.BOTTOM),
            PipeSection.RT to setOf(Side.RIGHT, Side.TOP),
            PipeSection.RB to setOf(Side.RIGHT, Side.BOTTOM),
            PipeSection.X to Side.entries.toSet(),
            PipeSection.RES to setOf(Side.LEFT, Side.RIGHT),
        )

        for ((section, sides) in expected) {
            assertEquals(sides, section.openSides, section.name)
        }
    }
}
