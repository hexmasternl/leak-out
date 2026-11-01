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
 * Two portals that link two cells of the board: liquid that enters one portal through its open
 * side comes out of the other through that portal's open side. A pair works both ways and is
 * placed by the level, never drawn from the stack. Each portal has the shape of an [Endpoint].
 */
data class PortalPair(val a: Endpoint, val b: Endpoint) {
    /** The portal of this pair on [position], or `null` when neither is there. */
    fun portalAt(position: Position): Endpoint? = when (position) {
        a.position -> a
        b.position -> b
        else -> null
    }

    /** The other portal of this pair than the one on [position], or `null` when neither is there. */
    fun partnerOf(position: Position): Endpoint? = when (position) {
        a.position -> b
        b.position -> a
        else -> null
    }
}

/**
 * One playable level. Instances hold concrete values only: defaults are resolved while parsing.
 * [blocked], [mandatory], [portals] and [leaky] keep their entries in authored order so that
 * validation can report duplicates; the first portal pair is pair 1.
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
    val portals: List<PortalPair> = emptyList(),
    val leaky: List<Position> = emptyList(),
) {
    fun isBlocked(position: Position): Boolean = position in blocked

    /** Whether [position] is a leaky tile: a pipe can go on it, but liquid passing it costs points. */
    fun isLeaky(position: Position): Boolean = position in leaky

    /** The mandatory section on [position], or `null` when there is none. */
    fun mandatoryAt(position: Position): MandatorySection? = mandatory.firstOrNull { it.position == position }

    /** The portal on [position], or `null` when there is none. */
    fun portalAt(position: Position): Endpoint? = portals.firstNotNullOfOrNull { it.portalAt(position) }

    /** The other portal of the pair whose portal is on [position], or `null` when there is no portal there. */
    fun partnerOf(position: Position): Endpoint? = portals.firstNotNullOfOrNull { it.partnerOf(position) }

    /** The cells of every portal, pair by pair. */
    val portalCells: List<Position> get() = portals.flatMap { listOf(it.a.position, it.b.position) }

    /**
     * The cells a pipe can go on: every cell of the grid except blocked cells, the start, the
     * finish and portals. Cells holding a mandatory section count as free.
     */
    val freeCells: Int
        get() {
            val taken = (blocked + start.position + finish.position + portalCells).filter { it in grid }.toSet()
            return grid.columns * grid.rows - taken.size
        }

    /**
     * The free cells on which the liquid can earn points: [freeCells] without the leaky tiles. The
     * designer's star thresholds come from it, so a leaky tile never adds points that cannot be won.
     */
    val scoringCells: Int
        get() {
            val taken = (blocked + start.position + finish.position + portalCells).toSet()
            return freeCells - leaky.filter { it in grid && it !in taken }.toSet().size
        }

    /** A section is allowed on the stack when it has a positive weight. */
    fun isAllowed(section: PipeSection): Boolean = stack.weightOf(section) > 0
}
