package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.theme.LeakOutColors

@Composable
fun PipeSectionGlyph(section: PipeSection, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(LeakOutColors.SurfaceCard.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) { drawPipeSection(section, size.toRect()) }
    }
}

/** Draws [section] inside [rect]. The stack glyph and the playing field share this drawing. */
fun DrawScope.drawPipeSection(section: PipeSection, rect: Rect) {
    val strokeWidth = rect.minDimension * 0.22f
    val center = rect.center
    val pipeBrush = Brush.linearGradient(
        colors = listOf(LeakOutColors.SteelLight, LeakOutColors.SteelBase, LeakOutColors.SteelDark),
        start = rect.topLeft,
        end = rect.bottomRight,
    )
    fun end(side: Side): Offset = when (side) {
        Side.LEFT -> Offset(rect.left, center.y)
        Side.RIGHT -> Offset(rect.right, center.y)
        Side.TOP -> Offset(center.x, rect.top)
        Side.BOTTOM -> Offset(center.x, rect.bottom)
    }
    for (side in section.openSides) {
        drawLine(pipeBrush, center, end(side), strokeWidth, StrokeCap.Round)
    }
    drawCircle(LeakOutColors.SteelLight, radius = strokeWidth * 0.65f, center = center)
    drawCircle(LeakOutColors.SteelDark, radius = strokeWidth * 0.38f, center = center)
    if (section == PipeSection.RES) {
        drawRoundRect(
            color = LeakOutColors.ReservoirPurple.copy(alpha = 0.85f),
            topLeft = Offset(rect.left + rect.width * 0.28f, rect.top + rect.height * 0.25f),
            size = Size(rect.width * 0.44f, rect.height * 0.5f),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = strokeWidth * 0.45f),
        )
    }
}
