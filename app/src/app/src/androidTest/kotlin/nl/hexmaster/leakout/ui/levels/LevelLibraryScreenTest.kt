package nl.hexmaster.leakout.ui.levels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.core.library.LevelLibraryConfig
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class LevelLibraryScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val started = mutableListOf<Int>()
    private var backs = 0
    private lateinit var history: MutableStateFlow<Map<Int, Int>>

    private fun finished(ids: IntRange, stars: Int = 1) = ids.associateWith { stars }

    /** Page 1 with exactly [total] stars, on its lowest levels. */
    private fun page1Stars(total: Int): Map<Int, Int> =
        finished(1..total / 3, stars = 3) + if (total % 3 > 0) mapOf(total / 3 + 1 to total % 3) else emptyMap()

    private fun showScreen(
        stars: Map<Int, Int> = emptyMap(),
        levels: Int = 48,
        config: LevelLibraryConfig = LevelLibraryConfig.Default,
        widthDp: Int = 800,
        heightDp: Int = 360,
    ) {
        history = MutableStateFlow(stars)
        val viewModel = LevelLibraryViewModel(history = { history }, catalog = { levels }, config = config)
        compose.setContent {
            LeakOutTheme {
                // The box stands for the safe area of a screen, so the real device insets must not shrink it further.
                Box(Modifier.size(widthDp.dp, heightDp.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    LevelLibraryScreen(viewModel, onStartLevel = { started += it }, onBack = { backs++ })
                }
            }
        }
        compose.waitForIdle()
    }

    private fun bar() = compose.onNodeWithTag(StarProgressTags.BAR)

    private fun barLabel() = compose.onNodeWithTag(StarProgressTags.LABEL, useUnmergedTree = true)

    /** The colour at [fraction] of the track's width, on its vertical centre line. */
    private fun trackColorAt(fraction: Float): Color {
        val pixels = compose.onNodeWithTag(StarProgressTags.TRACK, useUnmergedTree = true).captureToImage().toPixelMap()
        return pixels[(pixels.width * fraction).toInt(), pixels.height / 2]
    }

    private fun Color.isCloseTo(other: Color, tolerance: Float = 0.12f) =
        abs(red - other.red) < tolerance && abs(green - other.green) < tolerance && abs(blue - other.blue) < tolerance

    @Test
    fun firstPageShowsLevels1To24() {
        showScreen()
        (1..24).forEach { compose.onNodeWithTag(levelTileTag(it)).assertIsDisplayed() }
        compose.onNodeWithTag(levelTileTag(25)).assertDoesNotExist()
    }

    @Test
    fun tilesShowTheirState() {
        showScreen(stars = mapOf(1 to 3, 2 to 2, 3 to 1))
        compose.onNodeWithTag(levelTileTag(2)).assertContentDescriptionEquals("Level 2, completed, 2 of 3 stars")
        compose.onNodeWithTag(levelTileTag(6)).assertContentDescriptionEquals("Level 6, not played yet")
        compose.onNodeWithTag(levelTileTag(7)).assertContentDescriptionEquals("Level 7, locked")
    }

    @Test
    fun freshInstallLevels1To3AreTappableAndLevel4IsLocked() {
        showScreen()
        (1..3).forEach { compose.onNodeWithTag(levelTileTag(it)).performClick() }
        compose.onNodeWithTag(levelTileTag(4)).assertContentDescriptionEquals("Level 4, locked").performClick()
        compose.runOnIdle { assertEquals(listOf(1, 2, 3), started) }
    }

    @Test
    fun skippedLevelsAndTheWindowAfterAFinishedLevelArePlayable() {
        showScreen(stars = mapOf(1 to 3, 4 to 2))
        compose.onNodeWithTag(levelTileTag(2)).performClick()
        compose.onNodeWithTag(levelTileTag(7)).performClick()
        compose.onNodeWithTag(levelTileTag(8)).assertContentDescriptionEquals("Level 8, locked").performClick()
        compose.runOnIdle { assertEquals(listOf(2, 7), started) }
    }

    @Test
    fun tappingALockedLevelShowsTheMessageInTheBarRowThenTheBarReturns() {
        showScreen()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(levelTileTag(5)).performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag(LevelLibraryTags.HINT).assertTextEquals("Level 5 is locked")
        bar().assertDoesNotExist()
        compose.runOnIdle { assertTrue(started.isEmpty()) }

        compose.mainClock.advanceTimeBy(LOCKED_MESSAGE_MS + 100)
        compose.onNodeWithTag(LevelLibraryTags.HINT).assertDoesNotExist()
        bar().assertIsDisplayed()
    }

    @Test
    fun onPage1BelowTheThresholdNextIsDisabledAndTheBarShowsProgress() {
        showScreen(stars = finished(1..5, stars = 2))
        compose.onNodeWithTag(LevelLibraryTags.PREVIOUS).assertIsNotEnabled()
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsNotEnabled()
        barLabel().assertTextEquals("10 / 50")
        bar()
            .assertIsDisplayed()
            .assertContentDescriptionEquals("10 of 50 stars to open page 2")
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.2f, 0f..1f))
    }

    @Test
    fun nextIsDisabledUntilThePageHas50Stars() {
        showScreen(stars = page1Stars(49))
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsNotEnabled()
        barLabel().assertTextEquals("49 / 50")
        history.value = page1Stars(50)
        compose.waitForIdle()
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsEnabled()
        barLabel().assertTextEquals("50 / 50")
        bar().assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun theBarFillsWithLiquidBelowTheThresholdAndSuccessOnceReached() {
        showScreen(stars = page1Stars(34))
        assertTrue("liquid fill at 25%", trackColorAt(0.25f).isCloseTo(LeakOutColors.Liquid500))
        assertTrue("empty track at 90%", trackColorAt(0.9f).isCloseTo(LeakOutColors.SurfaceAbyss))

        history.value = page1Stars(55)
        compose.waitForIdle()
        barLabel().assertTextEquals("55 / 50")
        assertTrue("success fill at 25%", trackColorAt(0.25f).isCloseTo(LeakOutColors.Success500))
        assertTrue("success fill at 90%", trackColorAt(0.9f).isCloseTo(LeakOutColors.Success500))
    }

    @Test
    fun swipingCannotReachAClosedPageEvenWithLevel24Finished() {
        showScreen(stars = finished(1..24))
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsNotEnabled()
        compose.onNodeWithTag(LevelLibraryTags.PAGER).performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag(levelTileTag(1)).assertIsDisplayed()
        compose.onNodeWithTag(levelTileTag(25)).assertDoesNotExist()
    }

    @Test
    fun nextShowsAnOpenPageAndTheBarFollowsIt() {
        // 51 stars on page 1 open page 2; level 27 is next, so the library opens on page 2.
        showScreen(stars = page1Stars(51) + finished(25..26, stars = 3), levels = 72)
        compose.onNodeWithTag(levelTileTag(25)).assertIsDisplayed()
        barLabel().assertTextEquals("6 / 50")
        compose.onNodeWithTag(LevelLibraryTags.PREVIOUS).assertIsEnabled().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(levelTileTag(1)).assertIsDisplayed()
        barLabel().assertTextEquals("51 / 50")
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsEnabled().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(levelTileTag(25)).assertIsDisplayed()
        compose.onNodeWithTag(LevelLibraryTags.PREVIOUS).assertIsEnabled()
        barLabel().assertTextEquals("6 / 50")
        bar().assertContentDescriptionEquals("6 of 50 stars to open page 3")
    }

    @Test
    fun noBarOnTheLastPage() {
        showScreen(stars = finished(1..24, stars = 3))
        compose.onNodeWithTag(levelTileTag(25)).assertIsDisplayed()
        bar().assertDoesNotExist()
    }

    @Test
    fun opensOnThePageOfTheLevelAfterTheHighestFinished() {
        showScreen(stars = finished(1..24, stars = 3))
        compose.onNodeWithTag(levelTileTag(25)).assertIsDisplayed()
        (25..27).forEach {
            compose.onNodeWithTag(levelTileTag(it)).assertContentDescriptionEquals("Level $it, not played yet")
        }
        compose.onNodeWithTag(levelTileTag(28)).assertContentDescriptionEquals("Level 28, locked")
    }

    @Test
    fun aPartialLastPageLeavesCellsEmpty() {
        showScreen(stars = finished(1..24, stars = 3), levels = 30)
        (25..30).forEach { compose.onNodeWithTag(levelTileTag(it)).assertIsDisplayed() }
        compose.onAllNodesWithTag(LEVEL_EMPTY_CELL_TAG).assertCountEquals(18)
    }

    @Test
    fun anImprovedResultOpensTheNextPage() {
        showScreen(stars = page1Stars(49))
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsNotEnabled()
        history.value = history.value + (17 to 3) // 16 levels at 3 stars + 3 = 51
        compose.waitForIdle()
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsEnabled()
    }

    @Test
    fun theGridAndTheBarFitTheSmallestLandscapeScreen() {
        showScreen(widthDp = 640, heightDp = 320)
        val screen = compose.onNodeWithTag(LevelLibraryTags.SCREEN).getUnclippedBoundsInRoot()
        listOf(1, 6, 19, 24).forEach { id ->
            val tile = compose.onNodeWithTag(levelTileTag(id)).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("tile $id right", tile.right <= screen.right)
            assertTrue("tile $id bottom", tile.bottom <= screen.bottom)
            assertTrue("tile $id touch target", tile.bottom - tile.top >= 48.dp)
        }
        val lowestTile = compose.onNodeWithTag(levelTileTag(24)).getUnclippedBoundsInRoot()
        val bar = bar().assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("bar below the grid", bar.top >= lowestTile.bottom)
        assertTrue("bar bottom", bar.bottom <= screen.bottom)
        assertTrue("bar right", bar.right <= screen.right)
    }
}
