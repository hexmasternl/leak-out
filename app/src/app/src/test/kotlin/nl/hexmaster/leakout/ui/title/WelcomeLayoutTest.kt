package nl.hexmaster.leakout.ui.title

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WelcomeLayoutFitTest(private val widthDp: Float, private val heightDp: Float) {

    companion object {
        // Landscape sizes after insets: small phones, the preview sizes, common phones, tablets.
        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}")
        fun sizes(): List<Array<Float>> = listOf(
            arrayOf(480f, 260f),
            arrayOf(568f, 300f),
            arrayOf(640f, 320f),
            arrayOf(640f, 360f),
            arrayOf(732f, 412f),
            arrayOf(800f, 360f),
            arrayOf(915f, 412f),
            arrayOf(1024f, 600f),
            arrayOf(1280f, 800f),
            arrayOf(1920f, 1080f),
        )
    }

    private val layout = WelcomeLayout.compute(widthDp, heightDp)

    @Test
    fun `content fits the available height`() {
        assertTrue("content ${layout.contentHeightDp} > $heightDp", layout.contentHeightDp <= heightDp)
    }

    @Test
    fun `title fits the available width`() {
        val titleWidth = layout.titleSizeDp * WelcomeLayout.TITLE_WIDTH_EM
        assertTrue("title $titleWidth > $widthDp", titleWidth <= widthDp)
    }

    @Test
    fun `button row fits the available width`() {
        assertTrue("row ${layout.buttonRowWidthDp} > $widthDp", layout.buttonRowWidthDp <= widthDp)
    }

    @Test
    fun `buttons keep a 48dp touch target`() {
        val total = layout.buttonFaceHeightDp + WelcomeLayout.BUTTON_SHADOW_DP
        assertTrue("button $total < 48", total >= WelcomeLayout.MIN_TOUCH_TARGET_DP)
        assertTrue(layout.buttonWidthDp >= WelcomeLayout.MIN_TOUCH_TARGET_DP)
    }

    @Test
    fun `title is the dominant element`() {
        assertTrue(layout.titleSizeDp >= 36f)
        assertTrue(layout.titleHeightDp > layout.buttonFaceHeightDp)
    }
}

class WelcomeLayoutScalingTest {

    @Test
    fun `title grows with the screen and stays capped`() {
        val small = WelcomeLayout.compute(640f, 320f)
        val standard = WelcomeLayout.compute(800f, 360f)
        val tablet = WelcomeLayout.compute(1920f, 1080f)
        assertTrue(small.titleSizeDp < standard.titleSizeDp)
        assertTrue(standard.titleSizeDp <= tablet.titleSizeDp)
        assertEquals(120f, tablet.titleSizeDp, 0.001f)
    }

    @Test
    fun `short heights shrink the title before the buttons`() {
        val tall = WelcomeLayout.compute(800f, 480f)
        val short = WelcomeLayout.compute(800f, 260f)
        assertTrue(short.titleSizeDp < tall.titleSizeDp)
        assertTrue(short.buttonFaceHeightDp + WelcomeLayout.BUTTON_SHADOW_DP >= WelcomeLayout.MIN_TOUCH_TARGET_DP)
    }

    @Test
    fun `button face is 56dp on roomy screens`() {
        assertEquals(56f, WelcomeLayout.compute(1280f, 800f).buttonFaceHeightDp, 0.001f)
    }

    @Test
    fun `title never drops below its floor on absurdly small areas`() {
        assertEquals(28f, WelcomeLayout.compute(100f, 60f).titleSizeDp, 0.001f)
    }
}
