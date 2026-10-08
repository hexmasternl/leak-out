package nl.hexmaster.leakout.ui.pipes

import nl.hexmaster.leakout.core.level.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidSpansTest {
    private val h = ArtChannel("channel-main", Side.LEFT, Side.RIGHT, "M0 50 L100 50")
    private val lt = ArtChannel("channel-main", Side.LEFT, Side.TOP, "M0 50 A50 50 0 0 0 50 0")
    private val tank = TankCircle(50f, 50f, 26f)

    private fun assertSpan(start: Float, end: Float, head: Float?, span: PathSpan) {
        assertEquals("start", start, span.start, 1e-5f)
        assertEquals("end", end, span.end, 1e-5f)
        if (head == null) assertNull("head", span.head) else assertEquals("head", head, span.head!!, 1e-5f)
    }

    @Test
    fun `straight half filled from the left`() {
        assertSpan(0f, 0.5f, 0.5f, LiquidSpans.forChannel(h, ChannelFill(0.5f, Side.LEFT)))
    }

    @Test
    fun `straight a quarter filled from the right`() {
        assertSpan(0.75f, 1f, 0.75f, LiquidSpans.forChannel(h, ChannelFill(0.25f, Side.RIGHT)))
    }

    @Test
    fun `corner fills the leg of its entry side`() {
        // The written path runs left → top; half way is the middle of the elbow.
        assertSpan(0f, 0.5f, 0.5f, LiquidSpans.forChannel(lt, ChannelFill(0.5f, Side.LEFT)))
        assertSpan(0.5f, 1f, 0.5f, LiquidSpans.forChannel(lt, ChannelFill(0.5f, Side.TOP)))
        assertSpan(0.8f, 1f, 0.8f, LiquidSpans.forChannel(lt, ChannelFill(0.2f, Side.TOP)))
    }

    @Test
    fun `empty and full have no head`() {
        val empty = LiquidSpans.forChannel(h, ChannelFill(0f, Side.LEFT))
        assertTrue(empty.isEmpty)
        assertNull(empty.head)
        assertSpan(0f, 1f, null, LiquidSpans.forChannel(h, ChannelFill(1f, Side.RIGHT)))
    }

    @Test
    fun `progress is clamped`() {
        assertSpan(0f, 1f, null, LiquidSpans.forChannel(h, ChannelFill(1.7f, Side.LEFT)))
        assertTrue(LiquidSpans.forChannel(h, ChannelFill(-0.2f, Side.RIGHT)).isEmpty)
    }

    @Test
    fun `entry side not on the channel fails`() {
        assertThrows(IllegalArgumentException::class.java) { LiquidSpans.forChannel(h, ChannelFill(0.5f, Side.TOP)) }
        assertThrows(IllegalArgumentException::class.java) {
            LiquidSpans.forReservoir(h, tank, ChannelFill(0.5f, Side.BOTTOM))
        }
    }

    // Tank edges at x = 24 and x = 76: the stubs are 0–0.24 and 0.76–1 of the path.

    @Test
    fun `reservoir from the right at 5 percent fills the outer half of the right stub`() {
        val fill = LiquidSpans.forReservoir(h, tank, ChannelFill(0.05f, Side.RIGHT))
        assertSpan(0.88f, 1f, 0.88f, fill.inlet)
        assertEquals(0f, fill.tankLevel)
        assertTrue(fill.outlet.isEmpty)
    }

    @Test
    fun `reservoir from the left at half has a full inlet and a half tank`() {
        val fill = LiquidSpans.forReservoir(h, tank, ChannelFill(0.5f, Side.LEFT))
        assertSpan(0f, 0.24f, null, fill.inlet)
        assertEquals(0.5f, fill.tankLevel, 1e-5f)
        assertTrue(fill.outlet.isEmpty)
    }

    @Test
    fun `reservoir outlet fills from the tank to the exit`() {
        val fromLeft = LiquidSpans.forReservoir(h, tank, ChannelFill(0.95f, Side.LEFT))
        assertEquals(1f, fromLeft.tankLevel)
        assertSpan(0.76f, 0.88f, 0.88f, fromLeft.outlet)

        val fromRight = LiquidSpans.forReservoir(h, tank, ChannelFill(0.95f, Side.RIGHT))
        assertSpan(0.76f, 1f, null, fromRight.inlet)
        assertSpan(0.12f, 0.24f, 0.12f, fromRight.outlet)
    }

    @Test
    fun `reservoir phase boundaries`() {
        val atInletEnd = LiquidSpans.forReservoir(h, tank, ChannelFill(0.1f, Side.LEFT))
        assertSpan(0f, 0.24f, null, atInletEnd.inlet)
        assertEquals(0f, atInletEnd.tankLevel)

        val atOutletStart = LiquidSpans.forReservoir(h, tank, ChannelFill(0.9f, Side.LEFT))
        assertEquals(1f, atOutletStart.tankLevel, 1e-5f)
        assertTrue(atOutletStart.outlet.isEmpty)

        val full = LiquidSpans.forReservoir(h, tank, ChannelFill(1f, Side.LEFT))
        assertSpan(0.76f, 1f, null, full.outlet)
    }

    @Test
    fun `reservoir needs a horizontal channel`() {
        val v = ArtChannel("channel-main", Side.TOP, Side.BOTTOM, "M50 0 L50 100")
        assertThrows(IllegalArgumentException::class.java) {
            LiquidSpans.forReservoir(v, tank, ChannelFill(0.5f, Side.TOP))
        }
    }
}
