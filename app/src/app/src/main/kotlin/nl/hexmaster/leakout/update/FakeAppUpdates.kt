package nl.hexmaster.leakout.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * A scripted [AppUpdates] for tests: [next] is what every check returns, [failing] makes the check
 * throw, and the start calls only count. Lives in main, like the in-memory repositories, so both
 * JVM and instrumented tests can use it.
 */
class FakeAppUpdates(@Volatile var next: UpdateCheck = UpdateCheck.NONE) : AppUpdates {
    @Volatile var failing: Boolean = false
    @Volatile var checks = 0
        private set
    @Volatile var requiredStarts = 0
        private set
    @Volatile var optionalStarts = 0
        private set
    @Volatile var completes = 0
        private set

    private val downloadStates = MutableSharedFlow<DownloadState>(extraBufferCapacity = 8)

    override suspend fun check(): UpdateCheck {
        checks++
        if (failing) throw IllegalStateException("update check failed")
        return next
    }

    override fun startRequiredUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        requiredStarts++
        return true
    }

    override fun startOptionalUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        optionalStarts++
        return true
    }

    override suspend fun completeUpdate() {
        completes++
    }

    override val downloads: Flow<DownloadState> = downloadStates

    /** Reports download progress, as Play does after an accepted optional update. */
    fun download(state: DownloadState) {
        downloadStates.tryEmit(state)
    }
}
