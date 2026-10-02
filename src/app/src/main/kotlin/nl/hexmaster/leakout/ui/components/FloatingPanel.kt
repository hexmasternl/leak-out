package nl.hexmaster.leakout.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** Sizes of the level 3 dialog panel, from design-system.md 4.3 and 5.6. */
object FloatingPanelMetrics {
    val Gutter: Dp = 16.dp
    val Padding: Dp = 24.dp
    val MaxWidth: Dp = 360.dp
    val ButtonSpacing: Dp = 12.dp
    val CornerRadius: Dp = 24.dp
    val ShadowElevation: Dp = 24.dp
}

/**
 * How a dialog enters (design-system.md 7.2, 7.4): the scrim and panel fade in over `motion.quick`;
 * the panel also scales from [initialScale] to 1 with [scaleSpec], unless motion is reduced, when
 * there is no scale.
 */
data class DialogEntry(
    val initialScale: Float,
    val scaleSpec: AnimationSpec<Float>?,
    val fadeMs: Int,
) {
    companion object {
        const val FADE_MS = 160
        const val INITIAL_SCALE = 0.92f

        /** `motion.pop`: spring, stiffness medium-low, damping 0.6. */
        val PopSpring: AnimationSpec<Float> = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

        fun select(reducedMotion: Boolean): DialogEntry = when {
            reducedMotion -> DialogEntry(initialScale = 1f, scaleSpec = null, fadeMs = FADE_MS)
            else -> DialogEntry(initialScale = INITIAL_SCALE, scaleSpec = PopSpring, fadeMs = FADE_MS)
        }
    }
}

/** Test-only semantics: the panel's current entry scale. */
val PanelScale = SemanticsPropertyKey<Float>("PanelScale")
var SemanticsPropertyReceiver.panelScale by PanelScale

/**
 * Level 3 floating panel (design-system.md 4.3, 5.6): `radius.lg`, `surface.base`, a 1 dp
 * `line.highlight` top edge, no outline, at most [maxWidth] wide.
 */
@Composable
fun FloatingPanel(
    modifier: Modifier = Modifier,
    maxWidth: Dp = FloatingPanelMetrics.MaxWidth,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(FloatingPanelMetrics.CornerRadius)
    val shadow = Color.Black.copy(alpha = 0.6f)
    Box(
        modifier
            .widthIn(max = maxWidth)
            .shadow(FloatingPanelMetrics.ShadowElevation, shape, ambientColor = shadow, spotColor = shadow)
            .background(LeakOutColors.SurfaceBase, shape)
            .drawWithContent {
                drawContent()
                val inset = FloatingPanelMetrics.CornerRadius.toPx()
                val stroke = 1.dp.toPx()
                drawLine(LeakOutColors.LineHighlight, Offset(inset, stroke / 2), Offset(size.width - inset, stroke / 2), stroke)
            },
    ) { content() }
}
