package nl.hexmaster.leakout.ui.game

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import nl.hexmaster.leakout.core.board.Cell
import nl.hexmaster.leakout.core.engine.GameCommand
import nl.hexmaster.leakout.core.engine.GameEngine
import nl.hexmaster.leakout.core.engine.GameState
import nl.hexmaster.leakout.core.engine.LossReason
import nl.hexmaster.leakout.core.engine.Outcome
import nl.hexmaster.leakout.core.flow.MandatoryProgress
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.Position
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.history.RecordResult

/**
 * Holds the [GameState] of the running level for Compose. It keeps no game data of its own: every
 * change is a command to the [GameEngine], and the views read [state] and the few screen values
 * derived from it here.
 */
class GameSession(level: Level, private val engine: GameEngine = GameEngine()) {
    constructor(level: Level, random: Random) : this(level, GameEngine(random))

    /** The current attempt. Tests may set a scripted state; the game only changes it through [send]. */
    var state: GameState by mutableStateOf(engine.start(level))
        @VisibleForTesting internal set

    /**
     * Sends [command] to the engine. Returns whether it was accepted, that is, whether the state changed.
     * After a win it also takes the unused sections off the board that the tally has counted by now,
     * so the field and the penalty row change in the same frame.
     */
    fun send(command: GameCommand): Boolean {
        val before = state
        var next = engine.handle(before, command)
        if (next === before) return false
        winFrameOf(next)?.let { frame -> next = engine.handle(next, GameCommand.RemoveUnused(frame.sectionsRemoved)) }
        state = next
        before.replacement?.takeIf { landed(before, next) }?.let { lastLanding = Landing(next.attempt, it.at, it.landsAtMs) }
        return true
    }

    /** The replacement section that landed last in this session, if any; each landing plays the placement sound. */
    var lastLanding: Landing? by mutableStateOf(null)
        private set

    /** Whether a replacement runs, during which the player cannot place, drag or fast forward. */
    val isReplacing: Boolean get() = state.replacement != null

    /**
     * Places the stack's next section on [at] and says what that did: a new section on an empty
     * tile, one that destroyed the section the tile held, or nothing because the tile refused it.
     */
    fun place(at: Position): PlaceResult {
        val held = state.board[at] is Cell.Pipe
        return when {
            !send(GameCommand.Place(at)) -> PlaceResult.Rejected
            held -> PlaceResult.Replaced
            else -> PlaceResult.Placed
        }
    }

    /** Whether the next section can be dropped on [at] now: the same check a place command runs. */
    fun canPlace(at: Position): Boolean = engine.canPlace(state, at)

    /** The pause dialog is open exactly while the game is paused during play. */
    val showPauseDialog: Boolean get() = state.paused && state.outcome is Outcome.Playing

    /** Game time since the leak, which drives the leak effect; 0 unless lost. */
    val lossAgeMs: Long get() = (state.outcome as? Outcome.Lost)?.let { state.clockMs - it.atMs } ?: 0L

    /**
     * The mandatory sections the liquid did not use, which pulse on the field, when the attempt was
     * lost because one was missed; empty otherwise.
     */
    val missedMandatory: List<Position>
        get() {
            val lost = state.outcome as? Outcome.Lost
            if (lost?.reason != LossReason.MANDATORY_MISSED) return emptyList()
            return MandatoryProgress.unused(state.level.mandatory, state.flow.used).map { it.position }
        }

    /** Whether the level lost dialog is shown: once the leak effect has run for [LeakTiming.EFFECT_MS]. */
    val showLostDialog: Boolean get() = state.outcome is Outcome.Lost && lossAgeMs >= LeakTiming.EFFECT_MS

    /** The fast forward button is shown during play once the route is complete, and while fast forward is on. */
    val showFastForward: Boolean get() = state.outcome is Outcome.Playing && (state.routeComplete || state.fastForward)

    /** Whether the fast forward button can be tapped: not while a replacement runs. */
    val fastForwardEnabled: Boolean get() = !isReplacing

    /** What the fast forward button does: fast forward when off, normal speed when on. */
    fun toggleFastForward(): Boolean = send(if (state.fastForward) GameCommand.NormalSpeed else GameCommand.FastForward)

    /**
     * Ends the leak effect at once, so the level lost dialog shows as soon as the player comes back
     * to the app. After a loss a tick only moves the clock, so this ticks through the rest of the effect.
     */
    fun skipLeakEffect() {
        if (state.outcome !is Outcome.Lost) return
        val remaining = LeakTiming.EFFECT_MS - lossAgeMs
        if (remaining > 0) send(GameCommand.Tick(remaining))
    }

    /** Game time since the win, which drives the finish flash and the tally; 0 unless won. */
    val winAgeMs: Long get() = winAgeOf(state)

    /** Whether the level complete dialog is shown: once the finish has flashed for [WinTiming.FINISH_EFFECT_MS]. */
    val showWinDialog: Boolean get() = state.outcome is Outcome.Won && winAgeMs >= WinTiming.FINISH_EFFECT_MS

    /** What the level complete dialog shows now; null unless won. */
    val winFrame: WinFrame? get() = winFrameOf(state)

    /**
     * Ends the finish flash and the tally at once, so the finished level complete dialog shows as soon
     * as the player comes back to the app. After a win a tick only moves the clock, so this ticks to
     * the end of the tally, and [send] then removes every remaining unused section.
     */
    fun skipWinAnimation() {
        val score = state.finalScore ?: return
        if (state.outcome !is Outcome.Won) return
        val end = WinTiming.FINISH_EFFECT_MS + WinTally.totalDurationMs(score, state.finalStars)
        val remaining = end - winAgeMs
        if (remaining > 0) send(GameCommand.Tick(remaining))
    }

    /** The attempt that was recorded and what recording it did; the win path records each attempt once. */
    private var recorded: Pair<Int, RecordResult>? by mutableStateOf(null)

    /** What recording this attempt's win did to the history; null until then, and on any other attempt. */
    val recordResult: RecordResult? get() = recorded?.takeIf { it.first == state.attempt }?.second

    /**
     * Records this attempt in [history] if it was won: the final total with the stars the level's
     * thresholds give it. A leak or a game still running records nothing, and an attempt is
     * recorded once. A recording that has started finishes even if the caller is cancelled.
     */
    suspend fun recordWin(history: LevelHistoryRepository) {
        val won = state
        val score = won.finalScore ?: return
        if (won.outcome !is Outcome.Won || recorded?.first == won.attempt) return
        val result = withContext(NonCancellable) { history.recordCompletion(won.level.id, score.total, won.finalStars) }
        recorded = won.attempt to result
    }

    private fun winAgeOf(state: GameState): Long = (state.outcome as? Outcome.Won)?.let { state.clockMs - it.atMs } ?: 0L

    private fun winFrameOf(state: GameState): WinFrame? {
        val score = state.finalScore ?: return null
        if (state.outcome !is Outcome.Won) return null
        return winFrame(winAgeOf(state) - WinTiming.FINISH_EFFECT_MS, score, state.finalStars)
    }
}

/** What a [GameSession.place] did. */
enum class PlaceResult {
    /** A section was placed on an empty tile. */
    Placed,

    /** A section took the place of one the tile held, which is destroyed. */
    Replaced,

    /** The tile refused the section; nothing changed. */
    Rejected,
}

object LeakTiming {
    /** How long the leak is shown on the field before the level lost dialog appears. */
    const val EFFECT_MS = 1_000L
}
