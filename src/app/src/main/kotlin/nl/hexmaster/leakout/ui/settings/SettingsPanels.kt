package nl.hexmaster.leakout.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.audio.Track
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.ui.components.CartoonButton
import nl.hexmaster.leakout.ui.components.CartoonButtonVariant
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles

/** Whether the sound effects controls can be used: the game plays its effects. */
const val SOUND_EFFECTS_AVAILABLE = true

/** Whether the music controls can be used: the background music follows them. */
const val MUSIC_AVAILABLE = true

/**
 * Sound effects and music, each a switch with a volume slider, showing the stored [settings].
 * A group that is not available is disabled and its switch row notes "Coming soon". A slider
 * follows the finger while dragged and reports the volume once, on release; releasing the sound
 * slider while sound is on also calls [onSoundPreview], so the player hears the new level. The
 * music slider sends every value to [onMusicVolumePreview] while dragged, so the music follows it.
 */
@Composable
fun AudioPanel(
    settings: Settings,
    modifier: Modifier = Modifier,
    soundEffectsAvailable: Boolean = SOUND_EFFECTS_AVAILABLE,
    musicAvailable: Boolean = MUSIC_AVAILABLE,
    onSoundEnabledChange: (Boolean) -> Unit = {},
    onSoundVolumeChange: (Float) -> Unit = {},
    onSoundPreview: (Float) -> Unit = {},
    onMusicEnabledChange: (Boolean) -> Unit = {},
    onMusicVolumeChange: (Float) -> Unit = {},
    onMusicVolumePreview: (Float) -> Unit = {},
) {
    val comingSoon = stringResource(R.string.settings_coming_soon)
    SettingsPanel(title = stringResource(R.string.settings_audio), modifier = modifier.testTag(SettingsTags.AUDIO_PANEL)) {
        SettingsSwitchRow(
            label = stringResource(R.string.settings_sound_effects),
            icon = R.drawable.ic_sound,
            checked = settings.soundEnabled,
            onCheckedChange = onSoundEnabledChange,
            enabled = soundEffectsAvailable,
            note = if (soundEffectsAvailable) null else comingSoon,
            noteTag = SettingsTags.SOUND_COMING_SOON,
            modifier = Modifier.testTag(SettingsTags.SOUND_SWITCH),
        )
        VolumeSliderRow(
            stored = settings.soundVolume,
            onVolumeChange = { volume ->
                onSoundVolumeChange(volume)
                if (settings.soundEnabled) onSoundPreview(volume)
            },
            description = stringResource(R.string.settings_sound_volume),
            enabled = soundEffectsAvailable,
            modifier = Modifier.testTag(SettingsTags.SOUND_VOLUME),
        )
        SettingsSwitchRow(
            label = stringResource(R.string.settings_music),
            icon = R.drawable.ic_music,
            checked = settings.musicEnabled,
            onCheckedChange = onMusicEnabledChange,
            enabled = musicAvailable,
            note = if (musicAvailable) null else comingSoon,
            noteTag = SettingsTags.MUSIC_COMING_SOON,
            modifier = Modifier.testTag(SettingsTags.MUSIC_SWITCH),
        )
        VolumeSliderRow(
            stored = settings.musicVolume,
            onVolumeChange = onMusicVolumeChange,
            onDrag = onMusicVolumePreview,
            description = stringResource(R.string.settings_music_volume),
            enabled = musicAvailable,
            modifier = Modifier.testTag(SettingsTags.MUSIC_VOLUME),
        )
    }
}

/**
 * A volume slider that shows the [stored] value, follows the finger while dragged and reports the
 * new volume once on release; [onDrag] hears every value on the way. The dragged value stays until
 * the stored value catches up, so the thumb does not jump back while the write is on its way.
 */
@Composable
private fun VolumeSliderRow(
    stored: Float,
    onVolumeChange: (Float) -> Unit,
    description: String,
    onDrag: (Float) -> Unit = {},
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var dragged by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(stored) { dragged = null }
    SettingsSliderRow(
        value = dragged ?: stored,
        onValueChange = {
            dragged = it
            onDrag(it)
        },
        onValueChangeFinished = { dragged?.let(onVolumeChange) },
        description = description,
        enabled = enabled,
        modifier = modifier,
    )
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

/**
 * The "Music" section of the About panel: every background track as its title and artist, in
 * playlist order. Titles and artists are proper names, so they are not translated. Nothing is
 * shown without [credits].
 */
@Composable
fun MusicCredits(credits: List<Track>, modifier: Modifier = Modifier) {
    if (credits.isEmpty()) return
    Column(modifier.testTag(SettingsTags.MUSIC_CREDITS)) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_about_music).uppercase(),
            style = LeakOutTextStyles.DisplayXs,
            color = LeakOutColors.TextSecondary,
        )
        credits.forEachIndexed { index, track ->
            Text(
                text = buildAnnotatedString {
                    append(track.title)
                    withStyle(SpanStyle(color = LeakOutColors.TextMuted)) { append(" · ${track.artist}") }
                },
                style = LeakOutTextStyles.BodyMd,
                color = LeakOutColors.TextPrimary,
                modifier = Modifier.testTag(musicCreditTag(index)),
            )
        }
    }
}
