package nl.hexmaster.leakout.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** Stable handles for UI tests, independent of copy and layout. */
object SplashScreenTags {
    const val SCREEN = "splash_screen"
    const val WORDMARK = "splash_wordmark"
}

/**
 * The in-app splash: the two-line "LEAK / OUT" liquid wordmark centred on `surface.deep`. It always
 * uses the Deep palette (the constants, not the theme), so it matches the system splash before it.
 * The background runs under the system bars; there is nothing interactive to keep out of them.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceDeep)
            .testTag(SplashScreenTags.SCREEN),
        contentAlignment = Alignment.Center,
    ) {
        LiquidWordmark(
            text = stringResource(R.string.splash_wordmark),
            sizeDp = SplashLayout.wordmarkSizeDp(maxWidth.value, maxHeight.value),
            modifier = Modifier.testTag(SplashScreenTags.WORDMARK),
        )
    }
}

@Preview(name = "Splash 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun SplashScreenStandardPreview() {
    SplashScreen()
}

@Preview(name = "Splash 640x320 (small)", widthDp = 640, heightDp = 320)
@Composable
private fun SplashScreenSmallPreview() {
    SplashScreen()
}

@Preview(name = "Splash 640x320, font scale 1.3", widthDp = 640, heightDp = 320, fontScale = 1.3f)
@Composable
private fun SplashScreenFontScalePreview() {
    SplashScreen()
}
