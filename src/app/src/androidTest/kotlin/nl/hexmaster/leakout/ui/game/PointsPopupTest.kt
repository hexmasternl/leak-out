package nl.hexmaster.leakout.ui.game

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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Points popups on the playing field, awarded by the engine's flow as the liquid leaves a tile. The
 * frame loop does not run in these tests, so the game clock is moved with tick commands, and the
 * levels run at whole cells per second so every award lands on an exact tick.
 */
@RunWith(AndroidJUnit4::class)
class PointsPopupTest {
    val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(LandscapeRule()).around(compose)

    private lateinit var session: GameSession

    // 6.1
    @Test
    fun cornerExitShowsPlus15AndTheHudScoreThenThePopupGoes() {
        show(FieldTest.level(cellsPerSecond = 1.0))
        val corner = Position(1, 0)
        compose.runOnIdle {
            assertTrue(session.placeSection(PipeSection.LB, corner))
            assertTrue(session.placeSection(PipeSection.V, Position(1, 1)))
        }
        compose.cellNode(corner).assertContentDescriptionEquals("Placed LB")

        compose.runOnIdle { session.endCountdown() }
        tick(1_000) // through the start
        tick(1_000) // the liquid leaves the corner

        compose.onNodeWithTag(HudTags.SCORE).assertScore("15")
        assertPopups(corner, "+15")

        tick(999)
        assertPopups(corner, "+15")
        compose.onNodeWithTag(HudTags.SCORE).assertScore("15")
        tick(1) // the popup is over, and the liquid leaves the V below
        assertPopups(corner)
        assertPopups(Position(1, 1), "+10")
        compose.onNodeWithTag(HudTags.SCORE).assertScore("25")
    }

    // 6.2
    @Test
    fun twoCloseExitsShowBothPopupsAndTheFirstGoesFirst() {
        show(FieldTest.level(cellsPerSecond = 2.0))
        val straight = Position(1, 0)
        val corner = Position(2, 0)
        compose.runOnIdle {
            session.placeSection(PipeSection.H, straight)
            session.placeSection(PipeSection.LB, corner)
            session.placeSection(PipeSection.V, Position(2, 1))
            session.endCountdown()
        }
        tick(500) // through the start
        tick(500) // leaves the straight
        tick(500) // leaves the corner

        assertPopups(straight, "+10")
        assertPopups(corner, "+15")
        compose.onNodeWithTag(HudTags.SCORE).assertScore("25")

        tick(500)
        assertPopups(straight)
        assertPopups(corner, "+15")
        tick(500)
        assertPopups(corner)
    }

    // 6.3
    @Test
    fun pauseFreezesAPopupAndRestartRemovesAll() {
        show(FieldTest.level(cellsPerSecond = 0.5))
        val straight = Position(1, 0)
        compose.runOnIdle {
            for ((at, section) in FieldTest.ROUTE) assertTrue(session.placeSection(section, at))
            session.endCountdown()
        }
        tick(2_000) // through the start
        tick(2_000) // leaves the straight
        tick(400)

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        tick(5_000)
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.waitForIdle()
        assertPopups(straight, "+10")

        tick(599)
        assertPopups(straight, "+10")
        tick(1)
        assertPopups(straight)

        tick(1_000) // leaves the corner
        val corner = Position(2, 0)
        assertPopups(corner, "+15")
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESTART).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(HudTags.SCORE).assertScore("0")
        compose.runOnIdle { assertTrue(session.state.awards.isEmpty()) }
        assertPopups(corner)
    }

    // 6.4
    @Test
    fun popupStaysOnItsTileWhileThePlayerPansFromIt() {
        show(FieldTest.large(cellsPerSecond = 1.0))
        val cell = Position(2, 5)
        compose.runOnIdle {
            session.placeSection(PipeSection.H, Position(1, 5))
            session.placeSection(PipeSection.H, cell)
            session.endCountdown()
            session.sendTick(2_000) // the start and (1, 5)
            session.sendTick(1_000) // (2, 5); the popup of (1, 5) is just over
        }
        compose.waitForIdle()
        val before = compose.cellRect(cell)

        // The pan starts on the tile that shows the popup.
        compose.cellNode(cell).performTouchInput {
            down(center)
            for (step in 1..10) moveTo(center + Offset(0f, -compose.px(60.dp)) * (step / 10f))
            up()
        }
        compose.waitForIdle()

        val after = compose.cellRect(cell)
        assertTrue("field panned: ${before.top} -> ${after.top}", after.top < before.top - compose.px(20.dp))
        assertEquals(before.left, after.left, 1f)
        assertPopups(cell, "+10")
        compose.runOnIdle { assertEquals(Position(2, 5), session.state.awards.single().position) }
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.sendTick(ms) }
        compose.waitForIdle()
    }

    private fun assertPopups(cell: Position, vararg texts: String) {
        compose.cellNode(cell).assert(SemanticsMatcher.expectValue(PointsPopupText, texts.toList()))
    }

    private fun show(level: Level = FieldTest.level()) {
        session = GameSession(level, Random(7))
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    GameScreen(level = level, onExit = {}, session = session)
                }
            }
        }
        compose.waitForIdle()
    }
}
