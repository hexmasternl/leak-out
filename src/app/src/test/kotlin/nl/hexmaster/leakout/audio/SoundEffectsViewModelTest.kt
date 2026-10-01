package nl.hexmaster.leakout.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.settings.Settings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoundEffectsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val catalog = SoundCatalog.from(listOf("level_complete.mp3"))
    private val player = RecordingSoundEffectPlayer()

    @Test
    fun `the stored settings reach the sound effects`() = runTest {
        val settings = InMemorySettingsRepository(Settings(soundVolume = 0.4f))
        val vm = SoundEffectsViewModel(settings, catalog, player)
        vm.soundEffects.play(GameSound.LevelComplete)
        settings.setSoundEnabled(false)
        vm.soundEffects.play(GameSound.LevelComplete)
        assertEquals(listOf(0.4f), player.plays.map { it.volume })
    }
}
