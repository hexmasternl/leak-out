package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.graphics.toArgb
import nl.hexmaster.leakout.ui.components.DialogEntry
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PauseDialogEntryTest {
    @Test
    fun `with motion the backdrop fades in over motion quick and the panel pops from 0_92`() {
        val entry = PauseDialogEntry.select(reducedMotion = false)

        assertEquals(160, entry.backdropFadeMs)
        assertEquals(160, entry.panelFadeMs)
        assertEquals(0.92f, entry.initialScale, 0f)
        assertSame(DialogEntry.PopSpring, entry.scaleSpec)
    }

    @Test
    fun `with reduced motion the backdrop is there at once and the panel only fades`() {
        val entry = PauseDialogEntry.select(reducedMotion = true)

        assertEquals(0, entry.backdropFadeMs)
        assertEquals(160, entry.panelFadeMs)
        assertEquals(1f, entry.initialScale, 0f)
        assertNull(entry.scaleSpec)
    }

    @Test
    fun `backdrop runs from surface abyss to surface deep`() {
        assertEquals(0xFF07111C.toInt(), LeakOutColors.SurfaceAbyss.toArgb())
        assertEquals(0xFF0C1A2A.toInt(), LeakOutColors.SurfaceDeep.toArgb())
    }
}
