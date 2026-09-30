package nl.hexmaster.leakout.core.stack

import kotlin.random.Random
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.StackSettings

data class PipeStack(val sections: List<PipeSection>) {
    init {
        require(sections.isNotEmpty()) { "Pipe stack must contain at least one section" }
    }

    val next: PipeSection get() = sections.last()
    val size: Int get() = sections.size

    fun afterPlacement(pool: WeightedPool, random: Random): PipeStack =
        PipeStack(listOf(pool.draw(random)) + sections.dropLast(1))

    companion object {
        fun create(settings: StackSettings, random: Random): PipeStack {
            val pool = WeightedPool(settings.weights)
            return PipeStack(List(settings.visibleSize) { pool.draw(random) })
        }
    }
}
