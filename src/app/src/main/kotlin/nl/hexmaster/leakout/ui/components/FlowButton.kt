package nl.hexmaster.leakout.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/**
 * The Deep Flow button variants of design-system.md 5.1: a vertical gradient fill from [fillTop] to
 * [fillBottom], the [label] colour, the [lip] under the face (none for Tertiary), an optional 1 dp
 * [border] (glass) and an optional [glow] colour.
 */
enum class FlowButtonVariant(
    val fillTop: Color,
    val fillBottom: Color,
    val label: Color,
    val lip: Color?,
    val border: Color? = null,
    val glow: Color? = null,
) {
    Primary(LeakOutColors.Brass300, LeakOutColors.Brass500, LeakOutColors.TextOnBrass, LeakOutColors.Brass700, glow = LeakOutColors.Brass500),
    Secondary(LeakOutColors.SurfaceOverlay, LeakOutColors.SurfaceOverlay, LeakOutColors.TextPrimary, LeakOutColors.SurfaceAbyss, border = LeakOutColors.LineStrong),
    Destructive(LeakOutColors.Leak500, LeakOutColors.Leak700, LeakOutColors.TextOnStatus, LeakOutColors.Leak700),
    Success(LeakOutColors.Success500, LeakOutColors.Success700, LeakOutColors.TextOnStatus, LeakOutColors.Success700, glow = LeakOutColors.Success500),

    /** A text button: no fill, no lip, no glow, always [FlowButtonMetrics.MinTouchTarget] high. */
    Tertiary(Color.Transparent, Color.Transparent, LeakOutColors.Liquid300, lip = null),
    ;

    val hasLip: Boolean get() = lip != null
}

/** Face heights of design-system.md 5.1; the lip comes below the face. */
enum class FlowButtonSize(val face: Dp) {
    Large(56.dp),
    Medium(48.dp),
    Small(40.dp),
}

/** The disabled look of 5.1, the same for every variant. */
object FlowButtonDisabled {
    val Fill: Color = LeakOutColors.SurfaceOverlay
    val Label: Color = LeakOutColors.TextMuted
    const val ALPHA = 0.6f
}

/** The lip and press physics of design-system.md 4.4 as plain numbers, so they are JVM-testable. */
object FlowButtonMetrics {
    val Lip: Dp = 4.dp
    val PressedLip: Dp = 2.dp
    val PressTravel: Dp = 2.dp
    val MinTouchTarget: Dp = 48.dp
    val CornerRadius: Dp = 16.dp
    val HorizontalPadding: Dp = 24.dp
    val GlowRadius: Dp = 24.dp
    const val GLOW_ALPHA = 0.45f
    const val PRESS_MS = 90

    /** How far the face sits below its resting position. */
    fun faceOffset(pressed: Boolean): Dp = if (pressed) PressTravel else 0.dp

    /** Visible depth of the lip under the face. */
    fun visibleLip(pressed: Boolean): Dp = Lip - faceOffset(pressed)

    /** The button's constant footprint height: face + lip, never under the touch target. */
    fun height(variant: FlowButtonVariant, size: FlowButtonSize): Dp {
        if (!variant.hasLip) return MinTouchTarget
        val total = size.face + Lip
        return if (total < MinTouchTarget) MinTouchTarget else total
    }
}

/**
 * Deep Flow button (design-system.md 5.1, 4.4). The footprint never changes when pressed: the face
 * moves down 2 dp into the lip, which shrinks from 4 dp to 2 dp over 90 ms. Disabled: the glass
 * fill, a muted label, no lip, no glow, 60 % opacity, and taps are ignored. A [trailing] text, such
 * as a count, puts the label at the start and the trailing text at the end. Two buttons sharing a
 * row can narrow the [horizontalPadding] so their labels fit.
 */
@Composable
fun FlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FlowButtonVariant = FlowButtonVariant.Primary,
    size: FlowButtonSize = FlowButtonSize.Large,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    trailing: String? = null,
    horizontalPadding: Dp = FlowButtonMetrics.HorizontalPadding,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState().value && enabled
    val faceOffset by animateDpAsState(
        targetValue = if (variant.hasLip) FlowButtonMetrics.faceOffset(pressed) else 0.dp,
        animationSpec = tween(durationMillis = FlowButtonMetrics.PRESS_MS, easing = LinearEasing),
        label = "flowButtonPress",
    )
    val height = FlowButtonMetrics.height(variant, size)
    val faceHeight = if (variant.hasLip) height - FlowButtonMetrics.Lip else height
    val label = if (enabled) variant.label else if (variant.hasLip) FlowButtonDisabled.Label else LeakOutColors.TextMuted

    Box(
        modifier = modifier
            .height(height)
            .then(if (enabled) Modifier else Modifier.alpha(FlowButtonDisabled.ALPHA))
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .drawWithCache {
                val radius = CornerRadius(FlowButtonMetrics.CornerRadius.toPx())
                val face = Size(this.size.width, faceHeight.toPx())
                val lipDepth = FlowButtonMetrics.Lip.toPx()
                val glowRadius = FlowButtonMetrics.GlowRadius.toPx()
                val fill = if (enabled) {
                    Brush.verticalGradient(listOf(variant.fillTop, variant.fillBottom), endY = face.height)
                } else {
                    Brush.verticalGradient(listOf(FlowButtonDisabled.Fill, FlowButtonDisabled.Fill))
                }
                onDrawBehind {
                    val offset = faceOffset.toPx()
                    if (!variant.hasLip) {
                        if (pressed) drawRoundRect(LeakOutColors.Liquid500.copy(alpha = 0.14f), cornerRadius = radius)
                        return@onDrawBehind
                    }
                    val glow = variant.glow
                    if (enabled && glow != null) {
                        // An approximation of the `0 0 24` glow: rings fading outwards around the face.
                        val alpha = if (pressed) FlowButtonMetrics.GLOW_ALPHA * 1.3f else FlowButtonMetrics.GLOW_ALPHA
                        val steps = 6
                        for (i in steps downTo 1) {
                            val spread = glowRadius * i / steps
                            drawRoundRect(
                                color = glow.copy(alpha = alpha / steps),
                                topLeft = Offset(-spread, -spread + offset),
                                size = Size(face.width + 2 * spread, face.height + 2 * spread),
                                cornerRadius = CornerRadius(radius.x + spread),
                            )
                        }
                    }
                    if (enabled) {
                        drawRoundRect(variant.lip!!, topLeft = Offset(0f, lipDepth), size = face, cornerRadius = radius)
                    }
                    drawRoundRect(fill, topLeft = Offset(0f, offset), size = face, cornerRadius = radius)
                    val border = variant.border
                    if (enabled && border != null) {
                        val stroke = 1.dp.toPx()
                        drawRoundRect(
                            color = border,
                            topLeft = Offset(stroke / 2, offset + stroke / 2),
                            size = Size(face.width - stroke, face.height - stroke),
                            cornerRadius = CornerRadius(radius.x - stroke / 2),
                            style = Stroke(stroke),
                        )
                    }
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(faceHeight)
                .offset { IntOffset(0, faceOffset.roundToPx()) }
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (trailing == null) {
                Text(
                    text = text,
                    style = LeakOutTextStyles.DisplaySm,
                    color = label,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = text,
                        style = LeakOutTextStyles.DisplaySm,
                        color = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = trailing, style = LeakOutTextStyles.NumericMd, color = label, maxLines = 1)
                }
            }
        }
    }
}

@Preview(name = "Flow buttons", widthDp = 640, heightDp = 420, backgroundColor = 0xFF122538, showBackground = true)
@Composable
private fun FlowButtonPreview() {
    LeakOutTheme {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowButtonVariant.entries.forEach { variant ->
                FlowButton(text = variant.name, onClick = {}, variant = variant, modifier = Modifier.width(240.dp))
            }
            FlowButton(text = "Disabled", onClick = {}, enabled = false, size = FlowButtonSize.Medium, modifier = Modifier.width(240.dp))
        }
    }
}
