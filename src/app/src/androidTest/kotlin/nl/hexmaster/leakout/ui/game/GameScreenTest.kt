package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.pipes.pipeTileTag
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun stackShowsVisibleSectionsWithNextAtBottom() {
        show(FieldTest.level(visibleSize = 10))

        compose.onAllNodes(hasContentDescription("Horizontal straight"), useUnmergedTree = true)
            .assertCountEquals(9)
        compose.nextNode().assertIsDisplayed()
    }

    @Test
    fun draggingNextSectionOntoEmptyCellPlacesIt() {
        show()

        compose.dropOnCell(Position(1, 1))

        compose.cellNode(Position(1, 1)).assertContentDescriptionEquals("Placed H")
    }

    @Test
    fun rejectedDropsLeaveTheBoardUnchanged() {
        show(FieldTest.level(blocked = listOf(Position(2, 2))))

        compose.dropAt(compose.nextNode().fetchSemanticsNode().boundsInRoot.center + Offset(-120f, 0f))
        compose.dropOnCell(Position(2, 2))
        compose.dropOnCell(Position(0, 0))

        compose.cellNode(Position(2, 2)).assertContentDescriptionEquals("Blocked")
        compose.cellNode(Position(0, 0)).assertContentDescriptionEquals("Start")
        compose.onAllNodes(hasContentDescription("Placed H"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun previewSectionsDoNotDrag() {
        show()

        compose.onAllNodes(hasContentDescription("Horizontal straight"), useUnmergedTree = true)[0]
            .performTouchInput {
                down(center)
                moveBy(Offset(160f, 0f))
                up()
            }

        compose.onNodeWithTag(GameScreenTags.DRAGGED).assertDoesNotExist()
        compose.onAllNodes(hasContentDescription("Placed H"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun cancelledGestureLeavesStackAndBoardUnchanged() {
        show()

        compose.nextNode().performTouchInput {
            down(center)
            moveBy(Offset(120f, 0f))
            cancel()
        }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()

        compose.onAllNodes(hasContentDescription("Placed H"), useUnmergedTree = true).assertCountEquals(0)
        compose.nextNode().assertIsDisplayed()
    }

    @Test
    fun stackSlotsShowThePipeArtOfTheirSections() {
        val level = FieldTest.level()
        val session = GameSession(level, Random(7))
        // Top to bottom; the last one is the next section.
        val sections = listOf(PipeSection.RES, PipeSection.X, PipeSection.V, PipeSection.RT, PipeSection.H)
        session.state = session.state.copy(stack = session.state.stack.copy(sections = sections))
        show(level, session)

        val inStack = hasAnyAncestor(hasTestTag(GameScreenTags.STACK_PANEL))
        for (section in sections) {
            compose.onAllNodes(hasTestTag(pipeTileTag(section)) and inStack, useUnmergedTree = true).assertCountEquals(1)
        }
    }

    @Test
    fun draggedCopyShowsThePipeArtOfTheDraggedSection() {
        show()

        compose.beginDragTo(compose.cellRect(Position(1, 1)).center)

        compose.onAllNodes(
            hasTestTag(pipeTileTag(PipeSection.H)) and hasAnyAncestor(hasTestTag(GameScreenTags.DRAGGED)),
            useUnmergedTree = true,
        ).assertCountEquals(1)
        compose.releaseDrag()
    }

    private fun show(level: Level = FieldTest.level(), session: GameSession? = null) {
        compose.setContent {
            LeakOutTheme {
                // The test activity shows the system bars; the game hides them, so measure a bare 360 dp screen.
                Box(Modifier.size(800.dp, 360.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    if (session != null) {
                        GameScreen(level = level, onExit = {}, session = session)
                    } else {
                        GameScreen(level = level, onExit = {}, random = Random(7))
                    }
                }
            }
        }
        compose.waitForIdle()
    }
}
