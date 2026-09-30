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
}
