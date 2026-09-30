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
    const val REPLACE_PENALTY_MS: Long = 500

    /** Cross and reservoir are off unless a level gives them a weight. */
    val STACK_WEIGHTS: Map<PipeSection, Int> = mapOf(
        PipeSection.H to 3,
        PipeSection.V to 3,
        PipeSection.LT to 2,
        PipeSection.LB to 2,
        PipeSection.RT to 2,
        PipeSection.RB to 2,
        PipeSection.X to 0,
        PipeSection.RES to 0,
    )
}
