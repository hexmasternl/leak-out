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
}

object StackLayout {
    private val MaxPreviewSlot = 40.dp
    private val MinPreviewSlot = 20.dp
    private val MaxGap = 6.dp
    private val MinGap = 2.dp
    private const val NextScale = 1.25f

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
        val gap = if (gaps == 0) 0.dp else (remaining / gaps).coerceIn(MinGap, MaxGap)
        return StackLayoutMetrics(
            previewSlotSize = preview,
            nextSlotSize = next,
            gap = gap,
            columnWidth = next + 16.dp,
        )
    }
}
