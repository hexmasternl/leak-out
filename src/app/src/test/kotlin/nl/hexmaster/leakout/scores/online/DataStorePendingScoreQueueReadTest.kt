package nl.hexmaster.leakout.scores.online

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reading the stored queue. The file itself is tested on a device (androidTest
 * `DataStorePendingScoreQueueTest`): DataStore cannot replace its file on a Windows JVM host.
 */
class DataStorePendingScoreQueueReadTest {
    @Test
    fun `only level keys with a number are read`() {
        val preferences = mutablePreferencesOf(
            intPreferencesKey("level_7") to 50,
            intPreferencesKey("level_12") to 0,
            intPreferencesKey("other") to 1,
            intPreferencesKey("level_x") to 2,
        )
        assertEquals(mapOf(7 to 50, 12 to 0), DataStorePendingScoreQueue.read(preferences))
    }
}
