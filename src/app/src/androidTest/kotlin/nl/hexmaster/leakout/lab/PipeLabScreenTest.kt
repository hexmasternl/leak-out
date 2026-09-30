package nl.hexmaster.leakout.lab

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.ChannelFill
import nl.hexmaster.leakout.ui.pipes.PipeFillsKey
import nl.hexmaster.leakout.ui.pipes.pipeTileTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PipeLabScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<PipeLabActivity>()

    @Before
    fun controlTheClock() {
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun fills(section: PipeSection): Map<String, ChannelFill> =
        compose.onNodeWithTag(pipeTileTag(section)).fetchSemanticsNode().config[PipeFillsKey]

    private fun progress(section: PipeSection, channel: String = "channel-main") = fills(section)[channel]?.progress ?: 0f

    private fun click(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.mainClock.advanceTimeByFrame()
    }

    private fun runFor(ms: Long) {
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    @Test
    fun showsEveryLabelledSectionDry() {
        for (section in PipeSection.entries) {
            compose.onNodeWithTag(PipeLabTags.label(section)).assertIsDisplayed().assertTextEquals(section.name)
            compose.onNodeWithTag(pipeTileTag(section)).assertIsDisplayed()
            assertTrue(section.name, fills(section).isEmpty())
        }
    }

    @Test
    fun startFillsAndPauseFreezes() {
        click(PipeLabTags.START)
        runFor(500)
        val running = progress(PipeSection.H)
        assertTrue("H filling, was $running", running in 0.3f..0.7f)
        assertEquals(Side.LEFT, fills(PipeSection.H).getValue("channel-main").entry)

        click(PipeLabTags.PAUSE)
        val paused = progress(PipeSection.H)
        runFor(1000)
        assertEquals(paused, progress(PipeSection.H))

        click(PipeLabTags.START)
        runFor(2000)
        assertEquals(1f, progress(PipeSection.H))
    }

    @Test
    fun resetEmptiesEveryTile() {
        click(PipeLabTags.START)
        runFor(1500)
        click(PipeLabTags.RESET)
        runFor(500)
        for (section in PipeSection.entries) assertTrue(section.name, fills(section).isEmpty())
    }

    @Test
    fun reversingAStraightMakesItEnterFromTheRight() {
        click(PipeLabTags.direction(PipeSection.H, "channel-main"))
        compose.onNodeWithTag(PipeLabTags.direction(PipeSection.H, "channel-main")).assert(hasText("R → L"))
        click(PipeLabTags.START)
        runFor(300)
        assertEquals(Side.RIGHT, fills(PipeSection.H).getValue("channel-main").entry)
    }

    @Test
    fun crossDirectionsAreIndependentAndFillOneAfterTheOther() {
        click(PipeLabTags.direction(PipeSection.X, "channel-h"))
        click(PipeLabTags.direction(PipeSection.X, "channel-v"))
        click(PipeLabTags.START)
        runFor(500)
        assertTrue(progress(PipeSection.X, "channel-h") > 0f)
        assertEquals(0f, progress(PipeSection.X, "channel-v"))
        runFor(1000)
        val cross = fills(PipeSection.X)
        assertEquals(1f, cross.getValue("channel-h").progress)
        assertEquals(Side.RIGHT, cross.getValue("channel-h").entry)
        assertEquals(Side.BOTTOM, cross.getValue("channel-v").entry)
    }

    @Test
    fun slidersChangeTheSpeeds() {
        compose.onNodeWithTag(PipeLabTags.FLOW_SPEED).performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
        compose.onNodeWithTag(PipeLabTags.RESERVOIR_FACTOR).performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag(PipeLabTags.FLOW_SPEED_VALUE).assertTextEquals("2.00 cells/s")
        compose.onNodeWithTag(PipeLabTags.RESERVOIR_FACTOR_VALUE).assertTextEquals("×0.25")

        click(PipeLabTags.START)
        runFor(1000)
        assertEquals(1f, progress(PipeSection.V))
        val reservoir = progress(PipeSection.RES)
        assertTrue("reservoir at 2 × 0.25 cells/s should be about half full, was $reservoir", reservoir in 0.4f..0.6f)
    }
}
