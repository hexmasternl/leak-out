package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
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
import nl.hexmaster.leakout.ui.game.HudModel.Threshold
import nl.hexmaster.leakout.ui.game.HudModel.Warning
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The countdown bar and the score bar's sparkles (tasks 3.3 and 3.5). */
@RunWith(AndroidJUnit4::class)
class HudBarsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var session: GameSession
    private val stars = StarBenchmarks(100, 200, 350)

    // 3.3
    @Test
    fun countdownBarIsFullAtTheStart() {
        compose.mainClock.autoAdvance = false
        showGame()
        countdown().assertContentDescriptionEquals("Flow starts in 10 seconds")
        assertEquals(1f, countdownFraction(), 0.01f)
        countdown().assert(SemanticsMatcher.expectValue(CountdownWarning, Warning.Calm))
    }

    // 3.3
    @Test
    fun countdownBarDrainsWithFlowTime() {
        compose.mainClock.autoAdvance = false
        showGame()
        compose.mainClock.advanceTimeBy(6_000)
        assertEquals(0.4f, countdownFraction(), 0.02f)
        countdown().assert(SemanticsMatcher.expectValue(CountdownWarning, Warning.Brass))

        compose.mainClock.advanceTimeBy(2_500)
        countdown().assert(SemanticsMatcher.expectValue(CountdownWarning, Warning.Red))
    }

    // 3.3
    @Test
    fun countdownBarIsFrozenWhilePaused() {
        compose.mainClock.autoAdvance = false
        showGame()
        compose.runOnIdle {
            session.setCountdown(6_000)
            session.send(GameCommand.Pause)
        }
        compose.mainClock.advanceTimeBy(100)
        val paused = countdownFraction()
        assertEquals(0.6f, paused, 0.01f)

        compose.mainClock.advanceTimeBy(5_000)
        assertEquals(paused, countdownFraction(), 0f)
    }

    // 3.3
    @Test
    fun restartRefillsTheCountdownBar() {
        compose.mainClock.autoAdvance = false
        showGame()
        compose.mainClock.advanceTimeBy(6_000)
        compose.runOnIdle { session.send(GameCommand.Restart) }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1f, countdownFraction(), 0.01f)
    }

    // 3.3
    @Test
    fun countdownBarFadesOutWhenTheLiquidStarts() {
        compose.mainClock.autoAdvance = false
        showGame()
        compose.runOnIdle { session.endCountdown() }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        // Fading: still drawn, but no longer read.
        assertNull(countdown().fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription))

        compose.mainClock.advanceTimeBy(300)
        countdown().assertDoesNotExist()
    }

    // 3.3
    @Test
    fun countdownBarDisappearsAtOnceUnderReducedMotion() {
        var remaining by mutableStateOf<Long?>(3_000L)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LeakOutTheme { CountdownBar(remaining, 10_000, reducedMotion = true) }
        }
        compose.mainClock.advanceTimeByFrame()
        countdown().assertExists()

        remaining = null
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        countdown().assertDoesNotExist()
    }

    // 3.3
    @Test
    fun noCountdownBarWithoutAStartDelay() {
        showGame(initialDelayMs = 0)
        countdown().assertDoesNotExist()
    }

    // 3.5
    @Test
    fun reachingOneStarSparklesOnceTheFillGetsThere() {
        var score by mutableIntStateOf(90)
        showScoreBar { score }

        score = 100
        compose.mainClock.advanceTimeByFrame()
        // The score changed, but the fill has not reached the marker yet.
        assertEquals(emptyList<Threshold>(), sparkled())

        compose.mainClock.advanceTimeBy(SCORE_FILL_MS + 100L)
        assertEquals(listOf(Threshold.One), sparkled())
    }

    // 3.5
    @Test
    fun oneChangePastTwoThresholdsSparklesBoth() {
        var score by mutableIntStateOf(190)
        showScoreBar { score }

        score = 360
        compose.mainClock.advanceTimeBy(SCORE_FILL_MS + 100L)
        assertEquals(listOf(Threshold.Two, Threshold.Three), sparkled())
    }

    // 3.5
    @Test
    fun pointsBeyondThreeStarsDoNotSparkle() {
        var score by mutableIntStateOf(360)
        showScoreBar { score }

        score = 400
        compose.mainClock.advanceTimeBy(SCORE_FILL_MS + 100L)
        assertEquals(emptyList<Threshold>(), sparkled())
    }

    // app-game-leaky-tiles 4.3
    @Test
    fun aScoreBelowZeroIsShownWithAMinusSignOverAnEmptyBar() {
        var score by mutableIntStateOf(0)
        showScoreBar { score }

        score = -20
        compose.mainClock.advanceTimeBy(SCORE_FILL_MS + 100L)
        compose.onNodeWithTag(HudTags.SCORE)
            .assert(SemanticsMatcher.expectValue(ScoreFill, 0f))
            .assertContentDescriptionEquals("Score −20, 0 of 3 stars")
        assertEquals(emptyList<Threshold>(), sparkled())
    }

    // 3.5
    @Test
    fun eachThresholdSparklesOncePerAttempt() {
        var score by mutableIntStateOf(90)
        var attempt by mutableIntStateOf(1)
        showScoreBar(attempt = { attempt }) { score }

        score = 100
        compose.mainClock.advanceTimeBy(SCORE_FILL_MS + 100L)
        score = 120
        compose.mainClock.advanceTimeBy(SCORE_FILL_MS + 100L)
        assertEquals(listOf(Threshold.One), sparkled())

        attempt = 2
        score = 0
        compose.mainClock.advanceTimeByFrame()
        assertEquals(emptyList<Threshold>(), sparkled())
        score = 100
        compose.mainClock.advanceTimeBy(SCORE_FILL_MS + 100L)
        assertEquals(listOf(Threshold.One), sparkled())
    }

    // 3.5
    @Test
    fun noSparkleUnderReducedMotion() {
        var score by mutableIntStateOf(90)
        showScoreBar(reducedMotion = true) { score }

        score = 100
        compose.mainClock.advanceTimeBy(SCORE_FILL_REDUCED_MS + 100L)
        compose.onNodeWithTag(HudTags.SCORE).assert(SemanticsMatcher.expectValue(ScoreFill, 2f / 7))
        assertEquals(emptyList<Threshold>(), sparkled())
    }

    private fun showGame(initialDelayMs: Long = 10_000) {
        val level = level(initialDelayMs)
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session)
                }
            }
        }
        if (compose.mainClock.autoAdvance) compose.waitForIdle() else compose.mainClock.advanceTimeByFrame()
    }

    private fun showScoreBar(
        reducedMotion: Boolean = false,
        attempt: () -> Int = { 1 },
        score: () -> Int,
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LeakOutTheme { ScoreBar(score(), stars, attempt(), reducedMotion = reducedMotion) }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun countdown() = compose.onNodeWithTag(HudTags.COUNTDOWN)

    private fun countdownFraction(): Float = countdown().fetchSemanticsNode().config[CountdownFraction]

    private fun sparkled(): List<Threshold> =
        compose.onNodeWithTag(HudTags.SCORE).fetchSemanticsNode().config[SparkledThresholds]

    private fun level(initialDelayMs: Long): Level = Level(
        id = 1,
        name = "Test",
        grid = GridSize(4, 4),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(3, 3), Side.LEFT),
        blocked = emptyList(),
        flow = FlowSettings(initialDelayMs = initialDelayMs, cellsPerSecond = 0.8),
        stack = StackSettings(
            visibleSize = 5,
            weights = PipeSection.entries.associateWith { if (it == PipeSection.H) 1 else 0 },
        ),
        rules = PlacementRules(),
        stars = stars,
    )
}
