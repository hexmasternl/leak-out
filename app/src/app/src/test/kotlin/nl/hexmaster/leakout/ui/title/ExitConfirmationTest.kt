package nl.hexmaster.leakout.ui.title

import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.settings.InMemorySettingsRepository
import nl.hexmaster.leakout.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExitConfirmationTest {

    @Test
    fun `asks when the player has not opted out`() = runTest {
        assertTrue(ExitConfirmation(InMemorySettingsRepository()).shouldAsk())
    }

    @Test
    fun `exits straight away once the player opted out`() = runTest {
        val settings = InMemorySettingsRepository().apply { setSkipExitConfirmation(true) }
        assertFalse(ExitConfirmation(settings).shouldAsk())
    }

    @Test
    fun `a failed read asks`() = runTest {
        assertTrue(ExitConfirmation(BrokenSettings()).shouldAsk())
    }

    @Test
    fun `a checked answer is stored and the next exit does not ask`() = runTest {
        val settings = InMemorySettingsRepository()
        val confirmation = ExitConfirmation(settings)
        confirmation.answered(neverShowAgain = true)
        assertTrue(settings.skipExitConfirmation.first())
        assertFalse(confirmation.shouldAsk())
    }

    @Test
    fun `an unchecked answer stores nothing and the next exit asks again`() = runTest {
        val settings = InMemorySettingsRepository()
        val confirmation = ExitConfirmation(settings)
        confirmation.answered(neverShowAgain = false)
        assertEquals(emptyList<String>(), settings.writes)
        assertTrue(confirmation.shouldAsk())
    }

    @Test
    fun `a failed write is dropped so the player can still leave`() = runTest {
        ExitConfirmation(BrokenSettings()).answered(neverShowAgain = true)
    }

    /** Settings whose exit flag can be neither read nor written. */
    private class BrokenSettings(
        private val delegate: SettingsRepository = InMemorySettingsRepository(),
    ) : SettingsRepository by delegate {
        override val skipExitConfirmation: Flow<Boolean> = flow { throw IOException("unreadable") }

        override suspend fun setSkipExitConfirmation(skip: Boolean) = throw IOException("unwritable")
    }
}
