package nl.hexmaster.leakout.ui.game

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Position

/** Cell size limits of the playing field, in one place so they can be tuned. */
object FieldLayout {
    val MIN_CELL = 40.dp
    val MAX_CELL = 120.dp
}

/**
 * Which part of the grid the playing field shows, in pixels. [offset] is the top-left corner of
 * the grid in field coordinates. Every operation returns a clamped copy: cells stay within
 * [minCellPx]..[maxCellPx], an axis on which the grid fits is centred, and on any other axis no
 * empty space shows between a grid edge and the field edge.
 */
@Immutable
data class FieldViewport(
    val fieldSize: Size,
    val grid: GridSize,
    val cellPx: Float,
    val offset: Offset,
    private val minLimitPx: Float,
    private val maxLimitPx: Float,
) {
    /** The size at which the whole grid fits, within the limits: zooming out stops here. */
    val minCellPx: Float
        get() = minOf(fieldSize.width / grid.columns, fieldSize.height / grid.rows).coerceIn(minLimitPx, maxLimitPx)

    val maxCellPx: Float get() = maxLimitPx

    fun zoomBy(factor: Float, focus: Offset): FieldViewport {
        val newCell = (cellPx * factor).coerceIn(minCellPx, maxCellPx)
        val newOffset = focus - (focus - offset) * (newCell / cellPx)
        return copy(cellPx = newCell, offset = newOffset).clamped()
    }

    fun panBy(delta: Offset): FieldViewport = copy(offset = offset + delta).clamped()

    /** The grid cell under [fieldPoint], or null outside the field or the grid. */
    fun cellAt(fieldPoint: Offset): Position? {
        if (fieldPoint.x < 0f || fieldPoint.y < 0f || fieldPoint.x >= fieldSize.width || fieldPoint.y >= fieldSize.height) return null
        val position = Position(
            floor((fieldPoint.x - offset.x) / cellPx).toInt(),
            floor((fieldPoint.y - offset.y) / cellPx).toInt(),
        )
        return position.takeIf { it in grid }
    }

    fun cellRect(position: Position): Rect =
        Rect(Offset(offset.x + position.x * cellPx, offset.y + position.y * cellPx), Size(cellPx, cellPx))

    /** The cells that are at least partly inside the field. */
    fun visibleCells(): List<Position> {
        val firstX = floor(-offset.x / cellPx).toInt().coerceAtLeast(0)
        val firstY = floor(-offset.y / cellPx).toInt().coerceAtLeast(0)
        val lastX = floor((fieldSize.width - offset.x) / cellPx).toInt().coerceAtMost(grid.columns - 1)
        val lastY = floor((fieldSize.height - offset.y) / cellPx).toInt().coerceAtMost(grid.rows - 1)
        return (firstY..lastY).flatMap { y -> (firstX..lastX).map { x -> Position(x, y) } }
    }

    private fun clamped(): FieldViewport = copy(
        offset = Offset(
            clampAxis(offset.x, fieldSize.width, cellPx * grid.columns),
            clampAxis(offset.y, fieldSize.height, cellPx * grid.rows),
        ),
    )

    companion object {
        /**
         * The view when a level starts: the smallest cell size, centred when the grid fits, and
         * otherwise centred on [start] as far as the edges allow.
         */
        fun initial(fieldSize: Size, grid: GridSize, start: Position, density: Density): FieldViewport {
            val base = with(density) {
                FieldViewport(fieldSize, grid, 0f, Offset.Zero, FieldLayout.MIN_CELL.toPx(), FieldLayout.MAX_CELL.toPx())
            }
            val cell = base.minCellPx
            val startCentre = Offset((start.x + 0.5f) * cell, (start.y + 0.5f) * cell)
            val offset = Offset(fieldSize.width / 2f, fieldSize.height / 2f) - startCentre
            return base.copy(cellPx = cell, offset = offset).clamped()
        }

        private fun clampAxis(offset: Float, field: Float, content: Float): Float =
            // Half a pixel of slack so a grid that fits exactly is not treated as larger by rounding.
            if (content <= field + 0.5f) (field - content) / 2f else offset.coerceIn(field - content, 0f)
    }
}
