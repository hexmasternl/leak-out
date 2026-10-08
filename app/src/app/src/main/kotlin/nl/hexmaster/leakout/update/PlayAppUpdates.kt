package nl.hexmaster.leakout.update

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.requestCompleteUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * [AppUpdates] on Google Play's [AppUpdateManager]. Every failure, such as no network, an install
 * that did not come from Play or a missing Play Store, counts as "no update" and never reaches the player.
 *
 * @param logFailure reports a swallowed failure; android.util.Log by default, replaced in JVM tests.
 */
class PlayAppUpdates(
    private val manager: AppUpdateManager,
    private val logFailure: (String, Throwable) -> Unit = { what, error -> Log.w(TAG, what, error) },
) : AppUpdates {
    // A flow needs the info of the check before it; Play allows each info to start one flow.
    @Volatile private var lastInfo: AppUpdateInfo? = null

    override suspend fun check(): UpdateCheck = try {
        val info = manager.requestAppUpdateInfo()
        lastInfo = info
        val availability = info.updateAvailability()
        UpdateCheck(
            available = availability == UpdateAvailability.UPDATE_AVAILABLE ||
                availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            priority = info.updatePriority(),
            immediateInProgress = availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                info.installStatus() != InstallStatus.DOWNLOADED,
            downloaded = info.installStatus() == InstallStatus.DOWNLOADED,
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logFailure("update check failed", e)
        UpdateCheck.NONE
    }

    override fun startRequiredUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean =
        start(launcher, AppUpdateType.IMMEDIATE)

    override fun startOptionalUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean =
        start(launcher, AppUpdateType.FLEXIBLE)

    private fun start(launcher: ActivityResultLauncher<IntentSenderRequest>, type: Int): Boolean {
        val info = lastInfo ?: return false
        return try {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(type)).also { started ->
                if (started) lastInfo = null
            }
        } catch (e: Exception) {
            logFailure("starting the update flow failed", e)
            false
        }
    }

    override suspend fun completeUpdate() {
        try {
            manager.requestCompleteUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFailure("installing the update failed", e)
        }
    }

    override val downloads: Flow<DownloadState> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.PENDING, InstallStatus.DOWNLOADING -> trySend(DownloadState.Downloading)
                InstallStatus.DOWNLOADED -> trySend(DownloadState.Downloaded)
                InstallStatus.FAILED, InstallStatus.CANCELED -> trySend(DownloadState.Failed)
                else -> Unit
            }
        }
        manager.registerListener(listener)
        awaitClose { manager.unregisterListener(listener) }
    }

    companion object {
        private const val TAG = "AppUpdates"

        fun create(context: Context): PlayAppUpdates = PlayAppUpdates(AppUpdateManagerFactory.create(context))
    }
}
