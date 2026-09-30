package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun stackShowsVisibleSectionsWithNextAtBottom() {
        show(level(visibleSize = 10))

        compose.onAllNodes(hasContentDescription("Horizontal straight"), useUnmergedTree = true)
            .assertCountEquals(9)
        compose.onNodeWithContentDescription("Next: Horizontal straight", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun draggingNextSectionOntoEmptyCellPlacesIt() {
        show()

        dragNextToCell(Position(1, 1))

        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "1_1").assertIsDisplayed()
    }

    @Test
    fun rejectedDropsLeaveTheBoardUnchanged() {
        show(level(blocked = listOf(Position(2, 2))))

        dragNextToOutsideBoard()
        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "0_0").assertDoesNotExist()

        dragNextToCell(Position(2, 2))
        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "2_2").assertDoesNotExist()

        dragNextToCell(Position(0, 0))
        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "0_0").assertDoesNotExist()
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
        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "1_1").assertDoesNotExist()
    }

    @Test
    fun cancelledGestureLeavesStackAndBoardUnchanged() {
        show()

        compose.onNodeWithContentDescription("Next: Horizontal straight", useUnmergedTree = true)
            .performTouchInput {
                down(center)
                moveBy(Offset(120f, 0f))
                cancel()
            }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()

        compose.onNodeWithTag(GameScreenTags.PLACED_PREFIX + "1_1").assertDoesNotExist()
        compose.onNodeWithContentDescription("Next: Horizontal straight", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun show(level: Level = level()) {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp)) {
                    GameScreen(level = level, random = Random(7))
                }
            }
        }
        compose.waitForIdle()
    }

    private fun dragNextToCell(cell: Position) {
        val target = cellCenter(cell)
        val next = compose.onNodeWithContentDescription("Next: Horizontal straight", useUnmergedTree = true)
        val nextBounds = next.getUnclippedBoundsInRoot()
        val localTarget = Offset(
            x = target.x - px(nextBounds.left),
            y = target.y - px(nextBounds.top),
        )
        next.performTouchInput {
            down(center)
            moveTo(localTarget)
            up()
        }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
    }

    private fun dragNextToOutsideBoard() {
        val next = compose.onNodeWithContentDescription("Next: Horizontal straight", useUnmergedTree = true)
        next.performTouchInput {
            down(center)
            moveBy(Offset(-120f, 0f))
            up()
        }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
    }

    private fun cellCenter(cell: Position): Offset {
        val board = compose.onNodeWithTag(GameScreenTags.BOARD).getUnclippedBoundsInRoot()
        val columns = 4
        val rows = 4
        val cellSize = minOf(px(board.width) / columns, px(board.height) / rows)
        val left = px(board.left) + (px(board.width) - cellSize * columns) / 2f
        val top = px(board.top) + (px(board.height) - cellSize * rows) / 2f
        return Offset(left + cellSize * (cell.x + 0.5f), top + cellSize * (cell.y + 0.5f))
    }

    private fun level(visibleSize: Int = 5, blocked: List<Position> = emptyList()): Level = Level(
        id = 99,
        name = "Test",
        grid = GridSize(4, 4),
        start = Endpoint(Position(0, 0), Side.RIGHT),
        finish = Endpoint(Position(3, 3), Side.LEFT),
        blocked = blocked,
        flow = FlowSettings(initialDelayMs = 0, cellsPerSecond = 1.0),
        stack = StackSettings(visibleSize = visibleSize, weights = PipeSection.entries.associateWith { if (it == PipeSection.H) 1 else 0 }),
        rules = PlacementRules(),
        stars = StarBenchmarks(1, 2, 3),
    )
    private fun px(value: Dp): Float = with(compose.density) { value.toPx() }
}

private val DpRect.width get() = right - left
private val DpRect.height get() = bottom - top



