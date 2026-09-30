package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
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
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.22f
            val center = Offset(size.width / 2f, size.height / 2f)
            val pipeBrush = Brush.linearGradient(
                colors = listOf(LeakOutColors.SteelLight, LeakOutColors.SteelBase, LeakOutColors.SteelDark),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            fun end(side: Side): Offset = when (side) {
                Side.LEFT -> Offset(0f, center.y)
                Side.RIGHT -> Offset(size.width, center.y)
                Side.TOP -> Offset(center.x, 0f)
                Side.BOTTOM -> Offset(center.x, size.height)
            }
            for (side in section.openSides) {
                drawLine(pipeBrush, center, end(side), strokeWidth, StrokeCap.Round)
            }
            drawCircle(LeakOutColors.SteelLight, radius = strokeWidth * 0.65f, center = center)
            drawCircle(LeakOutColors.SteelDark, radius = strokeWidth * 0.38f, center = center)
            if (section == PipeSection.RES) {
                drawRoundRect(
                    color = LeakOutColors.ReservoirPurple.copy(alpha = 0.85f),
                    topLeft = Offset(size.width * 0.28f, size.height * 0.25f),
                    size = Size(size.width * 0.44f, size.height * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    style = Stroke(width = strokeWidth * 0.45f),
                )
            }
        }
    }
}
