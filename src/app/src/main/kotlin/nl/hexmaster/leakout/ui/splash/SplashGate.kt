package nl.hexmaster.leakout.ui.splash

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * Shows [SplashScreen] for at least [SplashTiming.MIN_DURATION_MS], then cross-fades into
 * [content] for good. Both the "done" flag and the moment the splash appeared are saved, so a
 * configuration change or activity recreation never replays it, and one during the splash does
 * not restart the timer.
 *
 * @param reducedMotion when true (system "remove animations"), the switch is instant.
 * @param clock monotonic milliseconds; tests pass the Compose test clock.
 */
@Composable
fun SplashGate(
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
    clock: () -> Long = SystemClock::uptimeMillis,
    content: @Composable () -> Unit,
) {
    val shownAt = rememberSaveable { clock() }
    var done by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(done) {
        if (!done) {
            delay(SplashTiming.remainingDelayMs(clock() - shownAt))
            done = true
        }
    }

    Crossfade(
        targetState = done,
        modifier = modifier,
        animationSpec = if (reducedMotion) snap() else tween(SplashTiming.CROSSFADE_MS, easing = FastOutSlowInEasing),
        label = "splash",
    ) { isDone ->
        if (isDone) content() else SplashScreen()
    }
}

/** True when the system animator duration scale is 0 ("Remove animations" / animations off). */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
