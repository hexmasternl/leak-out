package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.core.level.PipeSection
import nl.hexmaster.leakout.ui.pipes.PipeTile
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/**
 * A dry section on a rounded card, as the stack and the dragged copy show it: the section's pipe
 * art and glass overlay from [PipeTile]. The playing field draws the same art with `drawPipe`.
 */
@Composable
fun PipeSectionGlyph(section: PipeSection, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(LeakOutColors.SurfaceCard.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
    ) {
        PipeTile(section, Modifier.fillMaxSize())
    }
}
