package nl.hexmaster.leakout.ui.title

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nl.hexmaster.leakout.ui.settings.AppInfo
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/** Text and timing of the version label the welcome screen shows once when the app starts. */
object StartupVersion {
    /** How long the label stays fully visible before it fades. */
    const val HOLD_MS = 5_000L

    /** The fade out. */
    const val FADE_MS = 300

    /** The fade out with the system "remove animations" setting on (design system 7.4). */
    const val REDUCED_FADE_MS = 160

    /** "v<versionName> (<versionCode>)", a compact form of the About panel's version line. */
    fun format(info: AppInfo): String = "v${info.versionName} (${info.versionCode})"
}

// The fade keeps its own length when the system scales animations to 0: 7.4 asks for 160 ms then.
private object UnscaledMotion : MotionDurationScale {
    override val scaleFactor: Float = 1f
}

/**
 * The installed version, fully visible for [StartupVersion.HOLD_MS] and then faded out over
 * [StartupVersion.FADE_MS] ([StartupVersion.REDUCED_FADE_MS] with reduced motion), after which it
 * leaves the composition. [onShown] is called as soon as it is first composed. Decorative: it has
 * no click handling, so taps fall through.
 */
@Composable
fun StartupVersionLabel(
    text: String,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    var visible by remember { mutableStateOf(true) }
    if (!visible) return
    val alpha = remember { Animatable(1f) }
    val shown by rememberUpdatedState(onShown)
    LaunchedEffect(Unit) {
        shown()
        delay(StartupVersion.HOLD_MS)
        val fadeMs = if (reducedMotion) StartupVersion.REDUCED_FADE_MS else StartupVersion.FADE_MS
        withContext(UnscaledMotion) {
            alpha.animateTo(0f, tween(fadeMs, easing = LinearOutSlowInEasing))
        }
        visible = false
    }
    Text(
        text = text,
        style = LeakOutTextStyles.Caption,
        color = LeakOutColors.TextSecondary,
        modifier = modifier
            .testTag(WelcomeScreenTags.VERSION)
            .graphicsLayer { this.alpha = alpha.value },
    )
}
