package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import nl.hexmaster.leakout.ui.theme.LeakOutColors

/** `radius.lg` on the bottom corners only; the top of the HUD runs into the screen edge. */
private val HudPanelShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)

/**
 * The HUD panel of design system 5.3: depth level 1 (shadow `0 4 12` black 35 % and a 1 dp
 * `line.highlight` top edge) over a `surface.raised → surface.deep` gradient, with a 1 dp
 * `line.subtle` bottom edge so it reads as its own surface over the game screen.
 */
fun Modifier.hudPanel(): Modifier = this
    .shadow(elevation = 6.dp, shape = HudPanelShape, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.35f))
    .clip(HudPanelShape)
    .background(Brush.verticalGradient(listOf(LeakOutColors.SurfaceRaised, LeakOutColors.SurfaceDeep)))
    .drawWithContent {
        drawContent()
        val line = 1.dp.toPx()
        drawLine(LeakOutColors.LineHighlight, Offset(0f, line / 2), Offset(size.width, line / 2), line)
        drawLine(LeakOutColors.LineSubtle, Offset(0f, size.height - line / 2), Offset(size.width, size.height - line / 2), line)
    }
