package nl.hexmaster.leakout.update

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.core.update.UpdateGate
import nl.hexmaster.leakout.core.update.UpdateStatus

/** A Play update flow the activity should open, since only it holds the result launcher. */
enum class UpdateFlow { Required, Optional }

/**
 * Checks Google Play for updates whenever the activity starts (app start and every return to the
 * foreground) and keeps the [UpdateGate] the screens follow (openspec/specs/app-updates). Observe
 * it on the activity's lifecycle.
 *
 * @param gateState the process-wide gate, so the optional prompt is offered once per process even
 *   when the activity is recreated.
 */
class UpdateViewModel(
    private val updates: AppUpdates,
    private val gateState: MutableStateFlow<UpdateGate> = MutableStateFlow(UpdateGate()),
) : ViewModel(), DefaultLifecycleObserver {
    val gate: StateFlow<UpdateGate> = gateState.asStateFlow()

    private val flowRequests = Channel<UpdateFlow>(Channel.CONFLATED)

    /** The Play flows to open; collect while the activity is resumed. */
    val requests: Flow<UpdateFlow> = flowRequests.receiveAsFlow()

    private var latest = UpdateCheck.NONE
    private var resumed = false
    private var checking: Job? = null

    init {
        viewModelScope.launch {
            updates.downloads.collect { state ->
                if (state == DownloadState.Downloaded) gateState.update { it.optionalDownloaded() }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) = check()

    override fun onResume(owner: LifecycleOwner) {
        resumed = true
        // While a check runs, its result decides instead; it may be newer than [latest].
        if (checking?.isActive != true) resumeUpdateInProgress()
    }

    override fun onPause(owner: LifecycleOwner) {
        resumed = false
    }

    /** Asks Play again; one check at a time. A failure, even a thrown one, means no update. */
    fun check() {
        if (checking?.isActive == true) return
        checking = viewModelScope.launch {
            latest = try {
                updates.check()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UpdateCheck.NONE
            }
            gateState.update { gate ->
                gate.checked(latest.status).let { if (latest.downloaded) it.optionalDownloaded() else it }
            }
            resumeUpdateInProgress()
        }
    }

    // An immediate update the player started and left is picked up again, as Play asks apps to do.
    private fun resumeUpdateInProgress() {
        val gate = gateState.value
        if (resumed && latest.immediateInProgress && latest.status == UpdateStatus.Required && !gate.inLevel) {
            flowRequests.trySend(UpdateFlow.Required)
        }
    }

    /** Update on the blocking screen. */
    fun updateNow() {
        flowRequests.trySend(UpdateFlow.Required)
    }

    /** A Play flow came back, completed or cancelled: the next check tells which. */
    fun onFlowResult() = check()

    fun acceptOptional() {
        gateState.update { it.optionalPromptClosed() }
        flowRequests.trySend(UpdateFlow.Optional)
    }

    fun dismissOptional() = gateState.update { it.optionalPromptClosed() }

    /** Installs the downloaded optional update; Play restarts the app. */
    fun install() {
        gateState.update { it.installPromptClosed() }
        viewModelScope.launch { updates.completeUpdate() }
    }

    fun dismissInstall() = gateState.update { it.installPromptClosed() }

    /** Whether the game screen is showing, which holds back every update screen and prompt. */
    fun setInLevel(inLevel: Boolean) = gateState.update { if (inLevel) it.levelEntered() else it.levelLeft() }
}
