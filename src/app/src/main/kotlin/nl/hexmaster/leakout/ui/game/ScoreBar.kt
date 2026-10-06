package nl.hexmaster.leakout.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.level.StarBenchmarks
import nl.hexmaster.leakout.ui.components.displayUppercase
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** The score bar's fixed size (design system 5.3). It never changes with the score. */
val ScoreBarWidth = 200.dp
val ScoreBarHeight = 20.dp
private val MarkerWidth = 2.dp

/** A sparkle being drawn on the score bar: at [fraction] along the bar, [frame] of its animation. */
data class ScoreBarSparkle(val fraction: Float, val frame: SparkleFrame)

/**
 * The score bar as drawn: a `surface.abyss` pill track with a `line.subtle` border, a
 * `brass.500 → brass.300` fill from the left to [fraction] (0..1), `line.strong` marker lines at
 * [markers] and [scoreText] centred over it with a `surface.abyss` shadow. [sparkles] are drawn on
 * top and may spill outside the bar. [warning] (0..1) turns both fill colours towards `leak.500`,
 * so at 1 the fill is solid red.
 */
@Composable
fun ScoreBarTrack(
    fraction: Float,
    markers: List<Float>,
    scoreText: String,
    modifier: Modifier = Modifier,
    sparkles: List<ScoreBarSparkle> = emptyList(),
    warning: Float = 0f,
) {
    // 1 dp `surface.abyss` text shadow, offset down 1 dp and blurred 2 dp, so the number reads over brass and track.
    val shadow = with(LocalDensity.current) { Shadow(LeakOutColors.SurfaceAbyss, Offset(0f, 1.dp.toPx()), 2.dp.toPx()) }
    Box(modifier.size(ScoreBarWidth, ScoreBarHeight), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = CornerRadius(size.height / 2)
            val pill = Path().apply { addRoundRect(RoundRect(size.toRect(), radius)) }
            drawRoundRect(LeakOutColors.SurfaceAbyss, cornerRadius = radius)
            clipPath(pill) {
                val width = size.width * fraction.coerceIn(0f, 1f)
                if (width > 0f) {
                    val red = warning.coerceIn(0f, 1f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(lerp(LeakOutColors.Brass500, LeakOutColors.Leak500, red), lerp(LeakOutColors.Brass300, LeakOutColors.Leak500, red)),
                            endX = width,
                        ),
                        size = Size(width, size.height),
                    )
                }
                val marker = MarkerWidth.toPx()
                for (m in markers) {
                    val x = size.width * m
                    drawRect(LeakOutColors.LineStrong, topLeft = Offset(x - marker / 2, 0f), size = Size(marker, size.height))
                }
            }
            val border = 1.dp.toPx()
            drawRoundRect(
                LeakOutColors.LineSubtle,
                topLeft = Offset(border / 2, border / 2),
                size = Size(size.width - border, size.height - border),
                cornerRadius = CornerRadius(size.height / 2 - border / 2),
                style = Stroke(border),
            )
        }
        Text(
            text = scoreText,
            style = LeakOutTextStyles.NumericMd.copy(shadow = shadow),
            color = LeakOutColors.TextPrimary,
            maxLines = 1,
            softWrap = false,
        )
        if (sparkles.isNotEmpty()) {
            Canvas(Modifier.fillMaxSize()) {
                for (sparkle in sparkles) drawSparkle(Offset(size.width * sparkle.fraction, size.height / 2), sparkle.frame)
            }
        }
    }
}

private fun DrawScope.drawSparkle(center: Offset, frame: SparkleFrame) {
    if (frame.flashAlpha > 0f) {
        val r = HudSparkle.FLASH_RADIUS_DP.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                listOf(LeakOutColors.Brass300.copy(alpha = 0.8f * frame.flashAlpha), Color.Transparent),
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
    }
    val dot = HudSparkle.PARTICLE_RADIUS_DP.dp.toPx()
    for (p in frame.particles) {
        val color = if (p.glow) LeakOutColors.LiquidGlow else LeakOutColors.Brass300
        drawCircle(color.copy(alpha = p.alpha), radius = dot, center = center + Offset(p.dx.dp.toPx(), p.dy.dp.toPx()))
    }
}

/** Test-only semantics: how full the score bar's fill is right now, 0..1. */
val ScoreFill = SemanticsPropertyKey<Float>("ScoreFill")
var SemanticsPropertyReceiver.scoreFill by ScoreFill

/** Test-only semantics: how far the score bar's fill is turned towards red by the gap warning, 0..1. */
val ScoreWarning = SemanticsPropertyKey<Float>("ScoreWarning")
var SemanticsPropertyReceiver.scoreWarning by ScoreWarning

/** Test-only semantics: whether the score bar shows "WARNING" instead of the score right now. */
val ScoreWarningText = SemanticsPropertyKey<Boolean>("ScoreWarningText")
var SemanticsPropertyReceiver.scoreWarningText by ScoreWarningText

/** Test-only semantics: the thresholds that have sparkled this attempt, in order. */
val SparkledThresholds = SemanticsPropertyKey<List<HudModel.Threshold>>("SparkledThresholds")
var SemanticsPropertyReceiver.sparkledThresholds by SparkledThresholds

/** The score bar's fill animation: 600 ms ease-out, 160 ms under reduced motion (design system 7.2). */
const val SCORE_FILL_MS = 600
const val SCORE_FILL_REDUCED_MS = 160

/**
 * The score bar of the HUD: [score] over the level's `stars.three`, markers at `stars.one` and
 * `stars.two`, and the actual score centred on it. The fill moves smoothly to a new score and is
 * empty at once on a new [attempt]. Each threshold sparkles once per attempt, as the moving fill
 * reaches it; under [reducedMotion] nothing sparkles. Read by a screen reader as one item:
 * "Score 150, 1 of 3 stars".
 *
 * The gap warning (spec `game-play-gap-warning`): [warning] (0..1) turns the fill red, and while
 * [warningText] is true the bar shows "WARNING" instead of the score. While [leakInSections] is set
 * (the bar blinks) the description adds the warning, so it does not change with every blink.
 */
@Composable
fun ScoreBar(
    score: Int,
    stars: StarBenchmarks,
    attempt: Int,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
    warning: Float = 0f,
    warningText: Boolean = false,
    leakInSections: Int? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scoreText = HudFormat.score(score, locale)
    val scoreDescription = stringResource(R.string.hud_score_description, scoreText, stars.starsFor(score))
    val description = if (leakInSections == null) {
        scoreDescription
    } else {
        pluralStringResource(R.plurals.game_hud_score_warning, leakInSections, scoreDescription, leakInSections)
    }
    val shownText = if (warningText) stringResource(R.string.game_hud_warning).displayUppercase(locale) else scoreText
    val target = HudModel.scoreFraction(score, stars)
    // Keyed on the attempt: a new attempt starts with an empty bar without animating.
    val fill = remember(attempt, stars) { Animatable(target) }
    val sparkled = remember(attempt, stars) { mutableStateListOf<HudModel.Threshold>() }
    val ages = remember(attempt, stars) { mutableStateMapOf<HudModel.Threshold, Long>() }

    LaunchedEffect(fill, target) {
        val duration = if (reducedMotion) SCORE_FILL_REDUCED_MS else SCORE_FILL_MS
        fill.animateTo(target, tween(duration, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(fill, reducedMotion) {
        val scope = this
        var previous = fill.value
        snapshotFlow { fill.value }.collect { value ->
            if (!reducedMotion) {
                for (threshold in HudModel.thresholdsPassed(previous, value, stars)) {
                    if (threshold in sparkled) continue
                    sparkled += threshold
                    ages[threshold] = 0L
                    scope.launch {
                        val start = withFrameMillis { it }
                        while (true) {
                            val age = withFrameMillis { it } - start
                            if (age >= HudSparkle.DURATION_MS) break
                            ages[threshold] = age
                        }
                        ages.remove(threshold)
                    }
                }
            }
            previous = value
        }
    }

    val fraction = fill.value
    val sparkles = ages.mapNotNull { (threshold, age) ->
        sparkleFrame(age)?.let { ScoreBarSparkle(HudModel.position(threshold, stars), it) }
    }
    ScoreBarTrack(
        fraction = fraction,
        markers = HudModel.markerFractions(stars),
        scoreText = shownText,
        sparkles = sparkles,
        warning = warning,
        modifier = modifier
            .testTag(HudTags.SCORE)
            .clearAndSetSemantics {
                contentDescription = description
                scoreFill = fraction
                sparkledThresholds = sparkled.toList()
                scoreWarning = warning
                scoreWarningText = warningText
            },
    )
}

/** The review states of the gap warning: halfway to red, and fully red while showing "WARNING". */
@Composable
internal fun ScoreBarWarningPreviewStates(modifier: Modifier = Modifier) {
    val stars = StarBenchmarks(100, 200, 350)
    Column(
        modifier.background(LeakOutColors.SurfaceDeep).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScoreBar(175, stars, attempt = 1, reducedMotion = true)
        ScoreBar(175, stars, attempt = 1, reducedMotion = true, warning = 0.5f)
        ScoreBar(175, stars, attempt = 1, reducedMotion = true, warning = 1f, leakInSections = 3)
        ScoreBar(175, stars, attempt = 1, reducedMotion = true, warning = 1f, warningText = true, leakInSections = 3)
    }
}

@Preview(name = "Score bar warning", widthDp = 240, heightDp = 200)
@Composable
private fun ScoreBarWarningPreview() {
    LeakOutTheme { ScoreBarWarningPreviewStates() }
}

@Preview(name = "Score bar warning, landscape", widthDp = 640, heightDp = 200)
@Composable
private fun ScoreBarWarningLandscapePreview() {
    LeakOutTheme { ScoreBarWarningPreviewStates() }
}
