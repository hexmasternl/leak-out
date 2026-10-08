package nl.hexmaster.leakout.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import nl.hexmaster.leakout.R

/**
 * Fredoka ships as one variable font (OFL, see assets/licenses/fredoka-OFL.txt). Each weight is a
 * named instance of the same file so `FontWeight.Bold` selects the real 700 axis value instead of
 * synthetic emboldening.
 */
@OptIn(ExperimentalTextApi::class)
val Fredoka = FontFamily(
    Font(
        R.font.fredoka,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.fredoka,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.fredoka,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

/** Wendy One (OFL, see assets/licenses/wendy-one-OFL.txt): the Deep Flow display face, one weight. */
val WendyOne = FontFamily(Font(R.font.wendy_one_regular, weight = FontWeight.Normal))

/** Text styles from `docs/design-system-slim.md` section 3. Text transform is applied by callers. */
object LeakOutTextStyles {
    /** Deep Flow `display.hero` (docs/design-system.md 3.2): 56 sp, line height 1.0, 0.5 sp tracking. */
    val DisplayHero = TextStyle(
        fontFamily = WendyOne,
        fontWeight = FontWeight.Normal,
        fontSize = 56.sp,
        lineHeight = 1.0.em,
        letterSpacing = 0.5.sp,
    )

    /** Deep Flow `display.xl` (design-system 3.1): the win / lose headline. */
    val DisplayXl = TextStyle(
        fontFamily = WendyOne,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 1.05.em,
        letterSpacing = 0.25.sp,
    )

    /** Deep Flow `numeric.lg`: the score in the result dialogs. */
    val NumericLg = TextStyle(fontFamily = WendyOne, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 1.0.em)

    /** Deep Flow `display.lg` (design-system 3.1): dialog and screen titles. */
    val DisplayLg = TextStyle(fontFamily = WendyOne, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 1.1.em)

    /** Deep Flow `display.md`: section headers. */
    val DisplayMd = TextStyle(fontFamily = WendyOne, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 1.15.em)

    /** Deep Flow `display.sm`: button labels and HUD values. */
    val DisplaySm = TextStyle(
        fontFamily = WendyOne,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 1.2.em,
        letterSpacing = 0.25.sp,
    )

    /** Deep Flow `numeric.md`: HUD counters such as the score. */
    val NumericMd = TextStyle(fontFamily = WendyOne, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 1.0.em)

    /** Deep Flow `display.xs`: uppercase eyebrow labels, such as panel titles. */
    val DisplayXs = TextStyle(
        fontFamily = WendyOne,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 1.2.em,
        letterSpacing = 0.5.sp,
    )

    // Deep Flow body styles. Varela Round is not bundled yet, so they use Fredoka, the UI face in use.

    /** Deep Flow `body.lg`: dialog body. */
    val BodyLg = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 1.5.em)

    /** Deep Flow `body.md`: default body, settings row labels. */
    val BodyMd = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 1.5.em)

    /** Deep Flow `label.md`: uppercase captions such as the score row labels (callers uppercase). */
    val LabelMd = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 1.3.em, letterSpacing = 0.4.sp)

    /** Deep Flow `body.sm`: secondary copy. */
    val BodySm = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 1.45.em)

    /** Title / Hero: 36-48 sp, bold, uppercase, line height 1.1. The welcome title scales it up. */
    val TitleHero = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 1.1.em,
        letterSpacing = 0.02.em,
    )

    /** Header 1 (modals): 28 sp, bold, uppercase. */
    val Header1 = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 1.2.em,
    )

    /** Header 2 (HUD): 20 sp, semibold. */
    val Header2 = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 1.25.em,
    )

    /** Button text: 18 sp, bold, uppercase, line height 1.0. */
    val ButtonText = TextStyle(
        fontFamily = Fredoka,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 1.0.em,
        letterSpacing = 0.04.em,
    )
}

internal val LeakOutTypography = Typography(
    displayLarge = LeakOutTextStyles.TitleHero,
    headlineMedium = LeakOutTextStyles.Header1,
    titleLarge = LeakOutTextStyles.Header2,
    labelLarge = LeakOutTextStyles.ButtonText,
)
