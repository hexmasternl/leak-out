package nl.hexmaster.leakout.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.achievements.playgames.SignInState
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.ui.components.displayUppercase
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Stable handles for UI tests, independent of copy and layout. */
object SettingsTags {
    const val SCREEN = "settings_screen"
    const val TITLE = "settings_title"
    const val BACK = "settings_back"
    const val LEFT_COLUMN = "settings_left_column"
    const val RIGHT_COLUMN = "settings_right_column"
    const val AUDIO_PANEL = "settings_audio_panel"
    const val SOUND_COMING_SOON = "settings_sound_coming_soon"
    const val MUSIC_COMING_SOON = "settings_music_coming_soon"
    const val SOUND_SWITCH = "settings_sound_switch"
    const val SOUND_VOLUME = "settings_sound_volume"
    const val MUSIC_SWITCH = "settings_music_switch"
    const val MUSIC_VOLUME = "settings_music_volume"
    const val FEEL_PANEL = "settings_feel_panel"
    const val VIBRATION_SWITCH = "settings_vibration_switch"
    const val PROGRESS_PANEL = "settings_progress_panel"
    const val PROGRESS_SUMMARY = "settings_progress_summary"
    const val RESET_PROGRESS = "settings_reset_progress"
    const val RESET_DIALOG = "settings_reset_dialog"
    const val RESET_CANCEL = "settings_reset_cancel"
    const val RESET_CONFIRM = "settings_reset_confirm"
    const val SHOW_TIPS_AGAIN = "settings_show_tips_again"
    const val TIPS_RESET_TEXT = "settings_tips_reset_text"
    const val ABOUT_PANEL = "settings_about_panel"
    const val ABOUT_VERSION = "settings_about_version"
    const val MUSIC_CREDITS = "settings_music_credits"
}

/** The tag of the music credit at [index] in playlist order, starting at 0. */
fun musicCreditTag(index: Int) = "settings_music_credit_$index"

/**
 * The settings screen bound to [viewModel]. The back button and system Back leave through [onBack].
 * Releasing the sound volume slider while sound is on plays a preview through [onSoundPreview];
 * dragging the music volume slider sends each value to [onMusicVolumePreview]. Sign in starts the
 * Play Games sign-in through [onSignIn]; Achievements opens the achievements screen through
 * [onOpenAchievements].
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    appInfo: AppInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSoundPreview: (Float) -> Unit = {},
    onMusicVolumePreview: (Float) -> Unit = {},
    onSignIn: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        appInfo = appInfo,
        onBack = onBack,
        onVibrationChange = viewModel::onVibrationChange,
        onResetConfirmed = viewModel::onResetConfirmed,
        onShowTipsAgain = viewModel::onShowTipsAgain,
        modifier = modifier,
        onSoundEnabledChange = viewModel::onSoundEnabledChange,
        onSoundVolumeChange = viewModel::onSoundVolumeChange,
        onSoundPreview = onSoundPreview,
        onMusicEnabledChange = viewModel::onMusicEnabledChange,
        onMusicVolumeChange = viewModel::onMusicVolumeChange,
        onMusicVolumePreview = onMusicVolumePreview,
        onSignIn = onSignIn,
        onOpenAchievements = onOpenAchievements,
    )
}

/**
 * Stateless settings screen apart from whether the reset dialog is open: a top bar with the back
 * button and title, then Audio and Feel on the left and Play Games, Progress and About on the right. Each
 * column scrolls on its own, so small screens and large fonts never push a control out of reach.
 * The panels appear once [state] has been read; until then only the top bar shows.
 */
@Composable
fun SettingsContent(
    state: SettingsUiState?,
    appInfo: AppInfo,
    onBack: () -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onResetConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    onShowTipsAgain: () -> Unit = {},
    initialDialogOpen: Boolean = false,
    onSoundEnabledChange: (Boolean) -> Unit = {},
    onSoundVolumeChange: (Float) -> Unit = {},
    onSoundPreview: (Float) -> Unit = {},
    onMusicEnabledChange: (Boolean) -> Unit = {},
    onMusicVolumeChange: (Float) -> Unit = {},
    onMusicVolumePreview: (Float) -> Unit = {},
    onSignIn: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
) {
    var dialogOpen by rememberSaveable { mutableStateOf(initialDialogOpen) }
    // The dialog is its own window and takes Back first, so this only leaves when it is closed.
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceDeep)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = SettingsMetrics.Gutter)
            .testTag(SettingsTags.SCREEN),
        verticalArrangement = Arrangement.spacedBy(SettingsMetrics.Gutter),
    ) {
        TopBar(onBack)
        if (state == null) return@Column
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(SettingsMetrics.Gutter),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(SettingsTags.LEFT_COLUMN)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SettingsMetrics.Gutter),
            ) {
                AudioPanel(
                    settings = state.settings,
                    onSoundEnabledChange = onSoundEnabledChange,
                    onSoundVolumeChange = onSoundVolumeChange,
                    onSoundPreview = onSoundPreview,
                    onMusicEnabledChange = onMusicEnabledChange,
                    onMusicVolumeChange = onMusicVolumeChange,
                    onMusicVolumePreview = onMusicVolumePreview,
                )
                FeelPanel(vibrationEnabled = state.settings.vibrationEnabled, onVibrationChange = onVibrationChange)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(SettingsTags.RIGHT_COLUMN)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SettingsMetrics.Gutter),
            ) {
                PlayGamesPanel(state = state.playGames, onSignIn = onSignIn, onOpenAchievements = onOpenAchievements)
                ProgressPanel(
                    progress = state.progress,
                    onResetProgress = { dialogOpen = true },
                    tips = state.tips,
                    onShowTipsAgain = onShowTipsAgain,
                )
                AboutPanel(appInfo = appInfo) { MusicCredits(state.musicCredits) }
            }
        }
    }
    if (dialogOpen && state != null) {
        ResetProgressDialog(
            onConfirm = {
                onResetConfirmed()
                dialogOpen = false
            },
            onDismiss = { dialogOpen = false },
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SettingsMetrics.Gutter)) {
        SettingsIconButton(
            icon = R.drawable.ic_back,
            description = stringResource(R.string.settings_back),
            onClick = onBack,
            modifier = Modifier.testTag(SettingsTags.BACK),
        )
        Text(
            text = stringResource(R.string.settings_title).displayUppercase(),
            style = LeakOutTextStyles.DisplayLg,
            color = LeakOutColors.TextPrimary,
            modifier = Modifier.testTag(SettingsTags.TITLE).semantics { heading() },
        )
    }
}

private val PreviewAppInfo = AppInfo(versionName = "0.1.0", versionCode = 1)

private val PreviewProgress = SettingsUiState(
    Settings(musicEnabled = false),
    ProgressSummary(12, 31),
    playGames = PlayGamesUiState(SignInState.SignedOut, unlocked = 9, total = 25),
)

@Preview(name = "Settings: with progress 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun SettingsWithProgressPreview() {
    LeakOutTheme {
        SettingsContent(PreviewProgress, PreviewAppInfo, onBack = {}, onVibrationChange = {}, onResetConfirmed = {})
    }
}

@Preview(name = "Settings: no progress 640x320 (small)", widthDp = 640, heightDp = 320)
@Composable
private fun SettingsNoProgressPreview() {
    LeakOutTheme {
        SettingsContent(
            SettingsUiState(Settings(vibrationEnabled = false), ProgressSummary.None),
            PreviewAppInfo,
            onBack = {},
            onVibrationChange = {},
            onResetConfirmed = {},
        )
    }
}

@Preview(name = "Settings: reset dialog 800x360", widthDp = 800, heightDp = 360)
@Composable
private fun SettingsResetDialogPreview() {
    LeakOutTheme {
        SettingsContent(
            PreviewProgress,
            PreviewAppInfo,
            onBack = {},
            onVibrationChange = {},
            onResetConfirmed = {},
            initialDialogOpen = true,
        )
    }
}
