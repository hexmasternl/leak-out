package nl.hexmaster.leakout.ui.components

import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartoonButtonMetricsTest {

    @Test
    fun `resting button has full 6dp shadow and no offset`() {
        assertEquals(0.dp, CartoonButtonMetrics.faceOffset(pressed = false))
        assertEquals(6.dp, CartoonButtonMetrics.visibleShadow(pressed = false))
    }

    @Test
    fun `pressed button moves down 4dp and shadow shrinks to 2dp`() {
        assertEquals(4.dp, CartoonButtonMetrics.faceOffset(pressed = true))
        assertEquals(2.dp, CartoonButtonMetrics.visibleShadow(pressed = true))
    }

    @Test
    fun `footprint is identical in both states so pressing never shifts the layout`() {
        for (pressed in listOf(false, true)) {
            assertEquals(
                CartoonButtonMetrics.RestingShadow,
                CartoonButtonMetrics.faceOffset(pressed) + CartoonButtonMetrics.visibleShadow(pressed),
            )
        }
    }

    @Test
    fun `recipe constants match the design system`() {
        assertEquals(3.dp, CartoonButtonMetrics.Border)
        assertEquals(3.dp, CartoonButtonMetrics.Shine)
        assertTrue(CartoonButtonMetrics.MinTouchTarget >= 48.dp)
    }

    @Test
    fun `every variant has distinct face and shadow colours`() {
        CartoonButtonVariant.entries.forEach { variant ->
            assertNotEquals(variant.name, variant.face, variant.shadow)
            assertNotEquals(variant.name, variant.face, variant.label)
        }
        assertEquals(4, CartoonButtonVariant.entries.map { it.face }.toSet().size)
    }

    @Test
    fun `the success variant is green on a darker green shadow`() {
        assertEquals(LeakOutColors.SuccessGreen, CartoonButtonVariant.Success.face)
        assertEquals(LeakOutColors.SuccessGreenDark, CartoonButtonVariant.Success.shadow)
        assertEquals(LeakOutColors.SurfaceCard, CartoonButtonVariant.Success.label)
    }

    @Test
    fun `the disabled look uses the disabled tokens and differs from every variant`() {
        assertEquals(LeakOutColors.DisabledFill, CartoonButtonDisabled.Face)
        assertEquals(LeakOutColors.DisabledShadow, CartoonButtonDisabled.Shadow)
        CartoonButtonVariant.entries.forEach { variant ->
            assertNotEquals(variant.name, variant.face, CartoonButtonDisabled.Face)
            assertNotEquals(variant.name, variant.shadow, CartoonButtonDisabled.Shadow)
        }
    }
}
