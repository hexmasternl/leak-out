package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object HudTags {
    const val HUD = "hud"
    const val LEVEL = "hud_level"
    const val SPEED = "hud_speed"
    const val COUNTDOWN_SLOT = "hud_countdown_slot"
    const val COUNTDOWN = "hud_countdown"
    const val SCORE = "hud_score"
    const val PAUSE = "hud_pause"
}

/** Width the countdown bar keeps at least (design system 5.3). */
private val CountdownMinWidth = 96.dp

/**
 * The HUD panel across the top of the game screen (design system 5.3): from left to right the level,
 * the flow speed, the countdown bar, the score bar and the cog wheel. [countdownRemainingMs] is the
 * countdown in flow time while the flow waits to start and null once it flows; the countdown keeps
 * its slot, so nothing moves when it disappears. [stars] are the level's thresholds and [attempt]
 * empties the score bar at once when it changes. The cog wheel stays visible but does nothing while
 * [pauseEnabled] is false, for example once the level is lost.
 *
 * [gapDistance] and [clockMs] are read lambdas, the gap distance in sections (null without a gap) and
 * the game time, so their per-frame changes only reach the score bar's warning. [onWarningShown] is
 * called each time the blinking bar switches to "Warning", from the same value that shows the text.
 */
@Composable
fun GameHud(
    levelId: Int,
    flowSpeed: Double,
    countdownRemainingMs: Long?,
    initialDelayMs: Long,
    stars: StarBenchmarks,
    score: Int,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
    attempt: Int = 0,
    pauseEnabled: Boolean = true,
    reducedMotion: Boolean = rememberReducedMotion(),
    gapDistance: () -> Double? = { null },
    clockMs: () -> Long = { 0L },
    onWarningShown: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val levelText = stringResource(R.string.hud_level, HudFormat.level(levelId))
    val speed = HudFormat.speed(flowSpeed, locale)
    val speedDescription = stringResource(R.string.hud_speed_description, speed)

    Row(
        modifier = modifier
            .testTag(HudTags.HUD)
            .fillMaxWidth()
            .height(HudLayout.HEIGHT)
            .hudPanel()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = levelText,
            style = LeakOutTextStyles.DisplaySm,
            color = LeakOutColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier
                .testTag(HudTags.LEVEL)
                .clearAndSetSemantics { contentDescription = levelText },
        )
        Row(
            modifier = Modifier
                .testTag(HudTags.SPEED)
                .clearAndSetSemantics { contentDescription = speedDescription },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_speed),
                contentDescription = null,
                tint = LeakOutColors.Liquid300,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = speed,
                style = LeakOutTextStyles.DisplaySm,
                color = LeakOutColors.Liquid300,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = CountdownMinWidth)
                .height(CountdownBarHeight)
                .testTag(HudTags.COUNTDOWN_SLOT),
            contentAlignment = Alignment.Center,
        ) {
            CountdownBar(countdownRemainingMs, initialDelayMs, reducedMotion = reducedMotion)
        }
        GapWarningScoreBar(score, stars, attempt, reducedMotion, gapDistance, clockMs, onWarningShown)
        // The 48 dp touch target is wider than the 24 dp cog; the 4 dp end padding puts the cog's
        // edge 16 dp from the panel's edge, like the level on the left.
        IconButton(
            onClick = onPause,
            enabled = pauseEnabled,
            modifier = Modifier
                .size(48.dp)
                .testTag(HudTags.PAUSE),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cog),
                contentDescription = stringResource(R.string.hud_pause_menu),
                tint = if (pauseEnabled) LeakOutColors.TextPrimary else LeakOutColors.TextMuted,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * The score bar with the gap warning (spec `game-play-gap-warning`). Each value is derived, so the
 * bar recomposes when the warning changes, not on every frame the distance does. The blink is timed
 * on the game clock from the moment it starts, so it stands still while paused; a new [attempt]
 * forgets it.
 */
@Composable
private fun GapWarningScoreBar(
    score: Int,
    stars: StarBenchmarks,
    attempt: Int,
    reducedMotion: Boolean,
    gapDistance: () -> Double?,
    clockMs: () -> Long,
    onWarningShown: () -> Unit,
) {
    val readDistance by rememberUpdatedState(gapDistance)
    // Measured once per frame; the warning values below are derived from it.
    val distance = remember { derivedStateOf { readDistance() } }
    val clock by rememberUpdatedState(clockMs)
    var blinkSince by remember(attempt) { mutableStateOf<Long?>(null) }
    val blinking by remember { derivedStateOf { GapWarning.blinking(distance.value) } }
    val warning by remember { derivedStateOf { GapWarning.redness(distance.value) } }
    val leakIn by remember { derivedStateOf { distance.value?.takeIf(GapWarning::blinking)?.let(GapWarning::sectionsToLeak) } }
    val warningText by remember { derivedStateOf { GapWarning.showWarningText(distance.value, clock(), blinkSince) } }
    LaunchedEffect(blinking, attempt) { blinkSince = if (blinking) clock() else null }
    // Every switch to "Warning" is reported once; switching back to the score is not.
    val currentOnWarningShown by rememberUpdatedState(onWarningShown)
    LaunchedEffect(Unit) {
        var shown = false
        snapshotFlow { warningText }.collect { now ->
            if (now && !shown) currentOnWarningShown()
            shown = now
        }
    }
    ScoreBar(
        score = score,
        stars = stars,
        attempt = attempt,
        reducedMotion = reducedMotion,
        warning = warning,
        warningText = warningText,
        leakInSections = leakIn,
    )
}

private val PreviewStars = StarBenchmarks(100, 200, 350)

/** The review states of issue #38: waiting with a countdown, mid-score, and past three stars. */
@Composable
internal fun GameHudPreviewStates(modifier: Modifier = Modifier) {
    Column(
        modifier.background(LeakOutColors.SurfaceDeep).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        for (remaining in listOf(7_000L, 4_000L, 1_500L)) {
            GameHud(7, 0.8, remaining, 10_000, PreviewStars, 0, onPause = {}, reducedMotion = true)
        }
        GameHud(7, 0.9, null, 10_000, PreviewStars, 175, onPause = {}, reducedMotion = true)
        GameHud(7, 1.25, null, 10_000, PreviewStars, 1_250, onPause = {}, reducedMotion = true)
    }
}

@Preview(name = "HUD", widthDp = 800, heightDp = 360)
@Composable
private fun GameHudPreview() {
    LeakOutTheme { GameHudPreviewStates() }
}

@Preview(name = "HUD, narrow", widthDp = 640, heightDp = 360)
@Composable
private fun GameHudNarrowPreview() {
    LeakOutTheme { GameHudPreviewStates() }
}
