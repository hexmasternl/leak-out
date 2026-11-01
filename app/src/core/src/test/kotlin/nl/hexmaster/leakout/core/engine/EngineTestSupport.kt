package nl.hexmaster.leakout.core.engine

import kotlin.random.Random
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.flow.Channel
import nl.hexmaster.leakout.core.flow.channelFor
import nl.hexmaster.leakout.core.level.Endpoint
import nl.hexmaster.leakout.core.level.FlowSettings
import nl.hexmaster.leakout.core.level.GridSize
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.MandatorySection
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.PlacementRules
import nl.hexmaster.leakout.core.level.PortalPair
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.core.level.StackSettings
import nl.hexmaster.leakout.core.level.TestLevels
import nl.hexmaster.leakout.core.route.Route
import nl.hexmaster.leakout.core.route.RouteTracer

internal fun p(x: Int, y: Int) = Position(x, y)

/**
 * A 5 by 5 level: start at (0, 1) open to the right, finish at (2, 3) open at the top, so
 * [ROUTE] (H, LB, V) connects them. By default the stack only draws [section] and the flow runs at
 * 1 cell per second after 10 seconds.
 */
internal fun engineLevel(
    flow: FlowSettings = FlowSettings(initialDelayMs = 10_000, cellsPerSecond = 1.0),
    section: PipeSection? = PipeSection.H,
    rules: PlacementRules = PlacementRules(),
    blocked: List<Position> = emptyList(),
    start: Endpoint = Endpoint(p(0, 1), Side.RIGHT),
    finish: Endpoint = Endpoint(p(2, 3), Side.TOP),
    visibleSize: Int = 5,
    mandatory: List<MandatorySection> = emptyList(),
    portals: List<PortalPair> = emptyList(),
    leaky: List<Position> = emptyList(),
): Level = TestLevels.valid(
    grid = GridSize(5, 5),
    portals = portals,
    leaky = leaky,
    start = start,
    finish = finish,
    blocked = blocked,
    flow = flow,
    stack = StackSettings(
        visibleSize = visibleSize,
        weights = if (section == null) StackSettings().weights else PipeSection.entries.associateWith { if (it == section) 1 else 0 },
    ),
    rules = rules,
    mandatory = mandatory,
)

/** The route of [engineLevel]: from the start through H, LB and V into the finish. 35 points. */
internal val ROUTE: List<Pair<Position, PipeSection>> =
    listOf(p(1, 1) to PipeSection.H, p(2, 1) to PipeSection.LB, p(2, 2) to PipeSection.V)

/**
 * A portal pair for [engineLevel]: portal a at (2, 1) open to the left, right after the start's
 * first tile, and portal b at (3, 2) open to the left, facing (2, 2) above the finish.
 */
internal val PORTAL_PAIR = PortalPair(Endpoint(p(2, 1), Side.LEFT), Endpoint(p(3, 2), Side.LEFT))

/** The route of [engineLevel] with [PORTAL_PAIR]: an H into portal a, out of portal b into an RB, into the finish. 25 points. */
internal val PORTAL_ROUTE: List<Pair<Position, PipeSection>> = listOf(p(1, 1) to PipeSection.H, p(2, 2) to PipeSection.RB)

/** The head inside the pair whose portal at [at] it entered through [entry], [progress] of the way through the pair. */
internal fun GameState.inPortalPair(progress: Double, at: Position = PORTAL_PAIR.a.position, entry: Side = PORTAL_PAIR.a.openSide): GameState =
    copy(flow = FlowState.Flowing(at, entry, Channel.PORTAL, progress, flow.used))

internal fun engine(seed: Int = 7) = GameEngine(Random(seed))

/** Puts [pipes] on the board directly, as scripted placements would, and checks the route again. */
internal fun GameState.withPipes(pipes: List<Pair<Position, PipeSection>>): GameState {
    val board = pipes.fold(board) { board, (at, section) -> board.with(at, Cell.Pipe(section)) }
    return copy(board = board, routeComplete = RouteTracer.trace(board, level) is Route.Complete)
}

internal fun GameState.withPipes(vararg pipes: Pair<Position, PipeSection>): GameState = withPipes(pipes.toList())

/** The head in the pipe at [at], entered through [entry], [progress] of the way; the pipe is marked entered. */
internal fun GameState.headAt(at: Position, entry: Side, progress: Double): GameState {
    val section = (board[at] as Cell.Pipe).section
    return copy(
        board = board.markLiquidEntered(at),
        flow = FlowState.Flowing(at, entry, section.channelFor(entry), progress, flow.used),
    )
}

/** Sends [command] and returns the new state. */
internal fun GameEngine.send(state: GameState, vararg commands: GameCommand): GameState =
    commands.fold(state) { s, command -> handle(s, command) }

/** Ticks [dtMs] at a time until the outcome is no longer playing, at most [maxTicks] times. */
internal fun GameEngine.runToEnd(state: GameState, dtMs: Long = 16, maxTicks: Int = 1_000_000): GameState {
    var s = state
    repeat(maxTicks) {
        if (s.outcome !is Outcome.Playing) return s
        s = handle(s, GameCommand.Tick(dtMs))
    }
    return s
}
