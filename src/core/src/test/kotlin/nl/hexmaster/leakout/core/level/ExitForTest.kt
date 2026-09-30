package nl.hexmaster.leakout.core.level

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExitForTest {
    private val expected: Map<PipeSection, Map<Side, Side?>> = mapOf(
        PipeSection.H to mapOf(Side.LEFT to Side.RIGHT, Side.RIGHT to Side.LEFT, Side.TOP to null, Side.BOTTOM to null),
        PipeSection.V to mapOf(Side.LEFT to null, Side.RIGHT to null, Side.TOP to Side.BOTTOM, Side.BOTTOM to Side.TOP),
        PipeSection.LT to mapOf(Side.LEFT to Side.TOP, Side.RIGHT to null, Side.TOP to Side.LEFT, Side.BOTTOM to null),
        PipeSection.LB to mapOf(Side.LEFT to Side.BOTTOM, Side.RIGHT to null, Side.TOP to null, Side.BOTTOM to Side.LEFT),
        PipeSection.RT to mapOf(Side.LEFT to null, Side.RIGHT to Side.TOP, Side.TOP to Side.RIGHT, Side.BOTTOM to null),
        PipeSection.RB to mapOf(Side.LEFT to null, Side.RIGHT to Side.BOTTOM, Side.TOP to null, Side.BOTTOM to Side.RIGHT),
        PipeSection.X to mapOf(Side.LEFT to Side.RIGHT, Side.RIGHT to Side.LEFT, Side.TOP to Side.BOTTOM, Side.BOTTOM to Side.TOP),
        PipeSection.RES to mapOf(Side.LEFT to Side.RIGHT, Side.RIGHT to Side.LEFT, Side.TOP to null, Side.BOTTOM to null),
    )

    @Test
    fun `every section and entry side has the expected exit`() {
        assertEquals(PipeSection.entries.toSet(), expected.keys)
        for ((section, exits) in expected) {
            for (entry in Side.entries) {
                assertEquals(exits.getValue(entry), section.exitFor(entry), "$section entered from $entry")
            }
        }
    }

    @Test
    fun `a reservoir has no channel from the top or the bottom`() {
        assertNull(PipeSection.RES.exitFor(Side.TOP))
        assertNull(PipeSection.RES.exitFor(Side.BOTTOM))
    }

    @Test
    fun `a cross is passed straight through from every side`() {
        for (entry in Side.entries) assertEquals(entry.opposite(), PipeSection.X.exitFor(entry))
    }

    @Test
    fun `the exit of an exit leads back to the entry`() {
        for (section in PipeSection.entries) {
            for (entry in Side.entries) {
                val exit = section.exitFor(entry) ?: continue
                assertEquals(entry, section.exitFor(exit), "$section entered from $entry")
            }
        }
    }
}
