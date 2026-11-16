package nl.hexmaster.leakout.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.achievements.AchievementProgress
import nl.hexmaster.leakout.achievements.AchievementRepository
import nl.hexmaster.leakout.achievements.InMemoryAchievementRepository
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.audio.Playlist
import nl.hexmaster.leakout.audio.Track
import nl.hexmaster.leakout.history.LevelHistory
import nl.hexmaster.leakout.history.LevelHistoryRepository
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.settings.SettingsRepository

/** The installed version, read once from the package manager by the host and shown in the About panel. */
data class AppInfo(val versionName: String, val versionCode: Long)

/** What the level history adds up to: the levels with a record and the stars stored with them. */
data class ProgressSummary(val levelsFinished: Int, val starsEarned: Int) {
    /** There is something to reset. */
    val hasProgress: Boolean get() = levelsFinished > 0

    companion object {
        val None = ProgressSummary(0, 0)

        fun of(records: List<LevelHistory>): ProgressSummary = ProgressSummary(records.size, records.sumOf { it.stars })
    }
}

/**
 * What the Play Games panel shows: the sign-in state and how many of the [total] catalogue
 * achievements are [unlocked], from the local store whatever the sign-in state.
 */
data class PlayGamesUiState(val signIn: SignInState, val unlocked: Int, val total: Int) {
    companion object {
        val Unavailable = PlayGamesUiState(SignInState.Unavailable, 0, 0)

        fun of(signIn: SignInState, achievements: List<AchievementProgress>): PlayGamesUiState =
            PlayGamesUiState(signIn, achievements.count { it.unlocked }, achievements.size)
    }
}

/**
 * What the settings screen shows: the stored settings, the progress summary and the background
 * tracks to credit on the About panel, in playlist order (empty when the credits cannot be read).
 * [tips] drives the Show tips again button; [playGames] the Play Games panel.
 */
data class SettingsUiState(
    val settings: Settings,
    val progress: ProgressSummary,
    val musicCredits: List<Track> = emptyList(),
    val tips: TipsState = TipsState.None,
    val playGames: PlayGamesUiState = PlayGamesUiState.Unavailable,
)

/** Whether any tip has been seen (there is something to show again), and whether this screen just reset them. */
data class TipsState(val anySeen: Boolean, val justReset: Boolean = false) {
    companion object {
        val None = TipsState(anySeen = false)
    }
}

/**
 * Combines the stored settings and the level history into [uiState], which is null until both have
 * been read once so the panels never flash the defaults over the real values. [musicCredits] is the
 * bundled playlist, or null when it could not be read. The Play Games panel follows [signIn] and
 * counts the unlocked [achievements]. [onTipsReset] is called after every "Show tips again", for the
 * achievements (Tips Addict).
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: LevelHistoryRepository,
    musicCredits: Playlist? = null,
    private val onProgressReset: suspend () -> Unit = {},
    achievements: AchievementRepository = InMemoryAchievementRepository(),
    signIn: StateFlow<SignInState> = MutableStateFlow(SignInState.Unavailable),
    private val onTipsReset: suspend () -> Unit = {},
    private val onRemindersChanged: suspend (enabled: Boolean) -> Unit = {},
) : ViewModel() {
    private val credits = musicCredits?.tracks.orEmpty()
    private val tipsJustReset = MutableStateFlow(false)

    private val playGames = combine(signIn, achievements.observeAll(), PlayGamesUiState::of)

    val uiState: StateFlow<SettingsUiState?> =
        combine(
            settingsRepository.settings,
            historyRepository.observeAll().map(ProgressSummary::of),
            settingsRepository.seenTips,
            tipsJustReset,
            playGames,
        ) { settings, progress, seenTips, justReset, playGames ->
            SettingsUiState(settings, progress, credits, TipsState(seenTips.isNotEmpty(), justReset && seenTips.isEmpty()), playGames)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Makes every tip show again; the level history and the settings are left alone. */
    fun onShowTipsAgain() {
        viewModelScope.launch {
            settingsRepository.resetTips()
            tipsJustReset.value = true
            onTipsReset()
        }
    }

    /** Stores the vibration preference. Nothing vibrates yet; the haptic tick will read it. */
    fun onVibrationChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
    }

    /**
     * Stores the reminders preference, then calls [onRemindersChanged] so the pending reminders are
     * cancelled (off) or scheduled again (on) straight away.
     */
    fun onRemindersChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
            onRemindersChanged(enabled)
        }
    }

    /** Turns the sound effects on or off; the next effect follows it. */
    fun onSoundEnabledChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    /** Stores the sound effects volume, once the slider is released. */
    fun onSoundVolumeChange(volume: Float) {
        viewModelScope.launch { settingsRepository.setSoundVolume(volume) }
    }

    /** Turns the background music on or off; the playing music follows straight away. */
    fun onMusicEnabledChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMusicEnabled(enabled) }
    }

    /** Stores the music volume, once the slider is released. The live value reaches the music through the screen's preview. */
    fun onMusicVolumeChange(volume: Float) {
        viewModelScope.launch { settingsRepository.setMusicVolume(volume) }
    }

    /**
     * Deletes the whole level history, then calls [onProgressReset] (the achievements look at the
     * history again; they are kept, so nothing is lost). The settings are kept on purpose.
     */
    fun onResetConfirmed() {
        viewModelScope.launch {
            historyRepository.clearAll()
            onProgressReset()
        }
    }
}
