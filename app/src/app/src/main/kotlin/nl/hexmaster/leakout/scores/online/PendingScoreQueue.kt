package nl.hexmaster.leakout.scores.online

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Scores waiting to be sent to the score server: at most one per level, the highest. Kept on the
 * device, so scores won offline survive a restart until they are sent.
 */
interface PendingScoreQueue {
    /** Every queued score, by level id. */
    val pending: Flow<Map<Int, Int>>

    /** Queues [score] for [level], unless a score at least as high is already queued. */
    suspend fun offer(level: Int, score: Int)

    /** Removes [level] only while its queued score is still [score]: a higher one queued since stays. */
    suspend fun remove(level: Int, score: Int)

    suspend fun snapshot(): Map<Int, Int> = pending.first()
}

/** [PendingScoreQueue] in its own Preferences DataStore file, one `level_<id>` key per level. */
class DataStorePendingScoreQueue(private val dataStore: DataStore<Preferences>) : PendingScoreQueue {
    override val pending: Flow<Map<Int, Int>> = dataStore.data.map(::read)

    override suspend fun offer(level: Int, score: Int) {
        dataStore.edit { preferences ->
            val key = keyOf(level)
            if ((preferences[key] ?: -1) < score) preferences[key] = score
        }
    }

    override suspend fun remove(level: Int, score: Int) {
        dataStore.edit { preferences ->
            val key = keyOf(level)
            if (preferences[key] == score) preferences.remove(key)
        }
    }

    companion object {
        const val DATASTORE_NAME = "score_queue"
        private const val PREFIX = "level_"

        private fun keyOf(level: Int) = intPreferencesKey("$PREFIX$level")

        /** The queued scores in [preferences]; keys that are not a level are ignored. */
        fun read(preferences: Preferences): Map<Int, Int> = preferences.asMap().entries.mapNotNull { (key, value) ->
            val level = key.name.removePrefix(PREFIX).takeIf { key.name.startsWith(PREFIX) }?.toIntOrNull()
            if (level != null && value is Int) level to value else null
        }.toMap()
    }
}

/** The DataStore behind the app's [DataStorePendingScoreQueue]. */
val Context.scoreQueueDataStore: DataStore<Preferences> by preferencesDataStore(name = DataStorePendingScoreQueue.DATASTORE_NAME)
