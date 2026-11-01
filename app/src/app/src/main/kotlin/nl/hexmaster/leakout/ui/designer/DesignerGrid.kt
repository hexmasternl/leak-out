package nl.hexmaster.leakout.ui.designer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.floor
import kotlin.math.min
import nl.hexmaster.leakout.core.board.Board
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.game.drawStillCell
import nl.hexmaster.leakout.ui.pipes.LiquidScratch
import nl.hexmaster.leakout.ui.pipes.rememberPipeArtLibrary
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/**
 * Where the designer grid's cells are inside a canvas of [size]: square cells as large as fit the
 * whole grid, the grid centred. Pure, so tests can find a cell's centre.
 */
data class DesignerGridLayout(val grid: GridSize, val cellPx: Float, val origin: Offset) {
    fun cellRect(position: Position): Rect =
        Rect(Offset(origin.x + position.x * cellPx, origin.y + position.y * cellPx), Size(cellPx, cellPx))

    /** The cell under [point], or `null` outside the grid. */
    fun cellAt(point: Offset): Position? {
        if (cellPx <= 0f) return null
        val position = Position(floor((point.x - origin.x) / cellPx).toInt(), floor((point.y - origin.y) / cellPx).toInt())
        return position.takeIf { it in grid }
    }

    companion object {
        fun compute(size: Size, grid: GridSize): DesignerGridLayout {
            val cell = min(size.width / grid.columns, size.height / grid.rows).coerceAtLeast(0f)
            val origin = Offset((size.width - cell * grid.columns) / 2f, (size.height - cell * grid.rows) / 2f)
            return DesignerGridLayout(grid, cell, origin)
        }
    }
}

/**
 * The design on the real playing field's look: cells, blocked cells, leaky tiles, mandatory
 * sections, portals and the endpoints with their open sides, drawn by the game's own cell drawing. The [unpaired]
 * portal, still waiting for its partner, is drawn as the next pair. [highlighted] cells, the
 * cells validation errors are about, get a `leak.500` frame and the [selected] portal a `brass.300`
 * one; a solvable design's [path] is drawn faintly through the cells as a hint.
 *
 * A tap on a cell calls [onTap]. A drag calls [onDragStart] with the cell it started on as soon as
 * it moves into a second cell, then [onDrag] for every further cell it enters, and [onDragEnd] when
 * it lifts.
 */
@Composable
fun DesignerGrid(
    level: Level,
    highlighted: Set<Position>,
    path: List<Position>,
    onTap: (Position) -> Unit,
    onDragStart: (Position) -> Unit,
    onDrag: (Position) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    unpaired: Endpoint? = null,
    selected: Position? = null,
) {
    val pipes = rememberPipeArtLibrary()
    val scratch = remember { LiquidScratch() }
    val board = remember(level, unpaired) {
        val complete = Board.from(level)
        if (unpaired == null) complete else complete.with(unpaired.position, Cell.Portal(level.portals.size + 1, unpaired.openSide, unpaired.position))
    }
    val grid = level.grid
    val currentGrid by rememberUpdatedState(grid)
    val tap by rememberUpdatedState(onTap)
    val dragStart by rememberUpdatedState(onDragStart)
    val drag by rememberUpdatedState(onDrag)
    val dragEnd by rememberUpdatedState(onDragEnd)
    Canvas(
        modifier = modifier
            .testTag(DesignerTags.GRID)
            .background(LeakOutColors.SurfaceDeep, GridShape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), GridShape)
            .clip(GridShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val layout = DesignerGridLayout.compute(size.toSize(), currentGrid)
                    val down = awaitFirstDown()
                    val first = layout.cellAt(down.position) ?: return@awaitEachGesture
                    down.consume()
                    var last = first
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val cell = layout.cellAt(change.position) ?: continue
                        if (cell == last) continue
                        if (!dragging) {
                            dragging = true
                            dragStart(first)
                        }
                        drag(cell)
                        last = cell
                    }
                    if (dragging) dragEnd() else tap(first)
                }
            },
    ) {
        val layout = DesignerGridLayout.compute(size, grid)
        for (y in 0 until grid.rows) {
            for (x in 0 until grid.columns) {
                val position = Position(x, y)
                drawStillCell(board[position], layout.cellRect(position), pipes, scratch, leaky = level.isLeaky(position))
            }
        }
        if (path.size > 1) {
            val line = Path().apply {
                path.forEachIndexed { index, cell ->
                    val c = layout.cellRect(cell).center
                    if (index == 0) moveTo(c.x, c.y) else lineTo(c.x, c.y)
                }
            }
            drawPath(
                line,
                LeakOutColors.Liquid300.copy(alpha = PATH_ALPHA),
                style = Stroke(layout.cellPx * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        val frames = highlighted.map { it to LeakOutColors.Leak500 } + listOfNotNull(selected?.let { it to LeakOutColors.Brass300 })
        for ((position, color) in frames) {
            if (position !in grid) continue
            val rect = layout.cellRect(position).deflate(layout.cellPx * 0.04f)
            drawRoundRect(
                color,
                rect.topLeft,
                rect.size,
                CornerRadius(layout.cellPx * 0.14f),
                style = Stroke(maxOf(2.dp.toPx(), layout.cellPx * 0.08f)),
            )
        }
    }
}

private const val PATH_ALPHA = 0.35f
private val GridShape = RoundedCornerShape(16.dp)
