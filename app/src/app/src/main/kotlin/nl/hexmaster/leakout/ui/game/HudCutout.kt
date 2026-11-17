package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

/** Replaces the device's display cutout rectangles, for tests on devices without a cutout. */
internal val LocalCutoutRects = compositionLocalOf<List<CutoutRect>?> { null }

/**
 * Pads the HUD clear of the display cutout only on a side where the cutout reaches into the HUD's
 * vertical band, so a camera hole beside the stack or the playing field leaves the HUD full width.
 */
@Composable
internal fun Modifier.hudCutoutPadding(): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val view = LocalView.current
    // Reading the cutout insets recomposes when they change, also when the device turns to the other
    // landscape side, which is no configuration change.
    val cutout = WindowInsets.displayCutout
    val sides = listOf(
        cutout.getLeft(density, layoutDirection),
        cutout.getTop(density),
        cutout.getRight(density, layoutDirection),
        cutout.getBottom(density),
    )
    val hudTop = WindowInsets.safeDrawing.getTop(density)
    val hudBottom = hudTop + with(density) { HudLayout.HEIGHT.roundToPx() }
    val screenWidth = view.rootView.width
    val override = LocalCutoutRects.current
    val rects = override ?: remember(view, sides, screenWidth) { deviceCutoutRects(view) }
    val insets = remember(rects, hudTop, hudBottom, screenWidth) {
        hudHorizontalInsets(rects, hudTop, hudBottom, screenWidth)
    }
    return with(density) { absolutePadding(left = insets.left.toDp(), right = insets.right.toDp()) }
}

private fun deviceCutoutRects(view: android.view.View): List<CutoutRect> =
    ViewCompat.getRootWindowInsets(view)?.displayCutout?.boundingRects.orEmpty()
        .map { CutoutRect(it.left, it.top, it.right, it.bottom) }
