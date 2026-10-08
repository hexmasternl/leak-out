package nl.hexmaster.leakout.update

import android.content.ContextWrapper
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.hexmaster.leakout.core.update.UpdateStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PlayAppUpdates] against Play's own [FakeAppUpdateManager]. The Android classes it touches are
 * stubs returning defaults in JVM tests (testOptions.unitTests.isReturnDefaultValues).
 */
class PlayAppUpdatesTest {
    private val context = object : ContextWrapper(null) {
        override fun getPackageName() = "nl.hexmaster.leakout"
    }
    private val fake = FakeAppUpdateManager(context)
    private val failures = mutableListOf<String>()
    private val updates = PlayAppUpdates(fake) { what, _ -> failures += what }

    @Test
    fun `no update available is none`() = runTest {
        fake.setUpdateNotAvailable()
        val check = updates.check()
        assertFalse(check.available)
        assertEquals(UpdateStatus.None, check.status)
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `an update with priority 5 is required`() = runTest {
        fake.setUpdateAvailable(2, AppUpdateType.IMMEDIATE)
        fake.setUpdatePriority(5)
        val check = updates.check()
        assertTrue(check.available)
        assertEquals(5, check.priority)
        assertEquals(UpdateStatus.Required, check.status)
        assertFalse(check.immediateInProgress)
    }

    @Test
    fun `an update with priority 0 is optional`() = runTest {
        fake.setUpdateAvailable(2, AppUpdateType.FLEXIBLE)
        fake.setUpdatePriority(0)
        assertEquals(UpdateStatus.Optional, updates.check().status)
    }

    @Test
    fun `a check that throws is no update and is logged`() = runTest {
        val broken = object : FakeAppUpdateManager(context) {
            override fun getAppUpdateInfo(): Task<AppUpdateInfo> = throw IllegalStateException("Play Store missing")
        }
        val check = PlayAppUpdates(broken) { what, _ -> failures += what }.check()
        assertEquals(UpdateCheck.NONE, check)
        assertEquals(listOf("update check failed"), failures)
    }

    @Test
    fun `no flow starts without a check`() {
        assertFalse(updates.startRequiredUpdate(NoLauncher))
        assertFalse(updates.startOptionalUpdate(NoLauncher))
    }

    @Test
    fun `a flexible download reports downloading and then downloaded`() = runTest {
        // The fake only opens a flow for an info with real PendingIntents, which JVM stubs leave null,
        // so the install states go straight to the listeners PlayAppUpdates registered.
        val listeners = mutableListOf<InstallStateUpdatedListener>()
        val manager = object : FakeAppUpdateManager(context) {
            override fun registerListener(listener: InstallStateUpdatedListener) {
                listeners += listener
            }

            override fun unregisterListener(listener: InstallStateUpdatedListener) {
                listeners -= listener
            }
        }
        val states = mutableListOf<DownloadState>()
        val collecting = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            PlayAppUpdates(manager) { what, _ -> failures += what }.downloads.toList(states)
        }

        for (status in listOf(InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.DOWNLOADED)) {
            listeners.toList().forEach { it.onStateUpdate(installState(status)) }
        }

        assertEquals(listOf(DownloadState.Downloading, DownloadState.Downloading, DownloadState.Downloaded), states)
        collecting.cancel()
        assertEquals("the listener is removed when collection stops", emptyList<InstallStateUpdatedListener>(), listeners)
    }

    @Test
    fun `a failed or cancelled download is reported as failed`() = runTest {
        val listeners = mutableListOf<InstallStateUpdatedListener>()
        val manager = object : FakeAppUpdateManager(context) {
            override fun registerListener(listener: InstallStateUpdatedListener) {
                listeners += listener
            }
        }
        val states = mutableListOf<DownloadState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { PlayAppUpdates(manager).downloads.toList(states) }
        listeners.single().onStateUpdate(installState(InstallStatus.FAILED))
        listeners.single().onStateUpdate(installState(InstallStatus.CANCELED))
        assertEquals(listOf(DownloadState.Failed, DownloadState.Failed), states)
    }

    private fun installState(status: Int) = object : InstallState() {
        override fun installErrorCode() = 0
        override fun installStatus() = status
        override fun bytesDownloaded() = 0L
        override fun totalBytesToDownload() = 0L
        override fun packageName() = "nl.hexmaster.leakout"
    }
}
