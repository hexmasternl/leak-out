package nl.hexmaster.leakout.core.level

/** Builders for levels used in tests. [valid] passes validation unless a test overrides a field. */
object TestLevels {
    fun valid(
        id: Int = 3,
        name: String = "First Crossing",
        grid: GridSize = GridSize(columns = 7, rows = 9),
        start: Endpoint = Endpoint(Position(0, 1), Side.RIGHT),
        finish: Endpoint = Endpoint(Position(6, 7), Side.LEFT),
        blocked: List<Position> = listOf(Position(3, 4)),
        flow: FlowSettings = FlowSettings(initialDelayMs = 12_000, cellsPerSecond = 0.6),
        stack: StackSettings = StackSettings(),
        rules: PlacementRules = PlacementRules(),
        stars: StarBenchmarks = StarBenchmarks(100, 200, 350),
    ): Level = Level(id, name, grid, start, finish, blocked, flow, stack, rules, stars)

    fun weights(vararg overrides: Pair<PipeSection, Int>): Map<PipeSection, Int> =
        LevelDefaults.STACK_WEIGHTS + overrides

    /** The full level from the design document, with every optional field given. */
    val FULL_JSON: String = """
        {
          "id": 3,
          "name": "First Crossing",
          "grid": { "columns": 7, "rows": 9 },
          "start":  { "x": 0, "y": 1, "openSide": "RIGHT" },
          "finish": { "x": 6, "y": 7, "openSide": "LEFT" },
          "blocked": [ { "x": 3, "y": 4 } ],
          "flow": {
            "initialDelayMs": 12000,
            "cellsPerSecond": 0.6,
            "reservoirSpeedFactor": 0.25,
            "speedRampPerCell": 0.02
          },
          "stack": {
            "visibleSize": 4,
            "weights": { "H": 4, "V": 4, "LT": 1, "LB": 1, "RT": 1, "RB": 1, "X": 1, "RES": 2 }
          },
          "rules": { "allowReplace": false, "replacePenaltyMs": 750 },
          "stars": { "one": 100, "two": 200, "three": 350 }
        }
    """.trimIndent()

    /** A level with only the required fields. [extraFlow] and [extra] add JSON members for override tests. */
    fun minimalJson(extraFlow: String = "", extra: String = ""): String = """
        {
          "id": 1,
          "name": "First Leak",
          "grid": { "columns": 6, "rows": 4 },
          "start":  { "x": 0, "y": 1, "openSide": "RIGHT" },
          "finish": { "x": 5, "y": 2, "openSide": "LEFT" },
          "blocked": [],
          "flow": { "initialDelayMs": 15000, "cellsPerSecond": 0.5 $extraFlow },
          "stars": { "one": 60, "two": 100, "three": 150 }
          $extra
        }
    """.trimIndent()
}
