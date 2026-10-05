package nl.hexmaster.leakout.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The "skip exit confirmation" flag on a real Preferences file and in memory. Each test writes the
 * file at most once: on a Windows JVM DataStore cannot rename over an existing file, so tests that
 * write twice live in the instrumented DataStoreSettingsRepositoryTest.
 */
class SkipExitConfirmationTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    /** A fresh DataStore on the test file. Only one may be open per file, so [closeAll] the previous one first. */
    private fun openStore(): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("settings.preferences_pb") }
    }

    private suspend fun closeAll() {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
        scopes.clear()
    }

    @After
    fun tearDown() = runBlocking { closeAll() }

    @Test
    fun `an empty file does not skip the confirmation`() = runBlocking {
        assertFalse(DataStoreSettingsRepository(openStore()).skipExitConfirmation.first())
    }

    @Test
    fun `setting the flag is read back`() = runBlocking {
        val repository = DataStoreSettingsRepository(openStore())
        repository.setSkipExitConfirmation(true)
        assertTrue(repository.skipExitConfirmation.first())
    }

    @Test
    fun `the flag survives recreating the DataStore`() = runBlocking {
        DataStoreSettingsRepository(openStore()).setSkipExitConfirmation(true)
        closeAll()

        assertTrue(DataStoreSettingsRepository(openStore()).skipExitConfirmation.first())
    }

    @Test
    fun `in memory repository starts not skipping and records the write`() = runBlocking {
        val repository = InMemorySettingsRepository()
        assertFalse(repository.skipExitConfirmation.first())
        repository.setSkipExitConfirmation(true)
        assertTrue(repository.skipExitConfirmation.first())
        assertEquals(listOf("skipExitConfirmation"), repository.writes)
    }
}
