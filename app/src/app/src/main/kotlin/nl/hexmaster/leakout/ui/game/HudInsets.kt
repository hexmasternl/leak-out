package nl.hexmaster.leakout.ui.game

/** A display cutout's bounding rectangle in window pixels; [right] and [bottom] are exclusive. */
data class CutoutRect(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** How far, in pixels, the HUD has to stay clear of the left and right screen edges. */
data class HudInsets(val left: Int, val right: Int) {
    companion object {
        val None = HudInsets(0, 0)
    }
}

/**
 * The horizontal insets the HUD needs to keep its content clear of the display cutout.
 *
 * Only a cutout that reaches into the HUD's vertical band, from [hudTop] (inclusive) to [hudBottom]
 * (exclusive), counts: a camera hole beside the stack or the playing field leaves the HUD full width.
 * A cutout counts for the screen edge its centre is closer to, and pushes the HUD in on that side as
 * far as it reaches from that edge.
 */
fun hudHorizontalInsets(cutouts: List<CutoutRect>, hudTop: Int, hudBottom: Int, screenWidth: Int): HudInsets {
    var left = 0
    var right = 0
    for (rect in cutouts) {
        val overlapsBand = rect.top < hudBottom && rect.bottom > hudTop
        if (!overlapsBand || rect.right <= rect.left) continue
        if (rect.left + rect.right < screenWidth) {
            left = maxOf(left, rect.right.coerceIn(0, screenWidth))
        } else {
            right = maxOf(right, (screenWidth - rect.left).coerceIn(0, screenWidth))
        }
    }
    return HudInsets(left, right)
}
