package nl.hexmaster.leakout.ui.levels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.hexmaster.leakout.R
import nl.hexmaster.leakout.core.library.LevelTile
import nl.hexmaster.leakout.core.library.TileState
import nl.hexmaster.leakout.ui.game.STAR_COUNT
import nl.hexmaster.leakout.ui.game.StarIcon
import nl.hexmaster.leakout.ui.splash.rememberReducedMotion
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** Colours of one tile state (docs/design-system.md 5.5). */
private data class TileColors(val face: Color, val wash: Color, val lip: Color, val number: Color, val alpha: Float)

private fun colorsFor(state: TileState): TileColors = when (state) {
    TileState.Locked -> TileColors(
        LeakOutColors.SurfaceBase, Color.Transparent, LeakOutColors.SurfaceAbyss, LeakOutColors.TextMuted, LOCKED_ALPHA,
    )
    TileState.Unlocked -> TileColors(
        LeakOutColors.SurfaceRaised, Color.Transparent, LeakOutColors.SurfaceAbyss, LeakOutColors.SurfaceCard, 1f,
    )
    is TileState.Completed -> TileColors(
        LeakOutColors.SurfaceRaised, LeakOutColors.SuccessTint, LeakOutColors.SurfaceAbyss, LeakOutColors.Success500, 1f,
    )
}

/** The tile's accessibility label, also what UI tests find it by. */
@Composable
fun levelTileDescription(tile: LevelTile): String = when (val state = tile.state) {
    TileState.Locked -> stringResource(R.string.level_tile_locked, tile.levelId)
    TileState.Unlocked -> stringResource(R.string.level_tile_unlocked, tile.levelId)
    is TileState.Completed -> stringResource(R.string.level_tile_completed, tile.levelId, state.stars)
}

fun levelTileTag(levelId: Int): String = "level_tile_$levelId"

/**
 * One level tile in the 3-layer cartoon recipe: a dark lip below, the face with a shine band and
 * an ink outline. Locked tiles are dimmed with a lock; unlocked show empty stars; completed show
 * their earned stars in brass on a success wash.
 *
 * @param lockedFeedback changes each time a tap on this locked tile must be acknowledged: the tile
 *   shakes and flashes `leak.tint` (no shake with reduced motion).
 */
@Composable
fun LevelTileButton(
    tile: LevelTile,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    lockedFeedback: Int? = null,
) {
    val colors = colorsFor(tile.state)
    val description = levelTileDescription(tile)
    val reducedMotion = rememberReducedMotion()
    val shake = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(lockedFeedback) {
        if (lockedFeedback == null) return@LaunchedEffect
        launch {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(durationMillis = LOCKED_FEEDBACK_MS))
        }
        if (!reducedMotion) {
            shake.animateTo(0f, keyframes {
                durationMillis = LOCKED_FEEDBACK_MS
                SHAKE_DP at 30
                -SHAKE_DP at 90
                SHAKE_DP at 150
                -SHAKE_DP at 210
            })
        }
    }
    val density = LocalDensity.current
    // Fixed to the tile size, not the font scale, so the number always fits the tile.
    val numberSize = with(density) { (height * NUMBER_RATIO).toSp() }
    val iconSize = (height * ICON_RATIO).coerceAtLeast(8.dp)

    Box(
        modifier = modifier
            .size(width, height)
            .testTag(levelTileTag(tile.levelId))
            .graphicsLayer { translationX = shake.value.dp.toPx() }
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick)
            .drawWithCache {
                val border = TILE_BORDER.toPx()
                val lip = TILE_LIP.toPx()
                val radius = TILE_RADIUS.toPx()
                val faceSize = Size(size.width, size.height - lip)
                val half = border / 2f
                val facePath = Path().apply {
                    addRoundRect(
                        RoundRect(half, half, faceSize.width - half, faceSize.height - half, CornerRadius(radius - half)),
                    )
                }
                val corner = CornerRadius(radius)
                onDrawBehind {
                    drawRoundRect(colors.lip, Offset(0f, lip), faceSize, corner, alpha = colors.alpha)
                    drawPath(facePath, colors.face, alpha = colors.alpha)
                    drawPath(facePath, colors.wash)
                    drawPath(facePath, LeakOutColors.LeakTint, alpha = flash.value)
                    clipPath(facePath) {
                        drawRect(LeakOutColors.Shine, Offset(0f, border), Size(faceSize.width, TILE_SHINE.toPx()), alpha = 0.5f * colors.alpha)
                    }
                    drawPath(facePath, LeakOutColors.InkDark, style = Stroke(border))
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(height - TILE_LIP)
                .padding(vertical = 2.dp)
                .clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = tile.levelId.toString(),
                style = LeakOutTextStyles.Header2.copy(fontSize = numberSize, lineHeight = numberSize),
                color = colors.number,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            when (val state = tile.state) {
                TileState.Locked -> Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = LeakOutColors.TextMuted,
                    modifier = Modifier.size(iconSize),
                )
                TileState.Unlocked -> TileStars(earned = 0, size = iconSize)
                is TileState.Completed -> TileStars(earned = state.stars, size = iconSize)
            }
        }
    }
}

@Composable
private fun TileStars(earned: Int, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(size * 0.15f)) {
        repeat(STAR_COUNT) { index ->
            StarIcon(earned = index < earned, scale = { 1f }, size = size, glowRadius = size * 0.7f)
        }
    }
}

private val TILE_BORDER = 2.dp
private val TILE_LIP = 4.dp
private val TILE_SHINE = 3.dp
private val TILE_RADIUS = 14.dp
private const val NUMBER_RATIO = 0.36f
private const val ICON_RATIO = 0.2f
private const val LOCKED_ALPHA = 0.6f
private const val SHAKE_DP = 3f
const val LOCKED_FEEDBACK_MS = 240

@Preview(name = "Level tiles", widthDp = 420, heightDp = 100, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun LevelTilePreview() {
    LeakOutTheme {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                LevelTile(1, TileState.Completed(3)),
                LevelTile(2, TileState.Completed(1)),
                LevelTile(3, TileState.Unlocked),
                LevelTile(4, TileState.Locked),
            ).forEach { LevelTileButton(it, onClick = {}, width = 88.dp, height = 72.dp) }
        }
    }
}
