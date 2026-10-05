package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GamePlayHudTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var session: GameSession
    private var exited = false

    // 4.1
    @Test
    fun hudShowsLevelSpeedCountdownScoreAndCogInOrder() {
        show(level(visibleSize = 10))

        val hud = bounds(HudTags.HUD)
        val levelBounds = bounds(HudTags.LEVEL)
        val speed = bounds(HudTags.SPEED)
        val countdown = bounds(HudTags.COUNTDOWN)
        val score = bounds(HudTags.SCORE)
        val cog = bounds(HudTags.PAUSE)
        assertTrue(levelBounds.right <= speed.left)
        assertTrue(speed.right <= countdown.left)
        assertTrue(countdown.right <= score.left)
        assertTrue(score.right <= cog.left)
        assertEquals(hud.left.value + 16f, levelBounds.left.value, 1f)
        assertEquals(hud.right.value - 4f, cog.right.value, 1f)
        assertEquals(ScoreBarWidth.value, (score.right - score.left).value, 1f)
        assertTrue(cog.bottom - cog.top >= 48.dp)
    }

    // 4.1: the game is landscape only, so 640 dp is about the narrowest HUD.
    @Test
    fun countdownKeepsItsMinimumWidthOnANarrowLandscapeScreen() {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.requiredWidth(640.dp)) {
                    GameHud(1, 0.8, 10_000L, 10_000L, StarBenchmarks(100, 200, 350), 1_250, onPause = {})
                }
            }
        }
        val countdown = bounds(HudTags.COUNTDOWN)
        val score = bounds(HudTags.SCORE)
        assertTrue(countdown.right - countdown.left >= 96.dp)
        assertTrue(countdown.right <= score.left)
        assertEquals(ScoreBarWidth.value, (score.right - score.left).value, 1f)
    }

    // 4.1
    @Test
    fun nothingMovesWhenTheCountdownDisappearsOrTheScoreGrows() {
        show()
        val tags = listOf(HudTags.LEVEL, HudTags.SPEED, HudTags.COUNTDOWN_SLOT, HudTags.SCORE, HudTags.PAUSE)
        val before = tags.map { bounds(it) }

        compose.runOnIdle {
            session.endCountdown()
            session.setScore(1_250)
        }
        compose.waitForIdle()

        compose.onNodeWithTag(HudTags.COUNTDOWN).assertDoesNotExist()
        assertEquals(before, tags.map { bounds(it) })
    }

    // 4.2
    @Test
    fun screenReaderReadsTheHud() {
        compose.mainClock.autoAdvance = false
        show(level().copy(id = 7))
        compose.runOnIdle {
            session.setCountdown(6_200)
            session.setScore(150)
        }
        compose.mainClock.advanceTimeByFrame()

        compose.onNodeWithTag(HudTags.LEVEL).assertContentDescriptionEquals("Level 7")
        compose.onNodeWithTag(HudTags.SPEED).assertContentDescriptionEquals("Flow speed 8")
        compose.onNodeWithTag(HudTags.COUNTDOWN).assertContentDescriptionEquals("Flow starts in 7 seconds")
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 150, 1 of 3 stars")
        compose.onNodeWithContentDescription("Pause menu").assertIsDisplayed()
        // The score bar, its markers and number included, is one item for a screen reader.
        compose.onAllNodes(hasContentDescription("Score", substring = true)).assertCountEquals(1)
    }

    // 4.2
    @Test
    fun noCountdownIsReadOnceTheFlowStarted() {
        show()
        compose.runOnIdle { session.endCountdown() }
        compose.waitForIdle()

        compose.onAllNodes(hasContentDescription("Flow starts", substring = true), useUnmergedTree = true)
            .assertCountEquals(0)
        compose.onNodeWithTag(HudTags.LEVEL).assertContentDescriptionEquals("Level 1")
        compose.onNodeWithTag(HudTags.SPEED).assertContentDescriptionEquals("Flow speed 8")
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 0, 0 of 3 stars")
        compose.onNodeWithContentDescription("Pause menu").assertIsDisplayed()
    }

    // 6.1
    @Test
    fun largestStackAndHudFitOn360dpTallScreen() {
        show(level(visibleSize = 10))

        compose.onNodeWithTag(HudTags.HUD).assertIsDisplayed()
        val previews = compose.onAllNodes(hasContentDescription("Horizontal straight"), useUnmergedTree = true)
        previews.assertCountEquals(9)
        for (i in 0 until 9) {
            previews[i].assertIsDisplayed()
            assertTrue(previews[i].getUnclippedBoundsInRoot().top >= bounds(HudTags.HUD).bottom)
        }
        val next = compose.onNodeWithContentDescription("Next: Horizontal straight", useUnmergedTree = true)
        next.assertIsDisplayed()
        assertTrue(next.getUnclippedBoundsInRoot().bottom <= 360.dp)
    }

    // 3.2
    @Test
    fun flowSpeedIsTheWholeNumber() {
        show()
        compose.onNodeWithTag(HudTags.SPEED).assertContentDescriptionEquals("Flow speed 8")
    }

    // 3.2
    @Test
    fun flowSpeedIsRoundedHalfUp() {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.requiredWidth(800.dp)) {
                    GameHud(1, 1.25, null, 10_000L, StarBenchmarks(100, 200, 350), 0, onPause = {})
                }
            }
        }
        compose.onNodeWithTag(HudTags.SPEED).assertContentDescriptionEquals("Flow speed 13")
    }

    // 3.4
    @Test
    fun scoreBarFollowsTheScoreAndEmptiesOnRestart() {
        show()
        scoreBar().assert(SemanticsMatcher.expectValue(ScoreFill, 0f))
        scoreBar().assertContentDescriptionEquals("Score 0, 0 of 3 stars")

        for ((score, fill) in listOf(175 to 0.5f, 350 to 1f, 1_250 to 1f)) {
            compose.runOnIdle { session.setScore(score) }
            compose.waitForIdle()
            scoreBar().assert(SemanticsMatcher.expectValue(ScoreFill, fill))
        }
        scoreBar().assertContentDescriptionEquals("Score 1,250, 3 of 3 stars")

        compose.runOnIdle { session.setScore(250) }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { session.send(GameCommand.Restart) }
        compose.mainClock.advanceTimeByFrame()
        // Empty at once, without animating down.
        scoreBar().assert(SemanticsMatcher.expectValue(ScoreFill, 0f))
        scoreBar().assertContentDescriptionEquals("Score 0, 0 of 3 stars")
    }

    // 3.4
    @Test
    fun scoreNumberChangesAtOnceWhileTheFillMoves() {
        show()
        compose.runOnIdle { session.setScore(50) }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false

        compose.runOnIdle { session.setScore(100) }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(100)
        scoreBar().assertScore("100")
        val moving = scoreBar().fetchSemanticsNode().config[ScoreFill]
        assertTrue("fill should be on its way, was $moving", moving > 1f / 7 && moving < 2f / 7)

        compose.mainClock.advanceTimeBy(SCORE_FILL_MS + 100L)
        scoreBar().assert(SemanticsMatcher.expectValue(ScoreFill, 2f / 7))
    }

    // 6.3
    @Test
    fun cogWheelOpensDialogInOrderAndPauses() {
        show()

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
        val order = listOf(PauseTags.RESUME, PauseTags.RESTART, PauseTags.DIVIDER, PauseTags.EXIT).map { bounds(it) }
        order.zipWithNext().forEach { (upper, lower) -> assertTrue(upper.bottom <= lower.top) }
        listOf(PauseTags.RESUME, PauseTags.RESTART, PauseTags.EXIT).forEach {
            assertTrue(bounds(it).bottom - bounds(it).top >= 48.dp)
        }
        compose.runOnIdle { assertTrue(session.state.paused) }
    }

    // 6.4
    @Test
    fun noDraggingWhilePaused() {
        show()
        val stack = compose.runOnIdle { session.send(GameCommand.Pause); session.state.stack }
        compose.waitForIdle()

        compose.dropOnCell(Position(1, 1))

        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Empty")
        compose.runOnIdle {
            assertEquals(stack, session.state.stack)
            assertTrue(session.state.board.pipes.isEmpty())
        }
    }

    // 6.4
    @Test
    fun dragGoingOnWhenPausedReturnsToSlot() {
        show()
        val stack = compose.runOnIdle { session.state.stack }
        val target = compose.cellRect(Position(1, 1)).center + Offset(0f, px(DraggedCopyYOffset))
        val next = nextNode()
        val nextBounds = next.getUnclippedBoundsInRoot()

        next.performTouchInput {
            down(center)
            moveBy(Offset(60f, 0f))
        }
        compose.runOnIdle { session.send(GameCommand.Pause) }
        compose.waitForIdle()
        next.performTouchInput {
            moveTo(Offset(target.x - px(nextBounds.left), target.y - px(nextBounds.top)))
            up()
        }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Empty")
        compose.runOnIdle {
            assertEquals(stack, session.state.stack)
            assertTrue(session.state.board.pipes.isEmpty())
        }
    }

    // 6.5
    @Test
    fun resumeKeepsBoardStackScoreAndStars() {
        show()
        compose.dropOnCell(Position(1, 1))
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed Horizontal straight")
        val stack = compose.runOnIdle { session.setScore(150); session.state.stack }

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed Horizontal straight")
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 150, 1 of 3 stars")
        compose.runOnIdle {
            assertFalse(session.state.paused)
            assertEquals(stack, session.state.stack)
        }
    }

    // 6.5
    @Test
    fun gameClockStopsWhilePausedAndRunsAgainOnResume() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(1_000)
        val running = compose.runOnIdle { session.state.clockMs }
        assertTrue("clock should run, was $running", running > 0)

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.mainClock.advanceTimeBy(100)
        val paused = compose.runOnIdle { session.state.clockMs }
        compose.mainClock.advanceTimeBy(5_000)
        compose.runOnIdle { assertEquals(paused, session.state.clockMs) }

        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle {
            assertTrue(session.state.clockMs > paused)
            assertTrue("no pause time leaked in", session.state.clockMs - paused <= 1_100)
        }
    }

    // 6.6
    @Test
    fun restartClearsBoardDrawsNewStackAndKeepsRunning() {
        show()
        compose.dropOnCell(Position(1, 1))
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed Horizontal straight")
        compose.runOnIdle { session.setScore(250) }

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESTART).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Empty")
        compose.onNodeWithTag(HudTags.LEVEL).assertContentDescriptionEquals("Level 1")
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 0, 0 of 3 stars")
        compose.onNodeWithTag(HudTags.SCORE).assert(SemanticsMatcher.expectValue(ScoreFill, 0f))
        compose.runOnIdle {
            assertFalse(session.state.paused)
            assertTrue(session.state.board.pipes.isEmpty())
            assertEquals(15, session.state.stack.size)
        }
    }

    // 6.7
    @Test
    fun exitCallsOnExit() {
        show()
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.EXIT).performClick()
        compose.waitForIdle()
        assertTrue(exited)
    }

    // 6.8
    @Test
    fun backOpensDialogAndBackInDialogResumes() {
        show()

        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
        compose.onNodeWithTag(GameScreenTags.SCREEN).assertExists()
        compose.runOnIdle { assertTrue(session.state.paused) }

        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertFalse(session.state.paused) }
        assertFalse(compose.activity.isFinishing)
    }

    // 6.8
    @Test
    fun tapOutsideDialogKeepsItOpen() {
        show()
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.waitForIdle()

        Espresso.onView(isRoot()).inRoot(isDialog()).perform(
            GeneralClickAction(Tap.SINGLE, { floatArrayOf(8f, 8f) }, Press.FINGER, 0, 0),
        )
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
        compose.runOnIdle { assertTrue(session.state.paused) }
    }

    // 6.9
    @Test
    fun goingToTheBackgroundPausesAndShowsDialog() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(500)
        val before = compose.runOnIdle { session.state.clockMs }

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(1_000)

        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(session.state.paused)
            assertTrue(session.showPauseDialog)
            assertEquals(before, session.state.clockMs)
        }
    }

    private fun show(level: Level = level()) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                // The test activity shows the system bars; the game hides them, so measure a bare 360 dp screen.
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = { exited = true }, session = session)
                }
            }
        }
        if (compose.mainClock.autoAdvance) compose.waitForIdle() else compose.mainClock.advanceTimeByFrame()
    }

    private fun scoreBar() = compose.onNodeWithTag(HudTags.SCORE)

    private fun nextNode() = compose.onNodeWithContentDescription("Next: Horizontal straight", useUnmergedTree = true)

    private fun bounds(tag: String): DpRect = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun level(visibleSize: Int = 5): Level = Level(
        id = 1,
        name = "Test",
        grid = GridSize(4, 4),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(3, 3), Side.LEFT),
        blocked = emptyList(),
        flow = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 0.8),
        stack = StackSettings(
            visibleSize = visibleSize,
            weights = PipeSection.entries.associateWith { if (it == PipeSection.H) 1 else 0 },
        ),
        rules = PlacementRules(),
        stars = StarBenchmarks(100, 200, 350),
    )

    private fun px(value: Dp): Float = with(compose.density) { value.toPx() }
}
