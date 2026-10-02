package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.ChannelFill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldLiquidTest {
    private val at = Position(2, 3)

    private fun head(channel: Channel, entry: Side, progress: Double, position: Position = at) =
        FlowState.Flowing(position, entry, channel, progress, emptySet())

    @Test
    fun `a dry section has no liquid`() {
        assertTrue(FieldLiquid.fills(at, PipeSection.H, emptySet(), null).isEmpty())
        assertTrue(FieldLiquid.fills(at, PipeSection.H, emptySet(), head(Channel.SINGLE, Side.LEFT, 0.5, Position(0, 0))).isEmpty())
    }

    @Test
    fun `the head tile fills from its entry side up to the progress`() {
        val fills = FieldLiquid.fills(at, PipeSection.LT, emptySet(), head(Channel.SINGLE, Side.TOP, 0.25))
        assertEquals(mapOf("channel-main" to ChannelFill(0.25f, Side.TOP)), fills)
    }

    @Test
    fun `a channel the liquid ran through is full`() {
        val fills = FieldLiquid.fills(at, PipeSection.RB, setOf(ChannelUse(at, Channel.SINGLE)), null)
        assertEquals(1f, fills.getValue("channel-main").progress)
        assertTrue(fills.getValue("channel-main").entry in PipeSection.RB.openSides)
    }

    @Test
    fun `cross channels fill independently`() {
        val used = setOf(ChannelUse(at, Channel.HORIZONTAL))
        val fills = FieldLiquid.fills(at, PipeSection.X, used, head(Channel.VERTICAL, Side.BOTTOM, 0.6))
        assertEquals(ChannelFill.full(Side.LEFT), fills["channel-h"])
        assertEquals(0.6f, fills.getValue("channel-v").progress, 1e-6f)
        assertEquals(Side.BOTTOM, fills.getValue("channel-v").entry)
    }

    @Test
    fun `used channels of other tiles do not leak in`() {
        val fills = FieldLiquid.fills(at, PipeSection.V, setOf(ChannelUse(Position(2, 2), Channel.SINGLE)), null)
        assertTrue(fills.isEmpty())
    }

    @Test
    fun `progress is passed through unchanged, so the flow is not eased`() {
        for (p in listOf(0.0, 0.1, 0.33, 0.5, 0.9, 0.999)) {
            val fill = FieldLiquid.fills(at, PipeSection.H, emptySet(), head(Channel.SINGLE, Side.LEFT, p)).getValue("channel-main")
            assertEquals(p.toFloat(), fill.progress, 1e-6f)
        }
    }

    @Test
    fun `the start is dry while the countdown runs`() {
        assertEquals(0f, FieldLiquid.startFill(at, emptySet(), null))
    }

    @Test
    fun `the start fills with the head's progress once the flow starts`() {
        assertEquals(0f, FieldLiquid.startFill(at, emptySet(), head(Channel.SINGLE, Side.LEFT, 0.0)))
        assertEquals(0.4f, FieldLiquid.startFill(at, emptySet(), head(Channel.SINGLE, Side.LEFT, 0.4)), 1e-6f)
    }

    @Test
    fun `the start stays full once the head has left it`() {
        val next = head(Channel.SINGLE, Side.LEFT, 0.1, Position(3, 3))
        assertEquals(1f, FieldLiquid.startFill(at, setOf(ChannelUse(at, Channel.SINGLE)), next))
        assertEquals(0f, FieldLiquid.startFill(at, emptySet(), next))
    }
}
