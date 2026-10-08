package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.graphics.Color
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.ChannelUse
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.ChannelFill
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalStyleTest {
    // Pair 1: portal a at (1, 1) open to the right, portal b at (6, 3) open to the left.
    private val a = Position(1, 1)
    private val b = Position(6, 3)
    private val portalA = Cell.Portal(1, Side.RIGHT, partner = b)
    private val portalB = Cell.Portal(1, Side.LEFT, partner = a)

    private fun inPair(progress: Double, entered: Position = a) =
        FlowState.Flowing(entered, if (entered == a) Side.RIGHT else Side.LEFT, Channel.PORTAL, progress, emptySet())

    @Test
    fun `the portal tokens have the design system values`() {
        assertEquals(Color(0xFFFF6FD8), LeakOutColors.Portal1)
        assertEquals(Color(0xFFB8F25C), LeakOutColors.Portal2)
        assertEquals(Color(0xFFFF8A3D), LeakOutColors.Portal3)
    }

    @Test
    fun `pairs 1 to 3 take the first to third colour and symbol`() {
        assertEquals(listOf(LeakOutColors.Portal1, LeakOutColors.Portal2, LeakOutColors.Portal3), (1..3).map(PortalStyle::color))
        assertEquals(listOf(PortalSymbol.CIRCLE, PortalSymbol.TRIANGLE, PortalSymbol.SQUARE), (1..3).map(PortalStyle::symbol))
    }

    @Test
    fun `two pairs differ in both colour and symbol`() {
        for (one in 1..3) for (other in 1..3) if (one != other) {
            assertNotEquals(PortalStyle.color(one), PortalStyle.color(other))
            assertNotEquals(PortalStyle.symbol(one), PortalStyle.symbol(other))
        }
    }

    @Test
    fun `the swirl turns once per motion drift and stands still with reduced motion`() {
        assertEquals(0f, PortalStyle.swirlDegrees(0, reducedMotion = false))
        assertEquals(90f, PortalStyle.swirlDegrees(400, reducedMotion = false))
        assertEquals(90f, PortalStyle.swirlDegrees(400 + PortalStyle.SWIRL_PERIOD_MS, reducedMotion = false))
        assertEquals(0f, PortalStyle.swirlDegrees(400, reducedMotion = true))
    }

    @Test
    fun `dry portals have empty stubs`() {
        assertEquals(0f, PortalStyle.stubProgress(a, portalA, emptySet(), null))
        assertEquals(0f, PortalStyle.stubProgress(b, portalB, emptySet(), null))
    }

    @Test
    fun `the first half of the pair fills the entry stub`() {
        assertEquals(0.5f, PortalStyle.stubProgress(a, portalA, emptySet(), inPair(0.25)), 1e-6f)
        assertEquals(0f, PortalStyle.stubProgress(b, portalB, emptySet(), inPair(0.25)), 1e-6f)
    }

    @Test
    fun `halfway through the pair the entry stub is full and the exit stub still empty`() {
        assertEquals(1f, PortalStyle.stubProgress(a, portalA, emptySet(), inPair(0.5)), 1e-6f)
        assertEquals(0f, PortalStyle.stubProgress(b, portalB, emptySet(), inPair(0.5)), 1e-6f)
    }

    @Test
    fun `at 750 ms of a pair at 1 cell per second the exit stub is half full`() {
        assertEquals(1f, PortalStyle.stubProgress(a, portalA, emptySet(), inPair(0.75)), 1e-6f)
        assertEquals(0.5f, PortalStyle.stubProgress(b, portalB, emptySet(), inPair(0.75)), 1e-6f)
    }

    @Test
    fun `the pair works the other way round`() {
        assertEquals(0.5f, PortalStyle.stubProgress(a, portalA, emptySet(), inPair(0.75, entered = b)), 1e-6f)
        assertEquals(1f, PortalStyle.stubProgress(b, portalB, emptySet(), inPair(0.75, entered = b)), 1e-6f)
    }

    @Test
    fun `both stubs stay full once the pair is passed`() {
        val used = setOf(ChannelUse(a, Channel.PORTAL), ChannelUse(b, Channel.PORTAL))
        assertEquals(1f, PortalStyle.stubProgress(a, portalA, used, null))
        assertEquals(1f, PortalStyle.stubProgress(b, portalB, used, null))
    }

    @Test
    fun `the entry stub fills inwards from the open side and the exit stub outwards from the ring`() {
        assertEquals(mapOf("channel-main" to ChannelFill(0.25f, Side.RIGHT)), FieldLiquid.portalFills(a, portalA, emptySet(), inPair(0.25)))
        assertEquals(mapOf("channel-main" to ChannelFill(0.75f, Side.RIGHT)), FieldLiquid.portalFills(b, portalB, emptySet(), inPair(0.75)))
        assertTrue(FieldLiquid.portalFills(b, portalB, emptySet(), inPair(0.25)).isEmpty())
        val full = FieldLiquid.portalFills(a, portalA, setOf(ChannelUse(a, Channel.PORTAL)), null).getValue("channel-main")
        assertEquals(1f, full.progress)
    }
}
