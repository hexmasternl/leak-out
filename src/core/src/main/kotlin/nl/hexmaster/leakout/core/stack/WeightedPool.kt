package nl.hexmaster.leakout.core.stack

import kotlin.random.Random
import nl.hexmaster.leakout.core.level.PipeSection

class WeightedPool(weights: Map<PipeSection, Int>) {
    private val entries: List<Entry>
    private val totalWeight: Int

    init {
        var total = 0
        entries = PipeSection.entries.mapNotNull { section ->
            val weight = weights[section] ?: 0
            require(weight >= 0) { "Weights must be zero or positive" }
            if (weight == 0) {
                null
            } else {
                total += weight
                Entry(section, total)
            }
        }
        require(total > 0) { "Total weight must be greater than 0" }
        totalWeight = total
    }

    fun draw(random: Random): PipeSection {
        val value = random.nextInt(totalWeight)
        return entries.first { value < it.cumulativeWeight }.section
    }

    private data class Entry(val section: PipeSection, val cumulativeWeight: Int)
}
