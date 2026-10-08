package nl.hexmaster.leakout.ui.title

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import nl.hexmaster.leakout.ui.components.displayUppercase
import nl.hexmaster.leakout.ui.splash.LiquidWordmark
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/**
 * The "LEAK OUT" logo in the design system's liquid gradient and aqua glow, the same treatment as
 * the splash wordmark (`docs/design-system.html`, `.hero .logo`).
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
    LiquidWordmark(
        text = text.displayUppercase(),
        sizeDp = sizeDp,
        modifier = modifier.semantics { heading() },
    )
}

@Preview(name = "Title", widthDp = 640, heightDp = 160, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun LeakOutTitlePreview() {
    LeakOutTheme {
        LeakOutTitle(text = "Leak Out", sizeDp = 96f)
    }
}
