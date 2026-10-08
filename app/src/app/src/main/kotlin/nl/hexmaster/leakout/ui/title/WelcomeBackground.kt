package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import nl.hexmaster.leakout.R

/**
 * Full-bleed welcome illustration (from `assets/welcome`, bundled at 1920x1080). Crop keeps the
 * aspect ratio and fills every landscape shape, trimming the edges instead of letterboxing.
 */
@Composable
fun WelcomeBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.welcome_background),
        // Decorative only; the title and buttons carry all meaning.
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

@Preview(name = "Background 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun WelcomeBackgroundPreview() {
    WelcomeBackground()
}
