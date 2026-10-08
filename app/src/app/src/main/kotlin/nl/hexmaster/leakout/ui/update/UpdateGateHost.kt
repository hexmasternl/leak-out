package nl.hexmaster.leakout.ui.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import nl.hexmaster.leakout.update.UpdateViewModel

/**
 * Puts the app's update rules (openspec/specs/app-updates) around its screens: [content] is
 * replaced by the blocking update screen while a required update is known outside a level, the
 * optional prompt shows on the welcome screen and the install prompt on any screen outside a level.
 * [content] gets `mayStartLevel`, which the game screen asks before next level and restart.
 *
 * The host observes the activity's lifecycle for [viewModel] (a check on every start) and opens
 * the Play flows it asks for, so it must be composed for as long as the activity shows.
 *
 * @param inLevel the game screen is showing, which holds back every update screen and prompt.
 * @param onWelcome the welcome screen is showing, the only place the optional prompt appears.
 */
@Composable
fun UpdateGateHost(
    viewModel: UpdateViewModel,
    inLevel: Boolean,
    onWelcome: Boolean,
    onCloseApp: () -> Unit,
    content: @Composable (mayStartLevel: () -> Boolean) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose { lifecycleOwner.lifecycle.removeObserver(viewModel) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onFlowResult()
    }
    LaunchedEffect(lifecycleOwner, viewModel, launcher) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.requests.collect { flow -> viewModel.launch(flow, launcher) }
        }
    }
    SideEffect { viewModel.setInLevel(inLevel) }

    // Decided on this composition's screen, not on the one the view model last heard of.
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    val shown = gate.copy(inLevel = inLevel)
    if (shown.showBlockingScreen()) {
        RequiredUpdateScreen(onUpdate = viewModel::updateNow, onClose = onCloseApp)
        return
    }
    content { viewModel.gate.value.mayStartLevel() }
    when {
        shown.showOptionalPrompt() && onWelcome ->
            OptionalUpdateDialog(onUpdate = viewModel::acceptOptional, onDismiss = viewModel::dismissOptional)
        shown.showInstallPrompt() -> InstallUpdateDialog(onRestart = viewModel::install, onDismiss = viewModel::dismissInstall)
    }
}
