package nl.hexmaster.leakout.core.stack

import kotlin.random.Random
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.StackSettings

/**
 * The sections of one attempt, [next] at the bottom, and the [dealer] that draws new ones.
 * The dealer remembers every draw of the attempt, so it travels with the stack.
 * [placements] counts the sections taken off the bottom; it gives every section a stable [idAt].
 */
data class PipeStack(val sections: List<PipeSection>, val dealer: Dealer, val placements: Int = 0) {
    init {
        require(sections.isNotEmpty()) { "Pipe stack must contain at least one section" }
    }

    val next: PipeSection get() = sections.last()
    val size: Int get() = sections.size

    /**
     * The draw number of the section at [index]: the next section of a new stack is 0, the one above it 1, and so on.
     * A section keeps its id while it moves down, so the UI can follow it from slot to slot.
     */
    fun idAt(index: Int): Int {
        require(index in sections.indices) { "Index $index is outside the stack" }
        return placements + (size - 1 - index)
    }

    /** Removes [next] and deals a new section on top. */
    fun afterPlacement(random: Random): PipeStack {
        val (section, nextDealer) = dealer.deal(random)
        return PipeStack(listOf(section) + sections.dropLast(1), nextDealer, placements + 1)
    }

    companion object {
        /** Sections the stack holds above the level's visible size. */
        const val EXTRA_SECTIONS: Int = 10

        /** A stack of `visibleSize` + [EXTRA_SECTIONS] sections, dealt one after the other by a fresh dealer. */
        fun create(settings: StackSettings, random: Random): PipeStack {
            var dealer = Dealer.initial(settings)
            val sections = List(settings.visibleSize + EXTRA_SECTIONS) {
                val (section, next) = dealer.deal(random)
                dealer = next
                section
            }
            return PipeStack(sections, dealer)
        }
    }
}
