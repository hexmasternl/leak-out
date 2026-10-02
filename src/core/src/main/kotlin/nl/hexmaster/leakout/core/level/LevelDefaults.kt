package nl.hexmaster.leakout.core.level

/**
 * Engine settings used when a level file leaves them out. A level can override each one
 * individually. Changing a value here changes every level that does not override it.
 */
object LevelDefaults {
    const val RESERVOIR_SPEED_FACTOR: Double = 0.3
    const val SPEED_RAMP_PER_CELL: Double = 0.0
    const val VISIBLE_STACK_SIZE: Int = 5
    const val ALLOW_REPLACE: Boolean = true
    const val REPLACE_PENALTY_MS: Long = 3_000

    /**
     * A section's weight divided by the sum of all weights is its target share of the sections dealt
     * in an attempt. The straights and corners are dealt evenly; cross and reservoir are off unless a
     * level gives them a weight.
     */
    val STACK_WEIGHTS: Map<PipeSection, Int> = mapOf(
        PipeSection.H to 7,
        PipeSection.V to 7,
        PipeSection.LT to 7,
        PipeSection.LB to 7,
        PipeSection.RT to 7,
        PipeSection.RB to 7,
        PipeSection.X to 0,
        PipeSection.RES to 0,
    )

    /**
     * How hard the stack steers towards the target shares: a section is drawn with weight
     * `share × e^(STEERING_STRENGTH × deficit)`. 0 is pure random; larger values approach a fixed rotation.
     */
    const val STEERING_STRENGTH: Double = 1.0

    /**
     * A straight or corner section that has not been dealt for `⌈DROUGHT_FACTOR / share⌉` draws is
     * dealt next, so no standard section is missing for more than 2.5 times its expected gap.
     */
    const val DROUGHT_FACTOR: Double = 2.5
}
