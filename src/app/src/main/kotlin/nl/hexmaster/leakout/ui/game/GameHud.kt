package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

object HudTags {
    const val HUD = "hud"
    const val LEVEL = "hud_level"
    const val SPEED = "hud_speed"
    const val STARS = "hud_stars"
    const val SCORE = "hud_score"
    const val PAUSE = "hud_pause"
}

/**
 * The HUD bar across the top of the game screen: level and flow speed on the left, then an empty
 * gap, then the stars, the right-aligned score and the cog wheel at the right edge. The cog wheel
 * stays visible but does nothing while [pauseEnabled] is false, for example once the level is lost.
 */
@Composable
fun GameHud(
    levelId: Int,
    flowSpeed: Double,
    stars: Int,
    score: Int,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
    pauseEnabled: Boolean = true,
) {
    val locale = LocalConfiguration.current.locales[0]
    val levelText = stringResource(R.string.hud_level, HudFormat.level(levelId))
    val speed = HudFormat.speed(flowSpeed, locale)
    val speedDescription = stringResource(R.string.hud_speed_description, speed)
    val scoreText = HudFormat.score(score, locale)
    val scoreDescription = stringResource(R.string.hud_score_description, scoreText)

    Row(
        modifier = modifier
            .testTag(HudTags.HUD)
            .fillMaxWidth()
            .height(HudLayout.HEIGHT)
            .background(LeakOutColors.SurfaceDeep)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = levelText,
            style = LeakOutTextStyles.Header2,
            color = LeakOutColors.SurfaceCard,
            maxLines = 1,
            modifier = Modifier
                .testTag(HudTags.LEVEL)
                .clearAndSetSemantics { contentDescription = levelText },
        )
        Row(
            modifier = Modifier
                .padding(start = 16.dp)
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
                text = stringResource(R.string.hud_speed_unit, speed),
                style = LeakOutTextStyles.Header2,
                color = LeakOutColors.Liquid300,
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        HudStars(earned = stars, modifier = Modifier.testTag(HudTags.STARS))
        Text(
            text = scoreText,
            style = LeakOutTextStyles.Header2,
            color = LeakOutColors.SurfaceCard,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .padding(start = 12.dp)
                .testTag(HudTags.SCORE)
                .clearAndSetSemantics { contentDescription = scoreDescription },
        )
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
                tint = if (pauseEnabled) LeakOutColors.SurfaceCard else LeakOutColors.TextMuted,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview(name = "HUD", widthDp = 800, heightDp = 48)
@Composable
private fun GameHudPreview() {
    LeakOutTheme {
        GameHud(levelId = 3, flowSpeed = 0.8, stars = 1, score = 1250, onPause = {})
    }
}
