package nl.hexmaster.leakout.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Button families from the design system: primary (Hydro Blue), secondary (Brass), danger (red), success (green). */
enum class CartoonButtonVariant(val face: Color, val shadow: Color, val label: Color) {
    Primary(LeakOutColors.PrimaryBase, LeakOutColors.PrimaryDark, LeakOutColors.SurfaceCard),
    Secondary(LeakOutColors.SecondaryBase, LeakOutColors.SecondaryDark, LeakOutColors.InkDark),
    Danger(LeakOutColors.ErrorRed, LeakOutColors.ErrorRedDark, LeakOutColors.SurfaceCard),
    Success(LeakOutColors.SuccessGreen, LeakOutColors.SuccessGreenDark, LeakOutColors.SurfaceCard),
}

/** The disabled look (design-system-slim 4.2), the same for every variant. */
object CartoonButtonDisabled {
    val Face: Color = LeakOutColors.DisabledFill
    val Shadow: Color = LeakOutColors.DisabledShadow
    val Label: Color = LeakOutColors.SurfaceCard
}

/**
 * The 3-layer cartoon recipe (design-system-slim 4.1/4.2) as plain numbers, so the press physics
 * can be unit tested without a device.
 */
object CartoonButtonMetrics {
    val Border: Dp = 3.dp
    val RestingShadow: Dp = 6.dp
    val PressedShadow: Dp = 2.dp
    val PressTravel: Dp = 4.dp
    val Shine: Dp = 3.dp
    val CornerRadius: Dp = 16.dp
    val MinTouchTarget: Dp = 48.dp

    /** How far the face sits below its resting position. */
    fun faceOffset(pressed: Boolean): Dp = if (pressed) PressTravel else 0.dp

    /** Visible depth of the bottom shadow under the face. */
    fun visibleShadow(pressed: Boolean): Dp = RestingShadow - faceOffset(pressed)
}

/**
 * Chunky cartoon button. The footprint never changes when pressed: the face slides down into the
 * space the shadow occupied, as in the CSS reference (translateY(4px), shadow 6px -> 2px).
 *
 * @param faceHeight height of the face; the total height adds [CartoonButtonMetrics.RestingShadow].
 * @param enabled when false the button uses [CartoonButtonDisabled], does not press and ignores taps.
 */
@Composable
fun CartoonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CartoonButtonVariant = CartoonButtonVariant.Primary,
    faceHeight: Dp = 56.dp,
    textStyle: TextStyle = LeakOutTextStyles.ButtonText,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState().value && enabled
    val faceColor = if (enabled) variant.face else CartoonButtonDisabled.Face
    val shadowColor = if (enabled) variant.shadow else CartoonButtonDisabled.Shadow
    val labelColor = if (enabled) variant.label else CartoonButtonDisabled.Label
    // design-system motion: press feedback is short and linear so it feels immediate.
    val faceOffset by animateDpAsState(
        targetValue = CartoonButtonMetrics.faceOffset(pressed),
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "cartoonButtonPress",
    )
    val face = faceHeight.coerceAtLeast(CartoonButtonMetrics.MinTouchTarget - CartoonButtonMetrics.RestingShadow)

    Box(
        modifier = modifier
            .heightIn(min = CartoonButtonMetrics.MinTouchTarget)
            .height(face + CartoonButtonMetrics.RestingShadow)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .drawWithCache {
                val border = CartoonButtonMetrics.Border.toPx()
                val shadowDepth = CartoonButtonMetrics.RestingShadow.toPx()
                val radius = CartoonButtonMetrics.CornerRadius.toPx()
                val shine = CartoonButtonMetrics.Shine.toPx()
                val faceSize = Size(size.width, size.height - shadowDepth)
                // Stroke is centred on the outline; inset by half so the full 3dp stays visible.
                val half = border / 2f
                val outline = RoundRect(
                    left = half, top = half,
                    right = faceSize.width - half, bottom = faceSize.height - half,
                    cornerRadius = CornerRadius(radius - half),
                )
                val facePath = Path().apply { addRoundRect(outline) }
                val stroke = Stroke(width = border)
                val corner = CornerRadius(radius)
                onDrawBehind {
                    drawRoundRect(
                        color = shadowColor,
                        topLeft = Offset(0f, shadowDepth),
                        size = faceSize,
                        cornerRadius = corner,
                    )
                    translate(top = faceOffset.toPx()) {
                        drawPath(facePath, faceColor)
                        clipPath(facePath) {
                            drawRect(
                                color = LeakOutColors.Shine,
                                topLeft = Offset(0f, border),
                                size = Size(faceSize.width, shine),
                            )
                        }
                        drawPath(facePath, LeakOutColors.InkDark, style = stroke)
                    }
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(face)
                .offset { IntOffset(0, faceOffset.roundToPx()) }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text.displayUppercase(),
                style = textStyle,
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(name = "Cartoon buttons", widthDp = 640, heightDp = 120, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun CartoonButtonPreview() {
    LeakOutTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            CartoonButtonVariant.entries.forEach { variant ->
                CartoonButton(
                    text = variant.name,
                    onClick = {},
                    variant = variant,
                    modifier = Modifier
                        .width(180.dp)
                        .padding(horizontal = 8.dp),
                )
            }
        }
    }
}
