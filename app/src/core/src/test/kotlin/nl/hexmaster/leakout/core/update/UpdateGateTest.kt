package nl.hexmaster.leakout.core.update

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateGateTest {
    private val start = UpdateGate()

    @Test
    fun `before any check the player can play and nothing is shown`() {
        assertTrue(start.mayStartLevel())
        assertFalse(start.showBlockingScreen())
        assertFalse(start.showOptionalPrompt())
        assertFalse(start.showInstallPrompt())
    }

    @Test
    fun `a failed check never blocks or prompts`() {
        val gate = start.checked(UpdateStatus.None)
        assertTrue(gate.mayStartLevel())
        assertFalse(gate.showBlockingScreen())
        assertFalse(gate.showOptionalPrompt())
    }

    @Test
    fun `a required update at start blocks every level`() {
        val gate = start.checked(UpdateStatus.Required)
        assertFalse(gate.mayStartLevel())
        assertTrue(gate.showBlockingScreen())
        assertFalse(gate.showOptionalPrompt())
    }

    @Test
    fun `the blocking screen stays after a cancelled update`() {
        // A cancelled Play flow is followed by another check that still finds the required update.
        val gate = start.checked(UpdateStatus.Required).checked(UpdateStatus.Required)
        assertTrue(gate.showBlockingScreen())
    }

    @Test
    fun `a required update found during a level lets the level continue`() {
        val gate = start.checked(UpdateStatus.None).levelEntered().checked(UpdateStatus.Required)
        assertFalse(gate.showBlockingScreen())
        assertFalse(gate.showOptionalPrompt())
    }

    @Test
    fun `next level and restart after a required update are refused`() {
        val gate = start.levelEntered().checked(UpdateStatus.Required)
        assertFalse(gate.mayStartLevel())
    }

    @Test
    fun `leaving the level shows the blocking screen`() {
        val gate = start.levelEntered().checked(UpdateStatus.Required).levelLeft()
        assertTrue(gate.showBlockingScreen())
    }

    @Test
    fun `an optional update at start is offered and leaves the game playable`() {
        val gate = start.checked(UpdateStatus.Optional)
        assertTrue(gate.showOptionalPrompt())
        assertTrue(gate.mayStartLevel())
        assertFalse(gate.showBlockingScreen())
    }

    @Test
    fun `a dismissed optional prompt is not shown again in the same process`() {
        val gate = start.checked(UpdateStatus.Optional).optionalPromptClosed()
        assertFalse(gate.showOptionalPrompt())
        assertTrue(gate.mayStartLevel())
        // Returning to the foreground checks again, but the prompt stays closed.
        assertFalse(gate.checked(UpdateStatus.Optional).showOptionalPrompt())
    }

    @Test
    fun `an optional update found only after start is not offered`() {
        val gate = start.checked(UpdateStatus.None).checked(UpdateStatus.Optional)
        assertFalse(gate.showOptionalPrompt())
    }

    @Test
    fun `the optional prompt waits while a level is played`() {
        val gate = start.levelEntered().checked(UpdateStatus.Optional)
        assertFalse(gate.showOptionalPrompt())
        assertTrue(gate.levelLeft().showOptionalPrompt())
    }

    @Test
    fun `a download finished during a level is offered once the level is left`() {
        val inLevel = start.checked(UpdateStatus.Optional).optionalPromptClosed().levelEntered().optionalDownloaded()
        assertFalse(inLevel.showInstallPrompt())
        assertTrue(inLevel.levelLeft().showInstallPrompt())
    }

    @Test
    fun `a dismissed install prompt is not shown again`() {
        val gate = start.checked(UpdateStatus.Optional).optionalDownloaded().installPromptClosed()
        assertFalse(gate.showInstallPrompt())
    }

    @Test
    fun `a required update outranks a downloaded optional one`() {
        val gate = start.checked(UpdateStatus.Optional).optionalDownloaded().checked(UpdateStatus.Required)
        assertFalse(gate.showInstallPrompt())
        assertTrue(gate.showBlockingScreen())
    }
}
