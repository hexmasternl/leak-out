package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.achievements.AchievementResources
import nl.hexmaster.leakout.core.achievements.Achievement
import nl.hexmaster.leakout.ui.components.DialogEntry
import nl.hexmaster.leakout.ui.components.displayUppercase
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** The texts of the "Achievement unlocked" row, as plain functions so they are JVM-testable. */
object AchievementRowText {
    /** How many names the row shows before "+N more". */
    const val MAX_NAMES = 2

    /** "Zero Waste · Measure Twice", then [more] with the number left out ("+2 more") when there are more. */
    fun names(names: List<String>, more: (Int) -> String): String {
        val shown = names.take(MAX_NAMES).joinToString(" · ")
        val hidden = names.size - MAX_NAMES
        return if (hidden > 0) "$shown ${more(hidden)}" else shown
    }

    /** Every name for a screen reader, not only the shown ones. */
    fun spokenNames(names: List<String>): String = names.joinToString(", ")
}

object AchievementRowTags {
    const val ROW = "achievement_unlocked_row"
}

/** Test-only semantics: the row's current pop scale, 1 once it has settled. */
val AchievementRowScale = SemanticsPropertyKey<Float>("AchievementRowScale")
private var SemanticsPropertyReceiver.achievementRowScale by AchievementRowScale

/** The pop of the row: from this scale to 1 with `motion.pop`, as a dialog panel enters. */
private const val ROW_INITIAL_SCALE = 0.6f

/**
 * The "Achievement unlocked" row of the result dialogs (design.md decision 9): a trophy, the label
 * in `display.xs` `brass.300` and at most two of the [achievements]' names, then "+N more". It
 * pops in with `motion.pop` when it first appears (still with reduced motion, or when [animate] is
 * off for a copy that is only measured). A screen reader reads it as one item: "Achievement
 * unlocked" and every name. Nothing is drawn without achievements.
 */
@Composable
fun AchievementUnlockedRow(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    if (achievements.isEmpty()) return
    val names = achievements.map { stringResource(AchievementResources.name(it)) }
    val more = (names.size - AchievementRowText.MAX_NAMES).coerceAtLeast(0)
    val moreText = pluralStringResource(R.plurals.achievement_unlocked_more, more, more)
    val shown = AchievementRowText.names(names) { moreText }
    val spoken = stringResource(R.string.achievement_unlocked_description, AchievementRowText.spokenNames(names))
    val pops = animate && !reducedMotion
    val scale = remember { Animatable(if (pops) ROW_INITIAL_SCALE else 1f) }
    LaunchedEffect(pops) { if (pops) scale.animateTo(1f, DialogEntry.PopSpring) else scale.snapTo(1f) }
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                achievementRowScale = scale.value
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                // Fades in with the pop; never below 0 when the spring overshoots back.
                alpha = ((scale.value - ROW_INITIAL_SCALE) / (1f - ROW_INITIAL_SCALE)).coerceIn(0f, 1f)
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_trophy),
            contentDescription = null,
            tint = LeakOutColors.Brass300,
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(
                text = stringResource(R.string.achievement_unlocked).displayUppercase(),
                style = LeakOutTextStyles.DisplayXs,
                color = LeakOutColors.Brass300,
            )
            Text(text = shown, style = LeakOutTextStyles.BodySm, color = LeakOutColors.TextPrimary, textAlign = TextAlign.Start)
        }
    }
}

@Preview(name = "Achievement row, one", widthDp = 320, showBackground = true, backgroundColor = 0xFF122538)
@Composable
private fun AchievementRowOnePreview() {
    LeakOutTheme { AchievementUnlockedRow(listOf(Achievement.ZERO_WASTE), Modifier.padding(16.dp), animate = false) }
}

@Preview(name = "Achievement row, four", widthDp = 320, showBackground = true, backgroundColor = 0xFF122538)
@Composable
private fun AchievementRowFourPreview() {
    LeakOutTheme {
        AchievementUnlockedRow(
            listOf(Achievement.ZERO_WASTE, Achievement.MEASURE_TWICE, Achievement.STEADY_HANDS, Achievement.PERSONAL_BEST),
            Modifier.padding(16.dp),
            animate = false,
        )
    }
}
