package nl.hexmaster.leakout.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowButtonMetricsTest {

    @Test
    fun `resting button has the full 4dp lip and no offset`() {
        assertEquals(0.dp, FlowButtonMetrics.faceOffset(pressed = false))
        assertEquals(4.dp, FlowButtonMetrics.visibleLip(pressed = false))
    }

    @Test
    fun `pressed button moves down 2dp and the lip shrinks to 2dp`() {
        assertEquals(2.dp, FlowButtonMetrics.faceOffset(pressed = true))
        assertEquals(2.dp, FlowButtonMetrics.visibleLip(pressed = true))
        assertEquals(FlowButtonMetrics.PressedLip, FlowButtonMetrics.visibleLip(pressed = true))
    }

    @Test
    fun `footprint is identical in both states`() {
        for (pressed in listOf(false, true)) {
            assertEquals(FlowButtonMetrics.Lip, FlowButtonMetrics.faceOffset(pressed) + FlowButtonMetrics.visibleLip(pressed))
        }
    }

    @Test
    fun `sizes are 56, 48 and 40 dp faces plus the lip, never under the touch target`() {
        assertEquals(60.dp, FlowButtonMetrics.height(FlowButtonVariant.Primary, FlowButtonSize.Large))
        assertEquals(52.dp, FlowButtonMetrics.height(FlowButtonVariant.Primary, FlowButtonSize.Medium))
        assertEquals(48.dp, FlowButtonMetrics.height(FlowButtonVariant.Primary, FlowButtonSize.Small))
        for (variant in FlowButtonVariant.entries) {
            for (size in FlowButtonSize.entries) {
                assertTrue("$variant $size", FlowButtonMetrics.height(variant, size) >= 48.dp)
            }
        }
    }

    @Test
    fun `tertiary has no lip and is 48dp high whatever the size`() {
        assertFalse(FlowButtonVariant.Tertiary.hasLip)
        assertNull(FlowButtonVariant.Tertiary.lip)
        assertNull(FlowButtonVariant.Tertiary.glow)
        assertEquals(Color.Transparent, FlowButtonVariant.Tertiary.fillTop)
        assertEquals(LeakOutColors.Liquid300, FlowButtonVariant.Tertiary.label)
        for (size in FlowButtonSize.entries) {
            assertEquals(48.dp, FlowButtonMetrics.height(FlowButtonVariant.Tertiary, size))
        }
    }

    @Test
    fun `success variant matches design system 5_1`() {
        val v = FlowButtonVariant.Success
        assertEquals(argb(0xFF3DDC97), v.fillTop.toArgb())
        assertEquals(argb(0xFF1FA971), v.fillBottom.toArgb())
        assertEquals(argb(0xFFFFFFFF), v.label.toArgb())
        assertEquals(argb(0xFF1FA971), v.lip!!.toArgb())
        assertEquals(LeakOutColors.Success500, v.glow)
    }

    @Test
    fun `primary, glass and destructive variants match design system 5_1`() {
        with(FlowButtonVariant.Primary) {
            assertEquals(LeakOutColors.Brass300, fillTop)
            assertEquals(LeakOutColors.Brass500, fillBottom)
            assertEquals(argb(0xFF2A1A03), label.toArgb())
            assertEquals(LeakOutColors.Brass700, lip)
            assertEquals(LeakOutColors.Brass500, glow)
        }
        with(FlowButtonVariant.Secondary) {
            assertEquals(LeakOutColors.SurfaceOverlay, fillTop)
            assertEquals(LeakOutColors.TextPrimary, label)
            assertEquals(LeakOutColors.SurfaceAbyss, lip)
            assertEquals(LeakOutColors.LineStrong, border)
            assertNull(glow)
        }
        with(FlowButtonVariant.Destructive) {
            assertEquals(argb(0xFFFF5A5F), fillTop.toArgb())
            assertEquals(argb(0xFFC9353B), fillBottom.toArgb())
            assertEquals(LeakOutColors.TextOnStatus, label)
            assertEquals(LeakOutColors.Leak700, lip)
            assertNull(glow)
        }
    }

    @Test
    fun `disabled look uses the glass fill and muted label at 60 percent`() {
        assertEquals(LeakOutColors.SurfaceOverlay, FlowButtonDisabled.Fill)
        assertEquals(LeakOutColors.TextMuted, FlowButtonDisabled.Label)
        assertEquals(0.6f, FlowButtonDisabled.ALPHA)
    }

    @Test
    fun `new colour tokens have the design system values`() {
        assertEquals(argb(0xFFC9353B), LeakOutColors.Leak700.toArgb())
        assertEquals(argb(0xFF1FA971), LeakOutColors.Success700.toArgb())
        assertEquals(argb(0xFF2A1A03), LeakOutColors.TextOnBrass.toArgb())
        assertEquals(argb(0xFFFFFFFF), LeakOutColors.TextOnStatus.toArgb())
        // surface.scrim: #07111C at 72 %
        assertEquals(argb(0x07111C).and(0xFFFFFF), LeakOutColors.Scrim.toArgb().and(0xFFFFFF))
        assertEquals(0.72f, LeakOutColors.Scrim.alpha, 0.01f)
    }

    private fun argb(value: Long): Int = value.toInt()
}
