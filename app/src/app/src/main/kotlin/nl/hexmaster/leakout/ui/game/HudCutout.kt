package nl.hexmaster.leakout.ui.game

import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat

/** Replaces the device's display cutout rectangles, for tests on devices without a cutout. */
internal val LocalCutoutRects = compositionLocalOf<List<CutoutRect>?> { null }

/** Replaces the device's rounded screen corners, for tests on devices with square corners. */
internal val LocalScreenCorners = compositionLocalOf<List<ScreenCorner>?> { null }

/** The HUD content's padding from the panel's left and right edges when the screen corners are square. */
val HudContentStart: Dp = 16.dp
val HudContentEnd: Dp = 4.dp

/** How far below the HUD's top edge its content starts; the rounded corners are measured there. */
private val HudContentTop: Dp = 8.dp

/** Extra room between the curve of a rounded screen corner and the HUD content. */
private val HudCornerMargin: Dp = 8.dp

/** Where the HUD panel sits and how far its content keeps from the panel's edges. */
@Immutable
data class HudEdges(
    val panelLeft: Dp = 0.dp,
    val panelRight: Dp = 0.dp,
    val contentLeft: Dp = HudContentStart,
    val contentRight: Dp = HudContentEnd,
)

/**
 * The HUD's edges on this screen. The panel only keeps clear of a display cutout on a side where the
 * cutout reaches into the HUD's band, so a camera hole beside the stack or the playing field leaves
 * the HUD full width. The content keeps inside the display's rounded corners, so nothing falls off
 * the screen where the panel runs into a curved corner.
 */
@Composable
internal fun rememberHudEdges(): HudEdges {
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
    val contentTop = hudTop + with(density) { HudContentTop.roundToPx() }
    val screenWidth = view.rootView.width
    val rects = LocalCutoutRects.current ?: remember(view, sides, screenWidth) { deviceCutoutRects(view) }
    val corners = LocalScreenCorners.current ?: remember(view, sides, screenWidth) { deviceScreenCorners(view) }
    return remember(rects, corners, hudTop, hudBottom, contentTop, screenWidth, density) {
        val panel = hudHorizontalInsets(rects, hudTop, hudBottom, screenWidth)
        val curve = hudCornerInsets(corners, contentTop, screenWidth)
        with(density) {
            val margin = HudCornerMargin.roundToPx()
            fun content(base: Dp, curveReach: Int, panelInset: Int): Dp {
                val reach = curveReach - panelInset
                return if (reach <= 0) base else maxOf(base, (reach + margin).toDp())
            }
            HudEdges(
                panelLeft = panel.left.toDp(),
                panelRight = panel.right.toDp(),
                contentLeft = content(HudContentStart, curve.left, panel.left),
                contentRight = content(HudContentEnd, curve.right, panel.right),
            )
        }
    }
}

/** Moves the HUD panel clear of a display cutout in its band, see [rememberHudEdges]. */
internal fun Modifier.hudPanelPadding(edges: HudEdges): Modifier =
    absolutePadding(left = edges.panelLeft, right = edges.panelRight)

private fun deviceCutoutRects(view: View): List<CutoutRect> =
    ViewCompat.getRootWindowInsets(view)?.displayCutout?.boundingRects.orEmpty()
        .map { CutoutRect(it.left, it.top, it.right, it.bottom) }

private fun deviceScreenCorners(view: View): List<ScreenCorner> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
    val insets = view.rootWindowInsets ?: return emptyList()
    return listOfNotNull(
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.let {
            ScreenCorner(it.radius, it.center.x, it.center.y, onLeft = true)
        },
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.let {
            ScreenCorner(it.radius, it.center.x, it.center.y, onLeft = false)
        },
    )
}
