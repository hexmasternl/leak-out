package nl.hexmaster.leakout.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Sizes from the design system (5.8 settings row, 10 touch targets, 4.2 shape) as plain numbers. */
object SettingsMetrics {
    val RowHeight: Dp = 56.dp
    val SliderRowHeight: Dp = 48.dp
    val IconButtonSize: Dp = 48.dp
    val IconSize: Dp = 24.dp
    val PanelRadius: Dp = 24.dp
    val PanelPadding: Dp = 16.dp
    val Gutter: Dp = 16.dp

    /** The design system's disabled treatment for controls that cannot be used yet. */
    const val DISABLED_ALPHA = 0.4f
}

/**
 * A level 1 panel (design system 4.3) with an uppercase [title] and an optional [note] beside it,
 * such as "Coming soon". The panel takes the [titleTag] so tests can find it by its heading.
 */
@Composable
fun SettingsPanel(
    title: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    noteTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(SettingsMetrics.PanelRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LeakOutColors.SurfaceBase, shape)
            .border(1.dp, LeakOutColors.LineSubtle, shape)
            .padding(SettingsMetrics.PanelPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title.uppercase(), style = LeakOutTextStyles.DisplayXs, color = LeakOutColors.TextSecondary)
            if (note != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = note,
                    style = LeakOutTextStyles.BodySm,
                    color = LeakOutColors.TextMuted,
                    modifier = if (noteTag != null) Modifier.testTag(noteTag) else Modifier,
                )
            }
        }
        content()
    }
}

/**
 * A 56 dp settings row (design system 5.8): leading icon, `body.md` label and a trailing pill
 * switch. The whole row is the toggle, so the label is part of the touch target and TalkBack reads
 * label and state together. A disabled row is drawn at 40 % and reports itself as disabled. An
 * optional [note], such as "Coming soon", sits before the switch and carries [noteTag] for tests.
 */
@Composable
fun SettingsSwitchRow(
    label: String,
    @DrawableRes icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    note: String? = null,
    noteTag: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SettingsMetrics.RowHeight)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .alpha(if (enabled) 1f else SettingsMetrics.DISABLED_ALPHA),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = LeakOutColors.TextPrimary,
            modifier = Modifier.size(SettingsMetrics.IconSize),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = LeakOutTextStyles.BodyMd, color = LeakOutColors.TextPrimary, modifier = Modifier.weight(1f))
        if (note != null) {
            Text(
                text = note,
                style = LeakOutTextStyles.BodySm,
                color = LeakOutColors.TextMuted,
                modifier = (if (noteTag != null) Modifier.testTag(noteTag) else Modifier).padding(end = 12.dp),
            )
        }
        // No handler of its own: the row toggles, so the switch is only the picture of the state.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = switchColors())
    }
}

/**
 * A 48 dp row holding a volume slider under its switch, indented to the switch row's label. The
 * slider is labelled by [description] for TalkBack; disabled, it is drawn at 40 %.
 * [onValueChangeFinished] is called when a drag or tap on the slider ends.
 */
@Composable
fun SettingsSliderRow(
    value: Float,
    onValueChange: (Float) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SettingsMetrics.SliderRowHeight)
            .padding(start = SettingsMetrics.IconSize + 12.dp)
            .alpha(if (enabled) 1f else SettingsMetrics.DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            colors = sliderColors(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
        )
    }
}

/** A 48 dp round glass icon button (design system 5.1). */
@Composable
fun SettingsIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(SettingsMetrics.IconButtonSize)
            .background(LeakOutColors.SurfaceOverlay, CircleShape)
            .border(1.dp, LeakOutColors.LineStrong, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = LeakOutColors.TextPrimary,
            modifier = Modifier.size(SettingsMetrics.IconSize),
        )
    }
}

// Disabled colours match the enabled ones: the row's 40 % alpha is the one disabled treatment.
@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = LeakOutColors.LiquidGlow,
    checkedTrackColor = LeakOutColors.Liquid500,
    checkedBorderColor = LeakOutColors.Liquid500,
    uncheckedThumbColor = LeakOutColors.Steel300,
    uncheckedTrackColor = LeakOutColors.SurfaceOverlay,
    uncheckedBorderColor = LeakOutColors.LineStrong,
    disabledCheckedThumbColor = LeakOutColors.LiquidGlow,
    disabledCheckedTrackColor = LeakOutColors.Liquid500,
    disabledCheckedBorderColor = LeakOutColors.Liquid500,
    disabledUncheckedThumbColor = LeakOutColors.Steel300,
    disabledUncheckedTrackColor = LeakOutColors.SurfaceOverlay,
    disabledUncheckedBorderColor = LeakOutColors.LineStrong,
)

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = LeakOutColors.LiquidGlow,
    activeTrackColor = LeakOutColors.Liquid500,
    inactiveTrackColor = LeakOutColors.SurfaceOverlay,
    disabledThumbColor = LeakOutColors.LiquidGlow,
    disabledActiveTrackColor = LeakOutColors.Liquid500,
    disabledInactiveTrackColor = LeakOutColors.SurfaceOverlay,
)

@Preview(name = "Settings rows", widthDp = 360, heightDp = 260, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun SettingsRowsPreview() {
    LeakOutTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsPanel(title = "Rows", note = "Coming soon") {
                SettingsSwitchRow("Enabled on", R.drawable.ic_sound, checked = true, onCheckedChange = {})
                SettingsSliderRow(value = 0.8f, onValueChange = {}, description = "Volume")
                SettingsSwitchRow("Disabled off", R.drawable.ic_music, false, {}, enabled = false)
            }
        }
    }
}
