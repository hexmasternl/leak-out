package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.ui.components.DialogEntry
import nl.hexmaster.leakout.ui.components.PanelScale
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class PlayMenuDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private var continues = 0
    private var selects = 0
    private var dismisses = 0

    /** Shows the dialog; every action closes it, as the welcome screen does. */
    private fun showDialog() {
        compose.setContent {
            LeakOutTheme {
                var open by remember { mutableStateOf(true) }
                if (open) {
                    PlayMenuDialog(
                        onContinue = {
                            continues++
                            open = false
                        },
                        onSelectLevel = {
                            selects++
                            open = false
                        },
                        onDismiss = {
                            dismisses++
                            open = false
                        },
                    )
                }
            }
        }
    }

    /** The dialog's content alone on a 640 × 360 dp screen, the smallest the game supports. */
    private fun showContentOnSmallestScreen(entry: DialogEntry = DialogEntry(1f, null, 0)) {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(640.dp, 360.dp)) {
                    PlayMenuDialogContent(onContinue = {}, onSelectLevel = {}, onDismiss = {}, entry = entry)
                }
            }
        }
    }

    @Test
    fun showsTitleAndBothButtonsInOrder() {
        showDialog()
        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(PlayMenuTags.TITLE).assertIsDisplayed().assertTextEquals("Play")
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).assertIsDisplayed().assertHasClickAction().assertTextEquals("Continue")
        compose.onNodeWithTag(PlayMenuTags.SELECT_LEVEL).assertIsDisplayed().assertHasClickAction()
            .assertTextEquals("Select Level")

        val order = listOf(PlayMenuTags.TITLE, PlayMenuTags.CONTINUE, PlayMenuTags.SELECT_LEVEL)
            .map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot() }
        order.zipWithNext().forEach { (upper, lower) -> assertTrue(upper.bottom <= lower.top) }
    }

    @Test
    fun panelIsAtMost360DpWide() {
        showDialog()
        val panel = compose.onNodeWithTag(PlayMenuTags.DIALOG).getUnclippedBoundsInRoot()
        assertTrue("width ${panel.width}", panel.width <= 360.dp)
    }

    @Test
    fun scrimCoversTheWholeWindow() {
        showDialog()
        val scrim = compose.onNodeWithTag(PlayMenuTags.SCRIM).assertIsDisplayed().fetchSemanticsNode()
        val root = scrim.layoutInfo.coordinates.findRootCoordinates().size
        assertEquals(root, scrim.size)
    }

    @Test
    fun continueIsBrassPrimaryAndSelectLevelIsGlassSecondary() {
        showContentOnSmallestScreen()
        // Sample the left end of each face, clear of the label.
        val brass = facePixel(PlayMenuTags.CONTINUE)
        val glass = facePixel(PlayMenuTags.SELECT_LEVEL)

        assertTrue("brass fill is warm: $brass", brass.red > 0.7f && brass.red > brass.blue + 0.3f)
        assertClose(LeakOutColors.SurfaceOverlay, glass)
    }

    @Test
    fun everythingFitsA640By360ScreenWithTheGutter() {
        showContentOnSmallestScreen()
        val panel = compose.onNodeWithTag(PlayMenuTags.DIALOG).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("$panel", panel.left >= 16.dp && panel.top >= 16.dp && panel.right <= 624.dp && panel.bottom <= 344.dp)
        for (tag in listOf(PlayMenuTags.TITLE, PlayMenuTags.CONTINUE, PlayMenuTags.SELECT_LEVEL)) {
            val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(tag, bounds.top >= panel.top && bounds.bottom <= panel.bottom && bounds.left >= panel.left && bounds.right <= panel.right)
        }
    }

    @Test
    fun continueInvokesOnlyOnContinueOnce() {
        showDialog()
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).performClick()
        compose.runOnIdle { assertCounts(cont = 1, select = 0, dismiss = 0) }
    }

    @Test
    fun selectLevelInvokesOnlyOnSelectLevelOnce() {
        showDialog()
        compose.onNodeWithTag(PlayMenuTags.SELECT_LEVEL).performClick()
        compose.runOnIdle { assertCounts(cont = 0, select = 1, dismiss = 0) }
    }

    @Test
    fun backDismissesTheDialog() {
        showDialog()
        // Target the dialog window: the default root picker can pick the activity behind it.
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(ViewActions.pressBack())
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertCounts(cont = 0, select = 0, dismiss = 1) }
    }

    @Test
    fun tapOnTheScrimDismissesTheDialog() {
        showDialog()
        // Relative to the scrim: a screen-space tap at the corner can land in the status bar, outside the dialog window.
        compose.onNodeWithTag(PlayMenuTags.SCRIM).performTouchInput { click(Offset(8.dp.toPx(), 8.dp.toPx())) }
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertCounts(cont = 0, select = 0, dismiss = 1) }
    }

    @Test
    fun tapInsideThePanelOffTheButtonsKeepsTheDialogOpen() {
        showDialog()
        val panel = compose.onNodeWithTag(PlayMenuTags.DIALOG).getUnclippedBoundsInRoot()
        val title = compose.onNodeWithTag(PlayMenuTags.TITLE).getUnclippedBoundsInRoot()
        // In the panel padding, level with the title: inside the panel, on no button.
        compose.onNodeWithTag(PlayMenuTags.DIALOG).performTouchInput {
            click(Offset(8.dp.toPx(), ((title.top + title.bottom) / 2 - panel.top).toPx()))
        }
        compose.waitForIdle()

        compose.onNodeWithTag(PlayMenuTags.DIALOG).assertIsDisplayed()
        compose.runOnIdle { assertCounts(cont = 0, select = 0, dismiss = 0) }
    }

    @Test
    fun panelPopsFrom092To1() {
        compose.mainClock.autoAdvance = false
        showContentOnSmallestScreen(DialogEntry.select(reducedMotion = false))
        compose.mainClock.advanceTimeByFrame()
        val start = panelScale()
        compose.mainClock.advanceTimeBy(1_000)
        val end = panelScale()

        assertEquals(0.92f, start, 0.03f)
        assertEquals(1f, end, 0.001f)
    }

    @Test
    fun reducedMotionDoesNotScale() {
        compose.mainClock.autoAdvance = false
        showContentOnSmallestScreen(DialogEntry.select(reducedMotion = true))
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1f, panelScale(), 0.001f)
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(1f, panelScale(), 0.001f)
    }

    @Test
    fun continueClosesTheDialogAtOnce() {
        showDialog()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(PlayMenuTags.CONTINUE).performClick()
        compose.mainClock.advanceTimeByFrame()

        compose.onNodeWithTag(PlayMenuTags.SCRIM).assertDoesNotExist()
        compose.mainClock.autoAdvance = true
    }

    private fun panelScale(): Float = compose.onNodeWithTag(PlayMenuTags.DIALOG).fetchSemanticsNode().config[PanelScale]

    /** A pixel near the left end of a button's face, at a quarter of its height. */
    private fun facePixel(tag: String): Color {
        val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
        return pixels[(pixels.width * 0.08f).toInt(), pixels.height / 4]
    }

    private fun assertClose(expected: Color, actual: Color) {
        val close = abs(expected.red - actual.red) < 0.05f && abs(expected.green - actual.green) < 0.05f &&
            abs(expected.blue - actual.blue) < 0.05f
        assertTrue("expected $expected, was $actual", close)
    }

    private fun assertCounts(cont: Int, select: Int, dismiss: Int) {
        assertEquals("onContinue", cont, continues)
        assertEquals("onSelectLevel", select, selects)
        assertEquals("onDismiss", dismiss, dismisses)
    }
}
