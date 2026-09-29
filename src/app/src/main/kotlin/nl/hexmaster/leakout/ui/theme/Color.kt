package nl.hexmaster.leakout.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens from `docs/design-system-slim.md` section 2. Components use these names, never raw
 * hex values.
 */
object LeakOutColors {
    // 2.1 Primary: Hydro Blue
    val PrimaryDark = Color(0xFF013C60)
    val PrimaryBase = Color(0xFF0277BD)
    val PrimaryMid = Color(0xFF0288D1)
    val PrimaryLight = Color(0xFF03A9F4)
    val PrimarySoft = Color(0xFFB2EBF2)
    val PrimaryBg = Color(0xFFE0F7FA)

    // 2.2 Secondary: Brass Yellow
    val SecondaryDark = Color(0xFFF57F17)
    val SecondaryBase = Color(0xFFFBC02D)
    val SecondaryLight = Color(0xFFFDD835)
    val SecondarySoft = Color(0xFFFFF59D)
    val SecondaryBg = Color(0xFFFFFDE7)

    // 2.3 Structural and neutral
    val InkDark = Color(0xFF1E293B)
    val InkLight = Color(0xFF475569)
    val SteelBase = Color(0xFF546E7A)
    val SteelDark = Color(0xFF37474F)
    val SteelLight = Color(0xFF90A4AE)
    val SurfaceCard = Color(0xFFFFFFFF)

    // 2.4 Functional and status
    val ReservoirPurple = Color(0xFF8E24AA)
    val SuccessGreen = Color(0xFF43A047)
    val ErrorRed = Color(0xFFE53935)

    // Not in the token table: the danger button needs a darker shade of ErrorRed for its bottom
    // shadow, the same way the primary and secondary families have a `-dark` token.
    val ErrorRedDark = Color(0xFFB71C1C)

    // 4.1 inner high shine: rgba(255, 255, 255, 0.4)
    val Shine = Color(0x66FFFFFF)

    // 4.2 disabled state
    val DisabledFill = Color(0xFF94A3B8)
    val DisabledShadow = Color(0xFF64748B)

    // The welcome art is a dark navy scene; this matches its base tone for the window background.
    val SceneNavy = Color(0xFF0C1A2A)
}
