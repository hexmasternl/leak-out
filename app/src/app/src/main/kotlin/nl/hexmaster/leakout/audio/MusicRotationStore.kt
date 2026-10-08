package nl.hexmaster.leakout.audio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import nl.hexmaster.leakout.settings.settingsDataStore
import java.io.IOException

/**
 * The id of the last background track that started, so the next launch begins one track further.
 * App state rather than a setting: Reset progress and the settings screen never touch it.
 */
interface MusicRotationStore {
    /** The stored id, or null when no track has started on this install. */
    suspend fun lastStartedTrackId(): String?

    suspend fun setLastStartedTrackId(id: String)
}

/**
 * The last-started id under [Key] in the app's shared settings DataStore. It takes the instance
 * instead of opening the file itself, because Android allows one DataStore per file per process.
 */
class DataStoreMusicRotationStore(private val dataStore: DataStore<Preferences>) : MusicRotationStore {

    // An unreadable file means "start at the top", never a crash.
    override suspend fun lastStartedTrackId(): String? =
        dataStore.data.catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }.first()[Key]

    override suspend fun setLastStartedTrackId(id: String) {
        dataStore.edit { it[Key] = id }
    }

    companion object {
        /** Part of the file format: renaming it restarts the rotation at the first track. */
        val Key = stringPreferencesKey("music_last_started_track")

        /** The store on the app's shared [settingsDataStore]. */
        fun create(context: Context): DataStoreMusicRotationStore =
            DataStoreMusicRotationStore(context.applicationContext.settingsDataStore)
    }
}

/** The last-started id in memory, for tests. */
class InMemoryMusicRotationStore(private var id: String? = null) : MusicRotationStore {
    /** Every id written, in order, so tests can prove a resume wrote nothing. */
    val writes = mutableListOf<String>()

    override suspend fun lastStartedTrackId(): String? = id

    override suspend fun setLastStartedTrackId(id: String) {
        writes += id
        this.id = id
    }
}
