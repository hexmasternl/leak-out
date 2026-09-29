package nl.hexmaster.leakout.core.level

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The JSON shape of a level file. Properties without a default are required. Optional properties
// default to LevelDefaults, so a parsed level always holds concrete values.

@Serializable
internal data class LevelDto(
    val id: Int,
    val name: String,
    val grid: GridDto,
    val start: EndpointDto,
    val finish: EndpointDto,
    val blocked: List<CellDto>,
    val flow: FlowDto,
    val stack: StackDto = StackDto(),
    val rules: RulesDto = RulesDto(),
    val stars: StarsDto,
) {
    fun toLevel(): Level = Level(
        id = id,
        name = name.trim(),
        grid = GridSize(grid.columns, grid.rows),
        start = start.toEndpoint(),
        finish = finish.toEndpoint(),
        blocked = blocked.map { Position(it.x, it.y) },
        flow = FlowSettings(flow.initialDelayMs, flow.cellsPerSecond, flow.reservoirSpeedFactor, flow.speedRampPerCell),
        stack = StackSettings(stack.visibleSize, stack.weights.toMap()),
        rules = PlacementRules(rules.allowReplace, rules.replacePenaltyMs),
        stars = StarBenchmarks(stars.one, stars.two, stars.three),
    )
}

@Serializable
internal data class GridDto(val columns: Int, val rows: Int)

@Serializable
internal data class EndpointDto(val x: Int, val y: Int, val openSide: Side) {
    fun toEndpoint(): Endpoint = Endpoint(Position(x, y), openSide)
}

@Serializable
internal data class CellDto(val x: Int, val y: Int)

@Serializable
internal data class FlowDto(
    val initialDelayMs: Long,
    val cellsPerSecond: Double,
    val reservoirSpeedFactor: Double = LevelDefaults.RESERVOIR_SPEED_FACTOR,
    val speedRampPerCell: Double = LevelDefaults.SPEED_RAMP_PER_CELL,
)

@Serializable
internal data class StackDto(
    val visibleSize: Int = LevelDefaults.VISIBLE_STACK_SIZE,
    val weights: WeightsDto = WeightsDto(),
)

@Serializable
internal data class WeightsDto(
    @SerialName("H") val h: Int = default(PipeSection.H),
    @SerialName("V") val v: Int = default(PipeSection.V),
    @SerialName("LT") val lt: Int = default(PipeSection.LT),
    @SerialName("LB") val lb: Int = default(PipeSection.LB),
    @SerialName("RT") val rt: Int = default(PipeSection.RT),
    @SerialName("RB") val rb: Int = default(PipeSection.RB),
    @SerialName("X") val x: Int = default(PipeSection.X),
    @SerialName("RES") val res: Int = default(PipeSection.RES),
) {
    fun toMap(): Map<PipeSection, Int> = mapOf(
        PipeSection.H to h,
        PipeSection.V to v,
        PipeSection.LT to lt,
        PipeSection.LB to lb,
        PipeSection.RT to rt,
        PipeSection.RB to rb,
        PipeSection.X to x,
        PipeSection.RES to res,
    )

    private companion object {
        fun default(section: PipeSection): Int = LevelDefaults.STACK_WEIGHTS.getValue(section)
    }
}

@Serializable
internal data class RulesDto(
    val allowReplace: Boolean = LevelDefaults.ALLOW_REPLACE,
    val replacePenaltyMs: Long = LevelDefaults.REPLACE_PENALTY_MS,
)

@Serializable
internal data class StarsDto(val one: Int, val two: Int, val three: Int)
