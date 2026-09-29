package nl.hexmaster.leakout.ui.title

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.components.CartoonButton
import nl.hexmaster.leakout.ui.components.CartoonButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Stable handles for UI tests, independent of copy and layout. */
object WelcomeScreenTags {
    const val SCREEN = "welcome_screen"
    const val TITLE = "welcome_title"
    const val PLAY = "welcome_play"
    const val SETTINGS = "welcome_settings"
    const val EXIT = "welcome_exit"
}

/**
 * Launch screen: full-bleed art, the centred logo and the Play / Settings / Exit row below it.
 * Stateless; the host decides what each button does.
 */
@Composable
fun WelcomeScreen(
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().testTag(WelcomeScreenTags.SCREEN)) {
        WelcomeBackground()
        // Art runs under the bars and cutout; interactive content stays inside the safe area.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val layout = remember(maxWidth, maxHeight) {
                WelcomeLayout.compute(maxWidth.value, maxHeight.value)
            }
            // Fixed like the logo: the button row is sized to fit, so font scaling must not break it.
            val buttonTextSize = with(LocalDensity.current) { BUTTON_TEXT_DP.dp.toSp() }
            val buttonTextStyle = LeakOutTextStyles.ButtonText.copy(fontSize = buttonTextSize)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LeakOutTitle(
                    text = stringResource(R.string.welcome_title),
                    sizeDp = layout.titleSizeDp,
                    modifier = Modifier.testTag(WelcomeScreenTags.TITLE),
                )
                Spacer(Modifier.height(layout.gapDp.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(layout.buttonSpacingDp.dp)) {
                    val buttonModifier = Modifier.width(layout.buttonWidthDp.dp)
                    CartoonButton(
                        text = stringResource(R.string.welcome_play),
                        onClick = onPlay,
                        variant = CartoonButtonVariant.Secondary,
                        faceHeight = layout.buttonFaceHeightDp.dp,
                        textStyle = buttonTextStyle,
                        modifier = buttonModifier.testTag(WelcomeScreenTags.PLAY),
                    )
                    CartoonButton(
                        text = stringResource(R.string.welcome_settings),
                        onClick = onSettings,
                        variant = CartoonButtonVariant.Primary,
                        faceHeight = layout.buttonFaceHeightDp.dp,
                        textStyle = buttonTextStyle,
                        modifier = buttonModifier.testTag(WelcomeScreenTags.SETTINGS),
                    )
                    CartoonButton(
                        text = stringResource(R.string.welcome_exit),
                        onClick = onExit,
                        variant = CartoonButtonVariant.Danger,
                        faceHeight = layout.buttonFaceHeightDp.dp,
                        textStyle = buttonTextStyle,
                        modifier = buttonModifier.testTag(WelcomeScreenTags.EXIT),
                    )
                }
            }
        }
    }
}

private const val BUTTON_TEXT_DP = 18f

@Preview(name = "Welcome 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun WelcomeScreenStandardPreview() {
    LeakOutTheme {
        WelcomeScreen(onPlay = {}, onSettings = {}, onExit = {})
    }
}

@Preview(name = "Welcome 640x320 (small)", widthDp = 640, heightDp = 320)
@Composable
private fun WelcomeScreenSmallPreview() {
    LeakOutTheme {
        WelcomeScreen(onPlay = {}, onSettings = {}, onExit = {})
    }
}
