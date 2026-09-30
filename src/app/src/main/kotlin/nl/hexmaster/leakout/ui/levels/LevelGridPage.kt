package nl.hexmaster.leakout.ui.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.core.library.LevelLibraryConfig
import nl.hexmaster.leakout.core.library.LevelLibraryLayout
import nl.hexmaster.leakout.core.library.LevelLibraryRules
import nl.hexmaster.leakout.core.library.LibraryPage
import nl.hexmaster.leakout.ui.theme.LeakOutTheme

/** A locked tile to acknowledge; [nonce] changes on every tap so a repeat tap replays the feedback. */
data class LockedFeedback(val levelId: Int, val nonce: Int)

fun levelPageTag(number: Int): String = "level_page_$number"

const val LEVEL_EMPTY_CELL_TAG = "level_empty_cell"

/** One page: 6 columns by 4 rows, sized to fit; cells past the last level stay empty. */
@Composable
fun LevelGridPage(
    page: LibraryPage,
    onLevelTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lockedFeedback: LockedFeedback? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag(levelPageTag(page.number)), contentAlignment = Alignment.Center) {
        val metrics = remember(maxWidth, maxHeight) { LevelGridLayout.compute(maxWidth.value, maxHeight.value) }
        val width = metrics.tileWidthDp.dp
        val height = metrics.tileHeightDp.dp
        Column(verticalArrangement = Arrangement.spacedBy(metrics.gapDp.dp)) {
            repeat(LevelLibraryLayout.ROWS) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(metrics.gapDp.dp)) {
                    repeat(LevelLibraryLayout.COLUMNS) { column ->
                        val tile = page.tiles.getOrNull(row * LevelLibraryLayout.COLUMNS + column)
                        if (tile == null) {
                            Spacer(Modifier.size(width, height).testTag(LEVEL_EMPTY_CELL_TAG))
                        } else {
                            LevelTileButton(
                                tile = tile,
                                onClick = { onLevelTap(tile.levelId) },
                                width = width,
                                height = height,
                                lockedFeedback = lockedFeedback?.takeIf { it.levelId == tile.levelId }?.nonce,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Partial last page", widthDp = 632, heightDp = 264, backgroundColor = 0xFF0C1A2A, showBackground = true)
@Composable
private fun LevelGridPagePreview() {
    val library = LevelLibraryRules.compute(30, (1..26).associateWith { it % 3 + 1 }, LevelLibraryConfig.Default)
    LeakOutTheme {
        LevelGridPage(page = library.page(2)!!, onLevelTap = {})
    }
}
