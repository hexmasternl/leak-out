package nl.hexmaster.leakout.ui.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGridLayoutTest {
    /** Landscape phone sizes in dp, from the smallest the app supports to a tablet. */
    private val screens = listOf(640f to 320f, 732f to 360f, 800f to 360f, 915f to 412f, 1280f to 800f)

    @Test
    fun `the whole 6x4 grid fits the grid area on every landscape screen`() {
        for ((w, h) in screens) {
            val (areaW, areaH) = LevelGridLayout.gridArea(w, h)
            val m = LevelGridLayout.forScreen(w, h)
            assertTrue("width on ${w}x$h: ${m.gridWidthDp} > $areaW", m.gridWidthDp <= areaW + 0.01f)
            assertTrue("height on ${w}x$h: ${m.gridHeightDp} > $areaH", m.gridHeightDp <= areaH + 0.01f)
        }
    }

    @Test
    fun `tiles stay at least a 48 dp touch target`() {
        for ((w, h) in screens) {
            val m = LevelGridLayout.forScreen(w, h)
            assertTrue("tile height on ${w}x$h is ${m.tileHeightDp}", m.tileHeightDp >= 48f)
            assertTrue("tile width on ${w}x$h is ${m.tileWidthDp}", m.tileWidthDp >= 48f)
        }
    }

    @Test
    fun `tiles never grow past the design system size`() {
        val m = LevelGridLayout.compute(4000f, 4000f)
        assertEquals(88f, m.tileWidthDp, 0f)
        assertEquals(104f, m.tileHeightDp, 0f)
    }

    @Test
    fun `a tiny area gives no negative sizes`() {
        val m = LevelGridLayout.compute(10f, 10f)
        assertEquals(0f, m.tileWidthDp, 0f)
        assertEquals(0f, m.tileHeightDp, 0f)
    }
}
