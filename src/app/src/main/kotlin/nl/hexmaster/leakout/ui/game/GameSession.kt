package nl.hexmaster.leakout.ui.game

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import nl.hexmaster.leakout.achievements.AchievementTracker
import nl.hexmaster.leakout.achievements.toRecordOutcome
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.core.achievements.AttemptSummary
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
import nl.hexmaster.leakout.scores.online.ScorePostStatus
import nl.hexmaster.leakout.scores.online.ScoreSubmissions
import nl.hexmaster.leakout.settings.TipId

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
        // A new attempt (restart) drops the tips of the old one; its own level-start tips are looked at again.
        if (next.attempt != before.attempt) clearTips()
        // The achievements are decided from the board at the moment the attempt ended.
        if (before.outcome is Outcome.Playing && next.outcome !is Outcome.Playing) ended = next
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
     * While a replacement runs nothing is placed: the tap is sent as a [GameCommand.ReplacementTap],
     * which only counts taps on the replaced tile, and the result is still a (silent) refusal.
     */
    fun place(at: Position): PlaceResult {
        if (isReplacing) {
            send(GameCommand.ReplacementTap(at))
            return PlaceResult.Rejected
        }
        val held = state.board[at] is Cell.Pipe
        return when {
            !send(GameCommand.Place(at)) -> PlaceResult.Rejected
            held -> PlaceResult.Replaced
            else -> PlaceResult.Placed
        }
    }

    /** Whether the next section can be dropped on [at] now: the same check a place command runs. */
    fun canPlace(at: Position): Boolean = engine.canPlace(state, at)

    /**
     * The pause dialog is open while the game is paused during play, except for the freeze of a
     * tip; the player pausing during a tip (by leaving the app) opens it over the tip.
     */
    val showPauseDialog: Boolean get() = state.paused && state.outcome is Outcome.Playing && (activeTip == null || pausedByUser)

    /** The tips waiting to be shown (game-play-onboarding); the first one is on screen. */
    var tipQueue: List<TipId> by mutableStateOf(emptyList())
        private set

    /** The tip on screen, or null. The game is frozen while there is one. */
    val activeTip: TipId? get() = tipQueue.firstOrNull()

    /** The step of the intro tour on screen, 0 to [INTRO_STEPS] − 1. */
    var tourStep: Int by mutableIntStateOf(0)
        private set

    /** Whether the player paused (left the app) while a tip was shown, so the pause dialog shows over it. */
    var pausedByUser: Boolean by mutableStateOf(false)
        private set

    /**
     * The attempt whose level-start tips have been looked at. Until then the game screen holds the
     * clock, so the countdown cannot move before the intro tour shows.
     */
    var tipsCheckedAttempt: Int? by mutableStateOf(null)
        private set

    /** Tips closed in this session, so one is never shown twice while its "seen" is still being stored. */
    var closedTips: Set<TipId> by mutableStateOf(emptySet())
        private set

    /**
     * Shows the [due] tips that were not closed yet, one after the other, freezing the game with a
     * pause. [atLevelStart] marks the attempt's level-start tips as looked at, even when none is due.
     * Nothing happens while a tip is already shown, after the attempt has ended or while paused.
     */
    fun offerTips(due: List<TipId>, atLevelStart: Boolean = false) {
        if (atLevelStart) tipsCheckedAttempt = state.attempt
        if (activeTip != null || state.outcome !is Outcome.Playing || state.paused) return
        val fresh = due.filter { it !in closedTips }
        if (fresh.isEmpty()) return
        tipQueue = fresh
        tourStep = 0
        send(GameCommand.Pause)
    }

    /** "Next" on the intro tour. */
    fun nextTourStep() {
        if (activeTip == TipId.Intro && tourStep < INTRO_STEPS - 1) tourStep++
    }

    /**
     * Closes the tip on screen ("Got it", "Let's go", "Skip tour" or Back) and returns it, so the
     * caller can store it as seen. After the last due tip the game continues where it froze.
     */
    fun closeTip(): TipId? {
        val tip = activeTip ?: return null
        closedTips = closedTips + tip
        tipQueue = tipQueue.drop(1)
        tourStep = 0
        if (tipQueue.isEmpty() && !pausedByUser) send(GameCommand.Resume)
        return tip
    }

    /** The player pauses (pause button, Back, leaving the app). During a tip the game is already frozen. */
    fun pauseByUser() {
        if (activeTip != null) {
            if (state.outcome is Outcome.Playing) pausedByUser = true
        } else {
            send(GameCommand.Pause)
        }
    }

    /** Resume in the pause dialog: back to the tip, still frozen, or back to the game. */
    fun resumeByUser() {
        val tipShown = activeTip != null
        pausedByUser = false
        if (!tipShown) send(GameCommand.Resume)
    }

    private fun clearTips() {
        tipQueue = emptyList()
        tourStep = 0
        pausedByUser = false
    }

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
        val end = winEffectsEndMs() ?: return
        val remaining = end - winAgeMs
        if (remaining > 0) send(GameCommand.Tick(remaining))
    }

    /** The win age at which the finish flash and the tally have both ended; null unless won. */
    private fun winEffectsEndMs(): Long? {
        val score = state.finalScore ?: return null
        if (state.outcome !is Outcome.Won) return null
        return WinTiming.FINISH_EFFECT_MS + WinTally.totalDurationMs(score, state.finalStars)
    }

    /**
     * Whether the game screen must keep ticking the clock (spec `game-play-frame-budget`): always
     * while playing, and after the end only until every effect driven by the clock has finished:
     * the leak effect, or the finish flash and the tally, and the last points popup. After that the
     * screen is still, and a tick would only redraw the same frame.
     */
    val needsClock: Boolean
        get() = when (state.outcome) {
            Outcome.Playing -> true
            is Outcome.Lost -> lossAgeMs < LeakTiming.EFFECT_MS || state.awards.isNotEmpty()
            is Outcome.Won -> winAgeMs < (winEffectsEndMs() ?: 0L) || state.awards.isNotEmpty()
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

    /** The attempt whose win was handed to the score server, and how its sending goes (null: nothing is sent). */
    private var postedScore: Pair<Int, StateFlow<ScorePostStatus>?>? by mutableStateOf(null)

    /** How the sending of this attempt's score goes; null when it is not sent, and on any other attempt. */
    val scorePost: StateFlow<ScorePostStatus>? get() = postedScore?.takeIf { it.first == state.attempt }?.second

    /**
     * Hands this attempt's final total to [scores] if it was won, once per attempt however often it
     * is called (coming back from the background shows the dialog again). A restart is a new attempt.
     */
    fun submitScore(scores: ScoreSubmissions) {
        val won = state
        val score = won.finalScore ?: return
        if (won.outcome !is Outcome.Won || postedScore?.first == won.attempt) return
        postedScore = won.attempt to scores.submitWin(won.level.id, score.total)
    }

    /** The state at the moment the last attempt was first won or lost; null while none has ended. */
    private var ended: GameState? = null

    /** The attempt whose end was handed to the achievement tracker, and what it unlocked once known. */
    private var trackedAttempt: Int? = null
    private var unlocked: Pair<Int, List<Achievement>>? by mutableStateOf(null)

    /** The unfinished attempt whose placements were handed to the tracker on a restart or exit. */
    private var abandonedAttempt: Int? = null

    /**
     * What this attempt's end unlocked, for the result dialogs; empty until the tracker has answered,
     * and on any other attempt.
     */
    val unlockedThisAttempt: List<Achievement>
        get() = unlocked?.takeIf { it.first == state.attempt }?.second.orEmpty()

    /**
     * Hands this attempt's end to [tracker] once it is won or lost, exactly once per attempt however
     * often it is called. Call it after [recordWin], so a win's [recordResult] and the updated
     * history are known. A tracking that has started finishes even if the caller is cancelled.
     */
    suspend fun recordAchievements(tracker: AchievementTracker) {
        val end = ended?.takeIf { it.attempt == state.attempt } ?: state.takeIf { it.outcome !is Outcome.Playing } ?: return
        if (trackedAttempt == end.attempt) return
        trackedAttempt = end.attempt
        val record = recorded?.takeIf { it.first == end.attempt }?.second?.toRecordOutcome()
        withContext(NonCancellable) {
            unlocked = end.attempt to tracker.onAttemptEnded(AttemptSummary.from(end, record))
        }
    }

    /**
     * The player restarts or exits this attempt before it ended: its placements go to [tracker]
     * once. An attempt that was won or lost was already counted, so nothing happens then.
     */
    fun abandonAttempt(tracker: AchievementTracker?) {
        val current = state
        if (current.outcome !is Outcome.Playing || abandonedAttempt == current.attempt) return
        abandonedAttempt = current.attempt
        tracker?.attemptAbandoned(current.placements)
    }

    /** Restart (pause dialog or a result dialog): an unfinished attempt is first counted as abandoned. */
    fun restart(tracker: AchievementTracker?): Boolean {
        abandonAttempt(tracker)
        return send(GameCommand.Restart)
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

/** The number of steps of the intro tour: start, finish, stack, placing, zoom and pan. */
const val INTRO_STEPS = 5

object LeakTiming {
    /** How long the leak is shown on the field before the level lost dialog appears. */
    const val EFFECT_MS = 1_000L
}
