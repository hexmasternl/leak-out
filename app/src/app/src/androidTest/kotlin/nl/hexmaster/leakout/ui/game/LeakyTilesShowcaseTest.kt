package nl.hexmaster.leakout.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.leakout.BundledLevels
import nl.hexmaster.leakout.core.engine.FlowState
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelSolver
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.SolverResult
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.settings.TipId
import nl.hexmaster.leakout.ui.game.onboarding.ShownTip
import nl.hexmaster.leakout.ui.game.onboarding.TipOverlayTags
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Leaky tiles on the real game screen, on the bundled level 25, the first level with leaky tiles. */
@RunWith(AndroidJUnit4::class)
class LeakyTilesShowcaseTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var session: GameSession

    private val level25: Level by lazy {
        checkNotNull(BundledLevels(InstrumentationRegistry.getInstrumentation().targetContext.assets).load(25))
    }

    @Test
    fun level25ShowsAndDescribesItsLeakyTiles() {
        show(level25)
        assertTrue(level25.leaky.isNotEmpty())
        for (cell in level25.leaky) {
            compose.cellNode(cell)
                .assert(SemanticsMatcher.expectValue(LeakyTile, LeakyWetness.DAMP))
                .assertContentDescriptionEquals("Leaky tile, empty")
        }
    }

    @Test
    fun aSectionDroppedOnALeakyTileIsPlacedThere() {
        show(level25)
        val cell = level25.leaky.first()
        compose.dropOnCell(cell)
        compose.cellNode(cell)
            .assert(SemanticsMatcher.expectValue(LeakyTile, LeakyWetness.DAMP))
            .assertContentDescriptionEquals("Placed ${session.state.board.pipes.getValue(cell).section.spoken} on a leaky tile")
    }

    @Test
    fun theLiquidDripsOnALeakyTileAndTheScoreGoesDownBy20() {
        show(level25)
        val route = solverRoute(level25)
        val leaky = route.first { (at, _) -> level25.isLeaky(at) }.first
        compose.runOnIdle {
            for ((at, section) in route) assertTrue("place $section on $at", session.placeSection(section, at))
            session.endCountdown()
            tickUntil { head() == leaky }
        }
        compose.waitForIdle()
        compose.cellNode(leaky).assert(SemanticsMatcher.expectValue(LeakyTile, LeakyWetness.DRIPPING))
        val before = compose.runOnIdle { session.state.score }

        compose.runOnIdle { tickUntil { head() != leaky } }
        compose.waitForIdle()
        compose.cellNode(leaky)
            .assert(SemanticsMatcher.expectValue(LeakyTile, LeakyWetness.WET))
            .assert(SemanticsMatcher.expectValue(PointsPopupText, listOf("−20")))
        compose.runOnIdle { assertEquals(before - 20, session.state.score) }

        compose.runOnIdle {
            session.flowToEnd()
            assertTrue(session.state.outcome is Outcome.Won)
        }
    }

    @Test
    fun theLeakyTileTipShowsOnceOnLevel25() {
        val tips = InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro))
        show(level25, tips)
        assertEquals(TipId.LeakyTile, shownTip())
        assertTrue(session.state.paused)
        compose.onNodeWithTag(TipOverlayTags.DONE, useUnmergedTree = true).performClick()
        compose.waitForIdle()
        assertEquals(null, shownTip())
        assertEquals(setOf(TipId.Intro, TipId.LeakyTile), runBlocking { tips.seenTips.first() })
    }

    @Test
    fun theLeakyTileTipIsNotShownOnceSeen() {
        show(level25, InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro, TipId.LeakyTile)))
        assertEquals(null, shownTip())
    }

    private fun head(): Position? = (session.state.flow as? FlowState.Flowing)?.position

    private fun tickUntil(done: () -> Boolean) {
        var guard = 0
        while (!done()) {
            check(++guard < 10_000) { "the flow never got there" }
            session.sendTick(20)
        }
    }

    private fun shownTip(): TipId? = compose.onAllNodesWithTag(TipOverlayTags.OVERLAY).fetchSemanticsNodes()
        .firstOrNull()?.config?.getOrNull(ShownTip)

    /** A straight or corner for every cell of the solver's route; level 25 has no mandatory sections or portals. */
    private fun solverRoute(level: Level): List<Pair<Position, PipeSection>> {
        val path = (LevelSolver.solve(level) as SolverResult.Solvable).path
        val cells = listOf(level.start.position) + path + level.finish.position
        fun side(from: Position, to: Position) = Side.entries.first { from.neighbour(it) == to }
        return path.mapIndexed { i, cell ->
            val sides = setOf(side(cell, cells[i]), side(cell, cells[i + 2]))
            cell to PipeSection.entries.first { it.alwaysOnStack && it.openSides == sides }
        }
    }

    /** Shows [level] with every tip seen unless [tips] says otherwise. */
    private fun show(level: Level, tips: InMemorySettingsRepository = InMemorySettingsRepository(initialSeenTips = TipId.entries.toSet())) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session, tips = tips)
                }
            }
        }
        compose.waitForIdle()
    }
}
