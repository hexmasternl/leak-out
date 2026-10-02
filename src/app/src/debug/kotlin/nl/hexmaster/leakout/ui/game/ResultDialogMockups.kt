package nl.hexmaster.leakout.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import nl.hexmaster.leakout.ui.theme.Fredoka
import nl.hexmaster.leakout.ui.theme.LeakOutColors
import nl.hexmaster.leakout.ui.theme.LeakOutTextStyles
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import nl.hexmaster.leakout.ui.theme.WendyOne

/**
 * Static design mock-ups of the result dialogs for issue #40 (design-system.md 5.6), drawn before
 * the real dialogs are rebuilt. Nothing here is wired to the game; the previews and
 * `ResultDialogMockupScreenshotTest` render them. Replaced by the real dialogs' previews once those
 * are built.
 */
enum class MockupDialog { Win, Leak }

/** The result dialog [dialog] in the Regular or the [compact] layout, over a faux field and the scrim. */
@Composable
fun ResultDialogMockupScreen(dialog: MockupDialog, compact: Boolean, newBest: Boolean = false) {
    Box(Modifier.fillMaxSize().background(LeakOutColors.SurfaceDeep), contentAlignment = Alignment.Center) {
        FauxField()
        Box(Modifier.fillMaxSize().background(Scrim))
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            MockPanel(maxWidth = if (compact) 600.dp else 360.dp) {
                when {
                    compact -> CompactContent(dialog, newBest)
                    else -> RegularContent(dialog, newBest)
                }
            }
        }
    }
}

@Composable
private fun RegularContent(dialog: MockupDialog, newBest: Boolean) {
    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(dialog)
        Spacer(Modifier.height(8.dp))
        if (dialog == MockupDialog.Leak) {
            MockSplash(72.dp)
            Spacer(Modifier.height(12.dp))
        }
        Headline(dialog, DisplayXl)
        Spacer(Modifier.height(8.dp))
        Message(dialog, newBest, LeakOutTextStyles.BodyMd)
        Spacer(Modifier.height(16.dp))
        if (dialog == MockupDialog.Win) {
            MockStars(40.dp)
            Spacer(Modifier.height(16.dp))
        }
        ScoreColumns(dialog)
        Spacer(Modifier.height(24.dp))
        Buttons(dialog, faceHeight = 56.dp)
    }
}

@Composable
private fun CompactContent(dialog: MockupDialog, newBest: Boolean) {
    Row(Modifier.height(IntrinsicSize.Min).padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Eyebrow(dialog)
            Spacer(Modifier.height(4.dp))
            if (dialog == MockupDialog.Leak) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MockSplash(48.dp)
                    Headline(dialog, LeakOutTextStyles.DisplayLg)
                }
                Spacer(Modifier.height(8.dp))
                Message(dialog, newBest = false, style = LeakOutTextStyles.BodySm)
                Spacer(Modifier.height(12.dp))
            } else {
                Headline(dialog, LeakOutTextStyles.DisplayLg)
                if (newBest) {
                    Spacer(Modifier.height(8.dp))
                    Message(dialog, newBest = true, style = LeakOutTextStyles.BodySm)
                }
                Spacer(Modifier.height(8.dp))
                MockStars(32.dp)
                Spacer(Modifier.height(12.dp))
            }
            ScoreColumns(dialog)
        }
        Box(Modifier.padding(horizontal = 24.dp).width(1.dp).fillMaxHeight().background(LeakOutColors.LineSubtle))
        Box(Modifier.width(200.dp)) { Buttons(dialog, faceHeight = 48.dp) }
    }
}

@Composable
private fun Eyebrow(dialog: MockupDialog) {
    Text(
        text = "LEVEL 7",
        style = LeakOutTextStyles.DisplayXs,
        color = if (dialog == MockupDialog.Win) LeakOutColors.Success500 else LeakOutColors.Leak500,
    )
}

@Composable
private fun Headline(dialog: MockupDialog, style: TextStyle) {
    Text(
        text = if (dialog == MockupDialog.Win) "Level Complete!" else "Leaked!",
        style = style,
        color = LeakOutColors.TextPrimary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Message(dialog: MockupDialog, newBest: Boolean, style: TextStyle) {
    when {
        dialog == MockupDialog.Win && newBest ->
            Text("New best!", style = LeakOutTextStyles.DisplaySm, color = LeakOutColors.Brass300)
        dialog == MockupDialog.Win ->
            Text("You kept the water flowing.", style = style, color = LeakOutColors.TextSecondary, textAlign = TextAlign.Center)
        else ->
            Text("The pipe is leaking. Level lost.", style = style, color = LeakOutColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MockStars(size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(STAR_COUNT) { StarIcon(earned = it < 2, scale = { 1f }, size = size) }
    }
}

@Composable
private fun ScoreColumns(dialog: MockupDialog) {
    Row(Modifier.fillMaxWidth()) {
        if (dialog == MockupDialog.Win) {
            ScoreColumn("POINTS", "120", LeakOutColors.TextPrimary, Modifier.weight(1f))
            ScoreColumn("PENALTY", "−12", LeakOutColors.Leak500, Modifier.weight(1f))
            ScoreColumn("SCORE", "108", LeakOutColors.Brass300, Modifier.weight(1f), glow = true)
        } else {
            ScoreColumn("SCORE", "85", LeakOutColors.Brass300, Modifier.weight(1f), glow = true)
        }
    }
}

@Composable
private fun ScoreColumn(caption: String, value: String, color: Color, modifier: Modifier, glow: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(caption, style = LabelMd, color = LeakOutColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        val style = if (glow) NumericLg.copy(shadow = Shadow(LeakOutColors.Brass500.copy(alpha = 0.45f), Offset.Zero, 24f)) else NumericLg
        Text(value, style = style, color = color)
    }
}

@Composable
private fun Buttons(dialog: MockupDialog, faceHeight: Dp) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (dialog == MockupDialog.Win) {
            MockButton("Next Level", MockVariant.Success, faceHeight)
            MockButton("Restart", MockVariant.Glass, faceHeight)
        } else {
            MockButton("Restart", MockVariant.Primary, faceHeight)
        }
        MockButton("Exit", MockVariant.Tertiary, faceHeight)
    }
}

private enum class MockVariant(val top: Color, val bottom: Color, val lip: Color?, val label: Color, val glow: Color?) {
    Success(LeakOutColors.Success500, Color(0xFF1FA971), Color(0xFF1FA971), Color.White, LeakOutColors.Success500),
    Primary(LeakOutColors.Brass300, LeakOutColors.Brass500, LeakOutColors.Brass700, Color(0xFF2A1A03), LeakOutColors.Brass500),
    Glass(LeakOutColors.SurfaceOverlay, LeakOutColors.SurfaceOverlay, LeakOutColors.SurfaceAbyss, LeakOutColors.TextPrimary, null),
    Tertiary(Color.Transparent, Color.Transparent, null, LeakOutColors.Liquid300, null),
}

/** A face with a 4 dp lip under it (design-system.md 4.4); the tertiary variant is a 48 dp text button. */
@Composable
private fun MockButton(label: String, variant: MockVariant, faceHeight: Dp) {
    val shape = RoundedCornerShape(16.dp)
    val lip = variant.lip
    if (lip == null) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            Text(label, style = LeakOutTextStyles.DisplaySm, color = variant.label)
        }
        return
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(faceHeight + 4.dp)
            .then(if (variant.glow != null) Modifier.shadow(16.dp, shape, ambientColor = variant.glow, spotColor = variant.glow) else Modifier)
            .background(lip, shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(faceHeight)
                .background(Brush.verticalGradient(listOf(variant.top, variant.bottom)), shape)
                .then(
                    if (variant == MockVariant.Glass) {
                        Modifier.drawWithContent {
                            drawContent()
                            drawRoundRect(LeakOutColors.LineStrong, cornerRadius = CornerRadius(16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = LeakOutTextStyles.DisplaySm, color = variant.label)
        }
    }
}

/** Level 3 floating panel: `radius.lg`, `surface.base`, a 1 dp `line.highlight` top edge, no outline. */
@Composable
private fun MockPanel(maxWidth: Dp, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .widthIn(max = maxWidth)
            .shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
            .background(LeakOutColors.SurfaceBase, shape)
            .drawWithContent {
                drawContent()
                val inset = 24.dp.toPx()
                drawLine(LeakOutColors.LineHighlight, Offset(inset, 0.5f), Offset(size.width - inset, 0.5f), 1.dp.toPx())
            },
    ) { content() }
}

/** The splash at rest: a leak blob with six droplets around it (design-system.md 7.2). */
@Composable
private fun MockSplash(size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(LeakOutColors.Leak500, r * 0.42f)
        drawCircle(Color.White.copy(alpha = 0.35f), r * 0.12f, center + Offset(-r * 0.14f, -r * 0.14f))
        repeat(6) { i ->
            val angle = Math.toRadians(30.0 + 60.0 * i)
            val c = center + Offset((cos(angle) * r * 0.8f).toFloat(), (sin(angle) * r * 0.8f).toFloat())
            drawCircle(if (i % 2 == 0) LeakOutColors.Liquid500 else LeakOutColors.LiquidGlow, r * 0.14f, c)
        }
    }
}

/** Rows of dry pipe-ish blocks so the see-through scrim reads as a scrim over the field. */
@Composable
private fun FauxField() {
    Canvas(Modifier.fillMaxSize()) {
        val cell = 56.dp.toPx()
        val gap = 4.dp.toPx()
        var y = gap
        var row = 0
        while (y < size.height) {
            var x = gap
            var col = 0
            while (x < size.width) {
                val color = if ((row + col) % 5 == 0) LeakOutColors.Liquid500 else LeakOutColors.SurfaceOverlay
                drawRoundRect(color, Offset(x, y), Size(cell, cell), CornerRadius(10.dp.toPx()))
                x += cell + gap
                col++
            }
            y += cell + gap
            row++
        }
    }
}

private val Scrim = Color(0xB807111C)

// Styles from design-system.md 3.1 not in LeakOutTextStyles yet. Varela Round is not bundled, so
// label.md uses Fredoka like the other body styles.
private val DisplayXl = TextStyle(fontFamily = WendyOne, fontSize = 40.sp, lineHeight = 1.05.em, letterSpacing = 0.25.sp)
private val NumericLg = TextStyle(fontFamily = WendyOne, fontSize = 32.sp, lineHeight = 1.0.em)
private val LabelMd = TextStyle(fontFamily = Fredoka, fontSize = 13.sp, lineHeight = 1.3.em, letterSpacing = 0.4.sp)

@Preview(name = "Win, Compact, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun WinCompactPreview() = LeakOutTheme { ResultDialogMockupScreen(MockupDialog.Win, compact = true) }

@Preview(name = "Win, Compact, 640 × 360, 130 % font", widthDp = 640, heightDp = 360, fontScale = 1.3f)
@Composable
private fun WinCompactLargeFontPreview() = LeakOutTheme { ResultDialogMockupScreen(MockupDialog.Win, compact = true, newBest = true) }

@Preview(name = "Win, Regular, 1280 × 800", widthDp = 1280, heightDp = 800)
@Composable
private fun WinRegularPreview() = LeakOutTheme { ResultDialogMockupScreen(MockupDialog.Win, compact = false) }

@Preview(name = "Leak, Compact, 640 × 360", widthDp = 640, heightDp = 360)
@Composable
private fun LeakCompactPreview() = LeakOutTheme { ResultDialogMockupScreen(MockupDialog.Leak, compact = true) }

@Preview(name = "Leak, Regular, 1280 × 800", widthDp = 1280, heightDp = 800)
@Composable
private fun LeakRegularPreview() = LeakOutTheme { ResultDialogMockupScreen(MockupDialog.Leak, compact = false) }
