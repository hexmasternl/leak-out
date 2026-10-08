package nl.hexmaster.leakout.haptics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.settings.SettingsRepository

/**
 * Owns the app's [haptics] for as long as the activity lives and keeps them in step with the
 * stored vibration setting. A running vibration stops when cleared.
 */
class HapticsViewModel(settings: SettingsRepository, player: HapticPlayer?) : ViewModel() {
    val haptics = Haptics(player)

    init {
        viewModelScope.launch { settings.settings.collect(haptics::apply) }
    }

    override fun onCleared() = haptics.stop()
}
