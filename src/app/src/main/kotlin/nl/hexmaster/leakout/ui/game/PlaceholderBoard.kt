package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.theme.LeakOutColors

@Composable
fun PlaceholderBoard(
    level: Level,
    placed: Map<Position, PipeSection>,
    onPlace: (Position, PipeSection) -> Boolean,
    modifier: Modifier = Modifier,
): SectionDropTarget {
    var containerBounds by remember { mutableStateOf(Rect.Zero) }
    val blocked = remember(level) { level.blocked.toSet() }
    val currentOnPlace by rememberUpdatedState(onPlace)
    fun boardGeometry(): BoardGeometry = boardGeometry(containerBounds, level)
    val target = remember(level, placed) {
        SectionDropTarget { section, position ->
            val geometry = boardGeometry()
            if (geometry.bounds == Rect.Zero || position !in geometry.bounds) return@SectionDropTarget false
            val x = floor((position.x - geometry.bounds.left) / geometry.cellSize).toInt()
            val y = floor((position.y - geometry.bounds.top) / geometry.cellSize).toInt()
            val cell = Position(x, y)
            if (x !in 0 until level.grid.columns || y !in 0 until level.grid.rows) return@SectionDropTarget false
            if (cell in blocked || cell == level.start.position || cell == level.finish.position || placed.containsKey(cell)) {
                return@SectionDropTarget false
            }
            currentOnPlace(cell, section)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(GameScreenTags.BOARD)
            .background(LeakOutColors.SurfaceDeep, RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
            .onGloballyPositioned { containerBounds = it.boundsInRoot() },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cellSize = minOf(size.width / level.grid.columns, size.height / level.grid.rows)
            val boardWidth = cellSize * level.grid.columns
            val boardHeight = cellSize * level.grid.rows
            val left = (size.width - boardWidth) / 2f
            val top = (size.height - boardHeight) / 2f

            for (y in 0 until level.grid.rows) {
                for (x in 0 until level.grid.columns) {
                    val pos = Position(x, y)
                    val color = when (pos) {
                        level.start.position -> LeakOutColors.SecondaryBase.copy(alpha = 0.80f)
                        level.finish.position -> LeakOutColors.SuccessGreen.copy(alpha = 0.80f)
                        in blocked -> LeakOutColors.ErrorRed.copy(alpha = 0.70f)
                        else -> LeakOutColors.SurfaceCard.copy(alpha = 0.10f)
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left + x * cellSize + 2.dp.toPx(), top + y * cellSize + 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(cellSize - 4.dp.toPx(), cellSize - 4.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    )
                }
            }
        }
        val density = LocalDensity.current
        val geometry = boardGeometry()
        placed.forEach { (position, section) ->
            if (geometry.cellSize > 0f) {
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                x = (geometry.localLeft + position.x * geometry.cellSize).roundToInt(),
                                y = (geometry.localTop + position.y * geometry.cellSize).roundToInt(),
                            )
                        }
                        .size(with(density) { geometry.cellSize.toDp() })
                        .testTag(GameScreenTags.PLACED_PREFIX + position.x + "_" + position.y)
                        .semantics { contentDescription = "Placed ${section.name}" },
                ) {
                    PipeSectionGlyph(section, Modifier.fillMaxSize())
                }
            }
        }
    }
    return target
}

private data class BoardGeometry(
    val bounds: Rect,
    val cellSize: Float,
    val localLeft: Float,
    val localTop: Float,
)

private fun boardGeometry(containerBounds: Rect, level: Level): BoardGeometry {
    if (containerBounds == Rect.Zero) return BoardGeometry(Rect.Zero, 0f, 0f, 0f)
    val cellSize = minOf(containerBounds.width / level.grid.columns, containerBounds.height / level.grid.rows)
    val boardWidth = cellSize * level.grid.columns
    val boardHeight = cellSize * level.grid.rows
    val localLeft = (containerBounds.width - boardWidth) / 2f
    val localTop = (containerBounds.height - boardHeight) / 2f
    return BoardGeometry(
        bounds = Rect(containerBounds.left + localLeft, containerBounds.top + localTop, containerBounds.left + localLeft + boardWidth, containerBounds.top + localTop + boardHeight),
        cellSize = cellSize,
        localLeft = localLeft,
        localTop = localTop,
    )
}
