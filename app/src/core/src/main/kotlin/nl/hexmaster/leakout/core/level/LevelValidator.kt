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
    const val MAX_GRID_COLUMNS: Int = 25
    const val MAX_GRID_ROWS: Int = 15
    const val MAX_CELLS_PER_SECOND: Double = 10.0
    const val MIN_VISIBLE_STACK_SIZE: Int = 1
    const val MAX_VISIBLE_STACK_SIZE: Int = 10
    const val MAX_MANDATORY_SECTIONS: Int = 6
    const val MAX_PORTAL_PAIRS: Int = 3

    /** The sections a level may list as mandatory. */
    val MANDATORY_KINDS: Set<PipeSection> = setOf(PipeSection.X, PipeSection.RES)

    fun validate(level: Level): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        errors.checkIdentity(level)
        errors.checkGrid(level.grid)
        errors.checkEndpoints(level)
        errors.checkBlocked(level)
        errors.checkMandatory(level)
        errors.checkPortals(level)
        errors.checkLeaky(level)
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
        if (grid.columns !in MIN_GRID_SIZE..MAX_GRID_COLUMNS) {
            error("grid.columns", "grid.columns must be between $MIN_GRID_SIZE and $MAX_GRID_COLUMNS, but had ${grid.columns}")
        }
        if (grid.rows !in MIN_GRID_SIZE..MAX_GRID_ROWS) {
            error("grid.rows", "grid.rows must be between $MIN_GRID_SIZE and $MAX_GRID_ROWS, but had ${grid.rows}")
        }
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
            level.portalAt(facing) != null ->
                error("$name.openSide", "The $name's facing cell $facing must not be a portal")
        }
    }

    // A portal is checked like the start and finish, and must also keep clear of everything the
    // level puts on the board. The facing-cell rules only apply to a portal inside the grid.
    private fun MutableList<ValidationError>.checkPortals(level: Level) {
        if (level.portals.size > MAX_PORTAL_PAIRS) {
            error("portals", "A level can have at most $MAX_PORTAL_PAIRS portal pairs, but had ${level.portals.size}")
        }
        val firstField = mutableMapOf<Position, String>()
        level.portals.forEachIndexed { index, pair ->
            listOf("a" to pair.a, "b" to pair.b).forEach { (name, portal) ->
                val field = "portals[$index].$name"
                val cell = portal.position
                when {
                    cell !in level.grid -> error(field, "Portal at $cell lies outside the ${level.grid} grid")
                    level.isBlocked(cell) -> error(field, "Portal at $cell is on a blocked cell")
                    cell == level.start.position || cell == level.finish.position ->
                        error(field, "Portal at $cell is on the start or finish; the start and finish cannot hold a portal")
                    level.mandatoryAt(cell) != null -> error(field, "Portal at $cell is on a cell that holds a mandatory section")
                    else -> checkPortalFacingCell(field, portal, level)
                }
                firstField.putIfAbsent(cell, field)?.let { error(field, "Portal at $cell duplicates $it") }
            }
        }
    }

    private fun MutableList<ValidationError>.checkPortalFacingCell(field: String, portal: Endpoint, level: Level) {
        val facing = portal.facingCell
        when {
            facing !in level.grid ->
                error("$field.openSide", "The portal's open side ${portal.openSide} faces the edge of the grid, so it can never be connected")
            level.isBlocked(facing) ->
                error("$field.openSide", "The portal is walled off: its facing cell $facing is blocked")
            facing == level.start.position || facing == level.finish.position || level.portalAt(facing) != null ->
                error("$field.openSide", "A portal must not face the start, the finish or another portal, but its facing cell $facing does")
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

    // A mandatory section ignores its stack weight: it is placed by the level, not drawn.
    private fun MutableList<ValidationError>.checkMandatory(level: Level) {
        if (level.mandatory.size > MAX_MANDATORY_SECTIONS) {
            error("mandatory", "A level can have at most $MAX_MANDATORY_SECTIONS mandatory sections, but had ${level.mandatory.size}")
        }
        val firstIndex = mutableMapOf<Position, Int>()
        level.mandatory.forEachIndexed { index, (cell, section) ->
            val field = "mandatory[$index]"
            if (section !in MANDATORY_KINDS) {
                error("$field.section", "Mandatory section $section at $cell is not allowed: only X and RES can be mandatory")
            }
            when {
                cell !in level.grid -> error(field, "Mandatory section at $cell lies outside the ${level.grid} grid")
                level.isBlocked(cell) -> error(field, "Mandatory section at $cell is on a blocked cell")
                cell == level.start.position || cell == level.finish.position ->
                    error(field, "Mandatory section at $cell is on the start or finish; the start and finish cannot hold a section")
            }
            firstIndex.putIfAbsent(cell, index)?.let { error(field, "Mandatory section at $cell duplicates mandatory[$it]") }
        }
    }

    // A leaky tile is an ordinary free cell that drains the liquid, so it must be one a pipe can go on.
    private fun MutableList<ValidationError>.checkLeaky(level: Level) {
        val firstIndex = mutableMapOf<Position, Int>()
        level.leaky.forEachIndexed { index, cell ->
            val field = "leaky[$index]"
            when {
                cell !in level.grid -> error(field, "Leaky tile $cell lies outside the ${level.grid} grid")
                level.isBlocked(cell) -> error(field, "Leaky tile $cell is on a blocked cell")
                cell == level.start.position || cell == level.finish.position ->
                    error(field, "Leaky tile $cell is on the start or finish; the start and finish cannot be leaky")
                level.mandatoryAt(cell) != null ->
                    error(field, "Leaky tile $cell holds a mandatory section; a mandatory section cannot lie on a leaky tile")
                level.portalAt(cell) != null -> error(field, "Leaky tile $cell holds a portal; a portal cannot lie on a leaky tile")
            }
            firstIndex.putIfAbsent(cell, index)?.let { error(field, "Leaky tile $cell duplicates leaky[$it]") }
        }
    }

    private fun MutableList<ValidationError>.checkFlow(flow: FlowSettings) {
        if (flow.initialDelayMs < 0) {
            error("flow.initialDelayMs", "flow.initialDelayMs must be 0 or greater, but had ${flow.initialDelayMs}")
        }
        if (!(flow.cellsPerSecond > 0.0 && flow.cellsPerSecond <= MAX_CELLS_PER_SECOND)) {
            error("flow.cellsPerSecond", "flow.cellsPerSecond must be greater than 0 and at most $MAX_CELLS_PER_SECOND, but had ${flow.cellsPerSecond}")
        }
        if (ReservoirType.fromFactor(flow.reservoirSpeedFactor) == null) {
            error("flow.reservoirSpeedFactor", "flow.reservoirSpeedFactor must be one of ${ReservoirType.FACTORS.joinToString(", ")}, but had ${flow.reservoirSpeedFactor}")
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
