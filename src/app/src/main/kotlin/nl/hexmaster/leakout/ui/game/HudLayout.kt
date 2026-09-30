package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fixed sizes of the HUD bar. The stack and board get the height below [HEIGHT]. */
object HudLayout {
    val HEIGHT: Dp = 48.dp
    val STAR_SIZE: Dp = 20.dp
    val STAR_SPACING: Dp = 4.dp
    val STAR_GLOW_RADIUS: Dp = 18.dp
    const val STAR_POP_SCALE: Float = 1.3f
}
