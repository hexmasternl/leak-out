package nl.hexmaster.leakout.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import nl.hexmaster.leakout.settings.settingsDataStore
import java.time.Instant

/**
 * What the reminders need to remember between runs: when the app was last opened (the window is
 * planned from it, also after a reboot) and whether the notification permission was asked for.
 */
interface ReminderStateStore {
    /** When the app was last opened, or null when it never was. */
    suspend fun lastOpened(): Instant?

    suspend fun setLastOpened(at: Instant)

    /** Whether the notification permission has been asked for on the welcome screen already. */
    suspend fun permissionAsked(): Boolean

    suspend fun setPermissionAsked()
}

/** The state in the app's shared settings DataStore, next to the settings. */
class DataStoreReminderStateStore(private val dataStore: DataStore<Preferences>) : ReminderStateStore {
    /** The preference keys; renaming one loses the stored value. */
    object Keys {
        val LastOpenedEpochMillis = longPreferencesKey("reminders_last_opened_epoch_millis")
        val PermissionAsked = booleanPreferencesKey("reminders_permission_asked")
    }

    override suspend fun lastOpened(): Instant? = dataStore.data.first()[Keys.LastOpenedEpochMillis]?.let(Instant::ofEpochMilli)

    override suspend fun setLastOpened(at: Instant) {
        dataStore.edit { it[Keys.LastOpenedEpochMillis] = at.toEpochMilli() }
    }

    override suspend fun permissionAsked(): Boolean = dataStore.data.first()[Keys.PermissionAsked] ?: false

    override suspend fun setPermissionAsked() {
        dataStore.edit { it[Keys.PermissionAsked] = true }
    }

    companion object {
        fun create(context: Context) = DataStoreReminderStateStore(context.applicationContext.settingsDataStore)
    }
}

/** The state in memory, for tests. */
class InMemoryReminderStateStore(
    private var opened: Instant? = null,
    private var asked: Boolean = false,
) : ReminderStateStore {
    override suspend fun lastOpened(): Instant? = opened

    override suspend fun setLastOpened(at: Instant) {
        opened = at
    }

    override suspend fun permissionAsked(): Boolean = asked

    override suspend fun setPermissionAsked() {
        asked = true
    }
}
