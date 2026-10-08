package nl.hexmaster.leakout.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.core.level.Side
import nl.hexmaster.leakout.ui.pipes.ArtChannel
import nl.hexmaster.leakout.ui.pipes.PipeTile
import nl.hexmaster.leakout.ui.pipes.rememberPipeArtLibrary
import nl.hexmaster.leakout.ui.theme.LeakOutColors

object PipeLabTags {
    const val START = "lab-start"
    const val PAUSE = "lab-pause"
    const val RESET = "lab-reset"
    const val FLOW_SPEED = "lab-flow-speed"
    const val FLOW_SPEED_VALUE = "lab-flow-speed-value"
    const val RESERVOIR_FACTOR = "lab-reservoir-factor"
    const val RESERVOIR_FACTOR_VALUE = "lab-reservoir-factor-value"

    fun label(section: PipeSection) = "lab-label-${section.name}"
    fun direction(section: PipeSection, channelId: String) = "lab-direction-${section.name}-$channelId"
}

/** The order of the tiles: straights, corners, then the two special sections. */
private val LAB_SECTIONS = listOf(
    PipeSection.H, PipeSection.V, PipeSection.X, PipeSection.RES,
    PipeSection.LT, PipeSection.LB, PipeSection.RT, PipeSection.RB,
)

/** Debug-only preview of every pipe section with a controllable fluid animation. */
@Composable
fun PipeLabScreen(modifier: Modifier = Modifier) {
    val library = rememberPipeArtLibrary()
    val state = remember(library) {
        PipeLabState(PipeSection.entries.associateWith { library.art(it).channels })
    }

    LaunchedEffect(state.running) {
        if (!state.running) return@LaunchedEffect
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) state.tick((now - last) / 1_000_000f)
                last = now
            }
        }
    }

    Row(
        modifier
            .fillMaxSize()
            .background(LeakOutColors.SurfaceDeep)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
            for (row in LAB_SECTIONS.chunked(4)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (section in row) SectionCell(state, section)
                }
            }
        }
        Controls(state, Modifier.width(300.dp).fillMaxHeight())
    }
}

@Composable
private fun SectionCell(state: PipeLabState, section: PipeSection) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            section.name,
            color = LeakOutColors.Steel100,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.testTag(PipeLabTags.label(section)),
        )
        Spacer(Modifier.height(4.dp))
        PipeTile(
            section,
            fills = state.fills(section),
            modifier = Modifier
                .size(80.dp)
                .background(LeakOutColors.SurfaceRaised, RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (channel in state.channels.getValue(section)) {
                val multiple = state.channels.getValue(section).size > 1
                OutlinedButton(
                    onClick = { state.reverse(section, channel.id) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp).testTag(PipeLabTags.direction(section, channel.id)),
                ) {
                    val prefix = if (multiple) (if (channel.isHorizontal()) "H " else "V ") else ""
                    Text(prefix + arrow(channel, state.entry(section, channel.id)), fontSize = 12.sp, color = LeakOutColors.Liquid300)
                }
            }
        }
    }
}

@Composable
private fun Controls(state: PipeLabState, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        Text("Pipe Lab", color = LeakOutColors.Steel100, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(12.dp))

        LabeledSlider(
            label = "Flow speed",
            value = state.flowSpeed,
            valueText = "${format(state.flowSpeed)} cells/s",
            range = PipeLabState.MIN_FLOW_SPEED..PipeLabState.MAX_FLOW_SPEED,
            onChange = { state.flowSpeed = it },
            tag = PipeLabTags.FLOW_SPEED,
            valueTag = PipeLabTags.FLOW_SPEED_VALUE,
        )
        LabeledSlider(
            label = "Reservoir speed",
            value = state.reservoirSpeedFactor,
            valueText = "×${format(state.reservoirSpeedFactor)}",
            range = PipeLabState.MIN_RESERVOIR_FACTOR..PipeLabState.MAX_RESERVOIR_FACTOR,
            onChange = { state.reservoirSpeedFactor = it },
            tag = PipeLabTags.RESERVOIR_FACTOR,
            valueTag = PipeLabTags.RESERVOIR_FACTOR_VALUE,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = state::start,
                enabled = !state.running,
                colors = ButtonDefaults.buttonColors(containerColor = LeakOutColors.Liquid500, contentColor = LeakOutColors.SurfaceDeep),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).testTag(PipeLabTags.START),
            ) { Text("Start") }
            OutlinedButton(
                onClick = state::pause,
                enabled = state.running,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).testTag(PipeLabTags.PAUSE),
            ) {
                Text("Pause", color = LeakOutColors.Steel100)
            }
            OutlinedButton(
                onClick = state::reset,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).testTag(PipeLabTags.RESET),
            ) {
                Text("Reset", color = LeakOutColors.Steel100)
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    tag: String,
    valueTag: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LeakOutColors.Steel100, fontSize = 14.sp)
        Text(valueText, color = LeakOutColors.Liquid300, fontSize = 14.sp, modifier = Modifier.testTag(valueTag))
    }
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = LeakOutColors.LiquidGlow,
            activeTrackColor = LeakOutColors.Liquid500,
            inactiveTrackColor = LeakOutColors.SurfaceRaised,
        ),
        modifier = Modifier.testTag(tag),
    )
}

private fun format(value: Float) = String.format(Locale.US, "%.2f", value)

private fun short(side: Side) = when (side) {
    Side.LEFT -> "L"
    Side.RIGHT -> "R"
    Side.TOP -> "T"
    Side.BOTTOM -> "B"
}

private fun arrow(channel: ArtChannel, entry: Side): String {
    val exit = if (entry == channel.from) channel.to else channel.from
    return "${short(entry)} → ${short(exit)}"
}
