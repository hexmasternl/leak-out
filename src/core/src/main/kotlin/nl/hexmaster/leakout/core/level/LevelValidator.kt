package nl.hexmaster.leakout.core.level

/** A problem with the meaning of a level. [field] is the dotted path of the value concerned. */
data class ValidationError(val field: String, val message: String)

data class ValidationResult(val errors: List<ValidationError>) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * Checks that a parsed [Level] can be played: value ranges, positions inside the grid, and start
 * and finish placed so that they can be connected. Reports every problem, not only the first.
 * It does not check that a level is solvable or that its star thresholds can be reached.
 */
object LevelValidator {
    const val MIN_GRID_SIZE: Int = 4
    const val MAX_GRID_SIZE: Int = 50
    const val MAX_CELLS_PER_SECOND: Double = 10.0
    const val MIN_VISIBLE_STACK_SIZE: Int = 1
    const val MAX_VISIBLE_STACK_SIZE: Int = 10

    fun validate(level: Level): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        errors.checkIdentity(level)
        errors.checkGrid(level.grid)
        errors.checkEndpoints(level)
        errors.checkBlocked(level)
        errors.checkFlow(level.flow)
        errors.checkStack(level.stack)
        errors.checkRules(level.rules)
        errors.checkStars(level.stars)
        return ValidationResult(errors)
    }

    /** Checks rules that span levels: every id is used by one level only. */
    fun validateSet(levels: Collection<Level>): ValidationResult = ValidationResult(
        levels.groupBy { it.id }
            .filterValues { it.size > 1 }
            .map { (id, same) ->
                ValidationError("id", "Id $id is used by ${same.size} levels: ${same.joinToString { "'${it.name}'" }}")
            },
    )

    private fun MutableList<ValidationError>.error(field: String, message: String) {
        add(ValidationError(field, message))
    }

    private fun MutableList<ValidationError>.checkIdentity(level: Level) {
        if (level.id < 1) error("id", "id must be a positive number, but had ${level.id}")
        if (level.name.isBlank()) error("name", "name must not be blank")
    }

    private fun MutableList<ValidationError>.checkGrid(grid: GridSize) {
        val range = MIN_GRID_SIZE..MAX_GRID_SIZE
        if (grid.columns !in range) error("grid.columns", "grid.columns must be between $MIN_GRID_SIZE and $MAX_GRID_SIZE, but had ${grid.columns}")
        if (grid.rows !in range) error("grid.rows", "grid.rows must be between $MIN_GRID_SIZE and $MAX_GRID_SIZE, but had ${grid.rows}")
    }

    private fun MutableList<ValidationError>.checkEndpoints(level: Level) {
        val start = level.start
        val finish = level.finish
        val startInGrid = checkInGrid("start", start, level.grid)
        val finishInGrid = checkInGrid("finish", finish, level.grid)
        if (start.position == finish.position) {
            error("finish", "Start and finish must be different cells, but both are at ${start.position}")
        }
        if (startInGrid) checkFacingCell("start", start, level)
        if (finishInGrid) checkFacingCell("finish", finish, level)

        val startFacesFinish = start.facingCell == finish.position
        val finishFacesStart = finish.facingCell == start.position
        when {
            startFacesFinish && finishFacesStart ->
                error("start", "Start and finish must not face each other directly: the level would be won without placing a pipe")
            startFacesFinish ->
                error("start.openSide", "The start's facing cell must not be the finish: the liquid would leak on the first tick")
            finishFacesStart ->
                error("finish.openSide", "The finish's facing cell must not be the start: the finish could never be reached")
        }
    }

    private fun MutableList<ValidationError>.checkInGrid(name: String, endpoint: Endpoint, grid: GridSize): Boolean {
        val (x, y) = endpoint.position
        if (x !in 0 until grid.columns) error("$name.x", "The $name's x $x lies outside the grid's columns 0 to ${grid.columns - 1}")
        if (y !in 0 until grid.rows) error("$name.y", "The $name's y $y lies outside the grid's rows 0 to ${grid.rows - 1}")
        return endpoint.position in grid
    }

    private fun MutableList<ValidationError>.checkFacingCell(name: String, endpoint: Endpoint, level: Level) {
        val facing = endpoint.facingCell
        when {
            facing !in level.grid ->
                error("$name.openSide", "The $name's open side ${endpoint.openSide} faces the edge of the grid, so it can never be connected")
            level.isBlocked(facing) ->
                error("$name.openSide", "The $name is walled off: its facing cell $facing is blocked")
        }
    }

    private fun MutableList<ValidationError>.checkBlocked(level: Level) {
        val firstIndex = mutableMapOf<Position, Int>()
        level.blocked.forEachIndexed { index, cell ->
            val field = "blocked[$index]"
            if (cell !in level.grid) error(field, "Blocked cell $cell lies outside the ${level.grid} grid")
            if (cell == level.start.position || cell == level.finish.position) {
                error(field, "Blocked cell $cell is on the start or finish; the start and finish cannot be blocked")
            }
            firstIndex.putIfAbsent(cell, index)?.let { error(field, "Blocked cell $cell duplicates blocked[$it]") }
        }
    }

    private fun MutableList<ValidationError>.checkFlow(flow: FlowSettings) {
        if (flow.initialDelayMs < 0) {
            error("flow.initialDelayMs", "flow.initialDelayMs must be 0 or greater, but had ${flow.initialDelayMs}")
        }
        if (!(flow.cellsPerSecond > 0.0 && flow.cellsPerSecond <= MAX_CELLS_PER_SECOND)) {
            error("flow.cellsPerSecond", "flow.cellsPerSecond must be greater than 0 and at most $MAX_CELLS_PER_SECOND, but had ${flow.cellsPerSecond}")
        }
        if (!(flow.reservoirSpeedFactor > 0.0 && flow.reservoirSpeedFactor <= 1.0)) {
            error("flow.reservoirSpeedFactor", "flow.reservoirSpeedFactor must be greater than 0 and at most 1, but had ${flow.reservoirSpeedFactor}")
        }
        if (!(flow.speedRampPerCell >= 0.0)) {
            error("flow.speedRampPerCell", "flow.speedRampPerCell must be 0 or greater, but had ${flow.speedRampPerCell}")
        }
    }

    private fun MutableList<ValidationError>.checkStack(stack: StackSettings) {
        if (stack.visibleSize !in MIN_VISIBLE_STACK_SIZE..MAX_VISIBLE_STACK_SIZE) {
            error("stack.visibleSize", "stack.visibleSize must be between $MIN_VISIBLE_STACK_SIZE and $MAX_VISIBLE_STACK_SIZE, but had ${stack.visibleSize}")
        }
        for (section in PipeSection.entries) {
            val weight = stack.weightOf(section)
            val field = "stack.weights.${section.name}"
            when {
                weight < 0 -> error(field, "$field must be 0 or greater, but had $weight")
                section.alwaysOnStack && weight < 1 ->
                    error(field, "$field must be at least 1: straight and corner sections are always on the stack")
            }
        }
    }

    private fun MutableList<ValidationError>.checkRules(rules: PlacementRules) {
        if (rules.replacePenaltyMs < 0) {
            error("rules.replacePenaltyMs", "rules.replacePenaltyMs must be 0 or greater, but had ${rules.replacePenaltyMs}")
        }
    }

    private fun MutableList<ValidationError>.checkStars(stars: StarBenchmarks) {
        listOf("one" to stars.one, "two" to stars.two, "three" to stars.three).forEach { (name, points) ->
            if (points < 1) error("stars.$name", "stars.$name must be a positive number, but had $points")
        }
        if (!(stars.one < stars.two && stars.two < stars.three)) {
            error("stars", "Star thresholds must be strictly increasing, but had one ${stars.one}, two ${stars.two}, three ${stars.three}")
        }
    }
}
