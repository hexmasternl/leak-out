package nl.hexmaster.leakout.core.stack

import kotlin.random.Random
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.StackSettings

/**
 * The visible sections of one attempt, [next] at the bottom, and the [dealer] that draws new ones.
 * The dealer remembers every draw of the attempt, so it travels with the stack.
 */
data class PipeStack(val sections: List<PipeSection>, val dealer: Dealer) {
    init {
        require(sections.isNotEmpty()) { "Pipe stack must contain at least one section" }
    }

    val next: PipeSection get() = sections.last()
    val size: Int get() = sections.size

    /** Removes [next] and deals a new section on top. */
    fun afterPlacement(random: Random): PipeStack {
        val (section, nextDealer) = dealer.deal(random)
        return PipeStack(listOf(section) + sections.dropLast(1), nextDealer)
    }

    companion object {
        /** A stack of `visibleSize` sections, dealt one after the other by a fresh dealer. */
        fun create(settings: StackSettings, random: Random): PipeStack {
            var dealer = Dealer.initial(settings)
            val sections = List(settings.visibleSize) {
                val (section, next) = dealer.deal(random)
                dealer = next
                section
            }
            return PipeStack(sections, dealer)
        }
    }
}
