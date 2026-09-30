package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.score.ScoreSheet
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.title.WelcomeScreen
import nl.hexmaster.leakout.ui.title.WelcomeScreenTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The level lost state on the game screen. The frame loop does not run in these tests, so the game
 * clock is moved with [GameSession.tick] and the leak effect timing is exact. The game screen sits
 * in a small host that shows the welcome screen after Exit, like `MainActivity`.
 */
@RunWith(AndroidJUnit4::class)
class LevelLostTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var session: GameSession
    private val placed = Position(1, 0)

    // 6.1
    @Test
    fun leakShowsTheEffectThenTheDialogAfterOneSecond() {
        show()
        compose.runOnIdle {
            assertTrue(session.place(PipeSection.H, placed))
            session.sheet = ScoreSheet(total = 85)
            session.liquidLeaked(placed, Side.RIGHT)
        }
        compose.waitForIdle()

        compose.cellNode(placed).assert(SemanticsMatcher.expectValue(LeakAt, Side.RIGHT))
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertDoesNotExist()
        tick(999)
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertDoesNotExist()
        tick(1)

        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Level lost"))
        compose.onNodeWithText("Leaked!", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("The pipe is leaking. Level lost.").assertIsDisplayed()
        compose.onNodeWithTag(LevelLostTags.SCORE).assertTextEquals("Score 85")
        compose.onNodeWithText("Resume").assertDoesNotExist()
        for (tag in listOf(LevelLostTags.RESTART, LevelLostTags.EXIT)) {
            val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(bounds.bottom - bounds.top >= 48.dp)
        }
        compose.onNodeWithTag(LevelLostTags.RESTART).assertTextEquals("RESTART")
        compose.onNodeWithTag(LevelLostTags.EXIT).assertTextEquals("EXIT")
        // The leak stays marked behind the dialog.
        compose.cellNode(placed).assert(SemanticsMatcher.expectValue(LeakAt, Side.RIGHT))
    }

    // 6.2
    @Test
    fun whileLostNoDragStartsAndTheCogWheelDoesNothing() {
        show()
        lose()
        val stack = compose.runOnIdle { session.stack }

        compose.dropOnCell(Position(1, 1))
        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Empty")

        compose.onNodeWithTag(HudTags.PAUSE).assertIsNotEnabled().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(session.paused)
            assertEquals(stack, session.stack)
            assertEquals(1, session.board.pipes.size)
        }
    }

    // 6.2
    @Test
    fun dragGoingOnWhenTheLeakHappensReturnsToItsSlot() {
        show()
        compose.runOnIdle { assertTrue(session.place(PipeSection.H, placed)) }
        val stack = compose.runOnIdle { session.stack }
        val target = compose.cellRect(Position(1, 1)).center + Offset(0f, compose.px(DraggedCopyYOffset))
        val next = compose.nextNode()
        val nextBounds = next.getUnclippedBoundsInRoot()

        next.performTouchInput {
            down(center)
            moveBy(Offset(60f, 0f))
        }
        compose.runOnIdle { session.liquidLeaked(placed, Side.RIGHT) }
        compose.waitForIdle()
        next.performTouchInput {
            moveTo(Offset(target.x - compose.px(nextBounds.left), target.y - compose.px(nextBounds.top)))
            up()
        }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Empty")
        compose.runOnIdle { assertEquals(stack, session.stack) }
    }

    // 6.3
    @Test
    fun restartClosesTheDialogAndPlaysAgain() {
        show()
        lose(score = 40)
        tick(LeakTiming.EFFECT_MS)

        compose.onNodeWithTag(LevelLostTags.RESTART).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(LevelLostTags.DIALOG).assertDoesNotExist()
        compose.cellNode(placed).assertContentDescriptionEquals("Empty")
        compose.cellNode(placed).assert(SemanticsMatcher.keyNotDefined(LeakAt))
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 0")
        compose.runOnIdle { assertEquals(Outcome.Playing, session.outcome) }

        compose.dropOnCell(Position(1, 1))
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed H")
    }

    // 6.3
    @Test
    fun exitShowsTheWelcomeScreen() {
        show()
        lose()
        tick(LeakTiming.EFFECT_MS)

        compose.onNodeWithTag(LevelLostTags.EXIT).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(LevelLostTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    // 6.4
    @Test
    fun tapOutsideTheDialogKeepsItOpen() {
        show()
        lose()
        tick(LeakTiming.EFFECT_MS)

        Espresso.onView(isRoot()).inRoot(isDialog()).perform(
            GeneralClickAction(Tap.SINGLE, { floatArrayOf(8f, 8f) }, Press.FINGER, 0, 0),
        )
        compose.waitForIdle()

        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertExists()
    }

    // 6.4
    @Test
    fun backOnTheDialogShowsTheWelcomeScreen() {
        show()
        lose()
        tick(LeakTiming.EFFECT_MS)
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()

        // Target the dialog window: the default root picker can pick the activity behind it.
        Espresso.onView(isRoot()).inRoot(isDialog()).perform(ViewActions.pressBack())
        compose.waitForIdle()

        compose.onNodeWithTag(LevelLostTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    // 6.4
    @Test
    fun backDuringTheLeakEffectDoesNothing() {
        show()
        lose()
        tick(300)

        Espresso.pressBack()
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertFalse(session.paused) }
        assertFalse(compose.activity.isFinishing)

        tick(700)
        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
    }

    // 6.5
    @Test
    fun comingBackAfterALossShowsTheLostDialogNotThePauseDialog() {
        show()
        lose()
        tick(200)

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()

        compose.onNodeWithTag(LevelLostTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(session.paused)
            assertTrue(session.outcome is Outcome.Lost)
        }
    }

    /** Places H on [placed] and leaks it to the right, with the score at [score]. */
    private fun lose(score: Int = 0) {
        compose.runOnIdle {
            assertTrue(session.place(PipeSection.H, placed))
            session.sheet = ScoreSheet(total = score)
            session.liquidLeaked(placed, Side.RIGHT)
        }
        compose.waitForIdle()
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.tick(ms) }
        compose.waitForIdle()
    }

    private fun show(level: Level = FieldTest.level()) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                var inGame by remember { mutableStateOf(true) }
                // The test activity shows the system bars; the game hides them, so measure a bare 360 dp screen.
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    if (inGame) {
                        GameScreen(level = level, onExit = { inGame = false }, session = session)
                    } else {
                        WelcomeScreen(onPlay = {}, onSettings = {}, onExit = {})
                    }
                }
            }
        }
        compose.waitForIdle()
    }
}
