package nl.hexmaster.leakout.ui.splash

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/**
 * Text filled with the liquid gradient and wrapped in a soft aqua glow: the logo treatment of
 * `docs/design-system.html` (`.hero .logo`). All lines are one [Text], so the gradient runs over
 * the whole block, lightest at the top of the first line and darkest at the bottom of the last.
 *
 * @param sizeDp glyph size in dp. It is converted to sp without the user's font scale because
 *   display sizes are fixed to protect layout (design system 3.3).
 */
@Composable
fun LiquidWordmark(
    text: String,
    sizeDp: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val style = remember(sizeDp, density) {
        with(density) {
            LeakOutTextStyles.DisplayHero.copy(
                fontSize = sizeDp.dp.toSp(),
                lineHeight = LINE_HEIGHT.em,
                letterSpacing = LETTER_SPACING.em,
                textAlign = TextAlign.Center,
                brush = LiquidGradient,
                // CSS reference: drop-shadow(0 6px 24px rgba(25,198,242,.35)).
                shadow = Shadow(
                    color = LeakOutColors.Liquid500.copy(alpha = GLOW_ALPHA),
                    offset = Offset(0f, GLOW_OFFSET_DP.dp.toPx()),
                    blurRadius = GLOW_BLUR_DP.dp.toPx(),
                ),
            )
        }
    }
    Text(text = text, style = style, modifier = modifier)
}

/** `liquid.glow 0% → liquid.300 35% → liquid.500 70% → liquid.700 100%`, top to bottom. */
private val LiquidGradient = Brush.verticalGradient(
    0f to LeakOutColors.LiquidGlow,
    0.35f to LeakOutColors.Liquid300,
    0.70f to LeakOutColors.Liquid500,
    1f to LeakOutColors.Liquid700,
)

private const val LINE_HEIGHT = 0.95f
private const val LETTER_SPACING = 0.01f
private const val GLOW_ALPHA = 0.35f
private const val GLOW_OFFSET_DP = 6f
private const val GLOW_BLUR_DP = 24f

@Preview(name = "Wordmark", widthDp = 400, heightDp = 260, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun LiquidWordmarkPreview() {
    LiquidWordmark(text = "LEAK\nOUT", sizeDp = 96f)
}
