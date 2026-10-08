package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** Test-only semantics: how full the countdown bar is, 0..1. TalkBack does not read custom keys. */
val CountdownFraction = SemanticsPropertyKey<Float>("CountdownFraction")
var SemanticsPropertyReceiver.countdownFraction by CountdownFraction

/** Test-only semantics: the countdown bar's warning level. */
val CountdownWarning = SemanticsPropertyKey<HudModel.Warning>("CountdownWarning")
var SemanticsPropertyReceiver.countdownWarning by CountdownWarning

/** The fill colour for each warning level (design system 5.3). */
fun HudModel.Warning.color(): Color = when (this) {
    HudModel.Warning.Calm -> LeakOutColors.Liquid500
    HudModel.Warning.Brass -> LeakOutColors.Brass500
    HudModel.Warning.Red -> LeakOutColors.Leak500
}

/**
 * The countdown bar while the flow waits to start: [remainingMs] over [initialDelayMs], coloured by
 * [HudModel.countdownWarning]. [remainingMs] is null once the liquid flows; the bar then fades out
 * over `motion.quick` (at once under reduced motion) and is no longer composed or read, while the
 * caller keeps its slot so nothing moves. A level without a start delay never shows it.
 */
@Composable
fun CountdownBar(
    remainingMs: Long?,
    initialDelayMs: Long,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val visible = remainingMs != null && initialDelayMs > 0L
    val alpha = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) {
        when {
            visible -> alpha.snapTo(1f)
            reducedMotion -> alpha.snapTo(0f)
            else -> alpha.animateTo(0f, tween(LeakOutMotionQuickMs, easing = LinearOutSlowInEasing))
        }
    }
    // The last countdown seen, so the bar fades out as it was when the liquid started.
    val last = remember { longArrayOf(remainingMs ?: 0L) }
    if (remainingMs != null) last[0] = remainingMs
    if (!visible && alpha.value <= 0f) return

    val fraction = HudModel.countdownFraction(last[0], initialDelayMs)
    val warning = HudModel.countdownWarning(last[0])
    val seconds = HudModel.countdownSeconds(last[0])
    val description = pluralStringResource(R.plurals.hud_countdown_description, seconds, seconds)
    CountdownBarTrack(
        fraction = fraction,
        color = warning.color(),
        modifier = modifier
            .testTag(HudTags.COUNTDOWN)
            .clearAndSetSemantics {
                if (visible) contentDescription = description
                countdownFraction = fraction
                countdownWarning = warning
            }
            .alpha(alpha.value),
    )
}

/** `motion.quick` (design system 7.1). */
private const val LeakOutMotionQuickMs = 160

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
