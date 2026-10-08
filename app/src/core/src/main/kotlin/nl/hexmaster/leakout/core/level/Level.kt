package nl.hexmaster.leakout.core.level

/** How the liquid moves. Speeds are in cell lengths per second. */
data class FlowSettings(
    val initialDelayMs: Long,
    val cellsPerSecond: Double,
    val reservoirSpeedFactor: Double = LevelDefaults.RESERVOIR_SPEED_FACTOR,
    val speedRampPerCell: Double = LevelDefaults.SPEED_RAMP_PER_CELL,
)

/** The upcoming-sections queue: how many are shown, and how often each section is drawn. */
data class StackSettings(
    val visibleSize: Int = LevelDefaults.VISIBLE_STACK_SIZE,
    val weights: Map<PipeSection, Int> = LevelDefaults.STACK_WEIGHTS,
) {
    init {
        require(weights.keys == PipeSection.entries.toSet()) { "Every pipe section needs a weight" }
    }

    fun weightOf(section: PipeSection): Int = weights.getValue(section)
}

/** Whether an unfilled pipe may be replaced, and how long placing is locked after a replacement. */
data class PlacementRules(
    val allowReplace: Boolean = LevelDefaults.ALLOW_REPLACE,
    val replacePenaltyMs: Long = LevelDefaults.REPLACE_PENALTY_MS,
)

/** The minimum points for one, two and three stars on a won level. */
data class StarBenchmarks(val one: Int, val two: Int, val three: Int) {
    /** Stars earned for [points]: 0 to 3. A won level can earn 0 stars. */
    fun starsFor(points: Int): Int = when {
        points >= three -> 3
        points >= two -> 2
        points >= one -> 1
        else -> 0
    }

    companion object {
        /**
         * The thresholds the level designer gives a level with [freeCells] free cells: three stars
         * for 10 points per free cell, two and one star for 75% and 60% of that, each rounded to the
         * nearest 10 with halves rounded up.
         */
        fun forFreeCells(freeCells: Int): StarBenchmarks = StarBenchmarks(
            one = (60 * freeCells + 50) / 100 * 10,
            two = (75 * freeCells + 50) / 100 * 10,
            three = 10 * freeCells,
        )
    }
}

/**
 * A section that is on the board from the start of every attempt. It is never removed, and the
 * level is only won once the liquid has run through every channel of it. Only a cross or a
 * reservoir may be mandatory; [LevelValidator] enforces that.
 */
data class MandatorySection(val position: Position, val section: PipeSection)

/**
 * One playable level. Instances hold concrete values only: defaults are resolved while parsing.
 * [blocked] and [mandatory] keep their entries in authored order so that validation can report
 * duplicates.
 */
data class Level(
    val id: Int,
    val name: String,
    val grid: GridSize,
    val start: Endpoint,
    val finish: Endpoint,
    val blocked: List<Position>,
    val flow: FlowSettings,
    val stack: StackSettings,
    val rules: PlacementRules,
    val stars: StarBenchmarks,
    val mandatory: List<MandatorySection> = emptyList(),
) {
    fun isBlocked(position: Position): Boolean = position in blocked

    /** The mandatory section on [position], or `null` when there is none. */
    fun mandatoryAt(position: Position): MandatorySection? = mandatory.firstOrNull { it.position == position }

    /**
     * The cells a pipe can go on: every cell of the grid except blocked cells, the start and the
     * finish. Cells holding a mandatory section count as free.
     */
    val freeCells: Int
        get() {
            val taken = (blocked + start.position + finish.position).filter { it in grid }.toSet()
            return grid.columns * grid.rows - taken.size
        }

    /** A section is allowed on the stack when it has a positive weight. */
    fun isAllowed(section: PipeSection): Boolean = stack.weightOf(section) > 0
}
