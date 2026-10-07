package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.scores.online.LeaderboardEntry
import nl.hexmaster.leakout.scores.online.TopScoresState
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The top 10 dialog's content and states (spec `level-top-scores`), without the dialog window. */
@RunWith(AndroidJUnit4::class)
class TopScoresDialogTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var closes = 0
    private var retries = 0
    private var signIns = 0

    private fun entries(count: Int, own: Int? = null) =
        (1..count).map { LeaderboardEntry(it, "Player $it", 2000 - it * 100, isCurrentPlayer = it == own) }

    private fun show(state: TopScoresState, width: Dp = 640.dp, height: Dp = 360.dp, fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                LeakOutTheme {
                    Box(Modifier.requiredSize(width, height)) {
                        TopScoresDialogContent(
                            level = 7,
                            state = state,
                            onClose = { closes++ },
                            onRetry = { retries++ },
                            onSignIn = { signIns++ },
                            entry = PauseDialogEntry(0, 0, 1f, null),
                            reducedMotion = true,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun tenEntriesInRankOrderWithTheOwnEntryMarked() {
        show(TopScoresState.Loaded(entries(10, own = 4)))
        compose.onNodeWithTag(TopScoresTags.LEVEL, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.row(1)).assertContentDescriptionEquals("Rank 1, Player 1, 1,900 points")
        compose.onNodeWithTag(TopScoresTags.row(4)).assertContentDescriptionEquals("Rank 4, Player 4, 1,600 points, your score")
        compose.onNodeWithTag(TopScoresTags.row(10)).assertContentDescriptionEquals("Rank 10, Player 10, 1,000 points")
        val tops = (1..10).map { compose.onNodeWithTag(TopScoresTags.row(it)).fetchSemanticsNode().boundsInRoot }
        // Compact: 1–5 down the left column, 6–10 down the right one.
        for (i in 1 until 5) assertTrue(tops[i].top > tops[i - 1].top)
        assertTrue(tops[5].left > tops[0].right)
        assertEquals(tops[0].top, tops[5].top, 1f)
    }

    @Test
    fun aShortLandscapeScreenShowsTwoColumnsWithEveryRowVisible() {
        show(TopScoresState.Loaded(entries(10)))
        compose.onNodeWithTag(TopScoresTags.LAYOUT_COMPACT).assertIsDisplayed()
        for (rank in 1..10) compose.onNodeWithTag(TopScoresTags.row(rank)).assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.CLOSE).assertIsDisplayed()
    }

    @Test
    fun aTallScreenShowsOneColumn() {
        show(TopScoresState.Loaded(entries(10)), width = 1280.dp, height = 800.dp)
        compose.onNodeWithTag(TopScoresTags.LAYOUT_REGULAR).assertIsDisplayed()
        val first = compose.onNodeWithTag(TopScoresTags.row(1)).fetchSemanticsNode().boundsInRoot
        val sixth = compose.onNodeWithTag(TopScoresTags.row(6)).fetchSemanticsNode().boundsInRoot
        assertEquals(first.left, sixth.left, 1f)
        assertTrue(sixth.top > first.top)
    }

    @Test
    fun largeFontKeepsCloseVisibleAndTheLastRowReachable() {
        show(TopScoresState.Loaded(entries(10)), fontScale = 1.3f)
        compose.onNodeWithTag(TopScoresTags.CLOSE).assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.row(10)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.CLOSE).assertIsDisplayed()
    }

    @Test
    fun fewerScoresShowOnlyThoseRows() {
        show(TopScoresState.Loaded(entries(3)))
        for (rank in 1..3) compose.onNodeWithTag(TopScoresTags.row(rank)).assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.row(4)).assertDoesNotExist()
    }

    @Test
    fun loadingAndEmptyStates() {
        var state: TopScoresState by mutableStateOf(TopScoresState.Loading)
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.requiredSize(640.dp, 360.dp)) {
                    TopScoresDialogContent(7, state, {}, {}, {}, PauseDialogEntry(0, 0, 1f, null), reducedMotion = true)
                }
            }
        }
        compose.onNodeWithTag(TopScoresTags.LOADING).assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.LIST).assertDoesNotExist()
        val loadingPanel = compose.onNodeWithTag(TopScoresTags.DIALOG).fetchSemanticsNode().boundsInRoot

        state = TopScoresState.Empty
        compose.onNodeWithTag(TopScoresTags.EMPTY).assertIsDisplayed()
        state = TopScoresState.Loaded(entries(10))
        compose.waitForIdle()
        // The panel keeps its size when the scores arrive.
        assertEquals(loadingPanel, compose.onNodeWithTag(TopScoresTags.DIALOG).fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun signedOutOffersTheSignIn() {
        show(TopScoresState.SignedOut(canSignIn = true))
        compose.onNodeWithTag(TopScoresTags.SIGNED_OUT).assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.SIGN_IN).performClick()
        compose.runOnIdle { assertEquals(1, signIns) }
    }

    @Test
    fun withoutPlayGamesThereIsNoSignInButton() {
        show(TopScoresState.SignedOut(canSignIn = false))
        compose.onNodeWithTag(TopScoresTags.SIGNED_OUT).assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.SIGN_IN).assertDoesNotExist()
    }

    @Test
    fun anErrorOffersTryAgain() {
        show(TopScoresState.Failed)
        compose.onNodeWithTag(TopScoresTags.ERROR).assertIsDisplayed()
        compose.onNodeWithTag(TopScoresTags.RETRY).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun closeIsALabelledButtonOfAtLeast48dp() {
        show(TopScoresState.Empty)
        compose.onNodeWithTag(TopScoresTags.CLOSE)
            .assertContentDescriptionEquals("Close")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, closes) }
    }
}
