package nl.hexmaster.leakout.core.level

import kotlin.math.abs

/**
 * The four reservoir types a level can use. [factor] is the share of its normal speed the liquid keeps
 * inside a reservoir; it is what `flow.reservoirSpeedFactor` holds in a level file. The app tints the
 * reservoir from the type, not from the number.
 */
enum class ReservoirType(val factor: Double) {
    RED_10(0.1),
    BLUE_20(0.2),
    PURPLE_30(0.3),
    ORANGE_40(0.4);

    /** The percentage of normal speed, as shown on the reservoir's badge. */
    val percent: Int get() = Math.round(factor * 100).toInt()

    companion object {
        /** The type used when a level leaves the factor out. */
        val DEFAULT: ReservoirType = PURPLE_30

        /** Largest distance from a type's factor that still counts as that type. */
        const val TOLERANCE: Double = 1e-9

        /** The allowed factors, as listed in validation errors. */
        val FACTORS: List<Double> = entries.map { it.factor }

        /** The type whose factor is [factor] (within [TOLERANCE]), or `null` when none is. */
        fun fromFactor(factor: Double): ReservoirType? = entries.firstOrNull { abs(it.factor - factor) <= TOLERANCE }

        /** The type nearest to [factor]; a tie goes to the higher factor. */
        fun nearest(factor: Double): ReservoirType =
            entries.reduce { best, next ->
                val dBest = abs(best.factor - factor)
                val dNext = abs(next.factor - factor)
                if (dNext < dBest - TOLERANCE || abs(dNext - dBest) <= TOLERANCE) next else best
            }
    }
}
