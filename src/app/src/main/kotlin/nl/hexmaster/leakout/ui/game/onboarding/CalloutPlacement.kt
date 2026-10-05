package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** Where the callout card goes: its top-left corner and its pointer, null when it has none. */
data class CalloutPosition(val topLeft: Offset, val pointer: TipPointer?)

/**
 * The parts of the window along each edge where the card's buttons cannot reliably take a tap:
 * the display cutout and the system gesture areas (the side Back gesture in landscape).
 */
data class UnsafeEdges(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f) {
    companion object {
        val None = UnsafeEdges()
    }
}

/**
 * The callout placement of design-system.md 5.9, pure so it is JVM-tested. All values are in the
 * same unit (pixels in the app, dp in the tests).
 */
object CalloutPlacement {
    private enum class Placement(val pointer: TipPointerSide) {
        Right(TipPointerSide.Left),
        Left(TipPointerSide.Right),
        Below(TipPointerSide.Top),
        Above(TipPointerSide.Bottom),
    }

    /**
     * The card of [card] size next to [spotlight] (the lit rect, padding included) in a [window]:
     * on the side with the most room that fits the card, [gap] away from the spotlight, ties going
     * right, left, below, above; centred on the spotlight along that side and kept [gutter] from
     * the window edges, and from the [unsafe] edges on top of that. When no side fits, the card
     * goes in the window half away from the spotlight's centre without a pointer.
     */
    fun place(window: Size, spotlight: Rect, card: Size, gutter: Float, gap: Float, unsafe: UnsafeEdges = UnsafeEdges.None): CalloutPosition {
        val safe = Rect(gutter + unsafe.left, gutter + unsafe.top, window.width - gutter - unsafe.right, window.height - gutter - unsafe.bottom)
        val room = mapOf(
            Placement.Right to safe.right - spotlight.right - gap,
            Placement.Left to spotlight.left - gap - safe.left,
            Placement.Below to safe.bottom - spotlight.bottom - gap,
            Placement.Above to spotlight.top - gap - safe.top,
        )
        val fitting = Placement.entries.filter { side ->
            val needed = if (side == Placement.Right || side == Placement.Left) card.width else card.height
            room.getValue(side) >= needed
        }
        // maxBy keeps the first of equal values, so the entries order breaks ties.
        val side = fitting.maxByOrNull { room.getValue(it) } ?: return fallback(safe, spotlight, card)
        val x = when (side) {
            Placement.Right -> spotlight.right + gap
            Placement.Left -> spotlight.left - gap - card.width
            else -> clamp(spotlight.center.x - card.width / 2, safe.left, safe.right - card.width)
        }
        val y = when (side) {
            Placement.Below -> spotlight.bottom + gap
            Placement.Above -> spotlight.top - gap - card.height
            else -> clamp(spotlight.center.y - card.height / 2, safe.top, safe.bottom - card.height)
        }
        val along = if (side == Placement.Right || side == Placement.Left) spotlight.center.y - y else spotlight.center.x - x
        return CalloutPosition(Offset(x, y), TipPointer(side.pointer, along))
    }

    private fun fallback(safe: Rect, spotlight: Rect, card: Size): CalloutPosition {
        val x = if (spotlight.center.x < safe.center.x) safe.right - card.width else safe.left
        val y = clamp(safe.center.y - card.height / 2, safe.top, safe.bottom - card.height)
        return CalloutPosition(Offset(x.coerceAtLeast(safe.left), y), pointer = null)
    }

    /** [value] in [min]..[max]; [min] wins when the card is larger than the room. */
    private fun clamp(value: Float, min: Float, max: Float): Float = value.coerceAtMost(max).coerceAtLeast(min)
}
