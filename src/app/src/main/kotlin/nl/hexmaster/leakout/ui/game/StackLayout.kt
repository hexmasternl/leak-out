package nl.hexmaster.leakout.ui.game

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class StackLayoutMetrics(
    val previewSlotSize: Dp,
    val nextSlotSize: Dp,
    val gap: Dp,
    val columnWidth: Dp,
) {
    fun totalHeight(visibleSize: Int): Dp =
        previewSlotSize * (visibleSize - 1) + nextSlotSize + gap * (visibleSize - 1)

    /** Distance between the tops of two neighbouring slots: how far a section falls per placement. */
    val pitch: Dp get() = previewSlotSize + gap
}

object StackLayout {
    private val MaxPreviewSlot = 40.dp
    private val MinPreviewSlot = 20.dp
    private val MaxGap = 6.dp
    private val MinGap = 2.dp
    private const val NextScale = 1.25f

    /** Absorbs float rounding, so a slot that touches the rail's top edge counts as inside. */
    private val Tolerance = 0.01.dp

    /** Slot sizes for which the bottom [visibleSize] slots fit [availableHeight]. */
    fun metrics(availableHeight: Dp, visibleSize: Int): StackLayoutMetrics {
        require(visibleSize in 1..10) { "visibleSize must be between 1 and 10" }
        val gaps = visibleSize - 1
        val desiredGap = MaxGap
        val desired = MaxPreviewSlot * (visibleSize - 1 + NextScale) + desiredGap * gaps
        val preview = if (desired <= availableHeight) {
            MaxPreviewSlot
        } else {
            ((availableHeight - MinGap * gaps) / (visibleSize - 1 + NextScale)).coerceAtLeast(MinPreviewSlot)
        }
        val next = preview * NextScale
        val remaining = (availableHeight - preview * (visibleSize - 1) - next).coerceAtLeast(0.dp)
        // With one visible slot no gap counts towards the height; the extra sections above it still get a full gap.
        val gap = if (gaps == 0) MaxGap else (remaining / gaps).coerceIn(MinGap, MaxGap)
        return StackLayoutMetrics(
            previewSlotSize = preview,
            nextSlotSize = next,
            gap = gap,
            columnWidth = next + 16.dp,
        )
    }

    /**
     * The top of every slot, indexed like `PipeStack.sections` (top first), measured from the top of a rail of
     * [railHeight]. The next slot sits on the bottom of the rail and each slot above it is one
     * [StackLayoutMetrics.pitch] higher, so a slot that does not fit gets a negative top and is clipped at the edge.
     */
    fun slotTops(metrics: StackLayoutMetrics, stackSize: Int, railHeight: Dp): List<Dp> {
        val nextTop = railHeight - metrics.nextSlotSize
        return List(stackSize) { index -> nextTop - metrics.pitch * (stackSize - 1 - index) }
    }

    /** Indices of the slots that lie entirely inside the rail. The bottom `visibleSize` slots are always among them. */
    fun fullyVisibleSlots(metrics: StackLayoutMetrics, stackSize: Int, railHeight: Dp): Set<Int> {
        val tops = slotTops(metrics, stackSize, railHeight)
        return tops.indices.filterTo(mutableSetOf()) { index -> tops[index] >= -Tolerance }
    }

    /**
     * Indices of the slots worth composing: every slot that reaches into the rail, plus the slot just above the edge
     * whose section can fall into view. Slots more than one pitch above the edge are not composed.
     */
    fun composedSlots(metrics: StackLayoutMetrics, stackSize: Int, railHeight: Dp): Set<Int> {
        val tops = slotTops(metrics, stackSize, railHeight)
        return tops.indices.filterTo(mutableSetOf()) { index -> tops[index] > -metrics.pitch - Tolerance }
    }
}
