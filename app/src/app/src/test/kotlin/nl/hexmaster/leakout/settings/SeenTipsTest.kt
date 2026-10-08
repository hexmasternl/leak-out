package nl.hexmaster.leakout.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeenTipsTest {

    @Test
    fun `on first launch no tip is seen`() = runTest {
        assertEquals(emptySet<TipId>(), InMemorySettingsRepository().seenTips.first())
        assertEquals(emptySet<TipId>(), DataStoreSettingsRepository.readSeenTips(mutablePreferencesOf()))
    }

    @Test
    fun `a tip marked seen is seen and the others are not`() = runTest {
        val repository = InMemorySettingsRepository()
        repository.markTipSeen(TipId.Cross)
        assertEquals(setOf(TipId.Cross), repository.seenTips.first())
    }

    @Test
    fun `marking a tip seen twice changes nothing else`() = runTest {
        val repository = InMemorySettingsRepository()
        repository.markTipSeen(TipId.Reservoir)
        repository.markTipSeen(TipId.Reservoir)
        assertEquals(setOf(TipId.Reservoir), repository.seenTips.first())
    }

    @Test
    fun `reset makes every tip not seen and observers receive it`() = runTest {
        val repository = InMemorySettingsRepository(initialSeenTips = setOf(TipId.Intro, TipId.Cross))
        val next = async(start = CoroutineStart.UNDISPATCHED) { repository.seenTips.first { it.isEmpty() } }
        repository.resetTips()
        assertTrue(next.await().isEmpty())
    }

    @Test
    fun `reset leaves the settings alone`() = runTest {
        val repository = InMemorySettingsRepository(Settings(vibrationEnabled = false), setOf(TipId.Intro))
        repository.resetTips()
        assertEquals(Settings(vibrationEnabled = false), repository.settings.first())
        assertEquals(listOf("resetTips"), repository.writes)
    }

    @Test
    fun `stored keys are read as tips and unknown keys are ignored`() {
        val preferences = mutablePreferencesOf(DataStoreSettingsRepository.Keys.SeenTips to setOf("intro", "reservoir", "speed_ramp"))
        assertEquals(setOf(TipId.Intro, TipId.Reservoir), DataStoreSettingsRepository.readSeenTips(preferences))
    }

    @Test
    fun `seen tips do not change the settings read from the same file`() {
        val preferences = mutablePreferencesOf(DataStoreSettingsRepository.Keys.SeenTips to setOf("intro"))
        assertEquals(Settings.Default, DataStoreSettingsRepository.read(preferences))
    }

    @Test
    fun `the stored keys are the file format`() {
        assertEquals("seen_tips", DataStoreSettingsRepository.Keys.SeenTips.name)
        assertEquals(listOf("intro", "mandatory", "portal", "cross", "reservoir"), TipId.entries.map { it.key })
    }
}
