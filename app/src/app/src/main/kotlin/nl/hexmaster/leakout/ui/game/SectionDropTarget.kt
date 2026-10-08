package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.geometry.Offset
import nl.hexmaster.leakout.core.level.PipeSection

/**
 * Where a dragged section can land. [position] is in game-screen (root) coordinates. [canPlace]
 * answers the hover question and [tryPlace] drops; both run the same rule so they always agree.
 */
interface SectionDropTarget {
    fun canPlace(section: PipeSection, position: Offset): Boolean

    fun tryPlace(section: PipeSection, position: Offset): Boolean
}
