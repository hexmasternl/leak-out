package nl.hexmaster.leakout.core.level

import kotlin.math.abs

/** Which endpoint of a level an edit concerns. */
enum class EndpointKind { START, FINISH }

/**
 * A level being edited in the level designer. Every operation returns a new draft and keeps the
 * editing rules: the grid stays 4 to 50 cells each way, nothing lies outside the grid after a
 * resize, the start and finish never share a cell and are never blocked. It does not repeat
 * [LevelValidator]: a draft may be invalid, for example with a start that opens onto the edge, and
 * [status] reports that instead of preventing it.
 *
 * Mandatory sections are only placed with [toggleMandatory]: the Start, Finish and Block tools leave
 * their cells alone, and a resize drops those outside the grid.
 *
 * The star thresholds are not edited: every draft calculates them from the free cells with
 * [StarBenchmarks.forFreeCells], so [level] has them whatever level the draft was built from.
 */
class LevelDraft(source: Level) {
    val level: Level = source.copy(stars = StarBenchmarks.forFreeCells(source.freeCells))

    /** Validation first, then solvability: what the designer shows, and whether it may be played. */
    val status: DesignStatus by lazy { DesignStatus.of(level) }

    /**
     * Sets the grid size, clamped to [MIN_GRID_SIZE]..[MAX_GRID_SIZE]. Blocked cells and mandatory
     * sections outside the new grid are dropped; an endpoint outside it moves to the nearest cell
     * inside, keeping its open side.
     */
    fun resize(columns: Int, rows: Int): LevelDraft {
        val grid = GridSize(columns.coerceIn(MIN_GRID_SIZE, MAX_GRID_SIZE), rows.coerceIn(MIN_GRID_SIZE, MAX_GRID_SIZE))
        val start = level.start.copy(position = clampInto(level.start.position, grid))
        var finish = level.finish.copy(position = clampInto(level.finish.position, grid))
        if (finish.position == start.position) finish = finish.copy(position = nearestFree(finish.position, grid, start.position))
        val endpoints = setOf(start.position, finish.position)
        return LevelDraft(
            level.copy(
                grid = grid,
                start = start,
                finish = finish,
                blocked = level.blocked.filter { it in grid && it !in endpoints },
                mandatory = level.mandatory.filter { it.position in grid && it.position !in endpoints },
            ),
        )
    }

    /** Moves the start to [cell], keeping its open side. See [placeEndpoint]. */
    fun placeStart(cell: Position): LevelDraft = placeEndpoint(EndpointKind.START, cell)

    /** Moves the finish to [cell], keeping its open side. See [placeEndpoint]. */
    fun placeFinish(cell: Position): LevelDraft = placeEndpoint(EndpointKind.FINISH, cell)

    /**
     * Moves an endpoint to [cell] and unblocks that cell. Does nothing for a cell outside the grid,
     * the other endpoint's cell or a mandatory section's cell.
     */
    fun placeEndpoint(kind: EndpointKind, cell: Position): LevelDraft {
        val other = endpoint(kind.other()).position
        if (cell !in level.grid || cell == other || level.mandatoryAt(cell) != null) return this
        val moved = endpoint(kind).copy(position = cell)
        val unblocked = level.blocked.filter { it != cell }
        return LevelDraft(
            when (kind) {
                EndpointKind.START -> level.copy(start = moved, blocked = unblocked)
                EndpointKind.FINISH -> level.copy(finish = moved, blocked = unblocked)
            },
        )
    }

    fun setOpenSide(kind: EndpointKind, side: Side): LevelDraft = LevelDraft(
        when (kind) {
            EndpointKind.START -> level.copy(start = level.start.copy(openSide = side))
            EndpointKind.FINISH -> level.copy(finish = level.finish.copy(openSide = side))
        },
    )

    fun endpoint(kind: EndpointKind): Endpoint = when (kind) {
        EndpointKind.START -> level.start
        EndpointKind.FINISH -> level.finish
    }

    /** Blocks a free cell or frees a blocked one. Cells the tools may not block are left alone. */
    fun toggleBlocked(cell: Position): LevelDraft = paintBlocked(listOf(cell), blocked = !level.isBlocked(cell))

    /**
     * Blocks ([blocked] true) or frees every cell in [cells], as a drag across the grid does. The
     * start, the finish, mandatory cells and cells outside the grid are skipped, and a cell is never
     * listed twice.
     */
    fun paintBlocked(cells: Iterable<Position>, blocked: Boolean): LevelDraft {
        val targets = cells.filter(::canBlock).toSet()
        val result = if (blocked) {
            level.blocked + targets.filter { it !in level.blocked }
        } else {
            level.blocked.filter { it !in targets }
        }
        return if (result == level.blocked) this else LevelDraft(level.copy(blocked = result))
    }

    fun clearBlocked(): LevelDraft = LevelDraft(level.copy(blocked = emptyList()))

    /**
     * What a tap with a mandatory section tool does on [cell]: removes a mandatory [section] there,
     * turns a mandatory section of the other kind into [section] (keeping its place in the list), or
     * places a new one on a free cell. A cell outside the grid, a blocked cell, the start, the finish
     * and a new section beyond [LevelValidator.MAX_MANDATORY_SECTIONS] are left alone. Only a cross
     * or a reservoir can be mandatory.
     */
    fun toggleMandatory(cell: Position, section: PipeSection): LevelDraft {
        require(section in LevelValidator.MANDATORY_KINDS) { "Only a cross or a reservoir can be mandatory, not $section" }
        val index = level.mandatory.indexOfFirst { it.position == cell }
        val mandatory = when {
            index >= 0 && level.mandatory[index].section == section -> level.mandatory.filterIndexed { i, _ -> i != index }
            index >= 0 -> level.mandatory.mapIndexed { i, existing -> if (i == index) existing.copy(section = section) else existing }
            !canPlaceMandatory(cell) -> return this
            else -> level.mandatory + MandatorySection(cell, section)
        }
        return LevelDraft(level.copy(mandatory = mandatory))
    }

    /** Whether a new mandatory section may go on [cell]. */
    private fun canPlaceMandatory(cell: Position): Boolean =
        cell in level.grid && !level.isBlocked(cell) && cell != level.start.position && cell != level.finish.position &&
            level.mandatory.size < LevelValidator.MAX_MANDATORY_SECTIONS

    /** Whether the Block tool may change [cell]. */
    fun canBlock(cell: Position): Boolean =
        cell in level.grid && cell != level.start.position && cell != level.finish.position && level.mandatoryAt(cell) == null

    fun withId(id: Int): LevelDraft = LevelDraft(level.copy(id = id))

    fun withName(name: String): LevelDraft = LevelDraft(level.copy(name = name))

    fun withFlow(flow: FlowSettings): LevelDraft = LevelDraft(level.copy(flow = flow))

    fun withVisibleStackSize(size: Int): LevelDraft = LevelDraft(level.copy(stack = level.stack.copy(visibleSize = size)))

    fun withWeight(section: PipeSection, weight: Int): LevelDraft =
        LevelDraft(level.copy(stack = level.stack.copy(weights = level.stack.weights + (section to weight))))

    fun withRules(rules: PlacementRules): LevelDraft = LevelDraft(level.copy(rules = rules))

    override fun equals(other: Any?): Boolean = other is LevelDraft && other.level == level

    override fun hashCode(): Int = level.hashCode()

    override fun toString(): String = "LevelDraft(level=$level)"

    private fun EndpointKind.other(): EndpointKind = if (this == EndpointKind.START) EndpointKind.FINISH else EndpointKind.START

    companion object {
        const val MIN_GRID_SIZE: Int = LevelValidator.MIN_GRID_SIZE
        const val MAX_GRID_SIZE: Int = LevelValidator.MAX_GRID_SIZE
        const val NEW_LEVEL_NAME: String = "New level"

        /** A blank, valid and solvable level: a 7 by 5 grid with the start and finish facing each other across it. */
        fun new(id: Int): LevelDraft = LevelDraft(
            Level(
                id = id,
                name = NEW_LEVEL_NAME,
                grid = GridSize(columns = 7, rows = 5),
                start = Endpoint(Position(0, 2), Side.RIGHT),
                finish = Endpoint(Position(6, 2), Side.LEFT),
                blocked = emptyList(),
                flow = FlowSettings(initialDelayMs = 20_000, cellsPerSecond = 0.4),
                stack = StackSettings(),
                rules = PlacementRules(),
                // Replaced by the calculated thresholds: 200, 250 and 330 for the 33 free cells.
                stars = StarBenchmarks(one = 0, two = 0, three = 0),
            ),
        )

        private fun clampInto(position: Position, grid: GridSize): Position =
            Position(position.x.coerceIn(0, grid.columns - 1), position.y.coerceIn(0, grid.rows - 1))

        /** The cell of [grid] nearest to [from] other than [taken]; ties go to the lowest row, then column. */
        private fun nearestFree(from: Position, grid: GridSize, taken: Position): Position =
            (0 until grid.rows).flatMap { y -> (0 until grid.columns).map { x -> Position(x, y) } }
                .filter { it != taken }
                .minBy { abs(it.x - from.x) + abs(it.y - from.y) }
    }
}

/** What the designer reports about a draft. Only a [Solvable] design may be test played or exported. */
sealed interface DesignStatus {
    val isPlayable: Boolean get() = this is Solvable

    /** [LevelValidator] found problems; [highlightedCells] are the cells they are about. */
    data class Invalid(val errors: List<ValidationError>, val highlightedCells: Set<Position>) : DesignStatus

    /** Valid, but [LevelSolver] found no route. */
    data class Unsolvable(val reason: String) : DesignStatus

    /** Valid and winnable along [path], which the designer draws as a hint. */
    data class Solvable(val path: List<Position>) : DesignStatus

    companion object {
        private val indexed = Regex("""^(blocked|mandatory)\[(\d+)]""")

        fun of(level: Level): DesignStatus {
            val errors = LevelValidator.validate(level).errors
            if (errors.isNotEmpty()) return Invalid(errors, errors.mapNotNull { cellOf(it.field, level) }.toSet())
            return when (val result = LevelSolver.solve(level)) {
                is SolverResult.Solvable -> Solvable(result.path)
                is SolverResult.Unsolvable -> Unsolvable(result.reason)
            }
        }

        /** The cell an error [field] is about: `start.*`, `finish.*`, `blocked[i]` or `mandatory[i]`; otherwise `null`. */
        fun cellOf(field: String, level: Level): Position? {
            val root = field.substringBefore('.')
            if (root == "start") return level.start.position
            if (root == "finish") return level.finish.position
            val match = indexed.find(field) ?: return null
            val index = match.groupValues[2].toInt()
            return when (match.groupValues[1]) {
                "blocked" -> level.blocked.getOrNull(index)
                else -> level.mandatory.getOrNull(index)?.position
            }
        }
    }
}
