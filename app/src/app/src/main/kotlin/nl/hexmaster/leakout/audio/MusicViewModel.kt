package nl.hexmaster.leakout.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.settings.SettingsRepository

/**
 * Owns the app's [music] for as long as the activity lives, so a configuration change neither
 * restarts the track nor advances the rotation. It keeps the music in step with the stored
 * settings and frees the player when cleared. The activity drives [BackgroundMusic.onForeground]
 * and [BackgroundMusic.onBackground] from its lifecycle.
 */
class MusicViewModel(
    settings: SettingsRepository,
    store: MusicRotationStore,
    playlist: Playlist?,
    engine: MusicEngine,
    focus: AudioFocus,
) : ViewModel() {
    val music = BackgroundMusic(playlist, store, engine, focus, viewModelScope)

    init {
        viewModelScope.launch { settings.settings.collect(music::apply) }
    }

    override fun onCleared() = music.release()
}
