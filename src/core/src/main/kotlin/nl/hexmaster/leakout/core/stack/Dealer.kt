package nl.hexmaster.leakout.core.stack

import kotlin.math.ceil
import kotlin.random.Random
import nl.hexmaster.leakout.core.level.LevelDefaults
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.StackSettings

/**
 * Deals the sections of one attempt, steered towards the target shares of the level's weights.
 * A section's target share is its weight divided by the sum of all weights; its deficit is the
 * share times the draws so far minus how often it was dealt. A section is drawn with weight
 * `share × e^(STEERING_STRENGTH × deficit)`, so sections that are behind catch up and sections
 * that are ahead wait. A straight or corner section missing for its drought limit is dealt
 * without drawing. Immutable: [deal] returns the next dealer.
 *
 * Only allowed sections (weight above 0) are in [weights]; [dealt] and [droughts] hold a value for
 * each of them.
 */
data class Dealer(
    val weights: Map<PipeSection, Int>,
    val dealt: Map<PipeSection, Int>,
    val droughts: Map<PipeSection, Int>,
) {
    init {
        require(weights.isNotEmpty()) { "Dealer needs at least one section with a weight above 0" }
        require(weights.values.all { it > 0 }) { "Dealer weights must be greater than 0" }
        require(dealt.keys == weights.keys && droughts.keys == weights.keys) { "Every section needs a count and a drought" }
    }

    private val order: List<PipeSection> = PipeSection.entries.filter { it in weights }
    private val totalWeight: Int = weights.values.sum()

    /** Number of sections dealt so far in this attempt. */
    val draws: Int get() = dealt.values.sum()

    fun share(section: PipeSection): Double = weights.getValue(section).toDouble() / totalWeight

    fun deficit(section: PipeSection): Double = share(section) * draws - dealt.getValue(section)

    /**
     * The drought limit of a straight or corner section, `⌈DROUGHT_FACTOR / share⌉`, or `null` for
     * a section that is never forced (cross and reservoir).
     */
    fun droughtLimit(section: PipeSection): Int? =
        if (section.mandatory) ceil(LevelDefaults.DROUGHT_FACTOR * totalWeight / weights.getValue(section)).toInt() else null

    /** The section the drought limit deals next: the longest drought over its limit, ties in [PipeSection] order. */
    fun forced(): PipeSection? = order
        .filter { section -> droughtLimit(section)?.let { droughts.getValue(section) >= it } == true }
        .maxByOrNull { droughts.getValue(it) }

    /** The chance of each section on a random draw, ignoring the drought limit. Sums to 1. */
    fun chances(): Map<PipeSection, Double> {
        val steered = steeredWeights()
        val total = steered.sum()
        return order.withIndex().associate { (i, section) -> section to steered[i] / total }
    }

    fun deal(random: Random): Pair<PipeSection, Dealer> {
        val section = forced() ?: draw(random)
        return section to after(section)
    }

    private fun steeredWeights(): DoubleArray = DoubleArray(order.size) { i ->
        val section = order[i]
        share(section) * StrictMath.exp(LevelDefaults.STEERING_STRENGTH * deficit(section))
    }

    private fun draw(random: Random): PipeSection {
        val steered = steeredWeights()
        val value = random.nextDouble() * steered.sum()
        var cumulative = 0.0
        for (i in order.indices) {
            cumulative += steered[i]
            if (value < cumulative) return order[i]
        }
        return order.last()
    }

    private fun after(section: PipeSection): Dealer = copy(
        dealt = dealt + (section to dealt.getValue(section) + 1),
        droughts = droughts.mapValues { (s, age) -> if (s == section) 0 else age + 1 },
    )

    companion object {
        /** A dealer with nothing dealt yet, for the allowed sections of [settings]. */
        fun initial(settings: StackSettings): Dealer {
            val weights = PipeSection.entries.filter { settings.weightOf(it) > 0 }.associateWith { settings.weightOf(it) }
            return Dealer(weights, weights.mapValues { 0 }, weights.mapValues { 0 })
        }
    }
}
