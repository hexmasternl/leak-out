package nl.hexmaster.leakout.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.core.level.Level
import nl.hexmaster.leakout.core.level.LevelParser
import nl.hexmaster.leakout.core.level.LevelValidator
import nl.hexmaster.leakout.core.level.ParseResult
import nl.hexmaster.leakout.scores.online.DailyPipe
import nl.hexmaster.leakout.scores.online.DailyPipeDownload
import nl.hexmaster.leakout.scores.online.DailyPipes
import nl.hexmaster.leakout.scores.online.DailyStreak

/** A downloaded daily pipe whose level parsed and validated, ready to play with its seeded stack. */
data class DailyPipeGame(val date: LocalDate, val level: Level, val stackSeed: Long, val streak: DailyStreak)

/** Why tapping the Daily Pipe button did not open the game. */
enum class DailyPipeProblem {
    /** The server has not published today's pipe yet. */
    NotPublished,

    /** Offline, a timeout or a server error. */
    DownloadFailed,

    /** The level did not parse or validate, for example a newer level format than this app knows. */
    Unsupported,
}

/**
 * What the Daily Pipe button on the welcome screen shows: whether it is there at all (only for a
 * player signed in with Play Games), whether the device is [online], a download in progress, and
 * the [problem] of the last tap, if any.
 */
data class DailyPipeEntryState(
    val visible: Boolean = false,
    val online: Boolean = true,
    val loading: Boolean = false,
    val problem: DailyPipeProblem? = null,
) {
    /** The button can be tapped: shown, online and not already downloading. */
    val enabled: Boolean get() = visible && online && !loading
}

/**
 * The Daily Pipe entry on the welcome screen (spec `daily-pipe`). [open] downloads today's pipe
 * through [dailyPipes], parses it with `LevelParser` and validates it with `LevelValidator`; a pipe
 * that passes lands in [opened] for the host to start, anything else in the state's problem.
 */
class DailyPipeViewModel(
    private val dailyPipes: DailyPipes,
    signIn: Flow<SignInState>,
    online: Flow<Boolean>,
) : ViewModel() {
    private val loading = MutableStateFlow(false)
    private val problem = MutableStateFlow<DailyPipeProblem?>(null)
    private val _opened = MutableStateFlow<DailyPipeGame?>(null)

    val state: StateFlow<DailyPipeEntryState> =
        combine(signIn.map { it is SignInState.SignedIn }, online, loading, problem) { visible, isOnline, isLoading, lastProblem ->
            DailyPipeEntryState(visible, isOnline, isLoading, lastProblem)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, DailyPipeEntryState())

    /** The pipe to start; the host calls [consumeOpened] once it has navigated to it. */
    val opened: StateFlow<DailyPipeGame?> = _opened.asStateFlow()

    /** Downloads today's pipe; does nothing while the button is not enabled. */
    fun open() {
        if (!state.value.enabled) return
        loading.value = true
        problem.value = null
        viewModelScope.launch {
            try {
                when (val download = dailyPipes.today()) {
                    is DailyPipeDownload.Available -> prepare(download.pipe)
                        ?.let { _opened.value = it }
                        ?: run { problem.value = DailyPipeProblem.Unsupported }
                    DailyPipeDownload.NotPublished -> problem.value = DailyPipeProblem.NotPublished
                    DailyPipeDownload.Failed -> problem.value = DailyPipeProblem.DownloadFailed
                }
            } finally {
                loading.value = false
            }
        }
    }

    fun consumeOpened() {
        _opened.value = null
    }

    fun dismissProblem() {
        problem.value = null
    }

    companion object {
        /** The playable game of [pipe], or null when its level does not parse or validate. */
        fun prepare(pipe: DailyPipe): DailyPipeGame? {
            val level = (LevelParser.parse(pipe.levelJson) as? ParseResult.Success)?.level ?: return null
            if (!LevelValidator.validate(level).isValid) return null
            return DailyPipeGame(pipe.date, level, pipe.stackSeed, pipe.streak)
        }
    }
}
