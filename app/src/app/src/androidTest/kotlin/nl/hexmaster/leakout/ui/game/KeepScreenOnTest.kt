package nl.hexmaster.leakout.ui.game

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The game screen keeps the display on for as long as it is shown, its dialogs included, and
 * stops as soon as it leaves the composition. The game screen sits behind a toggle, like the
 * screen switch in `MainActivity`.
 */
@RunWith(AndroidJUnit4::class)
class KeepScreenOnTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var session: GameSession
    private var inGame by mutableStateOf(true)

    // 2.1
    @Test
    fun screenIsKeptOnWhilePlaying() {
        show()
        compose.waitUntil(TIMEOUT_MS) { compose.activity.keepsScreenOn() }
    }

    // 2.1
    @Test
    fun screenIsStillKeptOnWithThePauseDialog() {
        show()
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
        compose.runOnIdle { assertTrue(session.state.paused) }
        compose.waitUntil(TIMEOUT_MS) { compose.activity.keepsScreenOn() }
    }

    // 2.1
    @Test
    fun screenIsStillKeptOnWithTheLevelLostDialog() {
        show()
        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.H, Position(1, 0)))
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Lost)
            session.sendTick(LeakTiming.EFFECT_MS)
        }
        compose.waitForIdle()

        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
        compose.waitUntil(TIMEOUT_MS) { compose.activity.keepsScreenOn() }
    }

    // 2.1
    @Test
    fun screenIsNoLongerKeptOnOnceTheGameScreenIsGone() {
        show()
        compose.waitUntil(TIMEOUT_MS) { compose.activity.keepsScreenOn() }

        compose.runOnIdle { inGame = false }
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.SCREEN).assertDoesNotExist()
        compose.waitUntil(TIMEOUT_MS) { !compose.activity.keepsScreenOn() }
    }

    private fun show() {
        val level = FieldTest.level()
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    if (inGame) GameScreen(level = level, onExit = { inGame = false }, session = session)
                }
            }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}

/**
 * Whether the activity window asks Android to keep the display on: a view in its hierarchy has
 * [View.keepScreenOn] set, which the view root turns into `FLAG_KEEP_SCREEN_ON` on the window it
 * hands to the window manager. That copy is not `window.attributes`, which never shows the flag.
 */
internal fun Activity.keepsScreenOn(): Boolean = window.decorView.anyKeepsScreenOn()

private fun View.anyKeepsScreenOn(): Boolean =
    keepScreenOn || (this is ViewGroup && (0 until childCount).any { getChildAt(it).anyKeepsScreenOn() })
