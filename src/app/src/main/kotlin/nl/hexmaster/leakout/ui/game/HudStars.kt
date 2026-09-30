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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    StarIcon(
        earned = earned,
        scale = { scale.value },
        size = HudLayout.STAR_SIZE,
        glowRadius = HudLayout.STAR_GLOW_RADIUS,
        modifier = Modifier.testTag("hud_star_${index + 1}"),
    )
}

/**
 * One star, shared by the HUD and the level complete dialog. Not earned: [LeakOutColors.TextMuted]
 * without glow. Earned: [LeakOutColors.Brass300] with a static brass glow of [glowRadius]. It is drawn
 * at [scale], read while drawing, and carries the [StarEarned] and [StarScale] test semantics.
 */
@Composable
fun StarIcon(
    earned: Boolean,
    scale: () -> Float,
    size: Dp,
    modifier: Modifier = Modifier,
    glowRadius: Dp = size * 0.9f,
) {
    val glow = LeakOutColors.Brass500.copy(alpha = 0.45f)
    Icon(
        painter = painterResource(R.drawable.ic_star),
        contentDescription = null,
        tint = if (earned) LeakOutColors.Brass300 else LeakOutColors.TextMuted,
        modifier = modifier
            .semantics {
                starEarned = earned
                starScale = scale()
            }
            .graphicsLayer {
                scaleX = scale()
                scaleY = scale()
            }
            .drawBehind {
                if (earned) {
                    val radius = glowRadius.toPx()
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
            .size(size),
    )
}

/**
 * The three stars of the level complete dialog, 40 dp and 12 dp apart. The first [lit] are earned;
 * each is drawn at its scale from [scales], which the win tally pops. Read as "N of 3 stars".
 */
@Composable
fun WinStars(lit: Int, scales: List<Float>, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.hud_stars_description, lit.coerceIn(0, STAR_COUNT))
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(WIN_STAR_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(STAR_COUNT) { index ->
            StarIcon(
                earned = index < lit,
                scale = { scales.getOrElse(index) { 1f } },
                size = WIN_STAR_SIZE,
                modifier = Modifier.testTag("win_star_${index + 1}"),
            )
        }
    }
}

private val WIN_STAR_SIZE = 40.dp
private val WIN_STAR_SPACING = 12.dp
