package nl.hexmaster.leakout.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.flow.Flow
import nl.hexmaster.leakout.core.update.UpdateStatus
import nl.hexmaster.leakout.core.update.classifyUpdate

/**
 * What Google Play said about a newer version (openspec/specs/app-updates).
 *
 * @param immediateInProgress an immediate update was started and left unfinished; it is resumed on return.
 * @param downloaded an accepted optional update has downloaded and waits to be installed.
 */
data class UpdateCheck(
    val available: Boolean = false,
    val priority: Int = 0,
    val immediateInProgress: Boolean = false,
    val downloaded: Boolean = false,
) {
    val status: UpdateStatus get() = classifyUpdate(available, priority)

    companion object {
        /** No update, and what every failed or impossible check turns into. */
        val NONE = UpdateCheck()
    }
}

/** The progress of an accepted optional (flexible) update. */
enum class DownloadState { Downloading, Downloaded, Failed }

/** Google Play In-App Updates, behind an interface so screens can be tested with [FakeAppUpdates]. */
interface AppUpdates {
    /** Asks Play for a newer version. Never throws: a failed check is [UpdateCheck.NONE]. */
    suspend fun check(): UpdateCheck

    /** Opens Play's immediate (full screen) update flow for the last checked update; false when it could not start. */
    fun startRequiredUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean

    /** Opens Play's flexible (background download) update flow for the last checked update; false when it could not start. */
    fun startOptionalUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean

    /** Installs a downloaded optional update, which restarts the app. */
    suspend fun completeUpdate()

    /** The download progress of an accepted optional update. */
    val downloads: Flow<DownloadState>
}
