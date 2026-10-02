package nl.hexmaster.leakout.ui.designer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.ui.game.FieldTest
import nl.hexmaster.leakout.ui.game.GameScreenTags
import nl.hexmaster.leakout.ui.game.GameSession
import nl.hexmaster.leakout.ui.game.LevelCompleteTags
import nl.hexmaster.leakout.ui.game.WinTally
import nl.hexmaster.leakout.ui.game.WinTiming
import nl.hexmaster.leakout.ui.game.flowToEnd
import nl.hexmaster.leakout.ui.game.placeSection
import nl.hexmaster.leakout.ui.game.sendTick
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A test play is the normal game screen without a next level; the frame loop is driven with ticks. */
@RunWith(AndroidJUnit4::class)
class TestPlayScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val level = FieldTest.level(id = 26)
    private val session = GameSession(level, Random(7))
    private var exits = 0

    private fun show() {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    TestPlayScreen(level = level, onExit = { exits++ }, session = session)
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun aWonTestPlayOffersNoNextLevelAndExitsToTheHost() {
        show()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
        val end = compose.runOnIdle { WinTally.totalDurationMs(session.state.finalScore!!, session.state.finalStars) }
        compose.runOnIdle { session.sendTick(maxOf(1L, WinTiming.FINISH_EFFECT_MS + end - session.winAgeMs)) }
        compose.waitForIdle()

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertIsNotEnabled()
        compose.onNodeWithTag(LevelCompleteTags.EXIT).assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(1, exits)
    }
}
