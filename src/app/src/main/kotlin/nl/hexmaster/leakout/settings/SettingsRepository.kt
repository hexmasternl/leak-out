package nl.hexmaster.leakout.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The player's stored [Settings]. Audio and haptics read [settings]; the settings screen writes
 * through the setters. Volumes outside 0.0-1.0 are clamped, never rejected.
 */
interface SettingsRepository {
    /** The current settings now and after every change. Missing values read as their defaults. */
    val settings: Flow<Settings>

    suspend fun setSoundEnabled(enabled: Boolean)

    suspend fun setSoundVolume(volume: Float)

    suspend fun setMusicEnabled(enabled: Boolean)

    suspend fun setMusicVolume(volume: Float)

    suspend fun setVibrationEnabled(enabled: Boolean)

    /**
     * The tips the player has seen, now and after every change; none on first launch. Kept apart
     * from [settings] so a tip being closed does not recompose everything that reads the settings.
     */
    val seenTips: Flow<Set<TipId>>

    /** Stores [tip] as seen. Marking a seen tip again changes nothing. */
    suspend fun markTipSeen(tip: TipId)

    /** Makes every tip not seen again. The settings and the level history are left alone. */
    suspend fun resetTips()
}

/**
 * Settings kept in memory, for tests and previews. It clamps like the DataStore repository, so a
 * test cannot pass on it with a value the real store would not give.
 */
class InMemorySettingsRepository(
    initial: Settings = Settings.Default,
    initialSeenTips: Set<TipId> = emptySet(),
) : SettingsRepository {
    private val state = MutableStateFlow(clamped(initial))

    /** The name of every setter called, in order, so tests can prove nothing was written. */
    val writes = mutableListOf<String>()

    override val settings: StateFlow<Settings> = state.asStateFlow()

    override suspend fun setSoundEnabled(enabled: Boolean) = write("soundEnabled") { it.copy(soundEnabled = enabled) }

    override suspend fun setSoundVolume(volume: Float) =
        write("soundVolume") { it.copy(soundVolume = Settings.clampVolume(volume)) }

    override suspend fun setMusicEnabled(enabled: Boolean) = write("musicEnabled") { it.copy(musicEnabled = enabled) }

    override suspend fun setMusicVolume(volume: Float) =
        write("musicVolume") { it.copy(musicVolume = Settings.clampVolume(volume)) }

    override suspend fun setVibrationEnabled(enabled: Boolean) =
        write("vibrationEnabled") { it.copy(vibrationEnabled = enabled) }

    private val tips = MutableStateFlow(initialSeenTips)

    override val seenTips: StateFlow<Set<TipId>> = tips.asStateFlow()

    override suspend fun markTipSeen(tip: TipId) {
        writes += "markTipSeen"
        tips.update { it + tip }
    }

    override suspend fun resetTips() {
        writes += "resetTips"
        tips.value = emptySet()
    }

    private fun write(name: String, change: (Settings) -> Settings) {
        writes += name
        state.update(change)
    }

    private companion object {
        fun clamped(settings: Settings) = settings.copy(
            soundVolume = Settings.clampVolume(settings.soundVolume),
            musicVolume = Settings.clampVolume(settings.musicVolume),
        )
    }
}
