package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import kotlin.math.cos
import kotlin.math.sin
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** Test-only semantics: how far the splash droplets have flown, 0 at the blob to 1 at rest. */
val SplashProgress = SemanticsPropertyKey<Float>("SplashProgress")
var SemanticsPropertyReceiver.splashProgress by SplashProgress

object LeakSplashSpec {
    const val DROPLETS = 6
    const val DURATION_MS = 600

    /** Where droplet [index] is at [progress], relative to the centre, in units of the box's half size. */
    fun dropletOffset(index: Int, progress: Float): Offset {
        val angle = Math.toRadians(30.0 + 360.0 / DROPLETS * index)
        val distance = 0.8f * progress.coerceIn(0f, 1f)
        return Offset((cos(angle) * distance).toFloat(), (sin(angle) * distance).toFloat())
    }
}

/**
 * The leak dialog's splash (design-system.md 5.6, 7.2): a `leak.500` blob with six droplets that fly
 * out over 600 ms once and rest at the edge. With [reducedMotion] it is the still sprite at once.
 */
@Composable
fun LeakSplash(modifier: Modifier = Modifier, reducedMotion: Boolean = rememberReducedMotion()) {
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, tween(LeakSplashSpec.DURATION_MS, easing = LinearOutSlowInEasing))
        }
    }
    Canvas(modifier.semantics { splashProgress = progress.value }) {
        val half = size.minDimension / 2
        drawCircle(LeakOutColors.Leak500, half * 0.42f)
        drawCircle(Color.White.copy(alpha = 0.35f), half * 0.12f, center + Offset(-half * 0.14f, -half * 0.14f))
        repeat(LeakSplashSpec.DROPLETS) { index ->
            val offset = LeakSplashSpec.dropletOffset(index, progress.value)
            val color = if (index % 2 == 0) LeakOutColors.Liquid500 else LeakOutColors.LiquidGlow
            drawCircle(color, half * 0.14f, center + offset * half)
        }
    }
}
