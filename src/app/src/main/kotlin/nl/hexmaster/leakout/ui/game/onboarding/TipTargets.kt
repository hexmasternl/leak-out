package nl.hexmaster.leakout.ui.game.onboarding

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.toSize
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.ui.game.PlayingFieldState

/**
 * The on-screen bounds of everything a tip can point at (design-system.md 5.9), in root
 * coordinates, as the stack panel and the playing field report them on every layout pass. Field
 * cells come from the [field]'s viewport, so they follow pan and zoom.
 */
@Stable
class TipTargets(val field: PlayingFieldState? = null) {
    /** The whole stack column. */
    var stackColumn: Rect? by mutableStateOf(null)
        private set

    /** The part of the stack panel that is inside the window, in window coordinates. */
    private var stackVisibleInWindow: Rect? by mutableStateOf(null)
    private var slotCount: Int by mutableStateOf(0)

    /** The slots the stack rail shows whole, before the window has its say; null until the rail is laid out. */
    private var railVisible: Set<Int>? by mutableStateOf(null)
    private val slotsInRoot = mutableStateMapOf<Int, Rect>()
    private val slotsInWindow = mutableStateMapOf<Int, Rect>()

    /** Called by the stack panel with its own coordinates. */
    fun registerStackPanel(coordinates: LayoutCoordinates) {
        stackColumn = coordinates.rectInRoot()
        stackVisibleInWindow = coordinates.boundsInWindow()
    }

    /**
     * Called by the stack rail with its number of slots and the ones it shows whole; the slots
     * above the rail's clipped top edge are not among them, and may not be composed at all.
     */
    fun registerRail(slots: Int, fullyVisible: Set<Int>) {
        if (slotCount != slots) {
            slotCount = slots
            slotsInRoot.keys.retainAll { it < slots }
            slotsInWindow.keys.retainAll { it < slots }
        }
        if (railVisible != fullyVisible) railVisible = fullyVisible
    }

    /** Called by stack slot [index] (0 is the top, the last one the next section). */
    fun registerSlot(index: Int, coordinates: LayoutCoordinates) {
        slotsInRoot[index] = coordinates.rectInRoot()
        slotsInWindow[index] = Rect(coordinates.positionInWindow(), coordinates.size.toSize())
    }

    fun slot(index: Int): Rect? = slotsInRoot[index]

    fun cell(position: Position): Rect? = field?.cellInRoot(position)

    fun fieldBounds(): Rect? = field?.boundsInRoot()

    /**
     * The indices of the stack slots the rail shows whole and whose whole rect also lies inside
     * the part of the stack panel that is in the window; null until the rail, the panel and the
     * slots it shows have been laid out.
     */
    val fullyVisibleSlots: Set<Int>?
        get() {
            val visible = stackVisibleInWindow ?: return null
            val rail = railVisible ?: return null
            if (slotCount == 0 || rail.any { it !in slotsInWindow }) return null
            return fullyVisible(visible, rail.associateWith { slotsInWindow.getValue(it) })
        }

    companion object {
        /** Half a pixel of slack, so rounding never counts a slot that fits exactly as cut off. */
        private const val SLACK = 0.5f

        /** The keys of [slots] whose rect lies within [visible]. Pure, so it is JVM-tested. */
        fun fullyVisible(visible: Rect, slots: Map<Int, Rect>): Set<Int> = slots.filterValues { slot ->
            slot.left >= visible.left - SLACK && slot.top >= visible.top - SLACK &&
                slot.right <= visible.right + SLACK && slot.bottom <= visible.bottom + SLACK
        }.keys

        private fun LayoutCoordinates.rectInRoot(): Rect = Rect(positionInRoot(), size.toSize())
    }
}

/** The targets of the game screen, for the stack panel and the field to report to; null outside it. */
val LocalTipTargets = staticCompositionLocalOf<TipTargets?> { null }

/** Translates a root rect into a container placed at [origin] in the root. */
fun Rect.relativeTo(origin: Offset): Rect = translate(-origin)
