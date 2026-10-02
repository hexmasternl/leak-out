package nl.hexmaster.leakout.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the display on for as long as this composable is in the composition.
 *
 * It sets [android.view.View.keepScreenOn] on the Compose host view, which Android turns into the
 * window's `FLAG_KEEP_SCREEN_ON` while the window is visible: no wake lock, nothing held in the
 * background. Leaving the composition resets the flag, so only one screen at a time may call it;
 * a second caller on the same view would have its request cleared by the first one's dispose.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
