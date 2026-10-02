package nl.hexmaster.leakout.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ResultDialogLayoutTest {

    @Test
    fun `content that fits exactly uses Regular`() {
        assertEquals(ResultDialogLayout.Regular, ResultDialogLayout.choose(availableHeightPx = 900, regularHeightPx = 900))
    }

    @Test
    fun `one pixel over uses Compact`() {
        assertEquals(ResultDialogLayout.Compact, ResultDialogLayout.choose(availableHeightPx = 900, regularHeightPx = 901))
    }

    @Test
    fun `a 360 dp window with 440 dp of content uses Compact`() {
        assertEquals(ResultDialogLayout.Compact, ResultDialogLayout.choose(px(360 - 32), px(440)))
    }

    @Test
    fun `an 800 dp window with 440 dp of content uses Regular`() {
        assertEquals(ResultDialogLayout.Regular, ResultDialogLayout.choose(px(800 - 32), px(440)))
        // The estimated Regular win dialog (design-system.md 5.6) also fits on a tablet.
        assertEquals(ResultDialogLayout.Regular, ResultDialogLayout.choose(px(800 - 32), px(488)))
    }

    /** At a typical phone density of 2.625. */
    private fun px(dp: Int): Int = (dp * 2.625f).toInt()
}
