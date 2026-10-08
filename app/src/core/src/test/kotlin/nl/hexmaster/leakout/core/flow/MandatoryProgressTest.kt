package nl.hexmaster.leakout.core.flow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position

class MandatoryProgressTest {
    private val cross = MandatorySection(Position(3, 2), PipeSection.X)
    private val reservoir = MandatorySection(Position(5, 4), PipeSection.RES)

    @Test
    fun `a reservoir passed once is used`() {
        assertTrue(MandatoryProgress.isUsed(reservoir, setOf(ChannelUse(Position(5, 4), Channel.SINGLE))))
    }

    @Test
    fun `a cross passed on one channel is not used`() {
        assertFalse(MandatoryProgress.isUsed(cross, setOf(ChannelUse(Position(3, 2), Channel.HORIZONTAL))))
        assertFalse(MandatoryProgress.isUsed(cross, setOf(ChannelUse(Position(3, 2), Channel.VERTICAL))))
    }

    @Test
    fun `a cross passed on both channels is used`() {
        val used = setOf(ChannelUse(Position(3, 2), Channel.HORIZONTAL), ChannelUse(Position(3, 2), Channel.VERTICAL))
        assertTrue(MandatoryProgress.isUsed(cross, used))
    }

    @Test
    fun `channels on other cells do not count`() {
        val used = setOf(ChannelUse(Position(3, 3), Channel.HORIZONTAL), ChannelUse(Position(3, 3), Channel.VERTICAL))
        assertFalse(MandatoryProgress.isUsed(cross, used))
        assertFalse(MandatoryProgress.isUsed(reservoir, setOf(ChannelUse(Position(4, 4), Channel.SINGLE))))
    }

    @Test
    fun `unused lists the sections still missing, in order`() {
        val mandatory = listOf(cross, reservoir)
        assertEquals(mandatory, MandatoryProgress.unused(mandatory, emptySet()))
        assertEquals(listOf(cross), MandatoryProgress.unused(mandatory, setOf(ChannelUse(Position(5, 4), Channel.SINGLE))))
        val all = setOf(
            ChannelUse(Position(5, 4), Channel.SINGLE),
            ChannelUse(Position(3, 2), Channel.VERTICAL),
            ChannelUse(Position(3, 2), Channel.HORIZONTAL),
        )
        assertEquals(emptyList(), MandatoryProgress.unused(mandatory, all))
        assertEquals(emptyList(), MandatoryProgress.unused(emptyList(), emptySet()))
    }
}
