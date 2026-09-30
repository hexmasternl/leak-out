package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.core.library.NextLevelResolver
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
 * The win on the game screen. The frame loop does not run in these tests, so the game clock is
 * moved with tick commands and the flash and tally timing is exact. The game screen sits in a
 * small host that, like `MainActivity`, shows the welcome screen after Exit and the next level
 * after Next level.
 */
@RunWith(AndroidJUnit4::class)
class LevelCompleteTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // The route is worth 60; four unused sections cost 12, so the total is 48: two stars.
    private val stars = StarBenchmarks(20, 40, 200)
    private val level = FieldTest.level(id = 99, stars = stars)
    private val nextLevel = FieldTest.level(id = 100, stars = stars)

    /** Off the route; row-major they are removed in this order. */
    private val unused = listOf(
        Position(0, 1) to PipeSection.H,
        Position(3, 1) to PipeSection.X,
        Position(1, 2) to PipeSection.V,
        Position(0, 3) to PipeSection.LB,
    )
    private val finish = Position(3, 3)

    private lateinit var session: GameSession

    // 7.1
    @Test
    fun winFlashesTheFinishThenShowsTheDialogAfterOneSecond() {
        show()
        win()

        compose.cellNode(finish).assert(SemanticsMatcher.expectValue(FinishFlash, true))
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertDoesNotExist()
        tickToWinAge(999)
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertDoesNotExist()
        tick(1)

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Level complete"))
        compose.onNodeWithText("LEVEL COMPLETE").assertIsDisplayed()
        compose.onNodeWithText("Level complete!").assertIsDisplayed()
        compose.onNodeWithText("You kept the water flowing.").assertIsDisplayed()
        compose.onNodeWithTag(LevelCompleteTags.EARNED).assert(hasText("60")).assertContentDescriptionEquals("Pipe points 60")
        compose.onNodeWithTag(LevelCompleteTags.PENALTY).assert(hasText("-0")).assertContentDescriptionEquals("Unused pipes minus 0")
        compose.onNodeWithTag(LevelCompleteTags.TOTAL).assert(hasText("0")).assertContentDescriptionEquals("Total 0")
        compose.onNodeWithTag(LevelCompleteTags.STARS).assertContentDescriptionEquals("0 of 3 stars")
        for (index in 1..3) star(index).assert(SemanticsMatcher.expectValue(StarEarned, false))
        for (tag in buttons) {
            val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().assertIsNotEnabled().getUnclippedBoundsInRoot()
            assertTrue(bounds.bottom - bounds.top >= 48.dp)
        }
        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertTextEquals("NEXT LEVEL")
        compose.onNodeWithTag(LevelCompleteTags.RESTART).assertTextEquals("RESTART")
        compose.onNodeWithTag(LevelCompleteTags.EXIT).assertTextEquals("EXIT")
        // The finish stays marked behind the dialog, and nothing has been removed yet.
        compose.cellNode(finish).assert(SemanticsMatcher.expectValue(FinishFlash, true))
        for ((at, section) in unused) compose.cellNode(at).assertContentDescriptionEquals("Placed ${section.name}")
    }

    // 7.2
    @Test
    fun theTallyRemovesTheUnusedSectionsInStepAndLightsTheEarnedStars() {
        show()
        win()
        // Two of the four penalty steps done.
        tickToDialogAge(WinTiming.EARNED_HOLD_MS + 2 * WinTiming.PENALTY_STEP_MS)

        compose.onNodeWithTag(LevelCompleteTags.PENALTY).assert(hasText("-6"))
        compose.cellNode(unused[0].first).assertContentDescriptionEquals("Empty")
        compose.cellNode(unused[1].first).assertContentDescriptionEquals("Empty")
        compose.cellNode(unused[2].first).assertContentDescriptionEquals("Placed V")
        compose.cellNode(unused[3].first).assertContentDescriptionEquals("Placed LB")
        compose.cellNode(Position(1, 0)).assertContentDescriptionEquals("Placed H, filled")

        val end = compose.runOnIdle { WinTally.totalDurationMs(session.state.finalScore!!, 2) }
        tickToDialogAge(end - 1)
        compose.onNodeWithTag(LevelCompleteTags.PENALTY).assert(hasText("-12"))
        compose.onNodeWithTag(LevelCompleteTags.TOTAL).assert(hasText("48"))
        for ((at, _) in unused) compose.cellNode(at).assertContentDescriptionEquals("Empty")
        for ((at, section) in FieldTest.ROUTE) compose.cellNode(at).assertContentDescriptionEquals("Placed ${section.name}, filled")
        for (tag in buttons) compose.onNodeWithTag(tag).assertIsNotEnabled()

        tick(1)
        star(1).assert(SemanticsMatcher.expectValue(StarEarned, true))
        star(2).assert(SemanticsMatcher.expectValue(StarEarned, true))
        star(3).assert(SemanticsMatcher.expectValue(StarEarned, false))
        compose.onNodeWithTag(LevelCompleteTags.STARS).assertContentDescriptionEquals("2 of 3 stars")
        compose.onNodeWithTag(LevelCompleteTags.TOTAL).assertContentDescriptionEquals("Total 48")
        for (tag in buttons) compose.onNodeWithTag(tag).assertIsEnabled()
    }

    // 7.3
    @Test
    fun whileWonNoDragStartsAndTheCogWheelDoesNothing() {
        show()
        win()
        val stack = compose.runOnIdle { session.state.stack }

        compose.dropOnCell(Position(3, 0))
        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.cellNode(Position(3, 0)).assertContentDescriptionEquals("Empty")

        compose.onNodeWithTag(HudTags.PAUSE).assertIsNotEnabled().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(session.state.paused)
            assertEquals(stack, session.state.stack)
        }
    }

    // 7.3
    @Test
    fun dragGoingOnWhenTheFinishIsReachedReturnsToItsSlot() {
        show()
        compose.runOnIdle { placeAll() }
        val stack = compose.runOnIdle { session.state.stack }
        val target = compose.cellRect(Position(3, 0)).center + Offset(0f, compose.px(DraggedCopyYOffset))
        val next = compose.nextNode()
        val nextBounds = next.getUnclippedBoundsInRoot()

        next.performTouchInput {
            down(center)
            moveBy(Offset(60f, 0f))
        }
        compose.runOnIdle {
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
        compose.waitForIdle()
        next.performTouchInput {
            moveTo(Offset(target.x - compose.px(nextBounds.left), target.y - compose.px(nextBounds.top)))
            up()
        }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.cellNode(Position(3, 0)).assertContentDescriptionEquals("Empty")
        compose.runOnIdle { assertEquals(stack, session.state.stack) }
    }

    // 7.4
    @Test
    fun restartClosesTheDialogAndPlaysAgain() {
        show()
        winAndFinishTally()

        compose.onNodeWithTag(LevelCompleteTags.RESTART).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertDoesNotExist()
        compose.cellNode(Position(1, 0)).assertContentDescriptionEquals("Empty")
        compose.cellNode(finish).assert(SemanticsMatcher.keyNotDefined(FinishFlash))
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 0")
        compose.runOnIdle {
            assertEquals(Outcome.Playing, session.state.outcome)
            assertEquals(null, session.state.finalScore)
        }

        compose.dropOnCell(Position(1, 1))
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed H")
    }

    // 7.4
    @Test
    fun nextLevelShowsTheNextLevel() {
        show()
        winAndFinishTally()

        compose.onNodeWithTag(LevelCompleteTags.NEXT).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(HudTags.LEVEL).assert(hasText("Level 100"))
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 0")
        compose.cellNode(Position(1, 0)).assertContentDescriptionEquals("Empty")
        compose.dropOnCell(Position(1, 1))
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed H")
    }

    // 7.4
    @Test
    fun exitShowsTheWelcomeScreen() {
        show()
        winAndFinishTally()

        compose.onNodeWithTag(LevelCompleteTags.EXIT).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    // 7.4
    @Test
    fun onTheLastLevelNextLevelStaysDisabled() {
        show(hasNext = false)
        winAndFinishTally()

        compose.onNodeWithTag(LevelCompleteTags.RESTART).assertIsEnabled()
        compose.onNodeWithTag(LevelCompleteTags.EXIT).assertIsEnabled()
        compose.onNodeWithTag(LevelCompleteTags.NEXT).assertIsNotEnabled().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
    }

    // 7.5
    @Test
    fun tapOutsideTheDialogKeepsItOpen() {
        show()
        winAndFinishTally()

        Espresso.onView(isRoot()).inRoot(isDialog()).perform(
            GeneralClickAction(Tap.SINGLE, { floatArrayOf(8f, 8f) }, Press.FINGER, 0, 0),
        )
        compose.waitForIdle()

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertExists()
    }

    // 7.5
    @Test
    fun backWhileTheButtonsAreDisabledKeepsTheDialogOpen() {
        show()
        win()
        tickToDialogAge(300)
        compose.onNodeWithTag(LevelCompleteTags.RESTART).assertIsNotEnabled()

        Espresso.onView(isRoot()).inRoot(isDialog()).perform(ViewActions.pressBack())
        compose.waitForIdle()

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertExists()
        assertFalse(compose.activity.isFinishing)
    }

    // 7.5
    @Test
    fun backWithTheButtonsEnabledShowsTheWelcomeScreen() {
        show()
        winAndFinishTally()

        Espresso.onView(isRoot()).inRoot(isDialog()).perform(ViewActions.pressBack())
        compose.waitForIdle()

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(WelcomeScreenTags.SCREEN).assertIsDisplayed()
    }

    // 7.5
    @Test
    fun backDuringTheFinishFlashDoesNothing() {
        show()
        win()
        tickToWinAge(300)

        Espresso.pressBack()
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.SCREEN).assertIsDisplayed()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertFalse(session.state.paused) }
        assertFalse(compose.activity.isFinishing)

        tickToWinAge(WinTiming.FINISH_EFFECT_MS)
        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
    }

    // 7.6
    @Test
    fun comingBackDuringTheTallyShowsTheFinishedDialogNotThePauseDialog() {
        show()
        win()
        tickToDialogAge(600)

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()

        compose.onNodeWithTag(LevelCompleteTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(LevelCompleteTags.TOTAL).assert(hasText("48"))
        compose.onNodeWithTag(LevelCompleteTags.PENALTY).assert(hasText("-12"))
        star(1).assert(SemanticsMatcher.expectValue(StarEarned, true))
        star(2).assert(SemanticsMatcher.expectValue(StarEarned, true))
        for (tag in buttons) compose.onNodeWithTag(tag).assertIsEnabled()
        for ((at, _) in unused) compose.cellNode(at).assertContentDescriptionEquals("Empty")
        compose.runOnIdle {
            assertFalse(session.state.paused)
            assertEquals(4, session.state.sectionsRemoved)
        }
    }

    private val buttons = listOf(LevelCompleteTags.NEXT, LevelCompleteTags.RESTART, LevelCompleteTags.EXIT)

    private fun star(index: Int) = compose.onNodeWithTag("win_star_$index", useUnmergedTree = true)

    private fun placeAll() {
        for ((at, section) in FieldTest.ROUTE + unused) assertTrue(session.placeSection(section, at))
    }

    /** Places the route and the unused sections and runs the flow into the finish. */
    private fun win() {
        compose.runOnIdle {
            placeAll()
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
            assertEquals(60, session.state.score)
        }
        compose.waitForIdle()
    }

    private fun winAndFinishTally() {
        win()
        val end = compose.runOnIdle { WinTally.totalDurationMs(session.state.finalScore!!, session.state.finalStars) }
        tickToDialogAge(end)
        compose.onNodeWithTag(LevelCompleteTags.RESTART).assertIsEnabled()
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.waitForIdle()
    }

    /** Ticks until the win is [ageMs] old; the flow can overshoot the win by part of a tick. */
    private fun tickToWinAge(ageMs: Long) {
        val remaining = compose.runOnIdle { ageMs - session.winAgeMs }
        if (remaining > 0) tick(remaining)
    }

    private fun tickToDialogAge(ageMs: Long) = tickToWinAge(WinTiming.FINISH_EFFECT_MS + ageMs)

    private fun show(hasNext: Boolean = true) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                var current: Level? by remember { mutableStateOf(level) }
                // The test activity shows the system bars; the game hides them, so measure a bare 360 dp screen.
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    when (val shown = current) {
                        null -> WelcomeScreen(
                            resolveNextLevel = { NextLevelResolver.resolve(totalLevels = 1, stars = emptyMap()) },
                            onStartLevel = {},
                            onOpenLevelLibrary = {},
                            onSettings = {},
                            onExit = {},
                        )
                        else -> key(shown) {
                            GameScreen(
                                level = shown,
                                onExit = { current = null },
                                onNextLevel = if (hasNext && shown == level) ({ current = nextLevel }) else null,
                                random = Random(7),
                                session = remember { if (shown == level) session else GameSession(shown, Random(7)) },
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }
}
