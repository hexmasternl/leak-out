package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
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

    // 6.1
    @Test
    fun hudShowsLevelSpeedStarsScoreAndCogInOrder() {
        show(level(visibleSize = 10))

        compose.onNodeWithTag(HudTags.LEVEL).assertContentDescriptionEquals("Level 1")
        compose.onNodeWithTag(HudTags.SPEED).assertContentDescriptionEquals("Flow speed 0.8 cells per second")
        compose.onNodeWithTag(HudTags.STARS).assertContentDescriptionEquals("0 of 3 stars")
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 0")
        compose.onNodeWithContentDescription("Pause menu").assertIsDisplayed()
        for (star in 1..3) starNode(star).assert(SemanticsMatcher.expectValue(StarEarned, false))

        val hud = bounds(HudTags.HUD)
        val levelBounds = bounds(HudTags.LEVEL)
        val speed = bounds(HudTags.SPEED)
        val stars = bounds(HudTags.STARS)
        val score = bounds(HudTags.SCORE)
        val cog = bounds(HudTags.PAUSE)
        assertTrue(levelBounds.right <= speed.left)
        assertTrue(speed.right < stars.left)
        assertTrue(stars.right <= score.left)
        assertTrue(score.right <= cog.left)
        assertEquals(hud.right.value, cog.right.value, 1f)
        assertTrue(cog.bottom - cog.top >= 48.dp)
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

    // 6.2
    @Test
    fun starsFollowTheScoreAndResetOnRestart() {
        show()
        val expected = listOf(100 to 1, 199 to 1, 200 to 2, 350 to 3)
        for ((score, earned) in expected) {
            compose.runOnIdle { session.score = score }
            compose.waitForIdle()
            for (star in 1..3) {
                starNode(star).assert(SemanticsMatcher.expectValue(StarEarned, star <= earned))
                starNode(star).assert(SemanticsMatcher.expectValue(StarScale, 1f))
            }
            compose.onNodeWithTag(HudTags.STARS).assertContentDescriptionEquals("$earned of 3 stars")
        }

        compose.runOnIdle { session.restart() }
        compose.waitForIdle()
        for (star in 1..3) starNode(star).assert(SemanticsMatcher.expectValue(StarEarned, false))
        compose.onNodeWithTag(HudTags.STARS).assertContentDescriptionEquals("0 of 3 stars")
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
        compose.runOnIdle { assertTrue(session.paused) }
    }

    // 6.4
    @Test
    fun noDraggingWhilePaused() {
        show()
        val stack = compose.runOnIdle { session.pause(); session.stack }
        compose.waitForIdle()

        dragNextToCell(Position(1, 1))

        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "1_1").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(stack, session.stack)
            assertTrue(session.placed.isEmpty())
        }
    }

    // 6.4
    @Test
    fun dragGoingOnWhenPausedReturnsToSlot() {
        show()
        val stack = compose.runOnIdle { session.stack }
        val target = cellCenter(Position(1, 1))
        val next = nextNode()
        val nextBounds = next.getUnclippedBoundsInRoot()

        next.performTouchInput {
            down(center)
            moveBy(Offset(60f, 0f))
        }
        compose.runOnIdle { session.pause() }
        compose.waitForIdle()
        next.performTouchInput {
            moveTo(Offset(target.x - px(nextBounds.left), target.y - px(nextBounds.top)))
            up()
        }
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "1_1").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(stack, session.stack)
            assertTrue(session.placed.isEmpty())
        }
    }

    // 6.5
    @Test
    fun resumeKeepsBoardStackScoreAndStars() {
        show()
        dragNextToCell(Position(1, 1))
        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "1_1").assertIsDisplayed()
        val stack = compose.runOnIdle { session.score = 150; session.stack }

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "1_1").assertIsDisplayed()
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 150")
        compose.onNodeWithTag(HudTags.STARS).assertContentDescriptionEquals("1 of 3 stars")
        compose.runOnIdle {
            assertFalse(session.paused)
            assertEquals(stack, session.stack)
        }
    }

    // 6.5
    @Test
    fun gameClockStopsWhilePausedAndRunsAgainOnResume() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(1_000)
        val running = compose.runOnIdle { session.elapsedMs }
        assertTrue("clock should run, was $running", running > 0)

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.mainClock.advanceTimeBy(100)
        val paused = compose.runOnIdle { session.elapsedMs }
        compose.mainClock.advanceTimeBy(5_000)
        compose.runOnIdle { assertEquals(paused, session.elapsedMs) }

        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle {
            assertTrue(session.elapsedMs > paused)
            assertTrue("no pause time leaked in", session.elapsedMs - paused <= 1_100)
        }
    }

    // 6.6
    @Test
    fun restartClearsBoardDrawsNewStackAndKeepsRunning() {
        show()
        dragNextToCell(Position(1, 1))
        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "1_1").assertIsDisplayed()
        compose.runOnIdle { session.score = 250 }

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESTART).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "1_1").assertDoesNotExist()
        compose.onNodeWithTag(HudTags.LEVEL).assertContentDescriptionEquals("Level 1")
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 0")
        compose.onNodeWithTag(HudTags.STARS).assertContentDescriptionEquals("0 of 3 stars")
        for (star in 1..3) starNode(star).assert(SemanticsMatcher.expectValue(StarEarned, false))
        compose.runOnIdle {
            assertFalse(session.paused)
            assertTrue(session.placed.isEmpty())
            assertEquals(5, session.stack.size)
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
        compose.runOnIdle { assertTrue(session.paused) }

        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag(PauseTags.DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertFalse(session.paused) }
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
        compose.runOnIdle { assertTrue(session.paused) }
    }

    // 6.9
    @Test
    fun goingToTheBackgroundPausesAndShowsDialog() {
        compose.mainClock.autoAdvance = false
        show()
        compose.mainClock.advanceTimeBy(500)
        val before = compose.runOnIdle { session.elapsedMs }

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeBy(1_000)

        compose.onNodeWithTag(PauseTags.DIALOG).assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(session.paused)
            assertTrue(session.pauseMenuOpen)
            assertEquals(before, session.elapsedMs)
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

    private fun starNode(n: Int) = compose.onNodeWithTag("hud_star_$n", useUnmergedTree = true)

    private fun nextNode() = compose.onNodeWithContentDescription("Next: Horizontal straight", useUnmergedTree = true)

    private fun bounds(tag: String): DpRect = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun dragNextToCell(cell: Position) {
        val target = cellCenter(cell)
        val next = nextNode()
        val nextBounds = next.getUnclippedBoundsInRoot()
        next.performTouchInput {
            down(center)
            moveTo(Offset(target.x - px(nextBounds.left), target.y - px(nextBounds.top)))
            up()
        }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
    }

    private fun cellCenter(cell: Position): Offset {
        val board = compose.onNodeWithTag(GameScreenTags.BOARD).getUnclippedBoundsInRoot()
        val width = px(board.right - board.left)
        val height = px(board.bottom - board.top)
        val cellSize = minOf(width / 4, height / 4)
        val left = px(board.left) + (width - cellSize * 4) / 2f
        val top = px(board.top) + (height - cellSize * 4) / 2f
        // The drop point is a fixed offset above the finger.
        return Offset(left + cellSize * (cell.x + 0.5f), top + cellSize * (cell.y + 0.5f) + px(DraggedCopyYOffset))
    }

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
