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
 * Points popups on the playing field. The frame loop does not run in these tests, so the game
 * clock is moved with [GameSession.tick] and every timing is exact.
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
        show(FieldTest.level(section = PipeSection.LT))
        val corner = Position(1, 1)
        compose.runOnIdle { assertTrue(session.place(session.stack.next, corner)) }
        compose.cellNode(corner).assertContentDescriptionEquals("Placed LT")

        compose.runOnIdle { session.liquidExited(corner) }

        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 15")
        assertPopups(corner, "+15")

        tick(999)
        assertPopups(corner, "+15")
        tick(1)
        assertPopups(corner)
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 15")
    }

    // 6.2
    @Test
    fun twoCloseExitsShowBothPopupsAndTheFirstGoesFirst() {
        show()
        val straight = Position(1, 0)
        val next = Position(2, 0)
        compose.runOnIdle {
            session.place(PipeSection.H, straight)
            session.place(PipeSection.LB, next)
            session.liquidExited(straight)
        }
        tick(300)
        compose.runOnIdle { session.liquidExited(next) }

        assertPopups(straight, "+10")
        assertPopups(next, "+15")
        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 25")

        tick(700)
        assertPopups(straight)
        assertPopups(next, "+15")
        tick(300)
        assertPopups(next)
    }

    // 6.3
    @Test
    fun pauseFreezesAPopupAndRestartRemovesAll() {
        show()
        val cell = Position(1, 1)
        compose.runOnIdle {
            session.place(PipeSection.V, cell)
            session.liquidExited(cell)
        }
        tick(400)

        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        tick(5_000)
        compose.onNodeWithTag(PauseTags.RESUME).performClick()
        compose.waitForIdle()
        assertPopups(cell, "+10")

        tick(599)
        assertPopups(cell, "+10")
        tick(1)
        assertPopups(cell)

        compose.runOnIdle { session.liquidExited(cell) }
        assertPopups(cell, "+10")
        compose.onNodeWithTag(HudTags.PAUSE).performClick()
        compose.onNodeWithTag(PauseTags.RESTART).performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(HudTags.SCORE).assertContentDescriptionEquals("Score 0")
        compose.runOnIdle { assertTrue(session.popups.isEmpty()) }
        assertPopups(cell)
    }

    // 6.4
    @Test
    fun popupStaysOnItsTileWhileThePlayerPansFromIt() {
        show(FieldTest.large())
        val cell = Position(2, 5)
        compose.runOnIdle {
            session.place(PipeSection.H, cell)
            session.liquidExited(cell)
        }
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
        compose.runOnIdle { assertEquals(Position(2, 5), session.popups.single().position) }
    }

    private fun tick(ms: Long) {
        compose.runOnIdle { session.tick(ms) }
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
