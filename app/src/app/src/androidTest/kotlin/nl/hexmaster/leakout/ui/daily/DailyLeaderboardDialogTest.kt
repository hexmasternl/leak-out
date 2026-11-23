package nl.hexmaster.leakout.ui.daily

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import nl.hexmaster.leakout.scores.online.DailyLeaderboardEntry
import nl.hexmaster.leakout.scores.online.DailyPlayerRank
import nl.hexmaster.leakout.scores.online.DailyTopScores
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The daily leaderboard dialog (spec `daily-pipe`): its table, own row, states and buttons, in landscape. */
@RunWith(AndroidJUnit4::class)
class DailyLeaderboardDialogTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val date = LocalDate.of(2026, 11, 23)

    private fun entries(current: Int? = null) =
        listOf("Player v" to 512, "Player w" to 511, "Player x" to 501, "Player y" to 498, "Player z" to 495)
            .mapIndexed { index, (name, score) -> DailyLeaderboardEntry(index + 1, name, score, 3, isCurrentPlayer = index + 1 == current) }

    @Before
    fun landscape() {
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
    }

    @Test
    fun theTopFiveAndTheOwnRowAreAllVisibleWithStart() {
        var started = 0
        show(DailyLeaderboardState.Loaded(DailyTopScores(date, entries(), DailyPlayerRank(76, 125, 1))), DailyLeaderboardActions.Start { started++ })

        for (position in listOf(1, 2, 3, 4, 5, 76)) {
            compose.onNodeWithTag(DailyLeaderboardTags.row(position), useUnmergedTree = true).assertIsDisplayed()
        }
        compose.onNodeWithTag(DailyLeaderboardTags.row(76), useUnmergedTree = true)
            .assertContentDescriptionEquals("Position 76, Player a, you, 125 points")

        compose.onNodeWithTag(DailyLeaderboardTags.START).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, started) }
    }

    @Test
    fun aPlayerInTheTopFiveIsMarkedInPlace() {
        show(
            DailyLeaderboardState.Loaded(DailyTopScores(date, entries(current = 3), DailyPlayerRank(3, 501, 3))),
            DailyLeaderboardActions.AfterWin({}, {}),
        )

        compose.onNodeWithTag(DailyLeaderboardTags.row(3), useUnmergedTree = true)
            .assertContentDescriptionEquals("Position 3, Player x, you, 501 points")
        compose.onNodeWithTag(DailyLeaderboardTags.row(6), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(DailyLeaderboardTags.RESTART).assertIsDisplayed()
        compose.onNodeWithTag(DailyLeaderboardTags.EXIT).assertIsDisplayed()
        compose.onNodeWithTag(DailyLeaderboardTags.START).assertDoesNotExist()
    }

    @Test
    fun loadingEmptyAndFailedTakeTheTablesPlace() {
        var state: DailyLeaderboardState by mutableStateOf(DailyLeaderboardState.Loading)
        compose.setContent {
            LeakOutTheme { DailyLeaderboardDialog(state, DailyLeaderboardActions.Start {}, onBack = {}, eyebrow = "Daily Pipe", playerName = "Player a") }
        }
        compose.onNodeWithTag(DailyLeaderboardTags.LOADING, useUnmergedTree = true).assertExists()

        state = DailyLeaderboardState.Loaded(DailyTopScores(date, emptyList(), player = null))
        compose.onNodeWithTag(DailyLeaderboardTags.EMPTY, useUnmergedTree = true).assertIsDisplayed()

        state = DailyLeaderboardState.Failed
        compose.onNodeWithTag(DailyLeaderboardTags.FAILED, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(DailyLeaderboardTags.START).assertIsDisplayed()
    }

    private fun show(state: DailyLeaderboardState, actions: DailyLeaderboardActions) {
        compose.setContent {
            LeakOutTheme { DailyLeaderboardDialog(state, actions, onBack = {}, eyebrow = "Daily Pipe", playerName = "Player a") }
        }
        compose.waitForIdle()
    }
}
