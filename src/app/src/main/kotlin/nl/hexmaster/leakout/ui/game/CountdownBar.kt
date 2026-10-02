package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** The countdown bar's track height (design system 5.3). */
val CountdownBarHeight = 8.dp

/** Blur of the fill's glow. The `G` glow's 24 dp would spill over the whole 48 dp HUD. */
private val CountdownGlowBlur = 8.dp

/**
 * The countdown bar as drawn: a `line.subtle` pill track filled from the left to [fraction] (0..1)
 * in [color] with a glow in the same colour.
 */
@Composable
fun CountdownBarTrack(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(CountdownBarHeight)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(LeakOutColors.LineSubtle, cornerRadius = radius)
        val width = size.width * fraction.coerceIn(0f, 1f)
        if (width <= 0f) return@Canvas
        drawGlow(Size(width, size.height), color.copy(alpha = 0.45f), CountdownGlowBlur.toPx())
        drawRoundRect(color, size = Size(width, size.height), cornerRadius = radius)
    }
}

/** A blurred pill of [area] at the top left, behind a fill, for the `G` glow. */
internal fun DrawScope.drawGlow(area: Size, color: Color, blur: Float, topLeft: Offset = Offset.Zero) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            maskFilter = android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        val r = area.height / 2
        canvas.nativeCanvas.drawRoundRect(
            topLeft.x, topLeft.y, topLeft.x + area.width, topLeft.y + area.height, r, r, paint,
        )
    }
}
