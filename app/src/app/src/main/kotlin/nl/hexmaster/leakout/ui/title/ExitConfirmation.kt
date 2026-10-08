package nl.hexmaster.leakout.ui.title

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import nl.hexmaster.leakout.settings.SettingsRepository

/**
 * Decides whether the welcome screen's Exit asks first, and stores the player's "Never show this
 * again". Neither call throws: a failed read asks (the safe default) and a failed write is
 * dropped, so the player can always leave.
 */
class ExitConfirmation(private val settings: SettingsRepository) {

    /** True when Exit must show the dialog: a fresh snapshot of the stored opt-out, read at tap time. */
    suspend fun shouldAsk(): Boolean = try {
        !settings.skipExitConfirmation.first()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        true
    }

    /**
     * Records the player's answer on Yes or Cancel. Only a ticked [neverShowAgain] is stored, and
     * the call returns once it is written, so Yes can exit right after without losing it.
     */
    suspend fun answered(neverShowAgain: Boolean) {
        if (!neverShowAgain) return
        try {
            settings.setSkipExitConfirmation(true)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Not saved: the dialog shows again next time, which is the safe side.
        }
    }
}
