package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope

/**
 * A tap as a finger on a real touchscreen makes it: a down, a few sub-pixel moves while the finger
 * rests and an up. `performClick` sends no moves at all, so it misses whatever a move cancels.
 */
internal fun TouchInjectionScope.fingerTap(at: Offset = center) {
    down(at)
    moveBy(Offset(0.4f, 0.3f), delayMillis = 8)
    moveBy(Offset(-0.3f, 0.2f), delayMillis = 8)
    moveBy(Offset(0.2f, -0.4f), delayMillis = 8)
    up()
}
