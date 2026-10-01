package nl.hexmaster.leakout.ui.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.ui.components.CartoonButton
import nl.hexmaster.leakout.ui.components.CartoonButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/**
 * Whether the audio controls can be used. There is no audio engine yet, so the panel is complete
 * but disabled; enabling it is this flag plus wiring the callbacks of [AudioPanel].
 */
const val AUDIO_AVAILABLE = false

/**
 * Sound effects and music, each a switch with a volume slider, showing the stored [settings].
 * While [enabled] is false every control is disabled and the panel notes "Coming soon".
 */
@Composable
fun AudioPanel(
    settings: Settings,
    modifier: Modifier = Modifier,
    enabled: Boolean = AUDIO_AVAILABLE,
    onSoundEnabledChange: (Boolean) -> Unit = {},
    onSoundVolumeChange: (Float) -> Unit = {},
    onMusicEnabledChange: (Boolean) -> Unit = {},
    onMusicVolumeChange: (Float) -> Unit = {},
) {
    SettingsPanel(
        title = stringResource(R.string.settings_audio),
        note = if (enabled) null else stringResource(R.string.settings_coming_soon),
        noteTag = SettingsTags.COMING_SOON,
        modifier = modifier.testTag(SettingsTags.AUDIO_PANEL),
    ) {
        SettingsSwitchRow(
            label = stringResource(R.string.settings_sound_effects),
            icon = R.drawable.ic_sound,
            checked = settings.soundEnabled,
            onCheckedChange = onSoundEnabledChange,
            enabled = enabled,
            modifier = Modifier.testTag(SettingsTags.SOUND_SWITCH),
        )
        SettingsSliderRow(
            value = settings.soundVolume,
            onValueChange = onSoundVolumeChange,
            description = stringResource(R.string.settings_sound_volume),
            enabled = enabled,
            modifier = Modifier.testTag(SettingsTags.SOUND_VOLUME),
        )
        SettingsSwitchRow(
            label = stringResource(R.string.settings_music),
            icon = R.drawable.ic_music,
            checked = settings.musicEnabled,
            onCheckedChange = onMusicEnabledChange,
            enabled = enabled,
            modifier = Modifier.testTag(SettingsTags.MUSIC_SWITCH),
        )
        SettingsSliderRow(
            value = settings.musicVolume,
            onValueChange = onMusicVolumeChange,
            description = stringResource(R.string.settings_music_volume),
            enabled = enabled,
            modifier = Modifier.testTag(SettingsTags.MUSIC_VOLUME),
        )
    }
}

/** The Vibration switch. It only stores the preference; nothing vibrates yet. */
@Composable
fun FeelPanel(vibrationEnabled: Boolean, onVibrationChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SettingsPanel(title = stringResource(R.string.settings_feel), modifier = modifier.testTag(SettingsTags.FEEL_PANEL)) {
        SettingsSwitchRow(
            label = stringResource(R.string.settings_vibration),
            icon = R.drawable.ic_vibration,
            checked = vibrationEnabled,
            onCheckedChange = onVibrationChange,
            modifier = Modifier.testTag(SettingsTags.VIBRATION_SWITCH),
        )
    }
}

/**
 * The [progress] summary and the destructive Reset progress button, disabled while there is
 * nothing to reset so the confirm dialog cannot open for nothing.
 */
@Composable
fun ProgressPanel(progress: ProgressSummary, onResetProgress: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPanel(title = stringResource(R.string.settings_progress), modifier = modifier.testTag(SettingsTags.PROGRESS_PANEL)) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (progress.hasProgress) {
                pluralStringResource(
                    R.plurals.settings_progress_summary,
                    progress.levelsFinished,
                    progress.levelsFinished,
                    progress.starsEarned,
                )
            } else {
                stringResource(R.string.settings_progress_none)
            },
            style = LeakOutTextStyles.BodyMd,
            color = LeakOutColors.TextPrimary,
            modifier = Modifier.testTag(SettingsTags.PROGRESS_SUMMARY),
        )
        Spacer(Modifier.height(12.dp))
        CartoonButton(
            text = stringResource(R.string.settings_reset_progress),
            onClick = onResetProgress,
            variant = CartoonButtonVariant.Danger,
            enabled = progress.hasProgress,
            faceHeight = 48.dp,
            modifier = Modifier.fillMaxWidth().testTag(SettingsTags.RESET_PROGRESS),
        )
    }
}

/**
 * The app name, the installed version, the maker and the privacy line. [extra] is appended below
 * them, so a later change can add a section (such as music credits) without touching these lines.
 */
@Composable
fun AboutPanel(
    appInfo: AppInfo,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    SettingsPanel(title = stringResource(R.string.settings_about), modifier = modifier.testTag(SettingsTags.ABOUT_PANEL)) {
        Spacer(Modifier.height(12.dp))
        Text(text = stringResource(R.string.app_name), style = LeakOutTextStyles.DisplayMd, color = LeakOutColors.TextPrimary)
        Text(
            text = stringResource(R.string.settings_about_version, appInfo.versionName, appInfo.versionCode),
            style = LeakOutTextStyles.BodyMd,
            color = LeakOutColors.TextSecondary,
            modifier = Modifier.testTag(SettingsTags.ABOUT_VERSION),
        )
        Text(text = stringResource(R.string.settings_about_maker), style = LeakOutTextStyles.BodyMd, color = LeakOutColors.TextSecondary)
        Text(
            text = stringResource(R.string.settings_about_privacy),
            style = LeakOutTextStyles.BodySm,
            color = LeakOutColors.TextMuted,
        )
        extra()
    }
}
