package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/**
 * The "LEAK OUT" logo: brass fill, thick ink outline and a hard ink drop shadow, drawn as three
 * stacked copies of the same text so the outline sits outside the glyphs.
 *
 * @param sizeDp glyph size in dp. It is converted to sp without the user's font scale because
 *   the logo is sized to fit the screen (design system: display sizes are fixed to protect layout).
 */
@Composable
fun LeakOutTitle(
    text: String,
    sizeDp: Float,
    modifier: Modifier = Modifier,
) {
    val fontSize = with(LocalDensity.current) { sizeDp.dp.toSp() }
    val base = LeakOutTextStyles.TitleHero.copy(fontSize = fontSize, textAlign = TextAlign.Center)
    val strokePx = with(LocalDensity.current) { (sizeDp * OUTLINE_EM).dp.toPx() }
    val outline = remember(strokePx) { Stroke(width = strokePx, join = StrokeJoin.Round) }
    val fill = remember {
        Brush.verticalGradient(
            listOf(LeakOutColors.SecondaryLight, LeakOutColors.SecondaryBase, LeakOutColors.SecondaryDark),
        )
    }
    val shadowOffset = (sizeDp * SHADOW_EM).dp
    val label = text.uppercase()

    Box(
        modifier = modifier.semantics(mergeDescendants = true) { heading() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = base.copy(color = LeakOutColors.InkDark, drawStyle = outline),
            modifier = Modifier
                .offset(y = shadowOffset)
                .clearAndSetSemantics { },
        )
        Text(
            text = label,
            style = base.copy(color = LeakOutColors.InkDark, drawStyle = outline),
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(text = label, style = base.copy(brush = fill))
    }
}

// Outline and shadow scale with the glyphs so small and large titles look alike.
private const val OUTLINE_EM = 0.12f
private const val SHADOW_EM = 0.07f

@Preview(name = "Title", widthDp = 640, heightDp = 160, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun LeakOutTitlePreview() {
    LeakOutTheme {
        LeakOutTitle(text = "Leak Out", sizeDp = 96f)
    }
}
