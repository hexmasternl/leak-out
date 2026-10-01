package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.components.CartoonButtonMetrics
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object FastForwardTags {
    const val BUTTON = "fast_forward"
}

/** Size of the button, which is also its touch target. */
val FastForwardButtonSize = 56.dp

/**
 * The round fast forward button. Off, it is the brass ("touch me") fast forward button and pulses
 * gently so the player notices it. Once [active], it is the slow button: filled with liquid blue,
 * still, and a tap goes back to normal speed. While not [enabled] (during a replacement) it is
 * faded, does not pulse and ignores taps.
 */
@Composable
fun FastForwardButton(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val description = stringResource(if (active) R.string.fast_forward_normal_speed else R.string.fast_forward)
    val onState = stringResource(R.string.fast_forward_on)
    val scale = if (active || !enabled) 1f else pulseScale()
    val face = if (active) LeakOutColors.Liquid500 else LeakOutColors.Brass500
    val lip = if (active) LeakOutColors.Liquid700 else LeakOutColors.Brass700
    val iconTint = if (active) LeakOutColors.SurfaceCard else LeakOutColors.Brass900

    Box(
        modifier = modifier
            .size(FastForwardButtonSize)
            .testTag(FastForwardTags.BUTTON)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (active) stateDescription = onState
                if (!enabled) disabled()
                onClick { if (enabled) onClick(); enabled }
            }
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else DISABLED_ALPHA
            }
            .drawBehind {
                val border = CartoonButtonMetrics.Border.toPx()
                val lipDepth = 4.dp.toPx()
                val radius = size.minDimension / 2f - border / 2f
                // The lip is the darker circle showing below the face.
                drawCircle(lip, radius, center + Offset(0f, lipDepth / 2f))
                drawCircle(face, radius - lipDepth / 2f, center - Offset(0f, lipDepth / 4f))
                drawCircle(LeakOutColors.InkDark, radius, center, style = Stroke(border))
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (active) R.drawable.ic_slow else R.drawable.ic_fast_forward),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp),
        )
    }
}

private const val DISABLED_ALPHA = 0.45f

@Composable
private fun pulseScale(): Float {
    val transition = rememberInfiniteTransition(label = "fastForwardPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fastForwardPulseScale",
    )
    return scale
}

@Preview(name = "Fast forward", widthDp = 160, heightDp = 80, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun FastForwardButtonPreview() {
    LeakOutTheme {
        Row {
            FastForwardButton(active = false, onClick = {})
            FastForwardButton(active = true, onClick = {})
        }
    }
}
