package nl.hexmaster.leakout.ui.game

import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.ChannelFill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
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

    /** The single-pass `fills` from before the split into settled and head fills, as the reference. */
    private fun oldFills(position: Position, section: PipeSection, used: Set<ChannelUse>, head: FlowState.Flowing?): Map<String, ChannelFill> {
        var fills: Map<String, ChannelFill> = emptyMap()
        for (channel in Channel.entries) {
            if (ChannelUse(position, channel) in used) {
                val entry = when (channel) {
                    Channel.HORIZONTAL -> Side.LEFT
                    Channel.VERTICAL -> Side.TOP
                    Channel.SINGLE -> Side.entries.first { it in section.openSides }
                }
                fills = fills + (FieldLiquid.channelId(channel) to ChannelFill.full(entry))
            }
        }
        if (head != null && head.position == position) {
            fills = fills + (FieldLiquid.channelId(head.channel) to ChannelFill(head.progress.toFloat(), head.entry))
        }
        return fills
    }

    @Test
    fun `settled plus head fills equal the old fills for straight, cross and reservoir tiles`() {
        val elsewhere = Position(0, 0)
        val cases = listOf(
            Triple(PipeSection.H, emptySet(), head(Channel.SINGLE, Side.LEFT, 0.4)),
            Triple(PipeSection.H, setOf(ChannelUse(at, Channel.SINGLE)), null),
            Triple(PipeSection.V, setOf(ChannelUse(at, Channel.SINGLE)), head(Channel.SINGLE, Side.TOP, 0.2, elsewhere)),
            Triple(PipeSection.X, emptySet(), head(Channel.HORIZONTAL, Side.RIGHT, 0.7)),
            Triple(PipeSection.X, setOf(ChannelUse(at, Channel.HORIZONTAL)), head(Channel.VERTICAL, Side.BOTTOM, 0.6)),
            Triple(PipeSection.X, setOf(ChannelUse(at, Channel.HORIZONTAL), ChannelUse(at, Channel.VERTICAL)), null),
            Triple(PipeSection.RES, emptySet(), head(Channel.SINGLE, Side.LEFT, 0.9)),
            Triple(PipeSection.RES, setOf(ChannelUse(at, Channel.SINGLE)), head(Channel.SINGLE, Side.LEFT, 0.1, elsewhere)),
            Triple(PipeSection.RES, emptySet(), null),
        )
        for ((section, used, head) in cases) {
            val expected = oldFills(at, section, used, head)
            assertEquals("$section $used $head", expected, FieldLiquid.withHead(FieldLiquid.settledFills(at, section, used), at, head))
            assertEquals("$section $used $head", expected, FieldLiquid.fills(at, section, used, head))
        }
    }

    @Test
    fun `the settled fills are cached per board and used set`() {
        val cache = SettledFillCache()
        val board = Board(GridSize(4, 4), emptyMap())
        val used = setOf(ChannelUse(at, Channel.SINGLE))
        val first = cache.fills(board, used, at, PipeSection.H)
        assertSame(first, cache.fills(board, used, at, PipeSection.H))
        // A new used set, even an equal one, is worked out again; so is a new board.
        val moreUsed = used + ChannelUse(Position(1, 3), Channel.SINGLE)
        assertNotSame(first, cache.fills(board, moreUsed, at, PipeSection.H))
        assertEquals(first, cache.fills(Board(GridSize(4, 4), emptyMap()), moreUsed, at, PipeSection.H))
        assertTrue(cache.fills(board, emptySet(), at, PipeSection.H).isEmpty())
    }

    // The start valve's stub: `at` is the start, opening to the right, so the head enters from the left.

    @Test
    fun `the start stub is dry while waiting for the liquid`() {
        assertEquals(0f, FieldLiquid.startProgress(at, emptySet(), null))
        assertTrue(FieldLiquid.startFills(at, Side.RIGHT, emptySet(), null).isEmpty())
    }

    @Test
    fun `half the start stub is filled at head progress 0_5 in the start`() {
        val inStart = head(Channel.SINGLE, Side.LEFT, 0.5)
        assertEquals(0.5f, FieldLiquid.startProgress(at, emptySet(), inStart))
        // The channel runs from the far side; the visible half starts at 0.5, so a quarter more is half the stub.
        assertEquals(mapOf("channel-main" to ChannelFill(0.75f, Side.LEFT)), FieldLiquid.startFills(at, Side.RIGHT, emptySet(), inStart))
    }

    @Test
    fun `the start stub fills towards the open side whichever side it is`() {
        for (side in Side.entries) {
            val fill = FieldLiquid.startFills(at, side, emptySet(), head(Channel.SINGLE, side.opposite(), 0.2)).getValue("channel-main")
            assertEquals(side.opposite(), fill.entry)
            assertEquals(0.6f, fill.progress, 1e-6f)
        }
    }

    @Test
    fun `the start stub is full once the head has left`() {
        val used = setOf(ChannelUse(at, Channel.SINGLE))
        val next = head(Channel.SINGLE, Side.LEFT, 0.3, Position(3, 3))
        assertEquals(1f, FieldLiquid.startProgress(at, used, next))
        assertEquals(ChannelFill.full(Side.LEFT), FieldLiquid.startFills(at, Side.RIGHT, used, next)["channel-main"])
        assertEquals(1f, FieldLiquid.startProgress(at, used, null))
    }

    @Test
    fun `a head elsewhere does not fill the start stub`() {
        assertEquals(0f, FieldLiquid.startProgress(at, emptySet(), head(Channel.SINGLE, Side.LEFT, 0.7, Position(0, 0))))
    }

    @Test
    fun `the start stub is dry again after a restart`() {
        // A restart begins a new attempt: the flow is waiting again and nothing is used.
        val restarted: FlowState = FlowState.Waiting(3_000)
        assertEquals(0f, FieldLiquid.startProgress(at, restarted.used, restarted as? FlowState.Flowing))
        assertTrue(FieldLiquid.startFills(at, Side.RIGHT, restarted.used, null).isEmpty())
    }
}
