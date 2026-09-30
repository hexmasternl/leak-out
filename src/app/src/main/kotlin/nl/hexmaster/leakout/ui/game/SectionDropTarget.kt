package nl.hexmaster.leakout.ui.game

import androidx.compose.ui.geometry.Offset
import nl.hexmaster.leakout.core.level.PipeSection

fun interface SectionDropTarget {
    fun tryPlace(section: PipeSection, position: Offset): Boolean
}
