package nl.hexmaster.leakout.ui.levels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
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
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LevelLibraryScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val started = mutableListOf<Int>()
    private var backs = 0
    private lateinit var history: MutableStateFlow<Map<Int, Int>>

    private fun finished(ids: IntRange, stars: Int = 1) = ids.associateWith { stars }

    private fun showScreen(
        stars: Map<Int, Int> = emptyMap(),
        levels: Int = 48,
        threshold: Int = 36,
        widthDp: Int = 800,
        heightDp: Int = 360,
    ) {
        history = MutableStateFlow(stars)
        val viewModel = LevelLibraryViewModel(
            history = { history },
            catalog = { levels },
            config = LevelLibraryConfig(threshold),
        )
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(widthDp.dp, heightDp.dp)) {
                    LevelLibraryScreen(viewModel, onStartLevel = { started += it }, onBack = { backs++ })
                }
            }
        }
        compose.waitForIdle()
    }

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
        compose.onNodeWithTag(levelTileTag(4)).assertContentDescriptionEquals("Level 4, not played yet")
        compose.onNodeWithTag(levelTileTag(5)).assertContentDescriptionEquals("Level 5, locked")
    }

    @Test
    fun tappingAnUnlockedLevelStartsIt() {
        showScreen(stars = finished(1..6))
        compose.onNodeWithTag(levelTileTag(7)).performClick()
        compose.onNodeWithTag(levelTileTag(3)).performClick()
        compose.runOnIdle { assertEquals(listOf(7, 3), started) }
    }

    @Test
    fun tappingALockedLevelShowsFeedbackAndStartsNothing() {
        showScreen()
        compose.onNodeWithTag(levelTileTag(5)).performClick()
        compose.onNodeWithTag(LevelLibraryTags.HINT).assertTextEquals("Level 5 is locked")
        compose.runOnIdle { assertTrue(started.isEmpty()) }
    }

    @Test
    fun onPage1PreviousIsDisabledAndALockedNextShowsStarProgress() {
        showScreen(stars = finished(1..5, stars = 2))
        compose.onNodeWithTag(LevelLibraryTags.PREVIOUS).assertIsNotEnabled()
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsNotEnabled()
        compose.onNodeWithTag(LevelLibraryTags.HINT).assertTextEquals("10/36 stars to unlock page 2")
    }

    @Test
    fun swipingCannotReachALockedPage() {
        showScreen(stars = finished(1..5))
        compose.onNodeWithTag(LevelLibraryTags.PAGER).performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithTag(levelTileTag(1)).assertIsDisplayed()
        compose.onNodeWithTag(levelTileTag(25)).assertDoesNotExist()
    }

    @Test
    fun nextShowsAnUnlockedPage() {
        // 36 stars on page 1 open level 25; level 13 is still unfinished, so it opens on page 1.
        showScreen(stars = finished(1..12, stars = 3))
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsEnabled().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(levelTileTag(25)).assertIsDisplayed()
        compose.onNodeWithTag(LevelLibraryTags.PREVIOUS).assertIsEnabled()
    }

    @Test
    fun opensOnThePageOfTheNextUnfinishedLevel() {
        showScreen(stars = finished(1..24))
        compose.onNodeWithTag(levelTileTag(25)).assertIsDisplayed()
    }

    @Test
    fun aPartialLastPageLeavesCellsEmpty() {
        showScreen(stars = finished(1..24), levels = 30)
        (25..30).forEach { compose.onNodeWithTag(levelTileTag(it)).assertIsDisplayed() }
        compose.onAllNodesWithTag(LEVEL_EMPTY_CELL_TAG).assertCountEquals(18)
    }

    @Test
    fun anImprovedResultOpensTheNextPage() {
        showScreen(stars = finished(1..11, stars = 3) + (12 to 1))
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsNotEnabled()
        history.value = history.value + (12 to 3)
        compose.waitForIdle()
        compose.onNodeWithTag(LevelLibraryTags.NEXT).assertIsEnabled()
    }

    @Test
    fun theGridFitsTheSmallestLandscapeScreen() {
        showScreen(widthDp = 640, heightDp = 320)
        val screen = compose.onNodeWithTag(LevelLibraryTags.SCREEN).getUnclippedBoundsInRoot()
        listOf(1, 6, 19, 24).forEach { id ->
            val tile = compose.onNodeWithTag(levelTileTag(id)).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("tile $id right", tile.right <= screen.right)
            assertTrue("tile $id bottom", tile.bottom <= screen.bottom)
            assertTrue("tile $id touch target", tile.bottom - tile.top >= 48.dp)
        }
    }
}
