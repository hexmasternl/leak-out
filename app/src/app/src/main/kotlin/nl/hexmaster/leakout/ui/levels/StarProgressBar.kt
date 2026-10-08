package nl.hexmaster.leakout.ui.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.ui.game.StarIcon
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Stable handles for the star progress bar in UI tests. */
object StarProgressTags {
    const val BAR = "level_library_star_progress"
    const val LABEL = "level_library_star_progress_label"
    const val TRACK = "level_library_star_progress_track"
}

/**
 * The stars earned on a page towards the threshold that opens [nextPage]: a star, the
 * "earned / threshold" label and a pill track filled to [progress]. [reached] switches the fill and
 * label to the success colour. Sized to sit in the library's hint row.
 */
@Composable
fun StarProgressBar(
    earned: Int,
    threshold: Int,
    progress: Float,
    reached: Boolean,
    nextPage: Int,
    modifier: Modifier = Modifier,
) {
    val description = pluralStringResource(
        R.plurals.level_library_star_progress_description,
        threshold,
        earned,
        threshold,
        nextPage,
    )
    val state = if (reached) stringResource(R.string.level_library_star_progress_reached) else ""
    val textSize = with(LocalDensity.current) { LABEL_TEXT_DP.dp.toSp() }
    val fraction = progress.coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .widthIn(max = MAX_WIDTH_DP.dp)
            .fillMaxWidth()
            .testTag(StarProgressTags.BAR)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = state
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StarIcon(earned = true, scale = { 1f }, size = STAR_DP.dp, glowRadius = (STAR_DP * 0.7f).dp)
        Text(
            text = stringResource(R.string.level_library_star_progress_label, earned, threshold),
            style = LeakOutTextStyles.Header2.copy(fontSize = textSize),
            color = if (reached) LeakOutColors.Success500 else LeakOutColors.Brass300,
            maxLines = 1,
            modifier = Modifier.testTag(StarProgressTags.LABEL),
        )
        StarProgressTrack(
            progress = fraction,
            reached = reached,
            modifier = Modifier.weight(1f).height(TRACK_HEIGHT_DP.dp).testTag(StarProgressTags.TRACK),
        )
    }
}

/**
 * The pill track and its fill, drawn in one node. The fill is clipped by the track's rounded shape so
 * a small fraction still reads as part of the pill. Paths and brushes are rebuilt only on resize or
 * a style change, not per frame.
 */
@Composable
private fun StarProgressTrack(progress: Float, reached: Boolean, modifier: Modifier) {
    Box(
        modifier = modifier.drawWithCache {
            val radius = CornerRadius(size.height / 2f)
            val track = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, radius)) }
            val fill = if (reached) {
                SolidColor(LeakOutColors.Success500)
            } else {
                Brush.verticalGradient(listOf(LeakOutColors.Liquid300, LeakOutColors.Liquid500, LeakOutColors.Liquid700))
            }
            val border = Stroke(width = 1.dp.toPx())
            onDrawBehind {
                drawPath(track, LeakOutColors.SurfaceAbyss)
                clipPath(track) {
                    drawRect(fill, size = Size(size.width * progress, size.height))
                }
                drawPath(track, LeakOutColors.Steel700, style = border)
            }
        },
    )
}

private const val LABEL_TEXT_DP = 15f
private const val STAR_DP = 18f
private const val TRACK_HEIGHT_DP = 10f
private const val MAX_WIDTH_DP = 360f

@Preview(name = "Star progress: 34 of 50", widthDp = 400, heightDp = 28, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun StarProgressBelowPreview() {
    LeakOutTheme { StarProgressBar(earned = 34, threshold = 50, progress = 0.68f, reached = false, nextPage = 2) }
}

@Preview(name = "Star progress: reached", widthDp = 400, heightDp = 28, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun StarProgressReachedPreview() {
    LeakOutTheme { StarProgressBar(earned = 55, threshold = 50, progress = 1f, reached = true, nextPage = 2) }
}
