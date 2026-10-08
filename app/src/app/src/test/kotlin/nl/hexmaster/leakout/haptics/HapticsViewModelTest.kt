package nl.hexmaster.leakout.haptics

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
class HapticsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val player = RecordingHapticPlayer()

    @Test
    fun `the stored vibration setting reaches the haptics`() = runTest {
        val settings = InMemorySettingsRepository(Settings(vibrationEnabled = true))
        val vm = HapticsViewModel(settings, player)
        vm.haptics.play(HapticEffect.SectionReplaced)
        settings.setVibrationEnabled(false)
        vm.haptics.play(HapticEffect.SectionReplaced)
        assertEquals(1, player.vibrations.size)
        assertEquals(1, player.cancels)
    }

    @Test
    fun `a stored off is followed from the start`() = runTest {
        val vm = HapticsViewModel(InMemorySettingsRepository(Settings(vibrationEnabled = false)), player)
        vm.haptics.play(HapticEffect.LevelLost)
        assertEquals(0, player.vibrations.size)
    }
}
