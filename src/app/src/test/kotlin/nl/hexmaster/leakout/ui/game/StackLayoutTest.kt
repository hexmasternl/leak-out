package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StackLayoutTest {
    @Test
    fun `every visible size fits 360dp without scrolling`() {
        for (visibleSize in 1..10) {
            val metrics = StackLayout.metrics(360.dp, visibleSize)
            assertTrue("$visibleSize does not fit", metrics.totalHeight(visibleSize) <= 360.dp)
        }
    }

    @Test
    fun `next slot is larger than preview slots`() {
        for (visibleSize in 1..10) {
            val metrics = StackLayout.metrics(360.dp, visibleSize)
            assertTrue(metrics.nextSlotSize > metrics.previewSlotSize)
        }
    }

    @Test
    fun `maximum slot size is used for small stacks`() {
        val metrics = StackLayout.metrics(360.dp, 1)
        assertEquals(40.dp, metrics.previewSlotSize)
        assertEquals(50.dp, metrics.nextSlotSize)
    }

    @Test
    fun `every visible size fits 360dp minus the HUD`() {
        val available = 360.dp - HudLayout.HEIGHT
        for (visibleSize in 1..10) {
            val metrics = StackLayout.metrics(available, visibleSize)
            assertTrue("$visibleSize does not fit", metrics.totalHeight(visibleSize) <= available)
        }
    }

    @Test
    fun `largest stack keeps the bottom ten slots fully visible at the visible size metrics`() {
        val rail = 360.dp - HudLayout.HEIGHT
        val metrics = StackLayout.metrics(rail, 10)
        // 312 dp holds 9 previews, 9 minimum gaps of 2 dp and a next slot of 1.25 previews.
        assertEquals(((rail - 2.dp * 9) / 10.25f).value, metrics.previewSlotSize.value, 0.001f)
        assertEquals(2f, metrics.gap.value, 0.001f)

        val visible = StackLayout.fullyVisibleSlots(metrics, 20, rail)

        assertTrue((10 until 20).all { it in visible })
        assertEquals(10, visible.size)
    }

    @Test
    fun `next slot sits on the bottom of the rail and the others are one pitch apart`() {
        val metrics = StackLayout.metrics(312.dp, 5)
        val tops = StackLayout.slotTops(metrics, 15, 312.dp)

        assertEquals((312.dp - metrics.nextSlotSize).value, tops.last().value, 0.001f)
        for (index in 1 until tops.size) {
            assertEquals(metrics.pitch.value, (tops[index] - tops[index - 1]).value, 0.001f)
        }
    }

    @Test
    fun `a taller rail shows more slots at the same size`() {
        val small = StackLayout.metrics(312.dp, 5)
        val tall = StackLayout.metrics(600.dp, 5)
        assertEquals(small, tall)

        val onSmall = StackLayout.fullyVisibleSlots(small, 15, 312.dp)
        val onTall = StackLayout.fullyVisibleSlots(tall, 15, 600.dp)

        assertTrue(onSmall.size >= 5)
        assertTrue(onTall.size > onSmall.size)
        assertTrue(onTall.containsAll(onSmall))
    }

    @Test
    fun `a slot crossing the top edge is not fully visible but is composed`() {
        val rail = 312.dp
        val metrics = StackLayout.metrics(rail, 5)
        val tops = StackLayout.slotTops(metrics, 15, rail)
        val crossing = tops.indexOfFirst { it < 0.dp && it + metrics.previewSlotSize > 0.dp }
        assertTrue("no slot crosses the edge", crossing >= 0)

        assertTrue(crossing !in StackLayout.fullyVisibleSlots(metrics, 15, rail))
        assertTrue(crossing in StackLayout.composedSlots(metrics, 15, rail))
    }

    @Test
    fun `slots more than one pitch above the edge are not composed`() {
        val rail = 312.dp
        val metrics = StackLayout.metrics(rail, 5)
        val tops = StackLayout.slotTops(metrics, 15, rail)
        val composed = StackLayout.composedSlots(metrics, 15, rail)

        for (index in tops.indices) {
            assertEquals("slot $index", tops[index] > -metrics.pitch, index in composed)
        }
        assertTrue(composed.size < 15)
    }

    @Test
    fun `every visible size keeps its bottom slots fully visible on 360dp minus the HUD`() {
        val rail = 360.dp - HudLayout.HEIGHT
        for (visibleSize in 1..10) {
            val metrics = StackLayout.metrics(rail, visibleSize)
            val stackSize = visibleSize + 10
            val visible = StackLayout.fullyVisibleSlots(metrics, stackSize, rail)
            assertTrue("$visibleSize", (stackSize - visibleSize until stackSize).all { it in visible })
        }
    }
}
