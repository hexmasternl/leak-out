package nl.hexmaster.leakout.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

/** What the settings screen shows: the stored settings and the progress summary. */
data class SettingsUiState(val settings: Settings, val progress: ProgressSummary)

/**
 * Combines the stored settings and the level history into [uiState], which is null until both have
 * been read once so the panels never flash the defaults over the real values.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: LevelHistoryRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState?> =
        combine(settingsRepository.settings, historyRepository.observeAll().map(ProgressSummary::of)) { settings, progress ->
            SettingsUiState(settings, progress)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Stores the vibration preference. Nothing vibrates yet; the haptic tick will read it. */
    fun onVibrationChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
    }

    /** Deletes the whole level history. The settings are kept on purpose. */
    fun onResetConfirmed() {
        viewModelScope.launch { historyRepository.clearAll() }
    }
}
