package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** Test-only semantics: whether a HUD star is earned. TalkBack does not read custom keys. */
val StarEarned = SemanticsPropertyKey<Boolean>("StarEarned")
var SemanticsPropertyReceiver.starEarned by StarEarned

/** Test-only semantics: the current pop scale of a HUD star. */
val StarScale = SemanticsPropertyKey<Float>("StarScale")
var SemanticsPropertyReceiver.starScale by StarScale

const val STAR_COUNT = 3

/**
 * Three stars, earned from left to right. Not earned: [LeakOutColors.TextMuted] without glow.
 * Earned: [LeakOutColors.Brass300] with a static brass glow, popping in with `motion.pop`.
 */
@Composable
fun HudStars(earned: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.hud_stars_description, earned.coerceIn(0, STAR_COUNT))
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(HudLayout.STAR_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(STAR_COUNT) { index -> HudStar(earned = index < earned, index = index) }
    }
}

@Composable
private fun HudStar(earned: Boolean, index: Int) {
    val reducedMotion = rememberReducedMotion()
    val scale = remember { Animatable(1f) }
    // Remembers the earned state of the previous composition so only a change pops, not the start.
    val previous = remember { booleanArrayOf(earned) }
    LaunchedEffect(earned) {
        val becameEarned = earned && !previous[0]
        previous[0] = earned
        if (becameEarned && !reducedMotion) {
            scale.snapTo(HudLayout.STAR_POP_SCALE)
            scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        } else {
            scale.snapTo(1f)
        }
    }
    val glow = LeakOutColors.Brass500.copy(alpha = 0.45f)
    Icon(
        painter = painterResource(R.drawable.ic_star),
        contentDescription = null,
        tint = if (earned) LeakOutColors.Brass300 else LeakOutColors.TextMuted,
        modifier = Modifier
            .testTag("hud_star_${index + 1}")
            .semantics {
                starEarned = earned
                starScale = scale.value
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .drawBehind {
                if (earned) {
                    val radius = HudLayout.STAR_GLOW_RADIUS.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glow, Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                    )
                }
            }
            .size(HudLayout.STAR_SIZE),
    )
}
