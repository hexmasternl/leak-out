package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.leakout.scores.online.ScorePostStatus
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The status dot on screen: shown while sending, gone after its last blink, never read out (spec `online-score-submission`). */
@RunWith(AndroidJUnit4::class)
class ScorePostIndicatorTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val status = MutableStateFlow(ScorePostStatus.InProgress)
    private val dot get() = compose.onNodeWithTag(ScorePostIndicatorTags.DOT, useUnmergedTree = true)

    private fun show() {
        compose.mainClock.autoAdvance = false
        compose.setContent { LeakOutTheme { ScorePostIndicator(status) } }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun theDotShowsWhileSendingAndIsNotReadOut() {
        show()
        compose.mainClock.advanceTimeBy(5_000)
        dot.assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    @Test
    fun theDotIsGoneAfterTheGreenBlink() {
        show()
        status.value = ScorePostStatus.Succeeded
        compose.mainClock.advanceTimeBy(200)
        dot.assertExists()
        compose.mainClock.advanceTimeBy(ScorePostDot.SUCCESS_ON_MS + ScorePostDot.SUCCESS_OFF_MS + 100)
        dot.assertDoesNotExist()
    }

    @Test
    fun theDotIsGoneAfterThreeRedBlinks() {
        show()
        status.value = ScorePostStatus.Failed
        compose.mainClock.advanceTimeBy(1_000)
        dot.assertExists()
        compose.mainClock.advanceTimeBy(400)
        dot.assertDoesNotExist()
    }

    @Test
    fun aSendingThatEndedBeforeTheDotWasShownShowsNothing() {
        status.value = ScorePostStatus.Failed
        show()
        dot.assertDoesNotExist()
    }
}
