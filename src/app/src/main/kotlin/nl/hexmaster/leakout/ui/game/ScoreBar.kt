package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/** The score bar's fixed size (design system 5.3). It never changes with the score. */
val ScoreBarWidth = 200.dp
val ScoreBarHeight = 20.dp
private val MarkerWidth = 2.dp

/** A sparkle being drawn on the score bar: at [fraction] along the bar, [frame] of its animation. */
data class ScoreBarSparkle(val fraction: Float, val frame: SparkleFrame)

/**
 * The score bar as drawn: a `surface.abyss` pill track with a `line.subtle` border, a
 * `brass.500 → brass.300` fill from the left to [fraction] (0..1), `line.strong` marker lines at
 * [markers] and [scoreText] centred over it with a `surface.abyss` shadow. [sparkles] are drawn on
 * top and may spill outside the bar.
 */
@Composable
fun ScoreBarTrack(
    fraction: Float,
    markers: List<Float>,
    scoreText: String,
    modifier: Modifier = Modifier,
    sparkles: List<ScoreBarSparkle> = emptyList(),
) {
    // 1 dp `surface.abyss` text shadow, offset down 1 dp and blurred 2 dp, so the number reads over brass and track.
    val shadow = with(LocalDensity.current) { Shadow(LeakOutColors.SurfaceAbyss, Offset(0f, 1.dp.toPx()), 2.dp.toPx()) }
    Box(modifier.size(ScoreBarWidth, ScoreBarHeight), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = CornerRadius(size.height / 2)
            val pill = Path().apply { addRoundRect(RoundRect(size.toRect(), radius)) }
            drawRoundRect(LeakOutColors.SurfaceAbyss, cornerRadius = radius)
            clipPath(pill) {
                val width = size.width * fraction.coerceIn(0f, 1f)
                if (width > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(LeakOutColors.Brass500, LeakOutColors.Brass300), endX = width),
                        size = Size(width, size.height),
                    )
                }
                val marker = MarkerWidth.toPx()
                for (m in markers) {
                    val x = size.width * m
                    drawRect(LeakOutColors.LineStrong, topLeft = Offset(x - marker / 2, 0f), size = Size(marker, size.height))
                }
            }
            val border = 1.dp.toPx()
            drawRoundRect(
                LeakOutColors.LineSubtle,
                topLeft = Offset(border / 2, border / 2),
                size = Size(size.width - border, size.height - border),
                cornerRadius = CornerRadius(size.height / 2 - border / 2),
                style = Stroke(border),
            )
        }
        Text(
            text = scoreText,
            style = LeakOutTextStyles.NumericMd.copy(shadow = shadow),
            color = LeakOutColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
        )
        if (sparkles.isNotEmpty()) {
            Canvas(Modifier.fillMaxSize()) {
                for (sparkle in sparkles) drawSparkle(Offset(size.width * sparkle.fraction, size.height / 2), sparkle.frame)
            }
        }
    }
}

private fun DrawScope.drawSparkle(center: Offset, frame: SparkleFrame) {
    if (frame.flashAlpha > 0f) {
        val r = HudSparkle.FLASH_RADIUS_DP.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                listOf(LeakOutColors.Brass300.copy(alpha = 0.8f * frame.flashAlpha), Color.Transparent),
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
    }
    val dot = HudSparkle.PARTICLE_RADIUS_DP.dp.toPx()
    for (p in frame.particles) {
        val color = if (p.glow) LeakOutColors.LiquidGlow else LeakOutColors.Brass300
        drawCircle(color.copy(alpha = p.alpha), radius = dot, center = center + Offset(p.dx.dp.toPx(), p.dy.dp.toPx()))
    }
}
