package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/** The sizes of the tip overlay of design-system.md 5.9. */
object TipOverlayMetrics {
    /** How far the spotlight reaches past its target on every side (`space.2`). */
    val SpotlightPadding: Dp = 8.dp

    /** Spotlight corners (`radius.md`). */
    val SpotlightRadius: Dp = 16.dp
    val RingWidth: Dp = 2.dp
    val RingGlowBlur: Dp = 24.dp
    const val RING_GLOW_ALPHA = 0.45f
    const val RING_GLOW_PULSE_ALPHA = 0.70f
    const val RING_GLOW_REDUCED_ALPHA = 0.60f

    val CardMaxWidth: Dp = 296.dp
    val CardPadding: Dp = 16.dp
    val CardRadius: Dp = 24.dp
    val CardShadow: Dp = 24.dp

    /** Gap between the spotlight and the card (`space.3`). */
    val CardGap: Dp = 12.dp

    /** Distance the card keeps from the window edges. */
    val ScreenGutter: Dp = 16.dp
    val PointerWidth: Dp = 16.dp
    val PointerDepth: Dp = 8.dp
    val ButtonSpacing: Dp = 12.dp

    const val GHOST_ALPHA = 0.5f
}

/** The edge of the callout card that faces its target, where the pointer sits. */
enum class TipPointerSide { Left, Right, Top, Bottom }

/** A pointer on the card's [side], centred [along] pixels from the card's left (top/bottom) or top (left/right) edge. */
data class TipPointer(val side: TipPointerSide, val along: Float)

/**
 * The `surface.scrim` over the whole area with a rounded cut-out around every target in
 * [spotlights] (in this composable's pixels), each with its `liquid.glow` ring at [glowAlpha].
 * Cut-outs that overlap merge, so neighbouring mandatory cells read as one lit area.
 */
@Composable
fun TipSpotlightScrim(spotlights: List<Rect>, modifier: Modifier = Modifier, glowAlpha: Float = TipOverlayMetrics.RING_GLOW_ALPHA) {
    Canvas(modifier.fillMaxSize()) {
        val holes = spotlightShapes(spotlights)
        val scrim = holes.fold(Path().apply { addRect(Rect(Offset.Zero, size)) }) { acc, hole ->
            Path.combine(PathOperation.Difference, acc, Path().apply { addRoundRect(hole) })
        }
        drawPath(scrim, LeakOutColors.Scrim)
        for (hole in holes) drawRing(hole, glowAlpha)
    }
}

private fun DrawScope.spotlightShapes(spotlights: List<Rect>): List<RoundRect> {
    val padding = TipOverlayMetrics.SpotlightPadding.toPx()
    val radius = CornerRadius(TipOverlayMetrics.SpotlightRadius.toPx())
    return spotlights.map { RoundRect(it.inflate(padding), radius) }
}

/** The 2 dp ring and its blurred `G` glow around one cut-out. */
private fun DrawScope.drawRing(hole: RoundRect, glowAlpha: Float) {
    val ring = TipOverlayMetrics.RingWidth.toPx()
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = LeakOutColors.LiquidGlow.copy(alpha = glowAlpha).toArgb()
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = ring * 3
            maskFilter = android.graphics.BlurMaskFilter(TipOverlayMetrics.RingGlowBlur.toPx() / 2, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawRoundRect(hole.left, hole.top, hole.right, hole.bottom, hole.topLeftCornerRadius.x, hole.topLeftCornerRadius.y, paint)
    }
    drawPath(Path().apply { addRoundRect(hole) }, LeakOutColors.LiquidGlow, style = Stroke(ring))
}

/**
 * The callout card of 5.9: a level 3 `surface.base` panel, max 296 dp wide, with the [eyebrow]
 * (step counter or "TIP"), [title], [text] and the [buttons] row, and an optional [pointer]
 * towards the target drawn just outside the card edge.
 */
@Composable
fun TipCallout(
    eyebrow: String,
    eyebrowColor: Color,
    title: String,
    text: String,
    pointer: TipPointer?,
    modifier: Modifier = Modifier,
    eyebrowDescription: String? = null,
    textDescription: String? = null,
    buttons: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(TipOverlayMetrics.CardRadius)
    val shadow = Color.Black.copy(alpha = 0.6f)
    Column(
        modifier
            .widthIn(max = TipOverlayMetrics.CardMaxWidth)
            .drawBehind { pointer?.let { drawPointer(it) } }
            .shadow(TipOverlayMetrics.CardShadow, shape, ambientColor = shadow, spotColor = shadow)
            .background(LeakOutColors.SurfaceBase, shape)
            .drawWithContent {
                drawContent()
                val inset = TipOverlayMetrics.CardRadius.toPx()
                val stroke = 1.dp.toPx()
                drawLine(LeakOutColors.LineHighlight, Offset(inset, stroke / 2), Offset(size.width - inset, stroke / 2), stroke)
            }
            .padding(TipOverlayMetrics.CardPadding),
    ) {
        Text(
            eyebrow.uppercase(),
            style = LeakOutTextStyles.DisplayXs,
            color = eyebrowColor,
            modifier = eyebrowDescription?.let { Modifier.semantics { contentDescription = it } } ?: Modifier,
        )
        Spacer(Modifier.height(4.dp))
        Text(title, style = LeakOutTextStyles.DisplayMd, color = LeakOutColors.TextPrimary, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = LeakOutTextStyles.BodyMd,
            color = LeakOutColors.TextSecondary,
            modifier = textDescription?.let { Modifier.semantics { contentDescription = it } } ?: Modifier,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TipOverlayMetrics.ButtonSpacing, androidx.compose.ui.Alignment.End),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            content = buttons,
        )
    }
}

/** The 16 × 8 dp triangle on the card edge facing the target, kept clear of the rounded corners. */
private fun DrawScope.drawPointer(pointer: TipPointer) {
    val half = TipOverlayMetrics.PointerWidth.toPx() / 2
    val depth = TipOverlayMetrics.PointerDepth.toPx()
    val corner = TipOverlayMetrics.CardRadius.toPx()
    val length = if (pointer.side == TipPointerSide.Left || pointer.side == TipPointerSide.Right) size.height else size.width
    val at = pointer.along.coerceIn(corner + half, (length - corner - half).coerceAtLeast(corner + half))
    val path = Path().apply {
        when (pointer.side) {
            TipPointerSide.Left -> { moveTo(0f, at - half); lineTo(-depth, at); lineTo(0f, at + half) }
            TipPointerSide.Right -> { moveTo(size.width, at - half); lineTo(size.width + depth, at); lineTo(size.width, at + half) }
            TipPointerSide.Top -> { moveTo(at - half, 0f); lineTo(at, -depth); lineTo(at + half, 0f) }
            TipPointerSide.Bottom -> { moveTo(at - half, size.height); lineTo(at, size.height + depth); lineTo(at + half, size.height) }
        }
        close()
    }
    drawPath(path, LeakOutColors.SurfaceBase)
}

/** The reduced-motion drag hint: a 2 dp `liquid.glow` arrow from [from] to [to], in pixels. */
fun DrawScope.drawHintArrow(from: Offset, to: Offset) {
    val stroke = 2.dp.toPx()
    val head = 10.dp.toPx()
    drawLine(LeakOutColors.LiquidGlow, from, to, stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    val direction = (to - from).let { it / it.getDistance().coerceAtLeast(1f) }
    val normal = Offset(-direction.y, direction.x)
    val base = to - direction * head
    drawLine(LeakOutColors.LiquidGlow, to, base + normal * (head / 2), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(LeakOutColors.LiquidGlow, to, base - normal * (head / 2), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
}
