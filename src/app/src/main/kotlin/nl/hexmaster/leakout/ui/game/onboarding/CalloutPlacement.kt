package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** Where the callout card goes: its top-left corner and its pointer, null when it has none. */
data class CalloutPosition(val topLeft: Offset, val pointer: TipPointer?)

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
     * the window edges. When no side fits, the card goes in the window half away from the
     * spotlight's centre without a pointer.
     */
    fun place(window: Size, spotlight: Rect, card: Size, gutter: Float, gap: Float): CalloutPosition {
        val room = mapOf(
            Placement.Right to window.width - gutter - spotlight.right - gap,
            Placement.Left to spotlight.left - gap - gutter,
            Placement.Below to window.height - gutter - spotlight.bottom - gap,
            Placement.Above to spotlight.top - gap - gutter,
        )
        val fitting = Placement.entries.filter { side ->
            val needed = if (side == Placement.Right || side == Placement.Left) card.width else card.height
            room.getValue(side) >= needed
        }
        // maxBy keeps the first of equal values, so the entries order breaks ties.
        val side = fitting.maxByOrNull { room.getValue(it) } ?: return fallback(window, spotlight, card, gutter)
        val x = when (side) {
            Placement.Right -> spotlight.right + gap
            Placement.Left -> spotlight.left - gap - card.width
            else -> clamp(spotlight.center.x - card.width / 2, gutter, window.width - gutter - card.width)
        }
        val y = when (side) {
            Placement.Below -> spotlight.bottom + gap
            Placement.Above -> spotlight.top - gap - card.height
            else -> clamp(spotlight.center.y - card.height / 2, gutter, window.height - gutter - card.height)
        }
        val along = if (side == Placement.Right || side == Placement.Left) spotlight.center.y - y else spotlight.center.x - x
        return CalloutPosition(Offset(x, y), TipPointer(side.pointer, along))
    }

    private fun fallback(window: Size, spotlight: Rect, card: Size, gutter: Float): CalloutPosition {
        val x = if (spotlight.center.x < window.width / 2) window.width - gutter - card.width else gutter
        val y = clamp((window.height - card.height) / 2, gutter, window.height - gutter - card.height)
        return CalloutPosition(Offset(x.coerceAtLeast(gutter), y), pointer = null)
    }

    /** [value] in [min]..[max]; [min] wins when the card is larger than the room. */
    private fun clamp(value: Float, min: Float, max: Float): Float = value.coerceAtMost(max).coerceAtLeast(min)
}
