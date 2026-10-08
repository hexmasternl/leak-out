package nl.hexmaster.leakout.ui.game

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import kotlin.math.roundToInt
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.ui.components.PanelScale
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PauseDialogTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var session: GameSession

    // 3.1
    @Test
    fun backdropCoversTheWholeWindow() {
        show()
        pause()

        val backdrop = compose.onNodeWithTag(PauseTags.BACKDROP).fetchSemanticsNode()
        val decor = compose.activity.window.decorView
        assertEquals(decor.width, backdrop.size.width)
        assertEquals(decor.height, backdrop.size.height)
    }

    // Issue #89: the window reaches into the display cutout without the deprecated short-edges mode.
    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun windowUsesTheAlwaysCutoutMode() {
        show()
        pause()

        var mode = -1
        Espresso.onView(isRoot()).inRoot(isDialog()).check { view, _ ->
            mode = (view.rootView.layoutParams as WindowManager.LayoutParams).layoutInDisplayCutoutMode
        }
        assertEquals(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS, mode)
    }

    // 3.1
    @Test
    fun aTapOnTheBackdropKeepsTheDialogOpenAndTheGamePaused() {
        show()
        pause()

        compose.onNodeWithTag(PauseTags.BACKDROP).performTouchInput { click(topLeft + Offset(8f, 8f)) }
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
        compose.runOnIdle { assertTrue(session.state.paused) }
    }

    // 3.1
    @Test
    fun backResumesAndRemovesTheBackdropAtOnce() {
        show()
        pause()

        Espresso.pressBack()
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.BACKDROP).assertDoesNotExist()
        compose.runOnIdle { assertFalse(session.state.paused) }
    }

    // 3.2
    @Test
    fun backdropIsAnOpaqueGradientFromAbyssToDeep() {
        show()
        pause()

        val pixels = compose.onNodeWithTag(PauseTags.BACKDROP).captureToImage().toPixelMap()
        val panel = compose.onNodeWithTag(PauseTags.DIALOG).fetchSemanticsNode().boundsInRoot
        // The panel's level 3 shadow reaches beyond it; leave room for it.
        val shadow = with(compose.density) { 48.dp.toPx() }
        val keepOut = Rect(panel.left - shadow, panel.top - shadow, panel.right + shadow, panel.bottom + shadow)

        assertNear(LeakOutColors.SurfaceAbyss, pixels[0, 0])
        assertNear(LeakOutColors.SurfaceDeep, pixels[pixels.width - 1, pixels.height - 1])
        for (y in 0 until pixels.height step 4) {
            for (x in 0 until pixels.width step 4) {
                if (keepOut.contains(Offset(x.toFloat(), y.toFloat()))) continue
                val pixel = pixels[x, y]
                assertTrue("($x, $y) $pixel is not on the backdrop gradient", onGradient(pixel))
            }
        }
    }

    // 3.3
    @Test
    fun titleReadsPausedAndTheElementsAreInOrder() {
        show()
        pause()

        compose.onNodeWithTag(PauseTags.TITLE).assertTextEquals("Paused")
        val order = listOf(PauseTags.TITLE, PauseTags.RESUME, PauseTags.RESTART, PauseTags.DIVIDER, PauseTags.EXIT)
            .map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot() }
        order.zipWithNext().forEach { (upper, lower) -> assertTrue(upper.bottom <= lower.top) }
    }

    // 3.3
    @Test
    fun everythingFitsA640By360Screen() {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(640.dp, 360.dp)) {
                    PauseDialogContent({}, {}, {}, entry = PauseDialogEntry(0, 0, 1f, null))
                }
            }
        }

        for (tag in listOf(PauseTags.TITLE, PauseTags.RESUME, PauseTags.RESTART, PauseTags.DIVIDER, PauseTags.EXIT)) {
            val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("$tag", bounds.top >= 0.dp && bounds.bottom <= 360.dp && bounds.left >= 0.dp && bounds.right <= 640.dp)
        }
    }

    // 3.4
    @Test
    fun panelPopsFrom092To1() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.mainClock.advanceTimeByFrame()

        val start = panelScale()
        compose.mainClock.advanceTimeBy(1_000)
        val end = panelScale()

        assertEquals(0.92f, start, 0.03f)
        assertEquals(1f, end, 0.001f)
    }

    // 3.4
    @Test
    fun resumeClosesTheDialogAtOnce() {
        show()
        pause()
        compose.mainClock.autoAdvance = false

        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.mainClock.advanceTimeByFrame()

        compose.onNodeWithTag(PauseTags.BACKDROP).assertDoesNotExist()
        compose.mainClock.autoAdvance = true
    }

    private fun panelScale(): Float = compose.onNodeWithTag(PauseTags.DIALOG).fetchSemanticsNode().config[PanelScale]

    private fun pause() {
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.waitForIdle()
    }

    private fun show(level: Level = FieldTest.level()) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session)
                }
            }
        }
        compose.waitForIdle()
    }

    private fun channels(color: Color): List<Int> = listOf(color.red, color.green, color.blue).map { (it * 255).roundToInt() }

    private fun assertNear(expected: Color, actual: Color) {
        channels(expected).zip(channels(actual)).forEach { (e, a) -> assertTrue("expected $expected, was $actual", kotlin.math.abs(e - a) <= TOLERANCE) }
    }

    /** Every channel lies between surface.abyss and surface.deep: nothing of the game mixes in. */
    private fun onGradient(pixel: Color): Boolean {
        if (pixel.alpha < 0.99f) return false
        val low = channels(LeakOutColors.SurfaceAbyss)
        val high = channels(LeakOutColors.SurfaceDeep)
        return channels(pixel).withIndex().all { (i, c) -> c in (low[i] - TOLERANCE)..(high[i] + TOLERANCE) }
    }

    private companion object {
        const val TOLERANCE = 2
    }
}
