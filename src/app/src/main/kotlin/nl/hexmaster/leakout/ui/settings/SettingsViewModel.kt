package nl.hexmaster.leakout.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
 * What the settings screen shows: the stored settings, the progress summary and the background
 * tracks to credit on the About panel, in playlist order (empty when the credits cannot be read).
 */
data class SettingsUiState(
    val settings: Settings,
    val progress: ProgressSummary,
    val musicCredits: List<Track> = emptyList(),
)

/**
 * Combines the stored settings and the level history into [uiState], which is null until both have
 * been read once so the panels never flash the defaults over the real values. [musicCredits] is the
 * bundled playlist, or null when it could not be read.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: LevelHistoryRepository,
    musicCredits: Playlist? = null,
) : ViewModel() {
    private val credits = musicCredits?.tracks.orEmpty()

    val uiState: StateFlow<SettingsUiState?> =
        combine(settingsRepository.settings, historyRepository.observeAll().map(ProgressSummary::of)) { settings, progress ->
            SettingsUiState(settings, progress, credits)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Stores the vibration preference. Nothing vibrates yet; the haptic tick will read it. */
    fun onVibrationChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
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

    /** Deletes the whole level history. The settings are kept on purpose. */
    fun onResetConfirmed() {
        viewModelScope.launch { historyRepository.clearAll() }
    }
}
