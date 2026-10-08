package nl.hexmaster.leakout.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialogEntryTest {

    @Test
    fun `entry pops from 0_92 with motion pop and fades in over 160 ms`() {
        val entry = DialogEntry.select(reducedMotion = false)
        assertEquals(0.92f, entry.initialScale)
        assertEquals(DialogEntry.PopSpring, entry.scaleSpec)
        assertEquals(160, entry.fadeMs)
    }

    @Test
    fun `reduced motion fades in over 160 ms without a scale`() {
        val entry = DialogEntry.select(reducedMotion = true)
        assertEquals(1f, entry.initialScale)
        assertNull(entry.scaleSpec)
        assertEquals(160, entry.fadeMs)
    }

    @Test
    fun `the panel follows design system 5_6`() {
        assertEquals(24f, FloatingPanelMetrics.CornerRadius.value)
        assertEquals(24f, FloatingPanelMetrics.Padding.value)
        assertEquals(360f, FloatingPanelMetrics.MaxWidth.value)
        assertEquals(16f, FloatingPanelMetrics.Gutter.value)
        assertEquals(12f, FloatingPanelMetrics.ButtonSpacing.value)
    }
}
