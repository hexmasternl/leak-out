package nl.hexmaster.leakout.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.settings.SettingsRepository

/**
 * Owns the app's [soundEffects] for as long as the activity lives, so a configuration change does
 * not reload the sounds. It keeps them in step with the stored settings and frees the player when cleared.
 */
class SoundEffectsViewModel(
    settings: SettingsRepository,
    catalog: SoundCatalog,
    player: SoundEffectPlayer,
) : ViewModel() {
    val soundEffects = SoundEffects(catalog, player)

    init {
        viewModelScope.launch { settings.settings.collect(soundEffects::apply) }
    }

    override fun onCleared() = soundEffects.release()
}
