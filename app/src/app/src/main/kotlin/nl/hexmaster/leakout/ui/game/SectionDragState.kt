package nl.hexmaster.leakout.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import nl.hexmaster.leakout.core.level.PipeSection

class SectionDragState {
    var draggedSection: PipeSection? by mutableStateOf(null)
        private set
    var fingerPosition: Offset by mutableStateOf(Offset.Zero)
        private set
    var slotCenter: Offset by mutableStateOf(Offset.Zero)
        private set
    var isDragActive: Boolean by mutableStateOf(false)
        private set
    var isAnimatingBack: Boolean by mutableStateOf(false)
        private set
    var dropTarget: SectionDropTarget? by mutableStateOf(null)

    val isBusy: Boolean get() = isDragActive || isAnimatingBack

    private var dropOffset: Offset = Offset.Zero

    /** Where the section would land: the centre of the dragged copy. Null when no drag is active. */
    val dropPoint: Offset? get() = if (isDragActive) fingerPosition + dropOffset else null

    /** Starts a drag. [dropOffset] is where the dragged copy's centre sits relative to the finger. */
    fun beginDrag(section: PipeSection, position: Offset, slotCenter: Offset, dropOffset: Offset = Offset.Zero): Boolean {
        if (isBusy) return false
        draggedSection = section
        fingerPosition = position
        this.slotCenter = slotCenter
        this.dropOffset = dropOffset
        isDragActive = true
        return true
    }

    fun moveTo(position: Offset) {
        if (isDragActive) fingerPosition = position
    }

    fun endDrag(dropPoint: Offset): Boolean {
        val section = draggedSection ?: return false
        if (!isDragActive) return false
        val accepted = dropTarget?.tryPlace(section, dropPoint) == true
        if (accepted) {
            clear()
        } else {
            isDragActive = false
            isAnimatingBack = true
        }
        return accepted
    }

    fun cancelDrag() {
        if (draggedSection == null) return
        isDragActive = false
        isAnimatingBack = true
    }

    /** Ends any drag without trying to place it, for example when the game pauses. */
    fun cancel() {
        if (isDragActive) cancelDrag()
    }

    fun finishReturn() = clear()

    private fun clear() {
        draggedSection = null
        isDragActive = false
        isAnimatingBack = false
    }
}

fun LayoutCoordinates.positionInRoot(local: Offset = Offset.Zero): Offset = localToRoot(local)
