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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/**
 * A static mock-up of the HUD proposed in design system 5.3, for review on issue #38. It draws the
 * real panel, countdown bar and score bar with fixed values and no state or animation.
 */
@Composable
fun HudMockup(
    level: Int,
    speed: Int,
    countdownFraction: Float?,
    countdownColor: Color,
    scoreFraction: Float,
    scoreText: String,
    modifier: Modifier = Modifier,
    sparkles: List<ScoreBarSparkle> = emptyList(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HudLayout.HEIGHT)
            .hudPanel()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Level $level", style = LeakOutTextStyles.DisplaySm, color = LeakOutColors.TextPrimary, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(painterResource(R.drawable.ic_speed), null, tint = LeakOutColors.Liquid300, modifier = Modifier.size(20.dp))
            Text("$speed", style = LeakOutTextStyles.DisplaySm, color = LeakOutColors.Liquid300, maxLines = 1)
        }
        Box(Modifier.weight(1f).widthIn(min = 96.dp)) {
            CountdownBarTrack(
                fraction = countdownFraction ?: 0f,
                color = countdownColor,
                modifier = Modifier.alpha(if (countdownFraction == null) 0f else 1f),
            )
        }
        ScoreBarTrack(fraction = scoreFraction, markers = listOf(2f / 7, 4f / 7), scoreText = scoreText, sparkles = sparkles)
        Icon(painterResource(R.drawable.ic_cog), null, tint = LeakOutColors.TextPrimary, modifier = Modifier.size(24.dp))
    }
}

/** The three review states of issue #38, over the game screen background. */
@Composable
fun HudMockupStates(modifier: Modifier = Modifier) {
    Column(
        modifier.background(LeakOutColors.SurfaceDeep).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Waiting: 7 s of 10 s left, nothing scored yet.
        HudMockup(level = 7, speed = 8, countdownFraction = 0.7f, countdownColor = LeakOutColors.Liquid500, scoreFraction = 0f, scoreText = "0")
        // Last seconds: brass warning, then red.
        HudMockup(level = 7, speed = 8, countdownFraction = 0.4f, countdownColor = LeakOutColors.Brass500, scoreFraction = 0f, scoreText = "0")
        HudMockup(level = 7, speed = 8, countdownFraction = 0.15f, countdownColor = LeakOutColors.Leak500, scoreFraction = 0f, scoreText = "0")
        // Flowing, mid-score: 175 of 350, past the first marker, sparkling at the one-star marker.
        HudMockup(
            level = 7, speed = 9, countdownFraction = null, countdownColor = LeakOutColors.Liquid500,
            scoreFraction = 0.5f, scoreText = "175",
            sparkles = listOf(ScoreBarSparkle(2f / 7, sparkleFrame(120)!!)),
        )
        // Past three stars: full bar, the actual score.
        HudMockup(level = 7, speed = 13, countdownFraction = null, countdownColor = LeakOutColors.Liquid500, scoreFraction = 1f, scoreText = "1,250")
    }
}

@Preview(name = "HUD mock-up (issue #38)", widthDp = 800, heightDp = 360)
@Composable
private fun HudMockupPreview() {
    LeakOutTheme { HudMockupStates() }
}

@Preview(name = "HUD mock-up, narrow", widthDp = 640, heightDp = 360)
@Composable
private fun HudMockupNarrowPreview() {
    LeakOutTheme { HudMockupStates() }
}
