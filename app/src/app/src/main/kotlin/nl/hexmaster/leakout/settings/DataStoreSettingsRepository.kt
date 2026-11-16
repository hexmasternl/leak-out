package nl.hexmaster.leakout.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** The name of the app's one preferences file, `files/datastore/settings.preferences_pb`. */
const val SETTINGS_DATASTORE_NAME = "settings"

/**
 * The app's single Preferences DataStore on the file [SETTINGS_DATASTORE_NAME]. Android allows one
 * active DataStore per file per process, so every store that keeps its values in this file
 * (settings, and later the music player's state) takes this instance instead of opening its own.
 * The delegate is top level, so the instance is created once and shared by every Context.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_DATASTORE_NAME)

/**
 * [Settings] in a Preferences [dataStore], one key per field (see [Keys]). A missing key reads as
 * its default and volumes are clamped on write and on read, so a hand-edited or corrupt file
 * cannot put a slider out of range. Other keys in the same file are left alone.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    /** The preference keys. Renaming one loses the stored value, so they are part of the file format. */
    object Keys {
        val SoundEnabled = booleanPreferencesKey("sound_enabled")
        val SoundVolume = floatPreferencesKey("sound_volume")
        val MusicEnabled = booleanPreferencesKey("music_enabled")
        val MusicVolume = floatPreferencesKey("music_volume")
        val VibrationEnabled = booleanPreferencesKey("vibration_enabled")
        val RemindersEnabled = booleanPreferencesKey("reminders_enabled")

        /** The [TipId.key]s of the seen tips. */
        val SeenTips = stringSetPreferencesKey("seen_tips")

        /** True once the player opted out of the exit confirmation dialog. */
        val SkipExitConfirmation = booleanPreferencesKey("skip_exit_confirmation")
    }

    override val settings: Flow<Settings> = dataStore.data
        // An unreadable file reads as the defaults rather than crashing the screen; anything else is a bug.
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::read)
        // The file may change for keys owned by another store; only a change to the settings is passed on.
        .distinctUntilChanged()

    override suspend fun setSoundEnabled(enabled: Boolean) = write { it[Keys.SoundEnabled] = enabled }

    override suspend fun setSoundVolume(volume: Float) = write { it[Keys.SoundVolume] = Settings.clampVolume(volume) }

    override suspend fun setMusicEnabled(enabled: Boolean) = write { it[Keys.MusicEnabled] = enabled }

    override suspend fun setMusicVolume(volume: Float) = write { it[Keys.MusicVolume] = Settings.clampVolume(volume) }

    override suspend fun setVibrationEnabled(enabled: Boolean) = write { it[Keys.VibrationEnabled] = enabled }

    override suspend fun setRemindersEnabled(enabled: Boolean) = write { it[Keys.RemindersEnabled] = enabled }

    override val seenTips: Flow<Set<TipId>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::readSeenTips)
        .distinctUntilChanged()

    // Keys of tips this version does not know are kept, so a downgrade does not show them again.
    override suspend fun markTipSeen(tip: TipId) = write { it[Keys.SeenTips] = it[Keys.SeenTips].orEmpty() + tip.key }

    override suspend fun resetTips() = write { it.remove(Keys.SeenTips) }

    override val skipExitConfirmation: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[Keys.SkipExitConfirmation] ?: false }
        .distinctUntilChanged()

    override suspend fun setSkipExitConfirmation(skip: Boolean) = write { it[Keys.SkipExitConfirmation] = skip }

    private suspend fun write(change: (MutablePreferences) -> Unit) {
        dataStore.edit(change)
    }

    companion object {
        /** The settings stored in [preferences]; a missing key is its default. */
        fun read(preferences: Preferences): Settings = Settings(
            soundEnabled = preferences[Keys.SoundEnabled] ?: Settings.DEFAULT_SOUND_ENABLED,
            soundVolume = Settings.clampVolume(preferences[Keys.SoundVolume] ?: Settings.DEFAULT_SOUND_VOLUME),
            musicEnabled = preferences[Keys.MusicEnabled] ?: Settings.DEFAULT_MUSIC_ENABLED,
            musicVolume = Settings.clampVolume(preferences[Keys.MusicVolume] ?: Settings.DEFAULT_MUSIC_VOLUME),
            vibrationEnabled = preferences[Keys.VibrationEnabled] ?: Settings.DEFAULT_VIBRATION_ENABLED,
            remindersEnabled = preferences[Keys.RemindersEnabled] ?: Settings.DEFAULT_REMINDERS_ENABLED,
        )

        /** The seen tips stored in [preferences]; none when the key is missing, unknown keys ignored. */
        fun readSeenTips(preferences: Preferences): Set<TipId> = TipId.fromKeys(preferences[Keys.SeenTips].orEmpty())

        /** The repository on the app's shared [settingsDataStore]. */
        fun create(context: Context): DataStoreSettingsRepository =
            DataStoreSettingsRepository(context.applicationContext.settingsDataStore)
    }
}
