package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.score.FinalScore
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** "New best!" on the level complete dialog: shown in the message's place once the tally is done. */
@RunWith(AndroidJUnit4::class)
class LevelCompleteNewBestTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val score = FinalScore(earned = 120, unusedSections = List(4) { Position(it, 0) }, total = 108)

    private fun show(newBest: Boolean, tallyDone: Boolean) {
        val age = if (tallyDone) WinTally.totalDurationMs(score, 2) else 0L
        compose.setContent {
            LeakOutTheme {
                LevelCompleteDialog(
                    level = 7,
                    score = score,
                    frame = winFrame(age, score, stars = 2),
                    hasNextLevel = true,
                    onNextLevel = {},
                    onRestart = {},
                    onExit = {},
                    newBest = newBest,
                )
            }
        }
    }

    @Test
    fun aNewBestIsShownOnceTheTallyIsDone() {
        show(newBest = true, tallyDone = true)
        compose.onNodeWithTag(LevelCompleteTags.NEW_BEST).assertIsDisplayed()
        compose.onNodeWithText("New best!").assertIsDisplayed()
        compose.onNodeWithText("You kept the water flowing.").assertDoesNotExist()
    }

    @Test
    fun aNewBestWaitsForTheTally() {
        show(newBest = true, tallyDone = false)
        compose.onNodeWithTag(LevelCompleteTags.NEW_BEST).assertDoesNotExist()
        compose.onNodeWithText("You kept the water flowing.").assertIsDisplayed()
    }

    @Test
    fun noNewBestShowsTheMessage() {
        show(newBest = false, tallyDone = true)
        compose.onNodeWithTag(LevelCompleteTags.NEW_BEST).assertDoesNotExist()
        compose.onNodeWithText("You kept the water flowing.").assertIsDisplayed()
    }
}
